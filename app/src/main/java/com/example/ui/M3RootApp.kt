package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.domain.model.AppTab
import com.example.domain.model.VisualizerMode
import com.example.ui.components.AppearanceSkinDialog
import com.example.ui.components.DolbyCapabilitiesSheet
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.M3LogoBadge
import com.example.ui.components.NavidromeDialog
import com.example.ui.components.PersistentMiniPlayer
import com.example.ui.components.StudioPlayerSheet
import com.example.ui.components.UpNextQueueSheet
import com.example.ui.screens.DownloadVaultScreen
import com.example.ui.screens.BrainwaveScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.screens.WebMoviesScreen
import com.example.ui.theme.SpatialCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun M3RootApp(
    viewModel: M3ViewModel,
    widthSizeClass: WindowWidthSizeClass
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val webMoviesState by viewModel.webMoviesState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val telemetry by viewModel.visualizerTelemetry.collectAsStateWithLifecycle()
    val aiEqState by viewModel.aiEqState.collectAsStateWithLifecycle()
    val visualizerMode by viewModel.visualizerMode.collectAsStateWithLifecycle()
    val visualizerPalette by viewModel.visualizerPalette.collectAsStateWithLifecycle()
    val appearanceSkin by viewModel.appearanceSkin.collectAsStateWithLifecycle()
    val isSweepPlaying by viewModel.isPlayingOpeningSweep.collectAsStateWithLifecycle()
    val dolbyCaps by viewModel.dolbyCapabilities.collectAsStateWithLifecycle()
    val hlsManifest by viewModel.hlsManifestInfo.collectAsStateWithLifecycle()
    val vaultTracks by viewModel.vaultTracks.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val curatedVideos by viewModel.curatedVideos.collectAsStateWithLifecycle()
    val navidromeConfig by viewModel.navidromeConfig.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val isStudioExpanded by viewModel.isStudioExpanded.collectAsStateWithLifecycle()
    val showDolbySheet by viewModel.showDolbySheet.collectAsStateWithLifecycle()
    val showNavidromeDialog by viewModel.showNavidromeDialog.collectAsStateWithLifecycle()
    val showAppearanceDialog by viewModel.showAppearanceDialog.collectAsStateWithLifecycle()
    val showEqualizerDialog by viewModel.showEqualizerDialog.collectAsStateWithLifecycle()
    val showQueueSheet by viewModel.showQueueSheet.collectAsStateWithLifecycle()
    val isVideoFullscreen by viewModel.isVideoFullscreen.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val snackbarHostState = remember { SnackbarHostState() }
    val videoListState = rememberLazyListState()
    val vaultIds = remember(vaultTracks) { vaultTracks.map { it.id }.toSet() }
    val useRail = widthSizeClass != WindowWidthSizeClass.Compact
    val colorScheme = MaterialTheme.colorScheme
    val neonTransition = rememberInfiniteTransition(label = "m3-night-bar-neon")
    val neonCyanAlpha by neonTransition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1450),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon-cyan-pulse"
    )
    val neonVioletAlpha by neonTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon-violet-pulse"
    )
    var webCustomFullscreen by remember {
        mutableStateOf<Pair<View, WebChromeClient.CustomViewCallback>?>(null)
    }

    var lastOrientation by remember { mutableIntStateOf(configuration.orientation) }
    LaunchedEffect(configuration.orientation, activeTab, playbackState.currentTrack?.isVideo) {
        val currentOrientation = configuration.orientation
        if (currentOrientation != lastOrientation) {
            if (activeTab == AppTab.VIDEO_PLAYER && playbackState.currentTrack?.isVideo == true) {
                if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    viewModel.setVideoFullscreen(true)
                } else if (currentOrientation == Configuration.ORIENTATION_PORTRAIT && isVideoFullscreen) {
                    viewModel.setVideoFullscreen(false)
                }
            }
            lastOrientation = currentOrientation
        }
    }

    LaunchedEffect(activeTab, isVideoFullscreen, webCustomFullscreen) {
        if (webCustomFullscreen == null && !isVideoFullscreen && activeTab != AppTab.VIDEO_PLAYER) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val enterVideoFullscreen: () -> Unit = {
        viewModel.setVideoFullscreen(true)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    val exitVideoFullscreen: () -> Unit = {
        viewModel.setVideoFullscreen(false)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    val toggleVideoRotation: () -> Unit = {
        val isCurrentlyLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isCurrentlyLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            viewModel.setVideoFullscreen(false)
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            viewModel.setVideoFullscreen(true)
        }
    }
    val exitWebVideoFullscreen: () -> Unit = {
        val current = webCustomFullscreen
        webCustomFullscreen = null
        current?.second?.onCustomViewHidden()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(playbackState.errorMessage) {
        val err = playbackState.errorMessage
        if (!err.isNullOrBlank()) {
            snackbarHostState.showSnackbar(err)
            viewModel.playerController.dismissError()
        }
    }

    // Back button returns non-Home tabs back to Home cleanly
    BackHandler(enabled = activeTab != AppTab.HOME && !isStudioExpanded && !showQueueSheet && !isVideoFullscreen && webCustomFullscreen == null) {
        viewModel.selectTab(AppTab.HOME)
    }

    if (isVideoFullscreen && playbackState.currentTrack?.isVideo == true) {
        FullscreenVideoPlayerOverlay(
            exoPlayer = viewModel.playerController.exoPlayer,
            onToggleRotation = toggleVideoRotation,
            onExitFullscreen = exitVideoFullscreen
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colorScheme.background, colorScheme.surface, colorScheme.background)
                )
            )
            .drawWithContent {
                drawContent()
                val inset = 3.dp.toPx()
                drawRoundRect(
                    color = Color(0xFF00E5FF).copy(alpha = neonCyanAlpha),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
                val innerInset = 7.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFB026FF).copy(alpha = neonVioletAlpha),
                    topLeft = Offset(innerInset, innerInset),
                    size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
                    cornerRadius = CornerRadius(21.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
    ) {
        Scaffold(
            containerColor = colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorScheme.surface.copy(alpha = 0.96f))
                    ) {
                        val activeTrack = playbackState.currentTrack
                        if (activeTrack != null) {
                            PersistentMiniPlayer(
                                track = activeTrack,
                                isPlaying = playbackState.isPlaying,
                                aiEqState = aiEqState,
                                onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                                onSkipNext = { viewModel.playerController.skipNext() },
                                onOpenQueue = { viewModel.setQueueSheetVisible(true) },
                                onExpandStudio = { viewModel.setStudioExpanded(true) },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        NavigationBar(
                            containerColor = colorScheme.surface,
                            contentColor = TextPrimary,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            AppTab.entries.forEach { tab ->
                                val selected = tab == activeTab
                                val (activeIcon, inactiveIcon) = tabIcons(tab)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) activeIcon else inactiveIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = colorScheme.secondary,
                                        selectedTextColor = colorScheme.secondary,
                                        indicatorColor = colorScheme.primary.copy(alpha = 0.24f),
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = colorScheme.surface,
                        contentColor = TextPrimary,
                        header = {
                            M3LogoBadge(
                                size = 40.dp,
                                isActive = playbackState.isPlaying,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        },
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = tab == activeTab
                            val (activeIcon, inactiveIcon) = tabIcons(tab)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) activeIcon else inactiveIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = colorScheme.secondary,
                                    selectedTextColor = colorScheme.secondary,
                                    indicatorColor = colorScheme.primary.copy(alpha = 0.24f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .then(if (useRail) Modifier.widthIn(max = 920.dp) else Modifier)

                    when (activeTab) {
                        AppTab.HOME -> HomeScreen(
                            homeState = homeState,
                            currentTrack = playbackState.currentTrack,
                            isPlaying = playbackState.isPlaying,
                            isSweepPlaying = isSweepPlaying,
                            visualizerMode = visualizerMode,
                            visualizerPalette = visualizerPalette,
                            appearanceSkin = appearanceSkin,
                            telemetry = telemetry,
                            aiEqState = aiEqState,
                            dolbyCapabilities = dolbyCaps,
                            navidromeConfig = navidromeConfig,
                            vaultIds = vaultIds,
                            onTriggerOpeningSweep = { viewModel.triggerOpeningSweep() },
                            onQuickLaunchMusic = {
                                val firstSong = homeState.songs.firstOrNull()
                                if (firstSong != null) {
                                    viewModel.playTrack(firstSong, homeState.songs)
                                    viewModel.setStudioExpanded(true)
                                } else {
                                    viewModel.showToast("Loading music streams...")
                                    viewModel.loadHomeFeed()
                                }
                            },
                            onQuickLaunchMovies = { viewModel.selectTab(AppTab.VIDEO_PLAYER) },
                            onQuickLaunchDownload = { viewModel.selectTab(AppTab.DOWNLOAD) },
                            onSearchQueryChange = { viewModel.updateHomeSearchQuery(it) },
                            onClearSearch = { viewModel.clearHomeSearch() },
                            onSelectLanguage = { viewModel.selectHomeLanguage(it) },
                            onPlayTrack = { track -> viewModel.playTrack(track, homeState.songs) },
                            onPlayNextTrack = { track -> viewModel.playNext(track) },
                            onAddTrackToQueue = { track -> viewModel.addToQueue(track) },
                            onDownloadTrack = { track -> viewModel.downloadTrackToVault(track) },
                            onCycleVisualizerMode = {
                                val modes = VisualizerMode.entries
                                val next = modes[(modes.indexOf(visualizerMode) + 1) % modes.size]
                                viewModel.setVisualizerMode(next)
                            },
                            onSelectLiveVizzFm = {
                                val target = if (visualizerMode == VisualizerMode.VIZZ_LIVE_WEB) {
                                    VisualizerMode.VIZZ_MESH_GRID
                                } else {
                                    VisualizerMode.VIZZ_LIVE_WEB
                                }
                                viewModel.setVisualizerMode(target)
                            },
                            onOpenStudio = {
                                if (playbackState.currentTrack != null) {
                                    viewModel.setStudioExpanded(true)
                                } else {
                                    homeState.songs.firstOrNull()?.let {
                                        viewModel.playTrack(it, homeState.songs)
                                        viewModel.setStudioExpanded(true)
                                    }
                                }
                            },
                            onOpenDolbySheet = { viewModel.setDolbySheetVisible(true) },
                            onOpenAppearanceDialog = { viewModel.setAppearanceDialogVisible(true) },
                            onOpenEqualizerDialog = { viewModel.setEqualizerDialogVisible(true) },
                            onOpenNavidromeDialog = { viewModel.setNavidromeDialogVisible(true) },
                            onToggleSpatializer = { viewModel.setSpatializerEffectEnabled(it) },
                            onImportLocalAudio = { uri -> viewModel.importLocalMedia(uri, isVideo = false) },
                            modifier = contentModifier
                        )

                        AppTab.VIDEO_PLAYER -> VideoPlayerScreen(
                            exoPlayer = viewModel.playerController.exoPlayer,
                            currentTrack = playbackState.currentTrack,
                            curatedVideos = curatedVideos,
                            audioTrackOptions = playbackState.audioTracks,
                            aiEqState = aiEqState,
                            isVideoFullscreen = isVideoFullscreen,
                            onPlayVideo = { video -> viewModel.playTrack(video, curatedVideos) },
                            onPlayStreamUrl = { url -> viewModel.playVideoUrl(url) },
                            onSelectAudioTrack = { opt -> viewModel.selectAudioTrack(opt) },
                            onImportLocalVideo = { uri -> viewModel.importLocalMedia(uri, isVideo = true) },
                            onEnterFullscreen = enterVideoFullscreen,
                            onToggleRotation = toggleVideoRotation,
                            onOpenDolbySheet = { viewModel.setDolbySheetVisible(true) },
                            onOpenEqualizerDialog = { viewModel.setEqualizerDialogVisible(true) },
                            modifier = contentModifier,
                            listState = videoListState
                        )

                        AppTab.WEB_MOVIES -> WebMoviesScreen(
                            state = webMoviesState,
                            onAddressInputChange = { viewModel.updateWebAddressInput(it) },
                            onUrlLoaded = { url, title, loading -> viewModel.updateWebUrl(url, title, loading) },
                            onStreamDetected = { streamUrl -> viewModel.registerDetectedWebStream(streamUrl) },
                            onPlayInM3Player = { streamUrl, title -> viewModel.playVideoUrl(streamUrl, title) },
                            onEnterWebVideoFullscreen = { customView, callback ->
                                webCustomFullscreen = customView to callback
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            },
                            onExitWebVideoFullscreen = exitWebVideoFullscreen,
                            modifier = contentModifier
                        )

                        AppTab.DOWNLOAD -> DownloadVaultScreen(
                            state = downloadState,
                            vaultTracks = vaultTracks,
                            currentTrack = playbackState.currentTrack,
                            onQueryChange = { viewModel.updateDownloadQuery(it) },
                            onSearchLossless = { viewModel.searchDownloadCandidates() },
                            onDownloadTrack = { viewModel.downloadTrackToVault(it) },
                            onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                            onDeleteVaultTrack = { id -> viewModel.deleteFromVault(id) },
                            modifier = contentModifier
                        )

                        AppTab.BRAINWAVE -> BrainwaveScreen(modifier = contentModifier)

                        AppTab.SEARCH -> SearchScreen(
                            state = searchState,
                            recentSearches = recentSearches,
                            currentTrack = playbackState.currentTrack,
                            vaultIds = vaultIds,
                            onQueryChange = { viewModel.updateUnifiedSearchQuery(it) },
                            onExecuteSearch = { viewModel.executeUnifiedSearch(it) },
                            onSelectBackendFilter = { viewModel.setSearchBackendFilter(it) },
                            onRemoveRecent = { viewModel.removeRecentSearch(it) },
                            onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                            onPlayNext = { track -> viewModel.playNext(track) },
                            onAddToQueue = { track -> viewModel.addToQueue(track) },
                            onDownloadTrack = { viewModel.downloadTrackToVault(it) },
                            modifier = contentModifier
                        )
                    }

                    // Mini-player on Medium/Expanded tablet rail layout
                    if (useRail && playbackState.currentTrack != null) {
                        PersistentMiniPlayer(
                            track = playbackState.currentTrack!!,
                            isPlaying = playbackState.isPlaying,
                            aiEqState = aiEqState,
                            onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                            onSkipNext = { viewModel.playerController.skipNext() },
                            onOpenQueue = { viewModel.setQueueSheetVisible(true) },
                            onExpandStudio = { viewModel.setStudioExpanded(true) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .widthIn(max = 680.dp)
                                .padding(16.dp)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        )
                    }
                }
            }
        }

        // Full-Screen Studio Player & vizz.fm Visualizer Overlay
        AnimatedVisibility(
            visible = isStudioExpanded && playbackState.currentTrack != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            StudioPlayerSheet(
                playbackState = playbackState,
                visualizerMode = visualizerMode,
                visualizerPalette = visualizerPalette,
                telemetry = telemetry,
                aiEqState = aiEqState,
                eqBandLabels = viewModel.aiEqEngine.bandLabels,
                lyrics = lyrics,
                onDismiss = { viewModel.setStudioExpanded(false) },
                onTogglePlayPause = { viewModel.playerController.togglePlayPause() },
                onSkipNext = { viewModel.playerController.skipNext() },
                onSkipPrevious = { viewModel.playerController.skipPrevious() },
                onSeekTo = { viewModel.playerController.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onOpenQueue = { viewModel.setQueueSheetVisible(true) },
                onSelectVisualizerMode = { viewModel.setVisualizerMode(it) },
                onSelectVisualizerPalette = { viewModel.setVisualizerPalette(it) },
                onToggleEqEnabled = { viewModel.setEqEnabled(it) },
                onSelectEqMode = { viewModel.selectEqMode(it) },
                onChangeEqBand = { idx, gain -> viewModel.setEqBandGain(idx, gain) },
                onResetEqFlat = { viewModel.resetEqFlat() },
                onOpenDolbyDiagnostics = { viewModel.setDolbySheetVisible(true) },
                onOpenAppearanceDialog = { viewModel.setAppearanceDialogVisible(true) }
            )
        }

        if (showQueueSheet) {
            UpNextQueueSheet(
                currentTrack = playbackState.currentTrack,
                queue = playbackState.queue,
                currentIndex = playbackState.currentIndex,
                shuffleEnabled = playbackState.shuffleEnabled,
                repeatMode = playbackState.repeatMode,
                onJumpToIndex = { index -> viewModel.jumpToQueueIndex(index) },
                onRemoveAt = { index -> viewModel.removeFromQueue(index) },
                onClearQueue = { viewModel.clearQueue() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onDismiss = { viewModel.setQueueSheetVisible(false) }
            )
        }

        if (showEqualizerDialog) {
            EqualizerDialog(
                aiEqState = aiEqState,
                eqBandLabels = viewModel.aiEqEngine.bandLabels,
                onToggleEqEnabled = { viewModel.setEqEnabled(it) },
                onSelectEqMode = { viewModel.selectEqMode(it) },
                onChangeEqBand = { idx, gain -> viewModel.setEqBandGain(idx, gain) },
                onResetFlat = { viewModel.resetEqFlat() },
                onDismiss = { viewModel.setEqualizerDialogVisible(false) }
            )
        }

        if (showAppearanceDialog) {
            AppearanceSkinDialog(
                currentSkin = appearanceSkin,
                currentVisualizerMode = visualizerMode,
                currentPalette = visualizerPalette,
                onSelectSkin = { viewModel.setAppearanceSkin(it) },
                onSelectVisualizerMode = { viewModel.setVisualizerMode(it) },
                onSelectPalette = { viewModel.setVisualizerPalette(it) },
                onDismiss = { viewModel.setAppearanceDialogVisible(false) }
            )
        }

        if (showDolbySheet) {
            DolbyCapabilitiesSheet(
                capabilities = dolbyCaps,
                hlsManifestInfo = hlsManifest,
                onToggleSpatializer = { viewModel.setSpatializerEffectEnabled(it) },
                onWebProbeResult = { map -> viewModel.updateWebMediaCapabilitiesResults(map) },
                onDismiss = { viewModel.setDolbySheetVisible(false) }
            )
        }

        // HTML5 WebView Fullscreen Video Overlay (rotated landscape, video only)
        webCustomFullscreen?.let { (customView, _) ->
            FullscreenWebVideoOverlay(
                customView = customView,
                onExitFullscreen = exitWebVideoFullscreen
            )
        }

        if (showNavidromeDialog) {
            NavidromeDialog(
                initialConfig = navidromeConfig,
                onConnect = { url, user, pass -> viewModel.connectNavidrome(url, user, pass) },
                onDismiss = { viewModel.setNavidromeDialogVisible(false) }
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun FullscreenVideoPlayerOverlay(
    exoPlayer: ExoPlayer,
    onToggleRotation: () -> Unit,
    onExitFullscreen: () -> Unit
) {
    val view = LocalView.current
    val activity = remember(view) { view.context.findActivity() }
    var areControlsVisible by remember { mutableStateOf(true) }
    var useZoomFill by remember { mutableStateOf(false) }

    BackHandler(onBack = onExitFullscreen)

    DisposableEffect(activity, view) {
        val window = activity?.window
        val prevCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && window != null) {
            val prev = window.attributes.layoutInDisplayCutoutMode
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            prev
        } else 0

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && window != null) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode = prevCutoutMode
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_fullscreen_overlay")
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    resizeMode = if (useZoomFill) {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    player = exoPlayer
                    setFullscreenButtonState(true)
                    setFullscreenButtonClickListener { _ ->
                        onExitFullscreen()
                    }
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            areControlsVisible = visibility == View.VISIBLE
                        }
                    )
                }
            },
            update = { playerView ->
                if (playerView.player !== exoPlayer) {
                    playerView.player = exoPlayer
                }
                playerView.resizeMode = if (useZoomFill) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
                playerView.setFullscreenButtonState(true)
                playerView.setFullscreenButtonClickListener { _ ->
                    onExitFullscreen()
                }
                playerView.setControllerVisibilityListener(
                    PlayerView.ControllerVisibilityListener { visibility ->
                        areControlsVisible = visibility == View.VISIBLE
                    }
                )
            },
            onRelease = { playerView ->
                playerView.setControllerVisibilityListener(null as PlayerView.ControllerVisibilityListener?)
                playerView.player = null
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { useZoomFill = !useZoomFill },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .minimumInteractiveComponentSize()
                        .testTag("video_fullscreen_resize_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AspectRatio,
                        contentDescription = if (useZoomFill) "Fit Video" else "Fill Screen",
                        tint = if (useZoomFill) SpatialCyan else TextPrimary
                    )
                }
                IconButton(
                    onClick = onToggleRotation,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .minimumInteractiveComponentSize()
                        .testTag("video_fullscreen_rotate_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ScreenRotation,
                        contentDescription = "Rotate Video Orientation",
                        tint = TextPrimary
                    )
                }
                IconButton(
                    onClick = onExitFullscreen,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .minimumInteractiveComponentSize()
                        .testTag("video_exit_fullscreen_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FullscreenExit,
                        contentDescription = "Exit Fullscreen Video",
                        tint = SpatialCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun FullscreenWebVideoOverlay(
    customView: View,
    onExitFullscreen: () -> Unit
) {
    val view = LocalView.current
    val activity = remember(view) { view.context.findActivity() }

    BackHandler(onBack = onExitFullscreen)

    DisposableEffect(activity, view) {
        val window = activity?.window
        val prevCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && window != null) {
            val prev = window.attributes.layoutInDisplayCutoutMode
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            prev
        } else 0

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && window != null) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode = prevCutoutMode
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("web_video_fullscreen_overlay")
    ) {
        AndroidView(
            factory = { customView },
            onRelease = { v ->
                (v.parent as? android.view.ViewGroup)?.removeView(v)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun tabIcons(tab: AppTab): Pair<ImageVector, ImageVector> = when (tab) {
    AppTab.HOME -> Icons.Rounded.Home to Icons.Outlined.Home
    AppTab.VIDEO_PLAYER -> Icons.Rounded.Movie to Icons.Outlined.Movie
    AppTab.WEB_MOVIES -> Icons.Rounded.Language to Icons.Outlined.Language
    AppTab.DOWNLOAD -> Icons.Rounded.Download to Icons.Outlined.Download
    AppTab.SEARCH -> Icons.Rounded.Search to Icons.Outlined.Search
    AppTab.BRAINWAVE -> Icons.Rounded.GraphicEq to Icons.Rounded.GraphicEq
}
