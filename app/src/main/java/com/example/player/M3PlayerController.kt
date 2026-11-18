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
import com.example.domain.model.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class PlaybackUiState(
    val currentTrack: MediaTrack? = null,
    val queue: List<MediaTrack> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
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

    private val activeQueue = mutableListOf<MediaTrack>()
    private val playedTrackIds = mutableSetOf<String>()

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
                if (playbackState == Player.STATE_ENDED) {
                    handlePlaybackEnded()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = exoPlayer.currentMediaItemIndex
                val track = activeQueue.getOrNull(idx)
                if (track != null) {
                    playedTrackIds.add(track.id)
                    _playbackState.value = _playbackState.value.copy(
                        currentTrack = track,
                        currentIndex = idx,
                        queue = activeQueue.toList(),
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

        // Position progress ticker
        scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.takeIf { it > 0 && it != C.TIME_UNSET }
                        ?: _playbackState.value.currentTrack?.durationMs ?: 0L
                    _playbackState.value = _playbackState.value.copy(
                        positionMs = pos,
                        durationMs = dur
                    )
                }
                delay(250L)
            }
        }
    }

    private fun handlePlaybackEnded() {
        val state = _playbackState.value
        when {
            state.repeatMode == RepeatMode.REPEAT_ONE && state.currentTrack != null -> {
                exoPlayer.seekTo(0L)
                exoPlayer.play()
            }
            state.shuffleEnabled && activeQueue.size > 1 -> {
                val nextIdx = pickRandomUnplayedIndex(allowCycleReset = state.repeatMode == RepeatMode.REPEAT_ALL)
                if (nextIdx >= 0) {
                    jumpToQueueIndex(nextIdx)
                }
            }
        }
    }

    private fun buildMediaItem(item: MediaTrack): MediaItem {
        val artworkUri = item.artworkUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
            ?: Uri.parse("android.resource://${context.packageName}/${item.fallbackDrawableRes}")

        val metadata = MediaMetadata.Builder()
            .setTitle(item.title)
            .setDisplayTitle(item.title)
            .setArtist(item.artist)
            .setSubtitle(item.subtitle ?: item.artist)
            .setAlbumTitle(item.album.ifBlank { item.backend.displayName })
            .setArtworkUri(artworkUri)
            .build()

        val builder = MediaItem.Builder()
            .setMediaId(item.id)
            .setUri(item.streamUrl)
            .setMediaMetadata(metadata)

        if (item.streamUrl.contains(".m3u8", ignoreCase = true)) {
            builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        }
        return builder.build()
    }

    private fun ensureServiceStarted() {
        runCatching {
            val serviceIntent = Intent(context, M3PlaybackService::class.java)
            context.startService(serviceIntent)
        }
    }

    fun playTrack(track: MediaTrack, queue: List<MediaTrack> = listOf(track)) {
        ensureServiceStarted()

        val normalizedQueue = if (queue.any { it.id == track.id }) queue else listOf(track) + queue
        activeQueue.clear()
        activeQueue.addAll(normalizedQueue)
        playedTrackIds.clear()
        playedTrackIds.add(track.id)

        val startIndex = activeQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val mediaItems = activeQueue.map { buildMediaItem(it) }

        exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.play()

        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            queue = activeQueue.toList(),
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

    fun addToQueue(track: MediaTrack) {
        if (activeQueue.isEmpty()) {
            playTrack(track, listOf(track))
            return
        }
        activeQueue.add(track)
        exoPlayer.addMediaItem(buildMediaItem(track))
        _playbackState.value = _playbackState.value.copy(
            queue = activeQueue.toList()
        )
    }

    fun playNext(track: MediaTrack) {
        if (activeQueue.isEmpty()) {
            playTrack(track, listOf(track))
            return
        }
        val curIdx = _playbackState.value.currentIndex.coerceIn(0, activeQueue.lastIndex)
        val insertIdx = (curIdx + 1).coerceIn(0, activeQueue.size)
        activeQueue.add(insertIdx, track)
        exoPlayer.addMediaItem(insertIdx, buildMediaItem(track))
        _playbackState.value = _playbackState.value.copy(
            queue = activeQueue.toList()
        )
    }

    fun jumpToQueueIndex(index: Int) {
        if (index !in activeQueue.indices) return
        ensureServiceStarted()
        val track = activeQueue[index]
        playedTrackIds.add(track.id)
        exoPlayer.seekTo(index, 0L)
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }
        exoPlayer.playWhenReady = true
        exoPlayer.play()
        _playbackState.value = _playbackState.value.copy(
            currentTrack = track,
            currentIndex = index,
            queue = activeQueue.toList(),
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

    fun removeFromQueue(index: Int) {
        if (index !in activeQueue.indices) return
        val curIdx = _playbackState.value.currentIndex
        activeQueue.removeAt(index)
        exoPlayer.removeMediaItem(index)

        if (activeQueue.isEmpty()) {
            exoPlayer.stop()
            playedTrackIds.clear()
            _playbackState.value = _playbackState.value.copy(
                currentTrack = null,
                queue = emptyList(),
                currentIndex = -1,
                isPlaying = false,
                positionMs = 0L
            )
            return
        }

        val nextIdx = when {
            index < curIdx -> (curIdx - 1).coerceIn(0, activeQueue.lastIndex)
            index == curIdx -> curIdx.coerceIn(0, activeQueue.lastIndex)
            else -> curIdx.coerceIn(0, activeQueue.lastIndex)
        }
        val nextTrack = activeQueue.getOrNull(nextIdx)
        _playbackState.value = _playbackState.value.copy(
            currentTrack = nextTrack,
            currentIndex = nextIdx,
            queue = activeQueue.toList()
        )
    }

    fun clearQueue() {
        if (activeQueue.isEmpty()) return
        val curIdx = _playbackState.value.currentIndex
        val current = activeQueue.getOrNull(curIdx)
        if (current != null && curIdx in activeQueue.indices) {
            if (curIdx + 1 < exoPlayer.mediaItemCount) {
                exoPlayer.removeMediaItems(curIdx + 1, exoPlayer.mediaItemCount)
            }
            if (curIdx > 0) {
                exoPlayer.removeMediaItems(0, curIdx)
            }
            activeQueue.clear()
            activeQueue.add(current)
            playedTrackIds.clear()
            playedTrackIds.add(current.id)
            _playbackState.value = _playbackState.value.copy(
                currentTrack = current,
                currentIndex = 0,
                queue = activeQueue.toList()
            )
        } else {
            activeQueue.clear()
            playedTrackIds.clear()
            exoPlayer.clearMediaItems()
            _playbackState.value = _playbackState.value.copy(
                currentTrack = null,
                currentIndex = -1,
                queue = emptyList(),
                isPlaying = false
            )
        }
    }

    fun toggleShuffle() {
        val next = !_playbackState.value.shuffleEnabled
        exoPlayer.shuffleModeEnabled = next
        if (next) {
            playedTrackIds.clear()
            _playbackState.value.currentTrack?.let { playedTrackIds.add(it.id) }
        }
        _playbackState.value = _playbackState.value.copy(shuffleEnabled = next)
    }

    fun cycleRepeatMode() {
        val next = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.REPEAT_ALL
            RepeatMode.REPEAT_ALL -> RepeatMode.REPEAT_ONE
            RepeatMode.REPEAT_ONE -> RepeatMode.OFF
        }
        exoPlayer.repeatMode = when (next) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.REPEAT_ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
        }
        _playbackState.value = _playbackState.value.copy(repeatMode = next)
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
        val state = _playbackState.value
        if (state.repeatMode == RepeatMode.REPEAT_ONE && state.currentTrack != null) {
            exoPlayer.seekTo(0L)
            exoPlayer.play()
            return
        }
        if (state.shuffleEnabled && activeQueue.size > 1) {
            val nextIdx = pickRandomUnplayedIndex(allowCycleReset = true)
            if (nextIdx >= 0) {
                jumpToQueueIndex(nextIdx)
                return
            }
        }
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        } else if (state.repeatMode == RepeatMode.REPEAT_ALL && activeQueue.isNotEmpty()) {
            jumpToQueueIndex(0)
        }
    }

    fun skipPrevious() {
        if (exoPlayer.currentPosition > 3500L) {
            exoPlayer.seekTo(0L)
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        } else if (_playbackState.value.repeatMode == RepeatMode.REPEAT_ALL && activeQueue.isNotEmpty()) {
            jumpToQueueIndex(activeQueue.lastIndex)
        }
    }

    private fun pickRandomUnplayedIndex(allowCycleReset: Boolean): Int {
        val curIdx = _playbackState.value.currentIndex
        val unplayed = activeQueue.indices.filter { idx ->
            idx != curIdx && !playedTrackIds.contains(activeQueue[idx].id)
        }
        if (unplayed.isNotEmpty()) {
            return unplayed[Random.nextInt(unplayed.size)]
        }
        if (!allowCycleReset || activeQueue.size <= 1) return -1
        playedTrackIds.clear()
        _playbackState.value.currentTrack?.let { playedTrackIds.add(it.id) }
        val pool = activeQueue.indices.filter { it != curIdx }
        return if (pool.isNotEmpty()) pool[Random.nextInt(pool.size)] else -1
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
