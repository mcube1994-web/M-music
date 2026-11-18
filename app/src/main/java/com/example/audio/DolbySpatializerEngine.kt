package com.example.audio

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.Spatializer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import androidx.core.content.ContextCompat
import com.example.domain.model.DolbyCapabilitiesState
import com.example.domain.model.DolbyFormatSupport
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10

object DolbySpatializerEngine {

    /**
     * Exact Kotlin implementation of multi-channel 16-bit PCM extraction from MediaCodec output buffers.
     * Assumes buffer PCM encoding is 16-bit signed linear PCM.
     */
    fun getSamplesForChannel(codec: MediaCodec, bufferId: Int, channelIx: Int): ShortArray? {
        val outputBuffer = codec.getOutputBuffer(bufferId) ?: return null
        val format = codec.getOutputFormat(bufferId)
        val samples = outputBuffer.duplicate().order(ByteOrder.nativeOrder()).asShortBuffer()
        val numChannels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        } else {
            return null
        }
        if (numChannels <= 0 || channelIx < 0 || channelIx >= numChannels) {
            return null
        }
        val frameCount = samples.remaining() / numChannels
        val res = ShortArray(frameCount)
        for (i in 0 until res.size) {
            res[i] = samples.get(i * numChannels + channelIx)
        }
        return res
    }

    /**
     * Configures a MediaCodec instance with Android's Spatializer check (API 32+) so multi-channel
     * Atmos/DD+ decoders output up to 99 channels for platform spatialization when supported
     * and enabled by the user toggle.
     */
    fun createAndConfigureSpatialCodec(
        context: Context,
        codecName: String,
        format: MediaFormat,
        audioAttributes: AudioAttributes,
        audioFormat: AudioFormat,
        userSpatializerEnabled: Boolean = true
    ): MediaCodec {
        val codec = MediaCodec.createByCodecName(codecName)
        if (userSpatializerEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val spatializer = audioManager.spatializer
            if (spatializer.immersiveAudioLevel != Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE) {
                if (spatializer.isAvailable &&
                    spatializer.isEnabled &&
                    spatializer.canBeSpatialized(audioAttributes, audioFormat)
                ) {
                    format.setInteger(MediaFormat.KEY_MAX_OUTPUT_CHANNEL_COUNT, 99)
                }
            }
        }
        codec.configure(format, null, null, 0)
        codec.start()
        return codec
    }

    fun isHeadphoneOutputDevice(deviceType: Int): Boolean {
        return when (deviceType) {
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> true
            AudioDeviceInfo.TYPE_BLE_HEADSET -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            AudioDeviceInfo.TYPE_HEARING_AID -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            else -> false
        }
    }

    fun detectConnectedHeadphones(audioManager: AudioManager): Pair<Boolean, String?> {
        val outputs = runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        }.getOrDefault(emptyList())
        val compatible = outputs.filter { isHeadphoneOutputDevice(it.type) }
        if (compatible.isEmpty()) return false to null

        val preferred = compatible.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        } ?: compatible.first()

        val label = preferred.productName?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            ?: when (preferred.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Headphones"
                AudioDeviceInfo.TYPE_BLE_HEADSET -> "LE Audio Headset"
                AudioDeviceInfo.TYPE_USB_HEADSET -> "USB-C Headphones"
                else -> "Wired Headphones"
            }
        return true to label
    }

    fun registerSpatializerCallbacks(
        context: Context,
        onStateChanged: () -> Unit
    ): () -> Unit {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val mainHandler = Handler(Looper.getMainLooper())

        val deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                onStateChanged()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                onStateChanged()
            }
        }
        runCatching { audioManager.registerAudioDeviceCallback(deviceCallback, mainHandler) }

        var spatializerListener: Spatializer.OnSpatializerStateChangedListener? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            runCatching {
                val spatializer = audioManager.spatializer
                val listener = object : Spatializer.OnSpatializerStateChangedListener {
                    override fun onSpatializerEnabledChanged(spat: Spatializer, enabled: Boolean) {
                        onStateChanged()
                    }

                    override fun onSpatializerAvailableChanged(spat: Spatializer, available: Boolean) {
                        onStateChanged()
                    }
                }
                spatializer.addOnSpatializerStateChangedListener(
                    ContextCompat.getMainExecutor(context),
                    listener
                )
                spatializerListener = listener
            }
        }

        return {
            runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
                spatializerListener?.let { listener ->
                    runCatching { audioManager.spatializer.removeOnSpatializerStateChangedListener(listener) }
                }
            }
        }
    }

    fun probeDeviceCapabilities(
        context: Context,
        userSpatializerEnabled: Boolean = true
    ): DolbyCapabilitiesState {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val (headphonesConnected, headphoneName) = detectConnectedHeadphones(audioManager)
        var spatialAvailable = false
        var spatialEnabled = false
        var canBeSpatialized = false
        var headTrackerAvailable = false
        var immersiveLabel = if (userSpatializerEnabled) {
            "Stereo / Headphone Binaural"
        } else {
            "Bypassed (Stereo Direct)"
        }
        var maxChannels = 2

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            runCatching {
                val spatializer = audioManager.spatializer
                spatialAvailable = spatializer.isAvailable
                spatialEnabled = spatializer.isEnabled

                val probeAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val probeFormat = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_5POINT1)
                    .setSampleRate(48000)
                    .build()
                canBeSpatialized = spatializer.canBeSpatialized(probeAttrs, probeFormat)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    headTrackerAvailable = spatializer.isHeadTrackerAvailable
                }

                immersiveLabel = when {
                    !userSpatializerEnabled -> "Spatializer Disabled by App Setting (Stereo)"
                    spatializer.immersiveAudioLevel == Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_MULTICHANNEL ->
                        "7.1.4 Multi-Channel Spatializer"
                    spatializer.immersiveAudioLevel == Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_OTHER ->
                        "Spatializer Active (Binaural/Transaural)"
                    else -> "Software Binaural 7.1.4"
                }
                if (userSpatializerEnabled && spatialAvailable && spatialEnabled) {
                    maxChannels = 99
                }
            }
        }

        val codecInfos = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
        fun hasDecoderForMime(mime: String, profileHint: String? = null): Boolean {
            return codecInfos.any { info ->
                !info.isEncoder && info.supportedTypes.any { type ->
                    type.equals(mime, ignoreCase = true) && (profileHint == null || info.name.contains(profileHint, true))
                }
            }
        }

        val hasDv = hasDecoderForMime(MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION)
        val hasAv1 = hasDecoderForMime("video/av01")
        val hasHevc = hasDecoderForMime(MediaFormat.MIMETYPE_VIDEO_HEVC)
        val hasAc4 = hasDecoderForMime("audio/ac4")
        val hasEac3Joc = hasDecoderForMime("audio/eac3-joc")
        val hasEac3 = hasDecoderForMime(MediaFormat.MIMETYPE_AUDIO_EAC3)
        val hasAc3 = hasDecoderForMime(MediaFormat.MIMETYPE_AUDIO_AC3)

        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val hdrTypes: List<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && display != null) {
            @Suppress("DEPRECATION")
            val caps = display.hdrCapabilities?.supportedHdrTypes ?: intArrayOf()
            caps.toList().mapNotNull { hdrType ->
                when (hdrType) {
                    Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                    Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                    Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
                    Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                    else -> null
                }
            }.distinct().ifEmpty { listOf("SDR (Auto Tone-Map)") }
        } else {
            listOf("SDR")
        }

        val formats = listOf(
            DolbyFormatSupport("dv_av1", "Dolby Vision AV1", "dav1.10.06", hasDv && hasAv1),
            DolbyFormatSupport("dv_p5", "Dolby Vision Profile 5", "dvh1.05.06", hasDv),
            DolbyFormatSupport("dv_p8", "Dolby Vision Profile 8 (HDR10 Fallback)", "dvh1.08.06", hasDv || hasHevc),
            DolbyFormatSupport("ac4_atmos_l4", "Dolby Atmos AC-4 L4", "ac-4.02.01.04", hasAc4),
            DolbyFormatSupport("ac4_atmos_l3", "Dolby Atmos AC-4 L3", "ac-4.02.01.03", hasAc4),
            DolbyFormatSupport("ac4_surround", "Dolby AC-4 Surround", "ac-4.02.01.02", hasAc4),
            DolbyFormatSupport("ec3_atmos", "Dolby Atmos DD+ (JOC)", "ec-3 (JOC)", hasEac3Joc || hasEac3),
            DolbyFormatSupport("ec3_surround", "Dolby Digital Plus 5.1", "ec-3", hasEac3),
            DolbyFormatSupport("ac3_surround", "Dolby Digital AC-3 5.1", "ac-3", hasAc3)
        )

        return DolbyCapabilitiesState(
            spatializerAvailable = spatialAvailable,
            spatializerEnabled = spatialEnabled,
            userSpatializerEnabled = userSpatializerEnabled,
            compatibleHeadphonesConnected = headphonesConnected,
            connectedHeadphoneName = headphoneName,
            canBeSpatialized = canBeSpatialized,
            headTrackerAvailable = headTrackerAvailable,
            immersiveLevelLabel = immersiveLabel,
            maxOutputChannelCount = maxChannels,
            hdrTypesLabel = hdrTypes.joinToString(" • "),
            formats = formats
        )
    }

    fun calculateChannelPeakDb(samples: ShortArray): Float {
        if (samples.isEmpty()) return -96f
        var maxSample = 0
        for (s in samples) {
            val v = abs(s.toInt())
            if (v > maxSample) maxSample = v
        }
        return if (maxSample <= 0) -96f else (20.0 * log10(maxSample / 32767.0)).toFloat()
    }

    const val WEB_MEDIA_CAPABILITIES_PROBE_JS = """
        (async function() {
          const candidates = [
            { key: 'dv_av1', type: 'media-source', video: { contentType: 'video/mp4; codecs="dav1.10.06"', width: 3840, height: 2160, bitrate: 15000000, framerate: 24 } },
            { key: 'dv_p5',  type: 'media-source', video: { contentType: 'video/mp4; codecs="dvh1.05.06"', width: 3840, height: 2160, bitrate: 15000000, framerate: 24 } },
            { key: 'dv_p8',  type: 'media-source', video: { contentType: 'video/mp4; codecs="dvh1.08.06"', width: 3840, height: 2160, bitrate: 15000000, framerate: 24 } },
            { key: 'ac4_atmos_l4', type: 'media-source', audio: { contentType: 'audio/mp4; codecs="ac-4.02.01.04"', spatialRendering: true } },
            { key: 'ac4_atmos_l3', type: 'media-source', audio: { contentType: 'audio/mp4; codecs="ac-4.02.01.03"', spatialRendering: true } },
            { key: 'ac4_surround', type: 'media-source', audio: { contentType: 'audio/mp4; codecs="ac-4.02.01.02"' } },
            { key: 'ec3_atmos',    type: 'media-source', audio: { contentType: 'audio/mp4; codecs="ec-3"', spatialRendering: true } },
            { key: 'ec3_surround', type: 'media-source', audio: { contentType: 'audio/mp4; codecs="ec-3"' } },
            { key: 'ac3_surround', type: 'media-source', audio: { contentType: 'audio/mp4; codecs="ac-3"' } }
          ];
          const supported = {};
          for (const item of candidates) {
            try {
              const res = await navigator.mediaCapabilities.decodingInfo(item);
              supported[item.key] = !!res.supported;
            } catch (e) {
              supported[item.key] = false;
            }
          }
          return JSON.stringify(supported);
        })();
    """
}
