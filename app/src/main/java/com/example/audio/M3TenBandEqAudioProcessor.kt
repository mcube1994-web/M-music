package com.example.audio

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Real-time studio-grade 10-band peaking biquad EQ AudioProcessor attached inside Media3's
 * DefaultAudioSink. Replaces hardware DynamicsProcessing (which can mute or conflict with
 * vendor Dolby/Spatializer HALs) while delivering clean, zero-clipping 31Hz-16kHz shaping.
 */
@OptIn(UnstableApi::class)
class M3TenBandEqAudioProcessor : BaseAudioProcessor() {

    private val centerFrequenciesHz = doubleArrayOf(
        31.25, 62.5, 125.0, 250.0, 500.0,
        1000.0, 2000.0, 4000.0, 8000.0, 16000.0
    )

    private class BiquadCoeffs(
        val b0: Double,
        val b1: Double,
        val b2: Double,
        val a1: Double,
        val a2: Double
    )

    private class EqSnapshot(
        val enabled: Boolean,
        val isFlat: Boolean,
        val preampScale: Double,
        val activeBands: Array<BiquadCoeffs>,
        val activeBandIndices: IntArray
    )

    @Volatile
    private var requestedEnabled: Boolean = true

    @Volatile
    private var requestedGainsDb: FloatArray = FloatArray(10)

    @Volatile
    private var snapshot: EqSnapshot = EqSnapshot(
        enabled = true,
        isFlat = true,
        preampScale = 1.0,
        activeBands = emptyArray(),
        activeBandIndices = IntArray(0)
    )

    // Filter state: [channel][band][z1, z2]
    private var z1 = Array(8) { DoubleArray(10) }
    private var z2 = Array(8) { DoubleArray(10) }
    private var configuredSampleRate = 44100

    fun updateSettings(enabled: Boolean, gainsDb: List<Float>) {
        requestedEnabled = enabled
        val next = FloatArray(10) { idx ->
            gainsDb.getOrElse(idx) { 0f }.coerceIn(-12f, 12f)
        }
        requestedGainsDb = next
        rebuildSnapshot(configuredSampleRate)
    }

    @Throws(UnhandledAudioFormatException::class)
    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        if (inputAudioFormat.channelCount <= 0 || inputAudioFormat.channelCount > 8) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        configuredSampleRate = inputAudioFormat.sampleRate.coerceAtLeast(8000)
        rebuildSnapshot(configuredSampleRate)
        resetFilterStates()
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val snap = snapshot
        val out = replaceOutputBuffer(remaining)

        // Bit-transparent unity pass-through whenever EQ is disabled or all bands are 0 dB
        if (!snap.enabled || snap.isFlat || snap.activeBands.isEmpty()) {
            out.put(inputBuffer)
            out.flip()
            return
        }

