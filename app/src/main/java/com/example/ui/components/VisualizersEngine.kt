package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.RuntimeShader
import android.net.Uri
import android.os.Build
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.audio.VisualizerTelemetry
import com.example.domain.model.VisualizerMode
import com.example.domain.model.VisualizerPalette
import com.example.ui.theme.EmeraldVault
import com.example.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val AGSL_SPHERICAL_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform float iBass;
uniform float iVocal;
uniform float iAir;
uniform float3 iCol1;
uniform float3 iCol2;
uniform float3 iCol3;

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * iResolution.xy) / max(min(iResolution.x, iResolution.y), 1.0);
    float r = length(uv);
    float a = atan(uv.y, uv.x);
    float ripple = 0.055 * sin(6.0 * a + iTime * 2.2) * cos(5.0 * r - iTime * 3.0) * (0.5 + iBass * 1.8);
    float sphereR = 0.34 + iBass * 0.09 + ripple;
    float edge = smoothstep(sphereR + 0.025, sphereR - 0.025, r);
    float halo = exp(-abs(r - sphereR) * (8.0 - iVocal * 3.5));
    float innerPattern = 0.5 + 0.5 * sin(12.0 * uv.x + iTime * 2.0 + sin(10.0 * uv.y - iTime));
    float3 orbCol = mix(iCol1, iCol2, innerPattern);
    orbCol = mix(orbCol, iCol3, clamp(iAir * 1.4, 0.0, 1.0) * (1.0 - r));
    float3 col = orbCol * edge + mix(iCol2, iCol1, 0.5) * halo * 0.9;
    return half4(clamp(col, 0.0, 1.0), 1.0);
}
"""

private const val AGSL_EMBER_ROSE = """
uniform float2 iResolution;
uniform float iTime;
uniform float iBass;
uniform float iVocal;
uniform float iAir;
uniform float3 iCol1;
uniform float3 iCol2;
uniform float3 iCol3;

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * iResolution.xy) / max(min(iResolution.x, iResolution.y), 1.0);
    float r = length(uv);
    float a = atan(uv.y, uv.x);
    float3 finalCol = float3(0.0);
    for (int i = 0; i < 4; i++) {
        float fi = float(i);
        float petals = 5.0 + fi;
        float rose = abs(cos(a * petals * 0.5 + iTime * (0.4 + fi * 0.15) * (mod(fi, 2.0) == 0.0 ? 1.0 : -1.0)));
        float targetR = (0.14 + fi * 0.09) * (0.65 + rose * (0.6 + iVocal * 0.5)) * (1.0 + iBass * 0.35);
        float glow = 0.012 / max(abs(r - targetR), 0.004);
        float3 layerCol = mix(iCol3, iCol1, fi / 3.0);
        layerCol = mix(layerCol, iCol2, iAir * 0.6);
        finalCol += layerCol * glow * 0.55;
    }
    return half4(clamp(finalCol, 0.0, 1.0), 1.0);
}
"""

@Composable
fun RichAudioVisualizerCanvas(
    mode: VisualizerMode,
    palette: VisualizerPalette,
    telemetry: VisualizerTelemetry,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    roundedCornerDp: Int = 16,
    enableLiveWebView: Boolean = false
) {
    val shape = RoundedCornerShape(roundedCornerDp.dp)
    val outline = MaterialTheme.colorScheme.outline
    val secondary = MaterialTheme.colorScheme.secondary

    if (mode == VisualizerMode.VIZZ_LIVE_WEB && enableLiveWebView) {
        VizzFmLiveWebViewStage(
            modifier = modifier,
            shape = shape,
            outline = outline,
            secondary = secondary
        )
        return
    }

    val infinite = rememberInfiniteTransition(label = "vizz_clock")
    val cycle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "vizz_cycle"
    )
    val tRad = cycle * (2f * PI.toFloat())
    val tSec = cycle * 24f

    val col1 = Color(palette.primaryHex)
    val col2 = Color(palette.secondaryHex)
    val col3 = Color(palette.tertiaryHex)
    val bgColor = Color(0xFF04060C)

    val sphericalShader = remember(mode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && mode == VisualizerMode.VIZZ_SPHERICAL_SHADER) {
            runCatching { RuntimeShader(AGSL_SPHERICAL_SHADER) }.getOrNull()
        } else null
    }
    val emberShader = remember(mode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && mode == VisualizerMode.VIZZ_EMBER_ROSE) {
            runCatching { RuntimeShader(AGSL_EMBER_ROSE) }.getOrNull()
        } else null
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(bgColor)
                .border(1.dp, outline, shape)
        ) {
            val w = size.width
            val h = size.height
            if (w <= 1f || h <= 1f) return@Canvas

            val bass = if (isPlaying) telemetry.subBassEnergy else 0.16f + 0.06f * sin(tRad * 4f)
            val vocal = if (isPlaying) telemetry.vocalEnergy else 0.18f + 0.05f * cos(tRad * 3f)
            val air = if (isPlaying) telemetry.brillianceEnergy else 0.14f + 0.05f * sin(tRad * 6f)

            when (mode) {
                VisualizerMode.VIZZ_MESH_GRID, VisualizerMode.VIZZ_LIVE_WEB -> drawVizzMeshGrid(
                    telemetry = telemetry,
                    isPlaying = isPlaying,
                    tRad = tRad,
                    bass = bass,
                    col1 = col1,
                    col2 = col2,
                    col3 = col3
                )

                VisualizerMode.VIZZ_SPHERICAL_SHADER -> {
                    val drawnShader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && sphericalShader != null) {
                        runCatching {
                            sphericalShader.setFloatUniform("iResolution", w, h)
                            sphericalShader.setFloatUniform("iTime", tSec)
                            sphericalShader.setFloatUniform("iBass", bass)
                            sphericalShader.setFloatUniform("iVocal", vocal)
                            sphericalShader.setFloatUniform("iAir", air)
                            sphericalShader.setFloatUniform("iCol1", col1.red, col1.green, col1.blue)
                            sphericalShader.setFloatUniform("iCol2", col2.red, col2.green, col2.blue)
                            sphericalShader.setFloatUniform("iCol3", col3.red, col3.green, col3.blue)
                            drawRect(brush = ShaderBrush(sphericalShader), size = size)
                            true
                        }.getOrDefault(false)
                    } else false

                    if (!drawnShader) {
                        drawSphericalFallback(telemetry, isPlaying, tRad, bass, vocal, col1, col2, col3)
                    }
                }

                VisualizerMode.VIZZ_AUDIO_TUNNEL -> drawVizzAudioTunnel(
                    telemetry = telemetry,
                    isPlaying = isPlaying,
                    cycle = cycle,
                    tRad = tRad,
                    bass = bass,
                    col1 = col1,
                    col2 = col2,
                    col3 = col3
                )

                VisualizerMode.VIZZ_CYCLONE_EYES -> drawVizzCycloneEyes(
                    telemetry = telemetry,
                    isPlaying = isPlaying,
                    tRad = tRad,
                    bass = bass,
                    vocal = vocal,
                    col1 = col1,
                    col2 = col2,
                    col3 = col3
                )

                VisualizerMode.VIZZ_EMBER_ROSE -> {
                    val drawnShader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && emberShader != null) {
                        runCatching {
                            emberShader.setFloatUniform("iResolution", w, h)
                            emberShader.setFloatUniform("iTime", tSec)
                            emberShader.setFloatUniform("iBass", bass)
                            emberShader.setFloatUniform("iVocal", vocal)
                            emberShader.setFloatUniform("iAir", air)
                            emberShader.setFloatUniform("iCol1", col1.red, col1.green, col1.blue)
                            emberShader.setFloatUniform("iCol2", col2.red, col2.green, col2.blue)
                            emberShader.setFloatUniform("iCol3", col3.red, col3.green, col3.blue)
                            drawRect(brush = ShaderBrush(emberShader), size = size)
                            true
                        }.getOrDefault(false)
                    } else false

                    if (!drawnShader) {
                        drawEmberRoseFallback(tRad, bass, vocal, col1, col2, col3)
                    }
                }

                VisualizerMode.VIZZ_GLACIER_SPIRAL -> drawVizzGlacierSpiral(
                    telemetry = telemetry,
                    isPlaying = isPlaying,
                    tRad = tRad,
                    bass = bass,
                    air = air,
                    col1 = col1,
                    col2 = col2,
                    col3 = col3
                )
            }
        }

        if (mode == VisualizerMode.VIZZ_LIVE_WEB) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, secondary.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "vizz.fm/app WebGL • Tap to open Live Stage",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VizzFmLiveWebViewStage(
    modifier: Modifier,
    shape: RoundedCornerShape,
    outline: Color,
    secondary: Color
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pendingPermRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasMicPermission = granted
            val req = pendingPermRequest
            pendingPermRequest = null
            if (req != null) {
                if (granted) req.grant(req.resources) else req.deny()
            }
        }
    )

    val audioFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            pendingFileCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
            pendingFileCallback = null
        }
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF04060C))
            .testTag("vizz_fm_live_webview_stage")
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    webChromeClient = object : WebChromeClient() {
                        override fun onPermissionRequest(request: PermissionRequest) {
                            val needsMic = request.resources.any {
                                it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
                            }
                            if (!needsMic || hasMicPermission) {
                                request.grant(request.resources)
                            } else {
                                pendingPermRequest = request
                                micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }

                        override fun onShowFileChooser(
                            webView: WebView?,
                            filePathCallback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            pendingFileCallback?.onReceiveValue(null)
                            pendingFileCallback = filePathCallback
                            audioFilePicker.launch(arrayOf("audio/*"))
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }
                    }
                    loadUrl("https://vizz.fm/app/")
                    webViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            LinearProgressIndicator(
                color = secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }

        // Compact floating controls: Mic access + Reload + External Browser fallback
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (!hasMicPermission) {
                            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                    .testTag("vizz_mic_perm_pill"),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(
                    0.5.dp,
                    if (hasMicPermission) EmeraldVault.copy(alpha = 0.6f) else secondary.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = if (hasMicPermission) "Microphone Enabled" else "Enable Microphone for vizz.fm",
                        tint = if (hasMicPermission) EmeraldVault else secondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (hasMicPermission) "Mic On" else "Allow Mic",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasMicPermission) EmeraldVault else TextPrimary
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        isLoading = true
                        webViewRef?.reload()
                    }
                    .testTag("vizz_reload_btn"),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(0.5.dp, secondary.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Reload vizz.fm/app",
                    tint = secondary,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(14.dp)
                )
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://vizz.fm/app/"))
                            )
                        }
                    }
                    .testTag("vizz_external_btn"),
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(0.5.dp, secondary.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.OpenInNew,
                    contentDescription = "Open vizz.fm/app in Browser",
                    tint = TextPrimary,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(14.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawVizzMeshGrid(
    telemetry: VisualizerTelemetry,
    isPlaying: Boolean,
    tRad: Float,
    bass: Float,
    col1: Color,
    col2: Color,
    col3: Color
) {
    val w = size.width
    val h = size.height
    val cols = 16
    val rows = 9
    val horizonY = h * 0.18f
    val bottomY = h * 0.96f

    val projected = Array(rows) { r ->
        val depth = (r + 1) / rows.toFloat()
        val persp = 0.35f + 0.65f * depth
        val rowY = horizonY + (bottomY - horizonY) * (depth * depth * 0.65f + depth * 0.35f)
        Array(cols) { c ->
            val nx = (c / (cols - 1).toFloat()) * 2f - 1f
            val binIdx = (c + r * 2) % telemetry.spectrumBins.size
            val bin = if (isPlaying) telemetry.spectrumBins[binIdx] else 0.16f
            val wave = sin(nx * 4.5f + tRad * 6f - depth * 5f) * (0.35f + bass * 0.8f)
            val elevation = (bin * 0.42f + abs(wave) * 0.22f) * h * 0.36f * persp
            Offset(
                x = w * 0.5f + nx * (w * 0.52f) * persp,
                y = rowY - elevation
            )
        }
    }

    for (r in 0 until rows) {
        val depthColor = if (r < rows / 2) col2 else col1
        for (c in 0 until cols) {
            val pt = projected[r][c]
            if (c + 1 < cols) {
                drawLine(
                    color = depthColor.copy(alpha = 0.45f + (r / rows.toFloat()) * 0.45f),
                    start = pt,
                    end = projected[r][c + 1],
                    strokeWidth = 1.4.dp.toPx()
                )
            }
            if (r + 1 < rows) {
                drawLine(
                    color = col3.copy(alpha = 0.28f + (r / rows.toFloat()) * 0.45f),
                    start = pt,
                    end = projected[r + 1][c],
                    strokeWidth = 1.1.dp.toPx()
                )
            }
        }
    }
}

private fun DrawScope.drawSphericalFallback(
    telemetry: VisualizerTelemetry,
    isPlaying: Boolean,
    tRad: Float,
    bass: Float,
    vocal: Float,
    col1: Color,
    col2: Color,
    col3: Color
) {
    val w = size.width
    val h = size.height
    val center = Offset(w / 2f, h / 2f)
    val baseR = minOf(w, h) * (0.34f + bass * 0.14f)

    drawCircle(
        brush = Brush.radialGradient(
            listOf(col1.copy(alpha = 0.55f), col2.copy(alpha = 0.22f), Color.Transparent),
            center = center,
            radius = baseR * 1.45f
        ),
        radius = baseR * 1.45f,
        center = center
    )

    val rings = 6
    val segments = 32
    for (ring in 1 until rings) {
        val phi = PI.toFloat() * ring / rings
        val path = Path()
        for (s in 0..segments) {
            val theta = (2f * PI.toFloat() * s / segments) + tRad * 2f
            val bin = if (isPlaying) telemetry.spectrumBins[s % telemetry.spectrumBins.size] else 0.18f
            val r = baseR * (0.85f + bin * 0.28f + 0.08f * sin(theta * 3f + tRad * 5f))
            val x3 = sin(phi) * cos(theta)
            val y3 = cos(phi)
            val z3 = sin(phi) * sin(theta)
            val tiltY = y3 * cos(0.45f) - z3 * sin(0.45f)
            val px = center.x + x3 * r
            val py = center.y + tiltY * r
            if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        drawPath(
            path = path,
            color = (if (ring % 2 == 0) col1 else col2).copy(alpha = 0.72f + vocal * 0.2f),
            style = Stroke(width = 1.6.dp.toPx()),
            blendMode = BlendMode.Plus
        )
    }

    drawCircle(
        color = col3.copy(alpha = 0.6f),
        radius = baseR * 1.05f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
}

private fun DrawScope.drawVizzAudioTunnel(
    telemetry: VisualizerTelemetry,
    isPlaying: Boolean,
    cycle: Float,
    tRad: Float,
    bass: Float,
    col1: Color,
    col2: Color,
    col3: Color
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val maxR = sqrt(w * w + h * h) * 0.55f

    val ringCount = 12
    for (rIdx in 0 until ringCount) {
        val depth = ((cycle * 6f + rIdx / ringCount.toFloat()) % 1f)
        val radius = maxR * (depth * depth)
        val bin = if (isPlaying) telemetry.spectrumBins[rIdx % telemetry.spectrumBins.size] else 0.2f
        val swayX = sin(tRad * 4f + depth * 4f) * w * 0.06f * (1f - depth)
        val swayY = cos(tRad * 3f + depth * 4f) * h * 0.06f * (1f - depth)
        val col = when (rIdx % 3) {
            0 -> col1
            1 -> col2
            else -> col3
        }
        drawCircle(
            color = col.copy(alpha = (depth * (0.45f + bin * 0.5f)).coerceIn(0.05f, 0.95f)),
            radius = radius.coerceAtLeast(2f),
            center = Offset(cx + swayX, cy + swayY),
            style = Stroke(width = (1f + depth * (2.5f + bass * 2f)).dp.toPx())
        )
    }

    val starCount = 48
    for (i in 0 until starCount) {
        val angle = i * 2.399963f + tRad * 0.5f
        val z = 1f - ((cycle * (2.5f + bass * 3f) + (i % 17) / 17f) % 1f)
        val prevZ = (z + 0.09f).coerceAtMost(1f)
        val s1 = 1f / (z + 0.12f)
        val s0 = 1f / (prevZ + 0.12f)
        val dx = cos(angle) * w * 0.16f
        val dy = sin(angle) * h * 0.16f
        drawLine(
            color = (if (i % 2 == 0) col1 else col2).copy(alpha = (1f - z).coerceIn(0.1f, 0.9f)),
            start = Offset(cx + dx * s0, cy + dy * s0),
            end = Offset(cx + dx * s1, cy + dy * s1),
            strokeWidth = ((1f - z) * 2.4f + 0.8f).dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawVizzCycloneEyes(
    telemetry: VisualizerTelemetry,
    isPlaying: Boolean,
    tRad: Float,
    bass: Float,
    vocal: Float,
    col1: Color,
    col2: Color,
    col3: Color
) {
    val w = size.width
    val h = size.height
    val eyeOffsets = listOf(Offset(w * 0.34f, h * 0.5f), Offset(w * 0.66f, h * 0.5f))
    val maxR = minOf(w, h) * 0.36f

    eyeOffsets.forEachIndexed { eyeIdx, center ->
        val dir = if (eyeIdx == 0) 1f else -1f
        for (arm in 0 until 4) {
            val path = Path()
            val steps = 36
            for (s in 0..steps) {
                val frac = s / steps.toFloat()
                val angle = frac * 3.5f * PI.toFloat() * dir + tRad * 5f * dir + arm * (PI.toFloat() / 2f)
                val bin = if (isPlaying) telemetry.spectrumBins[(s + arm * 5) % telemetry.spectrumBins.size] else 0.18f
                val r = maxR * frac * (0.75f + bass * 0.4f + bin * 0.2f)
                val px = center.x + cos(angle) * r
                val py = center.y + sin(angle) * r
                if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            val col = if (arm % 2 == 0) col1 else col2
            drawPath(
                path = path,
                color = col.copy(alpha = 0.8f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                blendMode = BlendMode.Plus
            )
        }
        drawCircle(
            color = col3.copy(alpha = 0.7f + vocal * 0.3f),
            radius = maxR * (0.18f + bass * 0.12f),
            center = center
        )
    }
}

private fun DrawScope.drawEmberRoseFallback(
    tRad: Float,
    bass: Float,
    vocal: Float,
    col1: Color,
    col2: Color,
    col3: Color
) {
    val w = size.width
    val h = size.height
    val center = Offset(w / 2f, h / 2f)
    val maxR = minOf(w, h) * 0.45f

    for (layer in 0 until 4) {
        val petals = 5 + layer
        val rot = tRad * (if (layer % 2 == 0) 2f else -3f)
        val path = Path()
        val steps = 64
        for (i in 0..steps) {
            val a = 2f * PI.toFloat() * i / steps
            val rose = abs(cos(a * petals * 0.5f + rot))
            val r = maxR * (0.28f + layer * 0.18f) * (0.55f + rose * (0.55f + vocal * 0.4f)) * (0.85f + bass * 0.3f)
            val px = center.x + cos(a) * r
            val py = center.y + sin(a) * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        val col = when (layer % 3) {
            0 -> col3
            1 -> col1
            else -> col2
        }
        drawPath(
            path = path,
            color = col.copy(alpha = 0.78f),
            style = Stroke(width = 2.4.dp.toPx()),
            blendMode = BlendMode.Plus
        )
    }
}

private fun DrawScope.drawVizzGlacierSpiral(
    telemetry: VisualizerTelemetry,
    isPlaying: Boolean,
    tRad: Float,
    bass: Float,
    air: Float,
    col1: Color,
    col2: Color,
    col3: Color
) {
    val w = size.width
    val h = size.height
    val center = Offset(w / 2f, h / 2f)
    val maxR = minOf(w, h) * 0.46f

    val arms = 6
    val pointsPerArm = 28
    for (a in 0 until arms) {
        val baseOffset = 2f * PI.toFloat() * a / arms
        var prevPt: Offset? = null
        for (p in 1..pointsPerArm) {
            val frac = p / pointsPerArm.toFloat()
            val bin = if (isPlaying) telemetry.spectrumBins[p % telemetry.spectrumBins.size] else 0.18f
            val angle = baseOffset + frac * 3.8f + tRad * 2.5f + sin(tRad * 4f + frac * 4f) * 0.25f
            val r = maxR * frac * (0.82f + bass * 0.25f + bin * 0.18f)
            val pt = Offset(center.x + cos(angle) * r, center.y + sin(angle) * r)
            prevPt?.let {
                drawLine(
                    brush = Brush.linearGradient(listOf(col1, col2)),
                    start = it,
                    end = pt,
                    strokeWidth = (1.2f + frac * 2f).dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            if (p % 4 == 0) {
                drawCircle(
                    color = col3.copy(alpha = 0.65f + air * 0.3f),
                    radius = (1.5f + bin * 2.5f).dp.toPx(),
                    center = pt
                )
            }
            prevPt = pt
        }
    }
}
