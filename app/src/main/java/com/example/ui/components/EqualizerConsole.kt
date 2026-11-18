package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.AiEqState
import com.example.domain.model.AiEqMode
import com.example.ui.theme.EmeraldVault
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TenBandEqualizerCard(
    aiEqState: AiEqState,
    eqBandLabels: List<String>,
    onToggleEqEnabled: (Boolean) -> Unit,
    onSelectEqMode: (AiEqMode) -> Unit,
    onChangeEqBand: (Int, Float) -> Unit,
    onResetFlat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    M3GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ten_band_eq_card"),
        borderColor = if (aiEqState.isEqEnabled) colorScheme.primary.copy(alpha = 0.45f) else colorScheme.outline
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header + ExoPlayer Session DSP Badge + Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = if (aiEqState.isEqEnabled) colorScheme.secondary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "10-Band Media3 Equalizer",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = if (aiEqState.audioSessionId > 0) {
                                "ExoPlayer Session #${aiEqState.audioSessionId} • ${aiEqState.dspEngineName}"
                            } else {
                                "${aiEqState.dspEngineName} • 31Hz–16kHz"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (aiEqState.isHardwareDspAttached) EmeraldVault else TextSecondary
                        )
                    }
                }

                Switch(
                    checked = aiEqState.isEqEnabled,
                    onCheckedChange = onToggleEqEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = colorScheme.primary
                    ),
                    modifier = Modifier.testTag("eq_master_switch")
                )
            }

            // Live 10-Band Frequency Curve Preview
            EqCurvePreviewCanvas(
                bandGainsDb = aiEqState.bandGainsDb,
                isEnabled = aiEqState.isEqEnabled,
                primary = colorScheme.primary,
                secondary = colorScheme.secondary,
                outline = colorScheme.outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            )

            // Preset Chips + Reset Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AiEqMode.entries.forEach { mode ->
                        val selected = aiEqState.selectedMode == mode ||
                            (aiEqState.selectedMode == AiEqMode.AUTO_DETECT && aiEqState.activeResolvedMode == mode)
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectEqMode(mode) },
                            label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.primary.copy(alpha = 0.25f),
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("eq_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                OutlinedButton(
                    onClick = onResetFlat,
                    border = BorderStroke(1.dp, colorScheme.outline),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("eq_reset_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = "Reset Equalizer to Flat 0 dB",
                        tint = colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("0 dB", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = aiEqState.detectedContentLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.secondary
                    )
                }
                Text(
                    text = "-12 dB to +12 dB",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            // 10 Frequency Band Sliders (31Hz .. 16kHz)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                aiEqState.bandGainsDb.forEachIndexed { idx, gainDb ->
                    val freqText = "${eqBandLabels.getOrElse(idx) { "" }}Hz"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = freqText,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(46.dp)
                        )
                        Slider(
                            value = gainDb,
                            onValueChange = { onChangeEqBand(idx, it) },
                            valueRange = -12f..12f,
                            enabled = aiEqState.isEqEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("eq_band_slider_$idx"),
                            colors = SliderDefaults.colors(
                                thumbColor = colorScheme.secondary,
                                activeTrackColor = colorScheme.primary,
                                inactiveTrackColor = colorScheme.surface
                            )
                        )
                        Text(
                            text = "${if (gainDb >= 0) "+" else ""}${"%.1f".format(gainDb)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (gainDb > 0.2f) colorScheme.secondary else TextSecondary,
                            modifier = Modifier
                                .width(42.dp)
                                .testTag("eq_band_value_$idx")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EqCurvePreviewCanvas(
    bandGainsDb: List<Float>,
    isEnabled: Boolean,
    primary: Color,
    secondary: Color,
    outline: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Canvas(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF05070D))
            .border(1.dp, outline, shape)
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        // 0 dB center reference line & +6 / -6 dB guide lines
        drawLine(outline.copy(alpha = 0.6f), Offset(0f, midY), Offset(w, midY), strokeWidth = 1f)
        drawLine(outline.copy(alpha = 0.25f), Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = 1f)
        drawLine(outline.copy(alpha = 0.25f), Offset(0f, h * 0.75f), Offset(w, h * 0.75f), strokeWidth = 1f)

        if (bandGainsDb.isEmpty()) return@Canvas
        val padX = 12.dp.toPx()
        val usableW = (w - padX * 2f).coerceAtLeast(1f)
        val stepX = usableW / (bandGainsDb.size - 1).coerceAtLeast(1)

        val points = bandGainsDb.mapIndexed { idx, gain ->
            val effective = if (isEnabled) gain else 0f
            val norm = (effective / 12f).coerceIn(-1f, 1f)
            Offset(
                x = padX + idx * stepX,
                y = midY - norm * (h * 0.40f)
            )
        }

        val strokePath = Path()
        val fillPath = Path()
        points.forEachIndexed { idx, pt ->
            if (idx == 0) {
                strokePath.moveTo(pt.x, pt.y)
                fillPath.moveTo(pt.x, h)
                fillPath.lineTo(pt.x, pt.y)
            } else {
                val prev = points[idx - 1]
                val cx = (prev.x + pt.x) / 2f
                strokePath.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
                fillPath.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y)
            }
        }
        fillPath.lineTo(points.last().x, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                listOf(
                    (if (isEnabled) secondary else TextMuted).copy(alpha = 0.26f),
                    Color.Transparent
                )
            )
        )
        drawPath(
            path = strokePath,
            brush = Brush.horizontalGradient(
                if (isEnabled) listOf(primary, secondary) else listOf(TextMuted, TextMuted)
            ),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
        points.forEach { pt ->
            drawCircle(
                color = if (isEnabled) secondary else TextMuted,
                radius = 3.5.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
fun EqualizerDialog(
    aiEqState: AiEqState,
    eqBandLabels: List<String>,
    onToggleEqEnabled: (Boolean) -> Unit,
    onSelectEqMode: (AiEqMode) -> Unit,
    onChangeEqBand: (Int, Float) -> Unit,
    onResetFlat: () -> Unit,
    onDismiss: () -> Unit
) {
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
            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.45f)),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Media3 Equalizer Console",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("eq_dialog_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Equalizer",
                            tint = TextSecondary
                        )
                    }
                }

                HorizontalDivider(
                    color = colorScheme.outline,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    TenBandEqualizerCard(
                        aiEqState = aiEqState,
                        eqBandLabels = eqBandLabels,
                        onToggleEqEnabled = onToggleEqEnabled,
                        onSelectEqMode = onSelectEqMode,
                        onChangeEqBand = onChangeEqBand,
                        onResetFlat = onResetFlat
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("eq_dialog_done_btn")
                ) {
                    Text("Done", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
