package com.example.audio

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

data class VisualizerTelemetry(
    val spectrumBins: List<Float> = List(32) { 0.04f },
    val waveform: List<Float> = List(96) { 0f },
    val subBassEnergy: Float = 0f,
    val vocalEnergy: Float = 0f,
    val brillianceEnergy: Float = 0f,
    val channelCount: Int = 2,
    val peakDb: Float = -60f
)

/**
 * Zero-permission real-time PCM tap using Media3 TeeAudioProcessor.AudioBufferSink.
 * Feeds live 32-band FFT spectrum, oscilloscope waveform, and AI Auto EQ spectral ratios.
 */
@UnstableApi
class PcmVisualizerProcessor : TeeAudioProcessor.AudioBufferSink {

    private val _telemetry = MutableStateFlow(VisualizerTelemetry())
    val telemetry: StateFlow<VisualizerTelemetry> = _telemetry.asStateFlow()

    private var sampleRateHz = 44100
    private var channelCount = 2
    private var encoding = C.ENCODING_PCM_16BIT
    private var lastUpdateMs = 0L

    private val fftSize = 256
    private val real = DoubleArray(fftSize)
    private val imag = DoubleArray(fftSize)
    private val smoothedBins = FloatArray(32) { 0.05f }

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRateHz = sampleRateHz.coerceAtLeast(8000)
        this.channelCount = channelCount.coerceAtLeast(1)
        this.encoding = encoding
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        val now = System.currentTimeMillis()
        if (now - lastUpdateMs < 32L) return // ~30fps smooth telemetry without UI churn
        lastUpdateMs = now

        if (encoding != C.ENCODING_PCM_16BIT && encoding != C.ENCODING_PCM_FLOAT) return
        val dup = buffer.duplicate().order(ByteOrder.nativeOrder())
        val wavePoints = FloatArray(96)

        if (encoding == C.ENCODING_PCM_16BIT) {
            val shorts = dup.asShortBuffer()
            val totalSamples = shorts.remaining()
            val frames = totalSamples / channelCount
            if (frames < 16) return

            var maxAbs = 0f
            for (i in 0 until fftSize) {
                val frameIdx = (i * frames) / fftSize
                val s = shorts.get(frameIdx * channelCount) / 32768f
                val absVal = kotlin.math.abs(s)
                if (absVal > maxAbs) maxAbs = absVal
                // Hann window
                val win = 0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))
                real[i] = s * win
                imag[i] = 0.0
            }

            for (w in 0 until 96) {
                val frameIdx = (w * frames) / 96
                wavePoints[w] = (shorts.get(frameIdx * channelCount) / 32768f).coerceIn(-1f, 1f)
            }

            computeFftAndEmit(wavePoints, maxAbs)
        } else {
            val floats = dup.asFloatBuffer()
            val frames = floats.remaining() / channelCount
            if (frames < 16) return

            var maxAbs = 0f
            for (i in 0 until fftSize) {
                val frameIdx = (i * frames) / fftSize
                val s = floats.get(frameIdx * channelCount).coerceIn(-1f, 1f)
                val absVal = kotlin.math.abs(s)
                if (absVal > maxAbs) maxAbs = absVal
                val win = 0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))
                real[i] = s * win
                imag[i] = 0.0
            }

            for (w in 0 until 96) {
                val frameIdx = (w * frames) / 96
                wavePoints[w] = floats.get(frameIdx * channelCount).coerceIn(-1f, 1f)
            }

            computeFftAndEmit(wavePoints, maxAbs)
        }
    }

    private fun computeFftAndEmit(wavePoints: FloatArray, maxAbs: Float) {
        fftInPlace(real, imag)

        // Group 128 positive FFT bins into 32 perceptual logarithmic bands
        val bins = List(32) { band ->
            val start = (band * 3).coerceAtMost(120)
            val end = ((band + 1) * 4).coerceIn(start + 1, 127)
            var sum = 0.0
            for (k in start until end) {
                sum += hypot(real[k], imag[k])
            }
            val mag = (sum / (end - start)).toFloat()
            val normalized = (sqrt(mag * 0.25f)).coerceIn(0.03f, 1f)
            smoothedBins[band] = smoothedBins[band] * 0.58f + normalized * 0.42f
            smoothedBins[band]
        }

        val bass = bins.subList(0, 6).average().toFloat()
        val vocal = bins.subList(8, 20).average().toFloat()
        val brilliance = bins.subList(21, 32).average().toFloat()
        val peakDb = if (maxAbs <= 1e-5f) -60f else (20.0 * log10(maxAbs.toDouble())).toFloat()

        _telemetry.value = VisualizerTelemetry(
            spectrumBins = bins,
            waveform = wavePoints.toList(),
            subBassEnergy = bass,
            vocalEnergy = vocal,
            brillianceEnergy = brilliance,
            channelCount = channelCount,
            peakDb = peakDb
        )
    }

    fun emitIdleDecay() {
        val decayed = smoothedBins.mapIndexed { idx, v ->
            val next = (v * 0.85f).coerceAtLeast(0.04f)
            smoothedBins[idx] = next
            next
        }
        _telemetry.value = _telemetry.value.copy(
            spectrumBins = decayed,
            waveform = List(96) { 0f }
        )
    }

    private fun fftInPlace(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wlenRe = cos(ang)
            val wlenIm = sin(ang)
            var i = 0
            while (i < n) {
                var wRe = 1.0
                var wIm = 0.0
                for (k in 0 until len / 2) {
                    val uRe = re[i + k]
                    val uIm = im[i + k]
                    val vRe = re[i + k + len / 2] * wRe - im[i + k + len / 2] * wIm
                    val vIm = re[i + k + len / 2] * wIm + im[i + k + len / 2] * wRe
                    re[i + k] = uRe + vRe
                    im[i + k] = uIm + vIm
                    re[i + k + len / 2] = uRe - vRe
                    im[i + k + len / 2] = uIm - vIm
                    val nextWRe = wRe * wlenRe - wIm * wlenIm
                    wIm = wRe * wlenIm + wIm * wlenRe
                    wRe = nextWRe
                }
                i += len
            }
            len = len shl 1
        }
    }
}
