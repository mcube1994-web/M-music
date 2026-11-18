package com.example.audio

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
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
    val dspEngineName: String = "Media3 10-Band Biquad DSP"
)

/**
 * 10-Band AI Auto & Manual Equalizer (31Hz .. 16kHz) driving M3TenBandEqAudioProcessor
 * directly inside Media3 ExoPlayer's AudioSink (avoiding vendor DynamicsProcessing HAL muting).
 */
@OptIn(UnstableApi::class)
class AiAutoEqEngine {

    val bandLabels = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    private val _state = MutableStateFlow(AiEqState())
    val state: StateFlow<AiEqState> = _state.asStateFlow()

    private var eqProcessor: M3TenBandEqAudioProcessor? = null
    private var currentSessionId: Int = 0

    fun bindProcessor(processor: M3TenBandEqAudioProcessor) {
        eqProcessor = processor
        applyCurrentBandsToDsp()
    }

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0) return
        currentSessionId = audioSessionId
        _state.value = _state.value.copy(
            isHardwareDspAttached = eqProcessor != null || audioSessionId > 0,
            audioSessionId = audioSessionId,
            dspEngineName = "Media3 10-Band Biquad DSP"
        )
        applyCurrentBandsToDsp()
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
            contentLabel = "Cinema Video • Clean Surround"
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

        val targetGains = resolvedMode.defaultGainsDb
        if (current.activeResolvedMode == resolvedMode &&
            current.detectedContentLabel == contentLabel &&
            current.bandGainsDb == targetGains
        ) {
            return
        }

        _state.value = current.copy(
            activeResolvedMode = resolvedMode,
            detectedContentLabel = contentLabel,
            bandGainsDb = targetGains
        )
        applyCurrentBandsToDsp()
    }

    fun setEqEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isEqEnabled = enabled)
        applyCurrentBandsToDsp()
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
        applyCurrentBandsToDsp()
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
            detectedContentLabel = "Custom 10-Band Biquad EQ"
        )
        applyCurrentBandsToDsp()
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
        applyCurrentBandsToDsp()
    }

    private fun applyCurrentBandsToDsp() {
        val st = _state.value
        eqProcessor?.updateSettings(st.isEqEnabled, st.bandGainsDb)
    }

    fun releaseDsp() {
        currentSessionId = 0
        _state.value = _state.value.copy(isHardwareDspAttached = false, audioSessionId = 0)
    }
}
