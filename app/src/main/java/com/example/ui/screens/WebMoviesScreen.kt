package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.WebMoviesUiState
import com.example.ui.components.M3GlassCard
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldVault
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SpatialCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebMoviesScreen(
    state: WebMoviesUiState,
    onAddressInputChange: (String) -> Unit,
    onUrlLoaded: (url: String, title: String?, loading: Boolean?) -> Unit,
    onStreamDetected: (String) -> Unit,
    onPlayInM3Player: (streamUrl: String, title: String) -> Unit,
    onEnterWebVideoFullscreen: (View, WebChromeClient.CustomViewCallback) -> Unit = { _, _ -> },
    onExitWebVideoFullscreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var bridgeStreamInput by remember { mutableStateOf("") }

    val quickSites = listOf(
        "AllSiteHub" to "https://www.allsitehub.site/",
        "MultiMovies" to "https://m1.multimovies.courses/"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Quick-switch site pills + External Browser Fallback
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickSites.forEach { (label, url) ->
                    val selected = state.currentUrl.startsWith(url.removeSuffix("/"))
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onAddressInputChange(url)
                            webViewRef?.loadUrl(url)
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricViolet.copy(alpha = 0.28f),
                            selectedLabelColor = TextPrimary
                        ),
                        modifier = Modifier.testTag("web_site_pill_${label.lowercase()}")
                    )
                }
            }

            IconButton(
                onClick = {
                    val target = state.currentUrl.ifBlank { "https://www.allsitehub.site/" }
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                    }
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("web_external_browser_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = "Open in External Browser",
                    tint = SpatialCyan
                )
            }
        }

        // Editable Address Bar + Back / Forward / Reload Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { webViewRef?.takeIf { it.canGoBack() }?.goBack() },
                modifier = Modifier.testTag("web_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            IconButton(
                onClick = { webViewRef?.takeIf { it.canGoForward() }?.goForward() },
                modifier = Modifier.testTag("web_forward_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Forward",
                    tint = TextPrimary
                )
            }
            OutlinedTextField(
                value = state.addressBarInput,
                onValueChange = onAddressInputChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    focusManager.clearFocus()
                    val formatted = if (state.addressBarInput.startsWith("http")) {
                        state.addressBarInput
                    } else {
                        "https://${state.addressBarInput}"
                    }
                    webViewRef?.loadUrl(formatted)
                }),
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
                    .testTag("web_address_bar")
            )
            IconButton(
                onClick = { webViewRef?.reload() },
                modifier = Modifier.testTag("web_reload_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Reload",
                    tint = SpatialCyan
                )
            }
        }

        if (state.isLoadingPage) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = SpatialCyan,
                trackColor = ObsidianSurface
            )
        }

        // Play Chosen Show in m³ App Player Bridge
        M3GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (state.detectedStreamUrls.isNotEmpty()) EmeraldVault.copy(alpha = 0.6f) else GlassBorder
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SurroundSound,
                            contentDescription = null,
                            tint = EmeraldVault,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (state.detectedStreamUrls.isNotEmpty()) {
                                "Stream Ready for m³ Player (${state.detectedStreamUrls.size} detected)"
                            } else {
                                "m³ App Player Bridge (Auto-sniffs .m3u8 / .mp4)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.detectedStreamUrls.isNotEmpty()) EmeraldVault else TextSecondary
                        )
                    }
                }

                if (state.detectedStreamUrls.isNotEmpty()) {
                    val topStream = state.detectedStreamUrls.first()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = topStream,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onPlayInM3Player(topStream, state.pageTitle.ifBlank { "Web Movie Stream" })
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldVault),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("web_bridge_play_detected_btn")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = ObsidianSurface, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play in m³", color = ObsidianSurface, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = bridgeStreamInput,
                            onValueChange = { bridgeStreamInput = it },
                            placeholder = {
                                Text("Or paste show stream URL (.m3u8 / .mp4)", style = MaterialTheme.typography.labelSmall)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SpatialCyan,
                                unfocusedBorderColor = GlassBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("web_bridge_manual_url_input")
                        )
                        Button(
                            onClick = {
                                if (bridgeStreamInput.isNotBlank()) {
                                    onPlayInM3Player(bridgeStreamInput, state.pageTitle.ifBlank { "Web Show" })
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("web_bridge_play_manual_btn")
                        ) {
                            Text("Play in m³", color = TextPrimary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Integrated WebView Browser Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(ObsidianSurface, RoundedCornerShape(14.dp))
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true

                        webChromeClient = object : WebChromeClient() {
                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                onUrlLoaded(view?.url ?: state.currentUrl, title, null)
                            }

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                if (view != null && callback != null) {
                                    onEnterWebVideoFullscreen(view, callback)
                                }
                            }

                            override fun onHideCustomView() {
                                onExitWebVideoFullscreen()
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val reqUrl = request?.url?.toString().orEmpty()
                                if (reqUrl.contains(".m3u8", true) ||
                                    reqUrl.contains(".mp4", true) ||
                                    reqUrl.contains(".mkv", true) ||
                                    reqUrl.contains(".webm", true)
                                ) {
                                    view?.post { onStreamDetected(reqUrl) }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val target = request?.url?.toString().orEmpty()
                                if (target.endsWith(".m3u8", true) ||
                                    target.endsWith(".mp4", true) ||
                                    target.endsWith(".mkv", true)
                                ) {
                                    onStreamDetected(target)
                                    onPlayInM3Player(target, state.pageTitle.ifBlank { "Web Stream" })
                                    return true
                                }
                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                if (url != null) onUrlLoaded(url, null, true)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (url != null) onUrlLoaded(url, view?.title, false)
                                // Inspect DOM <video>/<source> elements for direct playable media URLs
                                view?.evaluateJavascript(
                                    """
                                    (function(){
                                      var urls=[];
                                      document.querySelectorAll('video, source').forEach(function(el){
                                        if(el.src) urls.push(el.src);
                                      });
                                      return JSON.stringify(urls);
                                    })();
                                    """.trimIndent()
                                ) { res ->
                                    runCatching {
                                        val cleaned = res.trim('"').replace("\\\"", "\"")
                                        if (cleaned.startsWith("[")) {
                                            val arr = org.json.JSONArray(cleaned)
                                            for (i in 0 until arr.length()) {
                                                val u = arr.optString(i)
                                                if (u.startsWith("http")) onStreamDetected(u)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        setDownloadListener { url, _, _, mime, _ ->
                            if (url.contains(".mp4", true) || url.contains(".m3u8", true) ||
                                url.contains(".mkv", true) || mime.startsWith("video/")
                            ) {
                                onStreamDetected(url)
                                onPlayInM3Player(url, state.pageTitle.ifBlank { "Web Movie" })
                            }
                        }
                        loadUrl(state.currentUrl)
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}
