package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.audio.AiEqState
import com.example.domain.model.MediaTrack
import com.example.ui.theme.EmeraldVault
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun M3GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 4.dp
    ) {
        Column(content = content)
    }
}

@Composable
fun M3LogoBadge(
    size: Dp = 42.dp,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val bg = MaterialTheme.colorScheme.background

    val infinite = rememberInfiniteTransition(label = "m3_logo_glow")
    val pulse by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = if (isActive) {
                        listOf(secondary.copy(alpha = pulse), primary)
                    } else {
                        listOf(primary.copy(alpha = 0.65f), secondary.copy(alpha = 0.55f))
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_m3_cube_foreground),
            contentDescription = "m³ 3D glassmorphic cube logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun QuickLaunchStrip(
    onLaunchMusic: () -> Unit,
    onLaunchMovies: () -> Unit,
    onLaunchDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickLaunchCard(
            title = "m³ Music",
            subtitle = "Stream & Hi-Fi",
            accent = primary,
            icon = Icons.Rounded.MusicNote,
            tag = "quick_launch_music",
            onClick = onLaunchMusic,
            modifier = Modifier.weight(1f)
        )
        QuickLaunchCard(
            title = "Movie Player",
            subtitle = "HLS & Atmos",
            accent = secondary,
            icon = Icons.Rounded.Movie,
            tag = "quick_launch_movies",
            onClick = onLaunchMovies,
            modifier = Modifier.weight(1f)
        )
        QuickLaunchCard(
            title = "Music Vault",
            subtitle = "Free FLAC",
            accent = tertiary,
            icon = Icons.Rounded.Download,
            tag = "quick_launch_download",
            onClick = onLaunchDownload,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickLaunchCard(
    title: String,
    subtitle: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    M3GlassCard(
        modifier = modifier
            .testTag(tag)
            .minimumInteractiveComponentSize(),
        borderColor = accent.copy(alpha = 0.42f),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.20f), Color.Transparent)
                    )
                )
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun PersistentMiniPlayer(
    track: MediaTrack,
    isPlaying: Boolean,
    aiEqState: AiEqState,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onExpandStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    M3GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mini_player_bar"),
        borderColor = primary.copy(alpha = 0.45f),
        onClick = onExpandStudio
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(primary.copy(alpha = 0.20f), secondary.copy(alpha = 0.10f))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(44.dp)) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = "${track.title} cover art",
                    fallback = painterResource(track.fallbackDrawableRes),
                    error = painterResource(track.fallbackDrawableRes),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                M3LogoBadge(
                    size = 18.dp,
                    isActive = isPlaying,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        color = secondary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = secondary,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = aiEqState.activeResolvedMode.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = secondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("mini_player_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = TextPrimary
                )
            }

            IconButton(
                onClick = onSkipNext,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("mini_player_next")
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next track",
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
fun TrackRowCard(
    track: MediaTrack,
    isActive: Boolean,
    isSavedInVault: Boolean,
    onPlay: () -> Unit,
    onDownload: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    M3GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("track_card_${track.id}"),
        borderColor = if (isActive) primary else MaterialTheme.colorScheme.outline,
        onClick = onPlay
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(54.dp)) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = "${track.title} artwork",
                    fallback = painterResource(track.fallbackDrawableRes),
                    error = painterResource(track.fallbackDrawableRes),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Now Playing",
                            tint = secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isActive) secondary else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • ${track.language.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormatPill(
                        text = track.backend.displayName,
                        color = primary
                    )
                    FormatPill(
                        text = track.formatBadge,
                        color = if (track.isLossless) tertiary else secondary
                    )
                }
            }

            if (onDownload != null) {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("download_btn_${track.id}")
                ) {
                    Icon(
                        imageVector = if (isSavedInVault) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                        contentDescription = if (isSavedInVault) "Saved in Offline Vault" else "Save to Offline Vault",
                        tint = if (isSavedInVault) EmeraldVault else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun FormatPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SpatialHeaderActionPill(
    isSweepPlaying: Boolean,
    spatializerReady: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val secondary = MaterialTheme.colorScheme.secondary
    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .testTag("spatial_sweep_pill"),
        shape = CircleShape,
        color = if (isSweepPlaying) secondary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (isSweepPlaying || spatializerReady) secondary.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SurroundSound,
                contentDescription = "7.1.4 Dolby Spatial Sweep",
                tint = secondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isSweepPlaying) "7.1.4 Sweep..." else "7.1.4 Atmos",
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
