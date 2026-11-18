package com.example.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.audio.AiAutoEqEngine
import com.example.audio.M3TenBandEqAudioProcessor
import com.example.audio.PcmVisualizerProcessor

@OptIn(UnstableApi::class)
object M3EngineRegistry {
    val eqAudioProcessor = M3TenBandEqAudioProcessor()
    val pcmProcessor = PcmVisualizerProcessor()
    val aiAutoEqEngine = AiAutoEqEngine().apply {
        bindProcessor(eqAudioProcessor)
    }

    @Volatile
    private var sharedPlayer: ExoPlayer? = null

    @Volatile
    private var sharedTrackSelector: DefaultTrackSelector? = null

    @Volatile
    var lastSpatializerEnabled: Boolean = true
        private set

    @Volatile
    private var appliedSpatializerConfig: Pair<Boolean, Boolean>? = null

    fun getOrCreatePlayer(context: android.content.Context): ExoPlayer {
        return sharedPlayer ?: synchronized(this) {
            sharedPlayer ?: createPlayer(context.applicationContext).also { sharedPlayer = it }
        }
    }

    fun applySpatializerSettings(enabled: Boolean, isVideo: Boolean = false) {
        lastSpatializerEnabled = enabled
        val player = sharedPlayer ?: return
        val nextConfig = enabled to isVideo
        if (appliedSpatializerConfig == nextConfig) return
        appliedSpatializerConfig = nextConfig

        val spatialBehavior = if (enabled) {
            C.SPATIALIZATION_BEHAVIOR_AUTO
        } else {
            C.SPATIALIZATION_BEHAVIOR_NEVER
        }
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(if (isVideo) C.AUDIO_CONTENT_TYPE_MOVIE else C.AUDIO_CONTENT_TYPE_MUSIC)
            .setSpatializationBehavior(spatialBehavior)
            .build()
        player.setAudioAttributes(audioAttributes, true)

        sharedTrackSelector?.let { selector ->
            selector.parameters = selector.buildUponParameters()
                .setConstrainAudioChannelCountToDeviceCapabilities(!enabled)
                .setMaxAudioChannelCount(if (enabled) Int.MAX_VALUE else 2)
                .build()
        } ?: run {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setMaxAudioChannelCount(if (enabled) Int.MAX_VALUE else 2)
                .build()
        }
    }

    private fun createPlayer(appContext: android.content.Context): ExoPlayer {
        val renderersFactory = object : DefaultRenderersFactory(appContext) {
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(eqAudioProcessor, TeeAudioProcessor(pcmProcessor)))
                    .build()
            }
        }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val trackSelector = DefaultTrackSelector(appContext).apply {
            parameters = buildUponParameters()
                .setConstrainAudioChannelCountToDeviceCapabilities(false)
                .build()
        }
        sharedTrackSelector = trackSelector

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setSpatializationBehavior(C.SPATIALIZATION_BEHAVIOR_AUTO)
            .build()

        return ExoPlayer.Builder(appContext, renderersFactory)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        aiAutoEqEngine.attachToAudioSession(audioSessionId)
                    }
                })
                if (audioSessionId > 0) {
                    aiAutoEqEngine.attachToAudioSession(audioSessionId)
                }
            }
    }

    fun release() {
        sharedPlayer?.release()
        sharedPlayer = null
        sharedTrackSelector = null
        appliedSpatializerConfig = null
        aiAutoEqEngine.releaseDsp()
    }
}

class M3PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = M3EngineRegistry.getOrCreatePlayer(this)
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
