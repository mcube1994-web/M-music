package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.audio.AiEqState
import com.example.domain.model.AudioTrackOption
import com.example.domain.model.MediaTrack
import com.example.ui.components.FormatPill
import com.example.ui.components.M3GlassCard
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoseLive
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SpatialCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    exoPlayer: ExoPlayer,
    currentTrack: MediaTrack?,
    curatedVideos: List<MediaTrack>,
    audioTrackOptions: List<AudioTrackOption>,
    aiEqState: AiEqState,
    isVideoFullscreen: Boolean,
    onPlayVideo: (MediaTrack) -> Unit,
    onPlayStreamUrl: (String) -> Unit,
    onSelectAudioTrack: (AudioTrackOption) -> Unit,
    onImportLocalVideo: (android.net.Uri) -> Unit,
    onEnterFullscreen: () -> Unit,
    onToggleRotation: () -> Unit,
    onOpenDolbySheet: () -> Unit,
    onOpenEqualizerDialog: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    var streamUrlInput by remember { mutableStateOf("") }
    val localVideoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri -> uri?.let { onImportLocalVideo(it) } }
    )

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("video_lazy_column"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "m³ Video Player",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "HLS .m3u8 • MKV/MP4/WebM • Dolby Vision Fallback",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onOpenEqualizerDialog,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("video_eq_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "10-Band Media3 Equalizer",
                            tint = ElectricViolet
                        )
                    }
                    IconButton(
                        onClick = onOpenDolbySheet,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("video_dolby_diagnostics_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SurroundSound,
                            contentDescription = "Dolby & HLS #EXTM3U Specs",
                            tint = SpatialCyan
                        )
                    }
                }
            }
        }

        // Active Video Surface
        if (currentTrack?.isVideo == true) {
            item {
                M3GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = SpatialCyan.copy(alpha = 0.45f)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color.Black)
                        ) {
                            if (!isVideoFullscreen) {
                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            useController = true
                                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            player = exoPlayer
                                            setFullscreenButtonState(false)
                                            setFullscreenButtonClickListener { _ ->
                                                onEnterFullscreen()
                                            }
                                        }
                                    },
                                    update = { view ->
                                        if (view.player !== exoPlayer) {
                                            view.player = exoPlayer
                                        }
                                        view.setFullscreenButtonState(false)
                                        view.setFullscreenButtonClickListener { _ ->
                                            onEnterFullscreen()
                                        }
                                    },
                                    onRelease = { view ->
                                        view.player = null
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("video_inline_player_view")
                                )
                            }

                            // Explicit Rotate & Fullscreen Controls
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = onToggleRotation,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.62f))
                                        .minimumInteractiveComponentSize()
                                        .testTag("video_rotate_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ScreenRotation,
                                        contentDescription = "Rotate Video Screen",
                                        tint = TextPrimary
                                    )
                                }
                                IconButton(
                                    onClick = onEnterFullscreen,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.62f))
                                        .minimumInteractiveComponentSize()
                                        .testTag("video_fullscreen_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Fullscreen,
                                        contentDescription = "Fullscreen Video",
                                        tint = SpatialCyan
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTrack.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentTrack.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                FormatPill(
                                    text = aiEqState.activeResolvedMode.label,
                                    color = SpatialCyan
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = SaffronGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "AI Auto Cinema EQ • ${currentTrack.hdrTag ?: "Adaptive SDR/HDR"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SaffronGold
                                )
                            }

                            // Multi-track Audio Switching Chips
                            if (audioTrackOptions.isNotEmpty()) {
                                Text(
                                    text = "Audio Tracks (Multi-Track Switching):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    audioTrackOptions.forEach { option ->
                                        FilterChip(
                                            selected = option.isSelected,
                                            onClick = { onSelectAudioTrack(option) },
                                            label = {
                                                Text(
                                                    text = "${option.label} • ${option.codecs}",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ElectricViolet.copy(alpha = 0.25f),
                                                selectedLabelColor = TextPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Direct HLS / MP4 URL & Local Video Picker
        item {
            M3GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Direct Stream URL or Local File",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = streamUrlInput,
                            onValueChange = { streamUrlInput = it },
                            placeholder = {
                                Text(
                                    "Paste .m3u8 or .mp4 stream URL",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpatialCyan,
                                unfocusedBorderColor = GlassBorder,
                                focusedContainerColor = ObsidianSurface,
                                unfocusedContainerColor = ObsidianSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("video_url_input")
                        )
                        Button(
                            onClick = { onPlayStreamUrl(streamUrlInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("video_play_url_btn")
                        ) {
                            Text("Play", color = TextPrimary)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            localVideoPicker.launch(arrayOf("video/mp4", "video/x-matroska", "video/webm", "video/*"))
                        },
                        border = BorderStroke(1.dp, GlassBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("video_local_picker_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = SpatialCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Local Video (.mp4 / .mkv / .webm)",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // "What Do You Want to Play?" Curated Visual Grid
        item {
            Text(
                text = "What Do You Want to Play?",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            Text(
                text = "Tap any movie, animated film, or live HLS channel to launch immediately",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        val chunked = curatedVideos.chunked(2)
        items(chunked.size) { rowIndex ->
            val pair = chunked[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { video ->
                    VideoPosterCard(
                        video = video,
                        isPlayingThis = currentTrack?.id == video.id,
                        onPlay = { onPlayVideo(video) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun VideoPosterCard(
    video: MediaTrack,
    isPlayingThis: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    M3GlassCard(
        modifier = modifier.testTag("video_card_${video.id}"),
        borderColor = if (isPlayingThis) SpatialCyan else GlassBorder,
        onClick = onPlay
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
            ) {
                AsyncImage(
                    model = video.artworkUrl,
                    contentDescription = video.title,
                    fallback = painterResource(video.fallbackDrawableRes),
                    error = painterResource(video.fallbackDrawableRes),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.85f))
                            )
                        )
                )
                if (video.isLiveStream) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(RoseLive)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LiveTv,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = "Play ${video.title}",
                    tint = SpatialCyan,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                )
            }
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isPlayingThis) SpatialCyan else TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FormatPill(
                    text = video.formatBadge,
                    color = if (video.isLiveStream) RoseLive else ElectricViolet
                )
            }
        }
    }
}
