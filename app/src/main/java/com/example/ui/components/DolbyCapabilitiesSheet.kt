package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.DolbySpatializerEngine
import com.example.domain.model.DolbyCapabilitiesState
import com.example.domain.model.HlsManifestInfo
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldVault
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoseLive
import com.example.ui.theme.SaffronGold
import com.example.ui.theme.SpatialCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DolbyCapabilitiesSheet(
    capabilities: DolbyCapabilitiesState,
    hlsManifestInfo: HlsManifestInfo,
    onToggleSpatializer: (Boolean) -> Unit,
    onWebProbeResult: (Map<String, Boolean>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

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
            color = ObsidianSurface,
            border = BorderStroke(1.dp, SpatialCyan.copy(alpha = 0.45f)),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Lightweight hidden WebView executes navigator.mediaCapabilities.decodingInfo() probe
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    view?.evaluateJavascript(
                                        DolbySpatializerEngine.WEB_MEDIA_CAPABILITIES_PROBE_JS
                                    ) { rawResult ->
                                        runCatching {
                                            val cleaned = rawResult.trim('"').replace("\\\"", "\"")
                                            if (cleaned.startsWith("{")) {
                                                val obj = JSONObject(cleaned)
                                                val map = mutableMapOf<String, Boolean>()
                                                obj.keys().forEach { k -> map[k] = obj.optBoolean(k) }
                                                onWebProbeResult(map)
                                            }
                                        }
                                    }
                                }
                            }
                            loadDataWithBaseURL("https://localhost/", "<html><body></body></html>", "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.size(0.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SurroundSound,
                            contentDescription = null,
                            tint = SpatialCyan
                        )
                        Text(
                            text = "Dolby Spatializer & HLS #EXTM3U",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dolby_sheet_close")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                HorizontalDivider(
                    color = SpatialCyan.copy(alpha = 0.25f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

            // Interactive Android Spatializer Effect Settings Card (AudioManager + Spatializer)
            M3GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (capabilities.userSpatializerEnabled) {
                    SpatialCyan.copy(alpha = 0.55f)
                } else {
                    TextSecondary.copy(alpha = 0.35f)
                }
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = "Headphones Spatializer",
                                tint = if (capabilities.compatibleHeadphonesConnected) EmeraldVault else SpatialCyan
                            )
                            Column {
                                Text(
                                    text = "Android Spatializer Effect",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                val routeText = if (capabilities.compatibleHeadphonesConnected) {
                                    "Connected: ${capabilities.connectedHeadphoneName ?: "Compatible Headphones"}"
                                } else {
                                    "No compatible wired/Bluetooth headphones detected"
                                }
                                Text(
                                    text = routeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (capabilities.compatibleHeadphonesConnected) EmeraldVault else SaffronGold
                                )
                            }
                        }

                        Switch(
                            checked = capabilities.userSpatializerEnabled,
                            onCheckedChange = onToggleSpatializer,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = SpatialCyan
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("spatializer_settings_switch")
                        )
                    }

                    Text(
                        text = if (capabilities.userSpatializerEnabled) {
                            "ExoPlayer Spatialization: AUTO (multi-channel binaural rendering when supported by headphones)"
                        } else {
                            "ExoPlayer Spatialization: NEVER (bypassed to clean stereo output)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FormatPill(
                            text = if (capabilities.spatializerAvailable) "OS Available" else "OS Unavailable",
                            color = if (capabilities.spatializerAvailable) EmeraldVault else TextSecondary
                        )
                        FormatPill(
                            text = if (capabilities.spatializerEnabled) "System ON" else "System OFF",
                            color = if (capabilities.spatializerEnabled) SpatialCyan else SaffronGold
                        )
                        if (capabilities.headTrackerAvailable) {
                            FormatPill(
                                text = "Head Tracking",
                                color = ElectricViolet
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                            }
                        },
                        border = BorderStroke(1.dp, SpatialCyan.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("spatializer_system_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Open Android System Sound Settings",
                            tint = SpatialCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open Android Sound Settings",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Native Android Spatializer & MediaCodec Summary
            M3GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ANDROID SPATIALIZER & MEDIACODEC",
                        style = MaterialTheme.typography.labelMedium,
                        color = SpatialCyan
                    )
                    Text(
                        text = "Immersive Level: ${capabilities.immersiveLevelLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Spatializer Available: ${capabilities.spatializerAvailable} • OS Enabled: ${capabilities.spatializerEnabled} • Can Spatialize 5.1: ${capabilities.canBeSpatialized}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "KEY_MAX_OUTPUT_CHANNEL_COUNT: ${capabilities.maxOutputChannelCount} • Display HDR: ${capabilities.hdrTypesLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SaffronGold
                    )
                }
            }

            // Format Matrix (Dolby Vision & Dolby Atmos / Surround)
            M3GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "CODEC CAPABILITIES (DOLBY VISION / ATMOS)",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricViolet
                    )
                    capabilities.formats.forEach { fmt ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fmt.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = fmt.mimeOrCodec,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (fmt.hardwareSupported) "Hardware" else "Fallback",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (fmt.hardwareSupported) EmeraldVault else TextSecondary
                                )
                                Icon(
                                    imageVector = if (fmt.hardwareSupported) Icons.Rounded.CheckCircle else Icons.Rounded.HighlightOff,
                                    contentDescription = if (fmt.hardwareSupported) "Supported" else "Fallback",
                                    tint = if (fmt.hardwareSupported) EmeraldVault else RoseLive.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // HLS #EXTM3U v7 Multi-Rendition Reference
            M3GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "HLS #EXT-X-VERSION:${hlsManifestInfo.version} MULTI-RENDITION",
                        style = MaterialTheme.typography.labelMedium,
                        color = SaffronGold
                    )
                    hlsManifestInfo.audioRenditions.forEach { audio ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• ${audio.name} (${audio.channels} ch)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            Text(
                                text = audio.uri,
                                style = MaterialTheme.typography.labelSmall,
                                color = SpatialCyan
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    hlsManifestInfo.videoVariants.forEach { variant ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ObsidianBg, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "${variant.resolution} • ${variant.bandwidth / 1_000_000} Mbps -> ${variant.uri}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "CODECS: ${variant.codecs}" +
                                        (variant.supplementalCodecs?.let { " | DV: $it" } ?: ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
