package com.example.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.domain.model.AudioTrackOption
import com.example.domain.model.MediaTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val currentTrack: MediaTrack? = null,
    val queue: List<MediaTrack> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val audioTracks: List<AudioTrackOption> = emptyList(),
    val errorMessage: String? = null
)

@OptIn(UnstableApi::class)
class M3PlayerController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val exoPlayer: ExoPlayer = M3EngineRegistry.getOrCreatePlayer(context)

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private var activeQueue: List<MediaTrack> = emptyList()

    init {
        if (exoPlayer.audioSessionId > 0) {
            M3EngineRegistry.aiAutoEqEngine.attachToAudioSession(exoPlayer.audioSessionId)
        }
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                M3EngineRegistry.aiAutoEqEngine.attachToAudioSession(audioSessionId)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                if (!isPlaying) {
                    M3EngineRegistry.pcmProcessor.emitIdleDecay()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val buffering = playbackState == Player.STATE_BUFFERING
                val dur = exoPlayer.duration.takeIf { it > 0 && it != C.TIME_UNSET }
                    ?: _playbackState.value.currentTrack?.durationMs ?: 0L
                _playbackState.value = _playbackState.value.copy(
                    isBuffering = buffering,
                    durationMs = dur
                )
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = exoPlayer.currentMediaItemIndex
                val track = activeQueue.getOrNull(idx)
                if (track != null) {
                    _playbackState.value = _playbackState.value.copy(
                        currentTrack = track,
                        currentIndex = idx,
                        errorMessage = null
                    )
                    M3EngineRegistry.aiAutoEqEngine.analyzeAndAdapt(
                        track,
                        M3EngineRegistry.pcmProcessor.telemetry.value
                    )
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val options = mutableListOf<AudioTrackOption>()
                tracks.groups.forEachIndexed { groupIdx, group ->
                    if (group.type == C.TRACK_TYPE_AUDIO) {
                        for (tIdx in 0 until group.length) {
                            val format = group.getTrackFormat(tIdx)
                            val label = format.label
                                ?: format.language?.uppercase()
                                ?: "Audio Track ${tIdx + 1}"
                            val codecs = format.codecs ?: format.sampleMimeType ?: "PCM/AAC"
                            val ch = format.channelCount.coerceAtLeast(2)
                            options.add(
                                AudioTrackOption(
                                    groupIndex = groupIdx,
                                    trackIndex = tIdx,
                                    label = "$label (${ch}ch)",
                                    codecs = codecs,
                                    channels = ch,
                                    isSelected = group.isTrackSelected(tIdx)
                                )
                            )
                        }
                    }
                }
                _playbackState.value = _playbackState.value.copy(audioTracks = options)
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackState.value = _playbackState.value.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = error.localizedMessage ?: "Stream playback error"
                )
            }
        })

        // Position & periodic AI Auto EQ spectral adaptation ticker
        scope.launch {
            var tickCounter = 0
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.takeIf { it > 0 && it != C.TIME_UNSET }
                        ?: _playbackState.value.currentTrack?.durationMs ?: 0L
                    _playbackState.value = _playbackState.value.copy(
                        positionMs = pos,
                        durationMs = dur
                    )
                    tickCounter++
                    if (tickCounter % 4 == 0) {
                        M3EngineRegistry.aiAutoEqEngine.analyzeAndAdapt(
                            _playbackState.value.currentTrack,
                            M3EngineRegistry.pcmProcessor.telemetry.value
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    fun playTrack(track: MediaTrack, queue: List<MediaTrack> = listOf(track)) {
        runCatching {
            val serviceIntent = Intent(context, M3PlaybackService::class.java)
            context.startService(serviceIntent)
        }

        val normalizedQueue = if (queue.any { it.id == track.id }) queue else listOf(track) + queue
        activeQueue = normalizedQueue
        val startIndex = normalizedQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        val mediaItems = normalizedQueue.map { item ->
            val metadata = MediaMetadata.Builder()
                .setTitle(item.title)
                .setArtist(item.artist)
                .setAlbumTitle(item.album)
                .setArtworkUri(item.artworkUrl?.let { Uri.parse(it) })
                .build()

            val builder = MediaItem.Builder()
                .setMediaId(item.id)
                .setUri(item.streamUrl)
                .setMediaMetadata(metadata)

            if (item.streamUrl.contains(".m3u8", ignoreCase = true)) {
                builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            builder.build()
        }

        exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.play()

        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            queue = normalizedQueue,
            currentIndex = startIndex,
            errorMessage = null
        )

        M3EngineRegistry.applySpatializerSettings(
            enabled = M3EngineRegistry.lastSpatializerEnabled,
            isVideo = track.isVideo
        )
        M3EngineRegistry.aiAutoEqEngine.analyzeAndAdapt(
            track,
            M3EngineRegistry.pcmProcessor.telemetry.value
        )
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE && _playbackState.value.currentTrack != null) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
        _playbackState.value = _playbackState.value.copy(positionMs = positionMs)
    }

    fun skipNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    fun skipPrevious() {
        if (exoPlayer.currentPosition > 3500L) {
            exoPlayer.seekTo(0L)
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    fun selectAudioTrack(option: AudioTrackOption) {
        val group = exoPlayer.currentTracks.groups.getOrNull(option.groupIndex) ?: return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(
                TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex)
            )
            .build()
    }

    fun applySpatializerSettings(enabled: Boolean) {
        val isVideo = _playbackState.value.currentTrack?.isVideo == true
        M3EngineRegistry.applySpatializerSettings(enabled = enabled, isVideo = isVideo)
    }

    fun dismissError() {
        _playbackState.value = _playbackState.value.copy(errorMessage = null)
    }
}