        val channels = inputAudioFormat.channelCount.coerceIn(1, 8)
        val activeCount = snap.activeBands.size
        val preamp = snap.preampScale

        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            while (inputBuffer.remaining() >= channels * 2) {
                for (ch in 0 until channels) {
                    val sampleShort = inputBuffer.short
                    var x = (sampleShort / 32768.0) * preamp
                    val chZ1 = z1[ch]
                    val chZ2 = z2[ch]
                    for (b in 0 until activeCount) {
                        val c = snap.activeBands[b]
                        val bandIdx = snap.activeBandIndices[b]
                        val y = c.b0 * x + chZ1[bandIdx]
                        chZ1[bandIdx] = c.b1 * x - c.a1 * y + chZ2[bandIdx]
                        chZ2[bandIdx] = c.b2 * x - c.a2 * y
                        x = y
                    }
                    // Soft-knee limiter above 0.92 linear to protect against digital clipping
                    val limited = if (abs(x) > 0.92) {
                        val sign = if (x >= 0.0) 1.0 else -1.0
                        sign * (0.92 + 0.08 * tanh((abs(x) - 0.92) / 0.08))
                    } else {
                        x
                    }
                    val pcmOut = (limited * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                    out.putShort(pcmOut)
                }
            }
            if (inputBuffer.hasRemaining()) {
                out.put(inputBuffer)
            }
        } else {
            while (inputBuffer.remaining() >= channels * 4) {
                for (ch in 0 until channels) {
                    var x = inputBuffer.float.toDouble() * preamp
                    val chZ1 = z1[ch]
                    val chZ2 = z2[ch]
                    for (b in 0 until activeCount) {
                        val c = snap.activeBands[b]
                        val bandIdx = snap.activeBandIndices[b]
                        val y = c.b0 * x + chZ1[bandIdx]
                        chZ1[bandIdx] = c.b1 * x - c.a1 * y + chZ2[bandIdx]
                        chZ2[bandIdx] = c.b2 * x - c.a2 * y
                        x = y
                    }
                    val limited = if (abs(x) > 0.92) {
                        val sign = if (x >= 0.0) 1.0 else -1.0
                        sign * (0.92 + 0.08 * tanh((abs(x) - 0.92) / 0.08))
                    } else {
                        x
                    }
                    out.putFloat(limited.toFloat())
                }
            }
            if (inputBuffer.hasRemaining()) {
                out.put(inputBuffer)
            }
        }

        out.flip()
    }

    override fun onFlush() {
        resetFilterStates()
    }

    override fun onReset() {
        resetFilterStates()
    }

    private fun resetFilterStates() {
        for (ch in z1.indices) {
            z1[ch].fill(0.0)
            z2[ch].fill(0.0)
        }
    }

    private fun rebuildSnapshot(sampleRate: Int) {
        val gains = requestedGainsDb
        val enabled = requestedEnabled
        if (!enabled) {
            snapshot = EqSnapshot(
                enabled = false,
                isFlat = true,
                preampScale = 1.0,
                activeBands = emptyArray(),
                activeBandIndices = IntArray(0)
            )
            return
        }

        val activeList = ArrayList<BiquadCoeffs>(10)
        val indexList = ArrayList<Int>(10)
        var maxPositiveGain = 0f

        for (i in 0 until 10) {
            val g = gains[i]
            if (g > maxPositiveGain) maxPositiveGain = g
            if (abs(g) >= 0.05f) {
                val fc = centerFrequenciesHz[i].coerceAtMost(sampleRate * 0.44)
                activeList.add(computePeakingBiquad(fc, sampleRate.toDouble(), q = 1.41, gainDb = g.toDouble()))
                indexList.add(i)
            }
        }

        if (activeList.isEmpty()) {
            snapshot = EqSnapshot(
                enabled = true,
                isFlat = true,
                preampScale = 1.0,
                activeBands = emptyArray(),
                activeBandIndices = IntArray(0)
            )
            return
        }

        // Gentle headroom attenuation only when positive gains are large, preserving full loudness
        val attenuationDb = -(maxPositiveGain * 0.35).coerceIn(0.0, 4.5)
        val preampScale = 10.0.pow(attenuationDb / 20.0)

        snapshot = EqSnapshot(
            enabled = true,
            isFlat = false,
            preampScale = preampScale,
            activeBands = activeList.toTypedArray(),
            activeBandIndices = indexList.toIntArray()
        )
    }

    private fun computePeakingBiquad(
        fc: Double,
        fs: Double,
        q: Double,
        gainDb: Double
    ): BiquadCoeffs {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * fc / fs
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / (2.0 * q)

        val b0 = 1.0 + alpha * a
        val b1 = -2.0 * cosW0
        val b2 = 1.0 - alpha * a
        val a0 = 1.0 + alpha / a
        val a1 = -2.0 * cosW0
        val a2 = 1.0 - alpha / a

        return BiquadCoeffs(
            b0 = b0 / a0,
            b1 = b1 / a0,
            b2 = b2 / a0,
            a1 = a1 / a0,
            a2 = a2 / a0
        )
    }
}
