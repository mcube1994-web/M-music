package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.audio.AiEqState
import com.example.audio.VisualizerTelemetry
import com.example.domain.model.AiEqMode
import com.example.domain.model.SyncedLyricLine
import com.example.domain.model.VisualizerMode
import com.example.domain.model.VisualizerPalette
import com.example.player.PlaybackUiState
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StudioPlayerSheet(
    playbackState: PlaybackUiState,
    visualizerMode: VisualizerMode,
    visualizerPalette: VisualizerPalette,
    telemetry: VisualizerTelemetry,
    aiEqState: AiEqState,
    eqBandLabels: List<String>,
    lyrics: List<SyncedLyricLine>,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSelectVisualizerMode: (VisualizerMode) -> Unit,
    onSelectVisualizerPalette: (VisualizerPalette) -> Unit,
    onToggleEqEnabled: (Boolean) -> Unit,
    onSelectEqMode: (AiEqMode) -> Unit,
    onChangeEqBand: (Int, Float) -> Unit,
    onResetEqFlat: () -> Unit,
    onOpenDolbyDiagnostics: () -> Unit,
    onOpenAppearanceDialog: () -> Unit
) {
    val track = playbackState.currentTrack ?: return
    val colorScheme = MaterialTheme.colorScheme
    var showLyrics by remember { mutableStateOf(false) }
    var isStageFullscreen by remember { mutableStateOf(false) }

    BackHandler {
        if (isStageFullscreen) {
            isStageFullscreen = false
        } else {
            onDismiss()
        }
    }

    if (isStageFullscreen) {
        // vizz.fm-inspired distraction-free full-screen visualizer stage
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020308))
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            RichAudioVisualizerCanvas(
                mode = visualizerMode,
                palette = visualizerPalette,
                telemetry = telemetry,
                isPlaying = playbackState.isPlaying,
                roundedCornerDp = 0,
                enableLiveWebView = true,
                modifier = Modifier.fillMaxSize()
            )

            // Minimal floating overlay controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.58f),
                    border = BorderStroke(1.dp, colorScheme.secondary.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${visualizerMode.label} • ${visualizerPalette.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.secondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.58f))
                            .minimumInteractiveComponentSize()
                            .testTag("studio_stage_play_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = { isStageFullscreen = false },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.58f))
                            .minimumInteractiveComponentSize()
                            .testTag("studio_stage_exit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FullscreenExit,
                            contentDescription = "Exit Full-Screen Visualizer Stage",
                            tint = colorScheme.secondary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(14.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisualizerMode.entries.forEach { mode ->
                    FilterChip(
                        selected = mode == visualizerMode,
                        onClick = { onSelectVisualizerMode(mode) },
                        label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Black.copy(alpha = 0.60f),
                            labelColor = TextSecondary,
                            selectedContainerColor = colorScheme.secondary.copy(alpha = 0.28f),
                            selectedLabelColor = colorScheme.secondary
                        )
                    )
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colorScheme.primary.copy(alpha = 0.28f),
                        colorScheme.background,
                        Color(0xFF04060B)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Studio Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("studio_collapse_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Minimize Studio Player",
                        tint = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "m³ STUDIO ENGINE",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.secondary
                    )
                    Text(
                        text = track.formatBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = { showLyrics = !showLyrics },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("studio_lyrics_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lyrics,
                            contentDescription = "Toggle Synced Lyrics",
                            tint = if (showLyrics) colorScheme.secondary else TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onOpenAppearanceDialog,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("studio_appearance_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = "Skins & Visualizers",
                            tint = colorScheme.tertiary
                        )
                    }
                    IconButton(
                        onClick = onOpenDolbyDiagnostics,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("studio_dolby_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SurroundSound,
                            contentDescription = "Dolby & Codec Capabilities",
                            tint = colorScheme.primary
                        )
                    }
                }
            }

            // Cover Art OR Synced Lyrics Panel
            if (showLyrics) {
                M3GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    borderColor = colorScheme.secondary.copy(alpha = 0.35f)
                ) {
                    if (lyrics.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Searching LRCLIB synced lyrics for ${track.title}...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(lyrics) { line ->
                                val isCurrent = playbackState.positionMs >= line.timeMs &&
                                    (lyrics.firstOrNull { it.timeMs > line.timeMs }?.timeMs ?: Long.MAX_VALUE) > playbackState.positionMs
                                Text(
                                    text = line.text,
                                    style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                    color = if (isCurrent) colorScheme.secondary else TextSecondary,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = "${track.title} Studio Cover",
                        fallback = painterResource(track.fallbackDrawableRes),
                        error = painterResource(track.fallbackDrawableRes),
                        modifier = Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FormatPill(track.backend.displayName, colorScheme.primary)
                            FormatPill("${telemetry.channelCount}ch PCM", colorScheme.secondary)
                        }
                    }
                }
            }

            // vizz.fm 3D & WebGL Visualizer + Mode & Palette Switcher
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "vizz.fm • ${visualizerMode.label.uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${visualizerMode.badge} • Peak ${"%.1f".format(telemetry.peakDb)} dBFS",
                            style = MaterialTheme.typography.labelSmall,
                            color = SaffronGold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSelectVisualizerMode(VisualizerMode.VIZZ_LIVE_WEB)
                                }
                                .testTag("studio_vizz_live_btn"),
                            color = colorScheme.secondary.copy(alpha = 0.14f),
                            border = BorderStroke(0.5.dp, colorScheme.secondary.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenInNew,
                                    contentDescription = "Launch vizz.fm/app Live WebGL",
                                    tint = colorScheme.secondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Live vizz.fm",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.secondary
                                )
                            }
                        }

                        IconButton(
                            onClick = { isStageFullscreen = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("studio_stage_mode_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fullscreen,
                                contentDescription = "Full-Screen Visualizer Stage",
                                tint = colorScheme.secondary
                            )
                        }
                    }
                }

                RichAudioVisualizerCanvas(
                    mode = visualizerMode,
                    palette = visualizerPalette,
                    telemetry = telemetry,
                    isPlaying = playbackState.isPlaying,
                    enableLiveWebView = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (visualizerMode == VisualizerMode.VIZZ_LIVE_WEB) 500.dp else 190.dp)
                )

                // vizz.fm Visualizers
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VisualizerMode.entries.forEach { mode ->
                        FilterChip(
                            selected = mode == visualizerMode,
                            onClick = { onSelectVisualizerMode(mode) },
                            label = { Text(mode.label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.secondary.copy(alpha = 0.22f),
                                selectedLabelColor = colorScheme.secondary
                            ),
                            modifier = Modifier.testTag("vis_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                // 5 Visualizer Color Palettes
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VisualizerPalette.entries.forEach { pal ->
                        FilterChip(
                            selected = pal == visualizerPalette,
                            onClick = { onSelectVisualizerPalette(pal) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(pal.primaryHex))
                                        .border(1.dp, Color(pal.secondaryHex), CircleShape)
                                )
                            },
                            label = { Text(pal.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.primary.copy(alpha = 0.22f),
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("studio_palette_${pal.name.lowercase()}")
                        )
                    }
                }
            }

            // Seek Slider & Transport Controls
            Column {
                val dur = playbackState.durationMs.coerceAtLeast(1L)
                val pos = playbackState.positionMs.coerceIn(0L, dur)
                Slider(
                    value = pos.toFloat() / dur.toFloat(),
                    onValueChange = { frac -> onSeekTo((frac * dur).toLong()) },
                    colors = SliderDefaults.colors(
                        thumbColor = colorScheme.secondary,
                        activeTrackColor = colorScheme.primary,
                        inactiveTrackColor = colorScheme.surfaceVariant
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(pos), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(formatTime(dur), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("studio_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = TextPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Surface(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onTogglePlayPause)
                            .testTag("studio_play_pause_button"),
                        shape = CircleShape,
                        color = colorScheme.primary,
                        border = BorderStroke(1.5.dp, colorScheme.secondary.copy(alpha = 0.65f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = TextPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("studio_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = TextPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            // 10-Band Media3 Equalizer Console
            TenBandEqualizerCard(
                aiEqState = aiEqState,
                eqBandLabels = eqBandLabels,
                onToggleEqEnabled = onToggleEqEnabled,
                onSelectEqMode = onSelectEqMode,
                onChangeEqBand = onChangeEqBand,
                onResetFlat = onResetEqFlat
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
