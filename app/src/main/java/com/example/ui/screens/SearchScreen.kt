package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.domain.model.MediaTrack
import com.example.domain.model.StreamingBackend
import com.example.ui.SearchUiState
import com.example.ui.components.M3GlassCard
import com.example.ui.components.TrackRowCard
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SpatialCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    state: SearchUiState,
    recentSearches: List<String>,
    currentTrack: MediaTrack?,
    vaultIds: Set<String>,
    onQueryChange: (String) -> Unit,
    onExecuteSearch: (String) -> Unit,
    onSelectBackendFilter: (StreamingBackend?) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onPlayTrack: (MediaTrack, List<MediaTrack>) -> Unit,
    onDownloadTrack: (MediaTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val filteredResults = if (state.selectedBackendFilter == null) {
        state.results
    } else {
        state.results.filter { it.backend == state.selectedBackendFilter }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Unified Multi-Source Search",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "JioSaavn • iTunes IN/Global • Audius • Internet Archive • Navidrome • Vault",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search any artist, movie album, track, or FLAC...") },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = "Search", tint = SpatialCyan)
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.testTag("unified_search_clear")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    onExecuteSearch(state.query)
                }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpatialCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("unified_search_input")
            )
        }

        // Backend Filter Pills
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedBackendFilter == null,
                    onClick = { onSelectBackendFilter(null) },
                    label = { Text("All Sources") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ObsidianElevated,
                        selectedContainerColor = ElectricViolet.copy(alpha = 0.28f),
                        selectedLabelColor = TextPrimary
                    )
                )
                StreamingBackend.entries.forEach { backend ->
                    FilterChip(
                        selected = state.selectedBackendFilter == backend,
                        onClick = { onSelectBackendFilter(backend) },
                        label = { Text(backend.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = ObsidianElevated,
                            selectedContainerColor = SpatialCyan.copy(alpha = 0.22f),
                            selectedLabelColor = SpatialCyan
                        )
                    )
                }
            }
        }

        if (state.query.isBlank() && recentSearches.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Searches",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            items(recentSearches, key = { "recent_$it" }) { recent ->
                M3GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onExecuteSearch(recent) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.History, contentDescription = null, tint = SpatialCyan)
                            Text(recent, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                        IconButton(onClick = { onRemoveRecent(recent) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove recent", tint = TextSecondary)
                        }
                    }
                }
            }
        }

        if (state.isSearching) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpatialCyan)
                }
            }
        } else if (state.query.isNotBlank() && filteredResults.isEmpty()) {
            item {
                M3GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No results found for '${state.query}' across selected sources.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(filteredResults, key = { "search_${it.id}" }) { track ->
                TrackRowCard(
                    track = track,
                    isActive = currentTrack?.id == track.id,
                    isSavedInVault = vaultIds.contains(track.id),
                    onPlay = { onPlayTrack(track, filteredResults) },
                    onDownload = if (!track.isVideo) {
                        { onDownloadTrack(track) }
                    } else null
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
