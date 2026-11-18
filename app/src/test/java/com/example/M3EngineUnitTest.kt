package com.example

import android.media.AudioDeviceInfo
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.example.audio.AiAutoEqEngine
import com.example.audio.DolbySpatializerEngine
import com.example.audio.HlsManifestParser
import com.example.audio.M3TenBandEqAudioProcessor
import com.example.audio.VisualizerTelemetry
import com.example.domain.model.AiEqMode
import com.example.domain.model.AppearanceSkin
import com.example.domain.model.MediaTrack
import com.example.domain.model.MusicLanguage
import com.example.domain.model.RepeatMode
import com.example.domain.model.StreamingBackend
import com.example.domain.model.VisualizerMode
import com.example.domain.model.VisualizerPalette
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M3EngineUnitTest {

    @Test
    fun visualizersAndSkins_includeAllVizzFmModesAndPalettes() {
        val modes = VisualizerMode.entries
        assertEquals(7, modes.size)
        assertTrue(modes.contains(VisualizerMode.VIZZ_MESH_GRID))
        assertTrue(modes.contains(VisualizerMode.VIZZ_SPHERICAL_SHADER))
        assertTrue(modes.contains(VisualizerMode.VIZZ_AUDIO_TUNNEL))
        assertTrue(modes.contains(VisualizerMode.VIZZ_CYCLONE_EYES))
        assertTrue(modes.contains(VisualizerMode.VIZZ_EMBER_ROSE))
        assertTrue(modes.contains(VisualizerMode.VIZZ_GLACIER_SPIRAL))
        assertTrue(modes.contains(VisualizerMode.VIZZ_LIVE_WEB))

        assertEquals(5, VisualizerPalette.entries.size)
        assertEquals(4, AppearanceSkin.entries.size)
    }

    @Test
    fun hlsManifestParser_parsesDolbyVisionAndAtmosRenditions() {
        val info = HlsManifestParser.parse()
        assertEquals(7, info.version)
        assertEquals(4, info.audioRenditions.size)
        assertEquals("Atmos AC-4", info.audioRenditions[0].name)
        assertTrue(info.audioRenditions[0].isDefault)
        assertEquals("16/JOC", info.audioRenditions[1].channels)
        assertEquals(3, info.videoVariants.size)
        assertEquals("dvh1.08.06/db4h", info.videoVariants[0].supplementalCodecs)
        assertEquals("1920x1080", info.videoVariants[2].resolution)
    }

    @Test
    fun aiAutoEq_switchesBetweenCinemaAndMusicModes() {
        val eq = AiAutoEqEngine()
        val videoTrack = MediaTrack(
            id = "v1",
            title = "Sci-Fi Feature",
            artist = "Blender",
            backend = StreamingBackend.HLS,
            streamUrl = "https://example.com/master.m3u8",
            isVideo = true,
            hdrTag = "Dolby Vision + Atmos"
        )
        eq.analyzeAndAdapt(videoTrack, VisualizerTelemetry(channelCount = 6))
        assertEquals(AiEqMode.CINEMA_SURROUND, eq.state.value.activeResolvedMode)

        val punjabiTrack = MediaTrack(
            id = "m1",
            title = "Heavy Bass Track",
            artist = "Artist",
            language = MusicLanguage.PUNJABI,
            backend = StreamingBackend.JIOSAAVN,
            streamUrl = "https://example.com/song.m4a",
            isVideo = false
        )
        eq.analyzeAndAdapt(punjabiTrack, VisualizerTelemetry(subBassEnergy = 0.6f))
        assertEquals(AiEqMode.BASS_BOOST, eq.state.value.activeResolvedMode)

        val hindiVocal = MediaTrack(
            id = "m2",
            title = "Acoustic Unplugged",
            artist = "Singer",
            language = MusicLanguage.HINDI,
            backend = StreamingBackend.JIOSAAVN,
            streamUrl = "https://example.com/vocal.m4a",
            isVideo = false
        )
        eq.analyzeAndAdapt(hindiVocal, VisualizerTelemetry(subBassEnergy = 0.2f, vocalEnergy = 0.45f))
        assertEquals(AiEqMode.VOCAL_CLARITY, eq.state.value.activeResolvedMode)
    }

    @Test
    fun tenBandEqualizer_supportsToggleManualTuningAndFlatReset() {
        val eq = AiAutoEqEngine()
        assertEquals(10, eq.bandLabels.size)
        assertEquals(10, eq.state.value.bandGainsDb.size)
        assertTrue(eq.state.value.isEqEnabled)

        eq.setEqEnabled(false)
        assertEquals(false, eq.state.value.isEqEnabled)

        eq.setBandGain(0, 7.5f)
        eq.setBandGain(9, -15.0f) // Clamped to -12f
        assertTrue(eq.state.value.isEqEnabled)
        assertEquals(7.5f, eq.state.value.bandGainsDb[0], 0.01f)
        assertEquals(-12.0f, eq.state.value.bandGainsDb[9], 0.01f)

        eq.resetToFlat()
        assertEquals(AiEqMode.FLAT_HIFI, eq.state.value.selectedMode)
        assertTrue(eq.state.value.bandGainsDb.all { it == 0f })
    }

    @Test
    fun spatializerEngine_recognizesCompatibleHeadphoneOutputs() {
        assertTrue(DolbySpatializerEngine.isHeadphoneOutputDevice(AudioDeviceInfo.TYPE_WIRED_HEADPHONES))
        assertTrue(DolbySpatializerEngine.isHeadphoneOutputDevice(AudioDeviceInfo.TYPE_WIRED_HEADSET))
        assertTrue(DolbySpatializerEngine.isHeadphoneOutputDevice(AudioDeviceInfo.TYPE_USB_HEADSET))
        assertTrue(DolbySpatializerEngine.isHeadphoneOutputDevice(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
        assertFalse(DolbySpatializerEngine.isHeadphoneOutputDevice(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
    }

    @Test
    fun eqPresets_maintainAntiClipHeadroom_andManualModeNotOverwrittenByAutoAnalyze() {
        AiEqMode.entries.forEach { mode ->
            val maxGain = mode.defaultGainsDb.maxOrNull() ?: 0f
            assertTrue("Preset ${mode.name} exceeds anti-clip ceiling: $maxGain", maxGain <= 3.5f)
        }

        val eq = AiAutoEqEngine()
        eq.resetToFlat()
        val track = MediaTrack(
            id = "m_flat",
            title = "Heavy Bass Track",
            artist = "Artist",
            language = MusicLanguage.PUNJABI,
            backend = StreamingBackend.JIOSAAVN,
            streamUrl = "https://example.com/song.m4a"
        )
        eq.analyzeAndAdapt(track, VisualizerTelemetry(subBassEnergy = 0.8f))
        assertEquals(AiEqMode.FLAT_HIFI, eq.state.value.selectedMode)
        assertTrue(eq.state.value.bandGainsDb.all { it == 0f })
    }

    @Test
    fun streamingBackends_excludeItunes_andRepeatModesAreComplete() {
        assertTrue(StreamingBackend.entries.none { it.name.contains("ITUNES", ignoreCase = true) })
        assertEquals(
            listOf(RepeatMode.OFF, RepeatMode.REPEAT_ALL, RepeatMode.REPEAT_ONE),
            RepeatMode.entries
        )
    }

    @Test
    fun tenBandEqAudioProcessor_deliversNonMutedAudioInBypassAndActiveDspModes() {
        val processor = M3TenBandEqAudioProcessor()
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)

        val frameCount = 256
        val inputBytes = ByteBuffer.allocateDirect(frameCount * 2 * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frameCount) {
            val sample = (12000.0 * sin(2.0 * Math.PI * 1000.0 * i / 44100.0)).toInt().toShort()
            inputBytes.putShort(sample)
            inputBytes.putShort(sample)
        }
        inputBytes.flip()

        // 1. Unity flat bypass: output must be loud & bit-transparent
        processor.updateSettings(enabled = true, gainsDb = List(10) { 0f })
        val bypassInput = inputBytes.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        processor.queueInput(bypassInput)
        val bypassOut = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(frameCount * 4, bypassOut.remaining())
        var bypassEnergy = 0L
        while (bypassOut.remaining() >= 2) {
            bypassEnergy += abs(bypassOut.short.toInt())
        }
        assertTrue("Flat EQ output should carry full audio energy", bypassEnergy > 500_000L)

        // 2. Active boosted 10-band EQ: must remain audible without muting or clipping
        processor.flush(AudioProcessor.StreamMetadata.DEFAULT)
        processor.updateSettings(
            enabled = true,
            gainsDb = listOf(4f, 3f, 1.5f, 0f, 1f, 2.5f, 2f, 3f, 2.5f, 2f)
        )
        val eqInput = inputBytes.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        processor.queueInput(eqInput)
        val eqOut = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(frameCount * 4, eqOut.remaining())
        var eqEnergy = 0L
        while (eqOut.remaining() >= 2) {
            eqEnergy += abs(eqOut.short.toInt())
        }
        assertTrue("Boosted EQ output must remain audible and non-muted", eqEnergy > 500_000L)
    }
}

