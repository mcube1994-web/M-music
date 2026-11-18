package com.example.audio

import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.os.Build
import com.example.domain.model.AiEqMode
import com.example.domain.model.MediaTrack
import com.example.domain.model.MusicLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AiEqState(
    val isEqEnabled: Boolean = true,
    val isAutoEnabled: Boolean = true,
    val selectedMode: AiEqMode = AiEqMode.AUTO_DETECT,
    val activeResolvedMode: AiEqMode = AiEqMode.SPATIAL_ATMOS,
    val detectedContentLabel: String = "Music • Ready",
    val bandGainsDb: List<Float> = AiEqMode.SPATIAL_ATMOS.defaultGainsDb,
    val isHardwareDspAttached: Boolean = false,
    val audioSessionId: Int = 0,
    val dspEngineName: String = "Media3 10-Band Equalizer"
)

/**
 * 10-Band AI Auto & Manual Equalizer (31Hz .. 16kHz) attached to Media3 ExoPlayer's audioSessionId.
 * Uses DynamicsProcessing (API 28+) or android.media.audiofx.Equalizer fallback.
 */
class AiAutoEqEngine {

    val bandLabels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    private val bandCenterHz = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)

    private val _state = MutableStateFlow(AiEqState())
    val state: StateFlow<AiEqState> = _state.asStateFlow()

    private var dynamicsProcessing: DynamicsProcessing? = null
    private var legacyEqualizer: Equalizer? = null
    private var currentSessionId: Int = 0

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        releaseDsp()
        currentSessionId = audioSessionId

        var attached = false
        var engineName = "Media3 10-Band Equalizer"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                val config = DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    1,
                    true,
                    10,
                    false,
                    0,
                    false,
                    0,
                    false
                ).build()
                val dp = DynamicsProcessing(0, audioSessionId, config)
                dp.enabled = _state.value.isEqEnabled
                for (i in 0 until 10) {
                    dp.setPreEqBandAllChannelsTo(
                        i,
                        DynamicsProcessing.EqBand(true, bandCenterHz[i], _state.value.bandGainsDb[i])
                    )
                }
                dynamicsProcessing = dp
                attached = true
                engineName = "Media3 DynamicsProcessing (10-Band)"
            }
        }

        if (!attached) {
            runCatching {
                val eq = Equalizer(0, audioSessionId).apply {
                    enabled = _state.value.isEqEnabled
                }
                legacyEqualizer = eq
                attached = true
                engineName = "Media3 Equalizer (${eq.numberOfBands}-Band)"
            }
        }

        _state.value = _state.value.copy(
            isHardwareDspAttached = attached,
            audioSessionId = audioSessionId,
            dspEngineName = engineName
        )
        applyCurrentBandsToHardware()
    }

    fun analyzeAndAdapt(
        track: MediaTrack?,
        telemetry: VisualizerTelemetry
    ) {
        if (track == null) return
        val current = _state.value

        if (!current.isEqEnabled || (!current.isAutoEnabled && current.selectedMode != AiEqMode.AUTO_DETECT)) {
            return
        }

        val resolvedMode: AiEqMode
        val contentLabel: String

        if (track.isVideo) {
            resolvedMode = AiEqMode.CINEMA_SURROUND
            contentLabel = "Cinema Video • Auto Surround"
        } else {
            val titleAlbum = "${track.title} ${track.album} ${track.subtitle.orEmpty()}".lowercase()
            resolvedMode = when {
                track.isLossless || track.language == MusicLanguage.LOSSLESS -> {
                    if (telemetry.subBassEnergy > 0.55f) AiEqMode.SPATIAL_ATMOS else AiEqMode.FLAT_HIFI
                }
                track.language == MusicLanguage.PUNJABI ||
                    titleAlbum.contains("bass") ||
                    titleAlbum.contains("edm") ||
                    titleAlbum.contains("trap") ||
                    telemetry.subBassEnergy > 0.52f -> AiEqMode.BASS_BOOST
                track.language == MusicLanguage.HINDI ||
                    track.language == MusicLanguage.BANGLA ||
                    titleAlbum.contains("acoustic") ||
                    titleAlbum.contains("unplugged") ||
                    telemetry.vocalEnergy > telemetry.subBassEnergy * 1.15f -> AiEqMode.VOCAL_CLARITY
                else -> AiEqMode.SPATIAL_ATMOS
            }
            contentLabel = "Music (${track.language.label}) • ${resolvedMode.label}"
        }

        val targetGains = resolvedMode.defaultGainsDb.mapIndexed { idx, base ->
            val dynamicOffset = when (idx) {
                0, 1 -> if (telemetry.subBassEnergy < 0.18f && !track.isLossless) 0.6f else 0f
                5, 6 -> if (telemetry.vocalEnergy < 0.2f) 0.5f else 0f
                8, 9 -> if (telemetry.brillianceEnergy < 0.15f) 0.7f else 0f
                else -> 0f
            }
            (base + dynamicOffset).coerceIn(-12f, 12f)
        }

        _state.value = current.copy(
            activeResolvedMode = resolvedMode,
            detectedContentLabel = contentLabel,
            bandGainsDb = targetGains
        )
        applyCurrentBandsToHardware()
    }

    fun setEqEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isEqEnabled = enabled)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dynamicsProcessing?.enabled = enabled
            }
            legacyEqualizer?.enabled = enabled
        }
        if (enabled) {
            applyCurrentBandsToHardware()
        }
    }

    fun selectMode(mode: AiEqMode) {
        val auto = mode == AiEqMode.AUTO_DETECT
        val gains = if (auto) _state.value.bandGainsDb else mode.defaultGainsDb
        _state.value = _state.value.copy(
            isEqEnabled = true,
            isAutoEnabled = auto,
            selectedMode = mode,
            activeResolvedMode = if (auto) _state.value.activeResolvedMode else mode,
            bandGainsDb = gains,
            detectedContentLabel = if (auto) "AI Auto Active" else "${mode.label} Preset"
        )
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dynamicsProcessing?.enabled = true
            }
            legacyEqualizer?.enabled = true
        }
        applyCurrentBandsToHardware()
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in 0..9) return
        val updated = _state.value.bandGainsDb.toMutableList()
        updated[bandIndex] = gainDb.coerceIn(-12f, 12f)
        _state.value = _state.value.copy(
            isEqEnabled = true,
            isAutoEnabled = false,
            selectedMode = AiEqMode.FLAT_HIFI,
            bandGainsDb = updated,
            detectedContentLabel = "Custom 10-Band Tuning"
        )
        applyCurrentBandsToHardware()
    }

    fun resetToFlat() {
        _state.value = _state.value.copy(
            isEqEnabled = true,
            isAutoEnabled = false,
            selectedMode = AiEqMode.FLAT_HIFI,
            activeResolvedMode = AiEqMode.FLAT_HIFI,
            bandGainsDb = List(10) { 0f },
            detectedContentLabel = "Flat Hi-Fi (0.0 dB)"
        )
        applyCurrentBandsToHardware()
    }

    private fun applyCurrentBandsToHardware() {
        val gains = _state.value.bandGainsDb
        val enabled = _state.value.isEqEnabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dynamicsProcessing?.let { dp ->
                runCatching {
                    dp.enabled = enabled
                    for (i in 0 until 10) {
                        dp.setPreEqBandAllChannelsTo(
                            i,
                            DynamicsProcessing.EqBand(enabled, bandCenterHz[i], gains[i])
                        )
                    }
                }
            }
        }
        legacyEqualizer?.let { eq ->
            runCatching {
                eq.enabled = enabled
                val numBands = eq.numberOfBands.toInt()
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]
                for (b in 0 until numBands) {
                    val mappedIdx = ((b.toFloat() / numBands) * 10).toInt().coerceIn(0, 9)
                    val millibels = (gains[mappedIdx] * 100f).toInt()
                        .coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                    eq.setBandLevel(b.toShort(), millibels)
                }
            }
        }
    }

    fun releaseDsp() {
        runCatching { dynamicsProcessing?.release() }
        runCatching { legacyEqualizer?.release() }
        dynamicsProcessing = null
        legacyEqualizer = null
        currentSessionId = 0
        _state.value = _state.value.copy(isHardwareDspAttached = false, audioSessionId = 0)
    }
}
