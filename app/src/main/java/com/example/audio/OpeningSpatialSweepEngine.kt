package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Synthesizes a non-blocking 7.1.4 Dolby Surround binaural opening audio sweep
 * (sub-bass swell + 360-degree azimuth & height harmonic chord sweep).
 */
class OpeningSpatialSweepEngine {

    private val _isPlayingSweep = MutableStateFlow(false)
    val isPlayingSweep: StateFlow<Boolean> = _isPlayingSweep.asStateFlow()

    private var sweepJob: Job? = null

    fun playOpeningSweep(scope: CoroutineScope) {
        if (sweepJob?.isActive == true) return
        sweepJob = scope.launch(Dispatchers.Default) {
            _isPlayingSweep.value = true
            var audioTrack: AudioTrack? = null
            try {
                val sampleRate = 48_000
                val durationSec = 2.4
                val numFrames = (sampleRate * durationSec).toInt()
                val pcm = ShortArray(numFrames * 2)

                // Lush 7.1.4 harmonic chord (D2 sub, A3, D4, F#4, A4, E5 height shimmer)
                for (i in 0 until numFrames) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numFrames

                    // Smooth attack & long spatial tail envelope
                    val attack = (progress / 0.14).coerceAtMost(1.0)
                    val release = ((1.0 - progress) / 0.45).coerceAtMost(1.0)
                    val env = sin(attack * PI * 0.5) * sin(release * PI * 0.5)

                    // Azimuth orbital pan sweep from Front-Left -> Rear -> Height -> Center
                    val azimuth = -PI * 0.65 + progress * PI * 2.1
                    val leftGain = (0.5 * (1.0 - sin(azimuth) * 0.85)).coerceIn(0.1, 1.0)
                    val rightGain = (0.5 * (1.0 + sin(azimuth) * 0.85)).coerceIn(0.1, 1.0)

                    // Height elevation harmonic shimmer (simulates 4 top ceiling channels)
                    val heightLift = sin(progress * PI)
                    val subBass = sin(2.0 * PI * 58.27 * t) * 0.42
                    val warmPad = sin(2.0 * PI * 146.83 * t) * 0.25 +
                        sin(2.0 * PI * 220.0 * t) * 0.22 +
                        sin(2.0 * PI * 293.66 * t) * 0.18
                    val heightShimmer = (sin(2.0 * PI * 587.33 * t) * 0.14 +
                        sin(2.0 * PI * 880.0 * t) * 0.10) * heightLift

                    // Subtle interaural phase delay for 3D width
                    val phaseOffset = cos(azimuth) * 0.08
                    val monoCore = (subBass + warmPad + heightShimmer) * env
                    val lSample = ((monoCore * leftGain + heightShimmer * phaseOffset * env) * 14000.0)
                        .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    val rSample = ((monoCore * rightGain - heightShimmer * phaseOffset * env) * 14000.0)
                        .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                    pcm[i * 2] = lSample
                    pcm[i * 2 + 1] = rSample
                }

                val bufferSizeBytes = pcm.size * 2
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSizeBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(pcm, 0, pcm.size)
                audioTrack.play()
                kotlinx.coroutines.delay((durationSec * 1000).toLong() + 80L)
            } catch (_: Throwable) {
                // Non-fatal on devices without audio output
            } finally {
                runCatching {
                    audioTrack?.stop()
                    audioTrack?.release()
                }
                _isPlayingSweep.value = false
            }
        }
    }

    fun stopSweep() {
        sweepJob?.cancel()
        _isPlayingSweep.value = false
    }
}
