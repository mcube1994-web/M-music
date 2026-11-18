package com.example.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.AppearanceSkin
import com.example.domain.model.VisualizerMode
import com.example.domain.model.VisualizerPalette
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AppearanceSkinDialog(
    currentSkin: AppearanceSkin,
    currentVisualizerMode: VisualizerMode,
    currentPalette: VisualizerPalette,
    onSelectSkin: (AppearanceSkin) -> Unit,
    onSelectVisualizerMode: (VisualizerMode) -> Unit,
    onSelectPalette: (VisualizerPalette) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .heightIn(max = 680.dp)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = colorScheme.surface,
            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.4f)),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Pinned Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = colorScheme.secondary
                        )
                        Column {
                            Text(
                                text = "Appearance & Visualizers",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Levyra DeepSound Skins • vizz.fm 3D & WebGL Visualizers",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("appearance_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Appearance Studio",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(
                    color = colorScheme.outline,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Dynamic App Skin Selector
                    Text(
                        text = "App Theme & Surface Skin",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppearanceSkin.entries.forEach { skin ->
                            val selected = skin == currentSkin
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectSkin(skin) }
                                    .testTag("skin_card_${skin.name.lowercase()}"),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(skin.surfaceHex),
                                border = BorderStroke(
                                    width = if (selected) 1.8.dp else 1.dp,
                                    color = if (selected) Color(skin.secondaryHex) else colorScheme.outline
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(skin.primaryHex))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(skin.secondaryHex))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(skin.tertiaryHex))
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = skin.label,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = skin.tagline,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Selected skin",
                                            tint = Color(skin.secondaryHex)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Built-in Visualizers Grid (9 modes)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "vizz.fm Visualizers (${VisualizerMode.entries.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        FormatPill(currentVisualizerMode.badge, colorScheme.secondary)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VisualizerMode.entries.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { mode ->
                                    val selected = mode == currentVisualizerMode
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onSelectVisualizerMode(mode) }
                                            .minimumInteractiveComponentSize()
                                            .testTag("appearance_vis_${mode.name.lowercase()}"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) colorScheme.primary.copy(alpha = 0.20f) else colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) colorScheme.secondary else colorScheme.outline
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = mode.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) colorScheme.secondary else TextPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = mode.badge,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // 3. Visualizer Color Palettes
                    Text(
                        text = "Visualizer Shader & Glow Palette",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VisualizerPalette.entries.forEach { palette ->
                            val selected = palette == currentPalette
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectPalette(palette) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(palette.primaryHex))
                                            .border(1.dp, Color(palette.secondaryHex), CircleShape)
                                    )
                                },
                                label = { Text(palette.label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.secondary.copy(alpha = 0.22f),
                                    selectedLabelColor = colorScheme.secondary
                                ),
                                modifier = Modifier.testTag("vis_palette_${palette.name.lowercase()}")
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = colorScheme.outline,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // Pinned Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://vizz.fm/app/"))
                                )
                            }
                        },
                        border = BorderStroke(1.dp, colorScheme.secondary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("appearance_open_vizz_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            tint = colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = " vizz.fm/app",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("appearance_done_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = " Apply & Close",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
