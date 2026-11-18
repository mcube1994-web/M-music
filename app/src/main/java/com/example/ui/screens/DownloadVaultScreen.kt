package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.MediaTrack
import com.example.ui.DownloadUiState
import com.example.ui.components.FormatPill
import com.example.ui.components.M3GlassCard
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldVault
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoseLive
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SpatialCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DownloadVaultScreen(
    state: DownloadUiState,
    vaultTracks: List<MediaTrack>,
    currentTrack: MediaTrack?,
    onQueryChange: (String) -> Unit,
    onSearchLossless: () -> Unit,
    onDownloadTrack: (MediaTrack) -> Unit,
    onPlayTrack: (MediaTrack, List<MediaTrack>) -> Unit,
    onDeleteVaultTrack: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val vaultIds = vaultTracks.map { it.id }.toSet()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Lossless Vault Banner
        item {
            M3GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = SaffronGold.copy(alpha = 0.40f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_lossless_vault_art),
                        contentDescription = "m³ Lossless FLAC Vault",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        ObsidianBg.copy(alpha = 0.92f),
                                        ObsidianBg.copy(alpha = 0.6f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.CenterStart),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FormatPill("FREE FLAC / WAV VAULT", SaffronGold)
                            Text(
                                text = "Offline Lossless Vault (${vaultTracks.size} Saved)",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Internet Archive Masters • Audius • Stream Extractors",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Lossless Search Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search FLAC / WAV masters (e.g. jazz, acoustic, sitar)") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = SaffronGold)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        onSearchLossless()
                    }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SaffronGold,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("download_search_input")
                )

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onSearchLossless()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronGold),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("download_search_btn")
                ) {
                    Text("Find", color = ObsidianBg)
                }
            }
        }

        // Persistent Offline Vault Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Offline Vault",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                FormatPill("${vaultTracks.size} offline", EmeraldVault)
            }
        }

        if (vaultTracks.isEmpty()) {
            item {
                M3GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HighQuality,
                            contentDescription = null,
                            tint = SaffronGold,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Your Offline Vault is empty. Tap Download on any FLAC/WAV master below to play without internet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(vaultTracks, key = { "vault_${it.id}" }) { saved ->
                M3GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_item_${saved.id}"),
                    borderColor = if (currentTrack?.id == saved.id) EmeraldVault else GlassBorder,
                    onClick = { onPlayTrack(saved, vaultTracks) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = saved.artworkUrl,
                            contentDescription = saved.title,
                            fallback = painterResource(saved.fallbackDrawableRes),
                            error = painterResource(saved.fallbackDrawableRes),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = saved.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = saved.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                            FormatPill(saved.formatBadge, EmeraldVault)
                        }
                        IconButton(
                            onClick = { onPlayTrack(saved, vaultTracks) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play offline", tint = SpatialCyan)
                        }
                        IconButton(
                            onClick = { onDeleteVaultTrack(saved.id) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("vault_delete_${saved.id}")
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete from Vault", tint = RoseLive)
                        }
                    }
                }
            }
        }

        // Lossless Download Candidates
        item {
            Text(
                text = "Available Free FLAC / Hi-Res Masters",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        }

        if (state.isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SaffronGold)
                }
            }
        } else {
            items(state.candidates, key = { "dl_${it.id}" }) { track ->
                val dlState = state.activeDownloads[track.id]
                val isSaved = vaultIds.contains(track.id) || dlState?.isCompleted == true

                M3GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (track.isLossless) SaffronGold.copy(alpha = 0.35f) else GlassBorder,
                    onClick = { onPlayTrack(track, state.candidates) }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = track.artworkUrl,
                                contentDescription = track.title,
                                fallback = painterResource(track.fallbackDrawableRes),
                                error = painterResource(track.fallbackDrawableRes),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${track.artist} • ${track.album}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FormatPill(track.backend.displayName, ElectricViolet)
                                    FormatPill(track.formatBadge, if (track.isLossless) SaffronGold else SpatialCyan)
                                }
                            }
                            IconButton(
                                onClick = { if (!isSaved && dlState?.isDownloading != true) onDownloadTrack(track) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dl_save_${track.id}")
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                                    contentDescription = if (isSaved) "Saved" else "Download",
                                    tint = if (isSaved) EmeraldVault else SaffronGold
                                )
                            }
                        }

                        if (dlState?.isDownloading == true) {
                            LinearProgressIndicator(
                                progress = { dlState.progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = SaffronGold,
                                trackColor = ObsidianSurface
                            )
                        } else if (dlState?.error != null) {
                            Text(
                                text = dlState.error,
                                style = MaterialTheme.typography.labelSmall,
                                color = RoseLive
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
