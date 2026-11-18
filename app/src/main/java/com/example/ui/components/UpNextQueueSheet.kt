package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.domain.model.MediaTrack
import com.example.domain.model.RepeatMode
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoseLive
import com.example.ui.theme.SpatialCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun UpNextQueueSheet(
    currentTrack: MediaTrack?,
    queue: List<MediaTrack>,
    currentIndex: Int,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onJumpToIndex: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val indexedQueue = queue.mapIndexed { idx, track -> idx to track }
    val upcomingEntries = if (currentIndex in queue.indices) {
        indexedQueue.filter { (idx, _) -> idx > currentIndex }
    } else {
        indexedQueue
    }
    val previousEntries = if (currentIndex in queue.indices && currentIndex > 0) {
        indexedQueue.filter { (idx, _) -> idx < currentIndex }
    } else {
        emptyList()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .testTag("up_next_queue_sheet"),
            shape = RoundedCornerShape(24.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = null,
                            tint = SpatialCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Up Next Queue",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "${upcomingEntries.size} upcoming • ${queue.size} total in queue",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("queue_sheet_close")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Queue Drawer",
                            tint = TextSecondary
                        )
                    }
                }

                // Quick Shuffle, Repeat & Clear Queue Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onToggleShuffle,
                        border = BorderStroke(
                            1.dp,
                            if (shuffleEnabled) SpatialCyan else GlassBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("queue_shuffle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Toggle Shuffle",
                            tint = if (shuffleEnabled) SpatialCyan else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (shuffleEnabled) "Shuffle On" else "Shuffle",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (shuffleEnabled) SpatialCyan else TextPrimary
                        )
                    }

                    OutlinedButton(
                        onClick = onCycleRepeat,
                        border = BorderStroke(
                            1.dp,
                            if (repeatMode != RepeatMode.OFF) colorScheme.primary else GlassBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("queue_repeat_btn")
                    ) {
                        Icon(
                            imageVector = if (repeatMode == RepeatMode.REPEAT_ONE) {
                                Icons.Rounded.RepeatOne
                            } else {
                                Icons.Rounded.Repeat
                            },
                            contentDescription = "Cycle Repeat Mode",
                            tint = if (repeatMode != RepeatMode.OFF) colorScheme.primary else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = repeatMode.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (repeatMode != RepeatMode.OFF) colorScheme.primary else TextPrimary
                        )
                    }

                    Button(
                        onClick = onClearQueue,
                        enabled = upcomingEntries.isNotEmpty() || previousEntries.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoseLive.copy(alpha = 0.2f),
                            contentColor = RoseLive
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("queue_clear_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear Queue",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Queue", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Now Playing Card at the top
                if (currentTrack != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "NOW PLAYING",
                            style = MaterialTheme.typography.labelSmall,
                            color = SpatialCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("queue_now_playing_card"),
                            shape = RoundedCornerShape(16.dp),
                            color = colorScheme.primary.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, SpatialCyan.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.size(48.dp)) {
                                    AsyncImage(
                                        model = currentTrack.artworkUrl,
                                        contentDescription = currentTrack.title,
                                        fallback = painterResource(currentTrack.fallbackDrawableRes),
                                        error = painterResource(currentTrack.fallbackDrawableRes),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ObsidianSurface.copy(alpha = 0.45f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.GraphicEq,
                                            contentDescription = "Playing",
                                            tint = SpatialCyan,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentTrack.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${currentTrack.artist} • ${currentTrack.backend.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = GlassBorder)

                // Upcoming & Remaining Queued Tracks
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .testTag("queue_lazy_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (upcomingEntries.isEmpty() && previousEntries.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Up Next is empty. Tap + Queue or Play Next on any track!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    if (upcomingEntries.isNotEmpty()) {
                        item {
                            Text(
                                text = "UP NEXT",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(upcomingEntries, key = { (idx, t) -> "up_${idx}_${t.id}" }) { (queueIdx, track) ->
                            QueueTrackRow(
                                queueIndex = queueIdx,
                                track = track,
                                onJump = { onJumpToIndex(queueIdx) },
                                onRemove = { onRemoveAt(queueIdx) }
                            )
                        }
                    }

                    if (previousEntries.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "EARLIER IN QUEUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(previousEntries, key = { (idx, t) -> "prev_${idx}_${t.id}" }) { (queueIdx, track) ->
                            QueueTrackRow(
                                queueIndex = queueIdx,
                                track = track,
                                onJump = { onJumpToIndex(queueIdx) },
                                onRemove = { onRemoveAt(queueIdx) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueTrackRow(
    queueIndex: Int,
    track: MediaTrack,
    onJump: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onJump)
            .testTag("queue_item_$queueIndex"),
        shape = RoundedCornerShape(14.dp),
        color = ObsidianElevated,
        border = BorderStroke(0.5.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                fallback = painterResource(track.fallbackDrawableRes),
                error = painterResource(track.fallbackDrawableRes),
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onJump,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play ${track.title} now",
                    tint = SpatialCyan
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("queue_remove_$queueIndex")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove ${track.title} from queue",
                    tint = TextSecondary
                )
            }
        }
    }
}
