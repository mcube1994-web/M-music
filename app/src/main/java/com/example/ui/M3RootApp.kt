package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
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
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppTab
import com.example.domain.model.VisualizerMode
import com.example.ui.components.AppearanceSkinDialog
import com.example.ui.components.DolbyCapabilitiesSheet
import com.example.ui.components.EqualizerDialog
import com.example.ui.components.M3LogoBadge
import com.example.ui.components.NavidromeDialog
import com.example.ui.components.PersistentMiniPlayer
import com.example.ui.components.StudioPlayerSheet
import com.example.ui.screens.DownloadVaultScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.screens.WebMoviesScreen
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
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val vaultIds = remember(vaultTracks) { vaultTracks.map { it.id }.toSet() }
    val useRail = widthSizeClass != WindowWidthSizeClass.Compact
    val colorScheme = MaterialTheme.colorScheme

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
    BackHandler(enabled = activeTab != AppTab.HOME && !isStudioExpanded) {
        viewModel.selectTab(AppTab.HOME)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colorScheme.background, colorScheme.surface, colorScheme.background)
                )
            )
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
                            onPlayVideo = { video -> viewModel.playTrack(video, curatedVideos) },
                            onPlayStreamUrl = { url -> viewModel.playVideoUrl(url) },
                            onSelectAudioTrack = { opt -> viewModel.selectAudioTrack(opt) },
                            onImportLocalVideo = { uri -> viewModel.importLocalMedia(uri, isVideo = true) },
                            onOpenDolbySheet = { viewModel.setDolbySheetVisible(true) },
                            onOpenEqualizerDialog = { viewModel.setEqualizerDialogVisible(true) },
                            modifier = contentModifier
                        )

                        AppTab.WEB_MOVIES -> WebMoviesScreen(
                            state = webMoviesState,
                            onAddressInputChange = { viewModel.updateWebAddressInput(it) },
                            onUrlLoaded = { url, title, loading -> viewModel.updateWebUrl(url, title, loading) },
                            onStreamDetected = { streamUrl -> viewModel.registerDetectedWebStream(streamUrl) },
                            onPlayInM3Player = { streamUrl, title -> viewModel.playVideoUrl(streamUrl, title) },
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

        if (showNavidromeDialog) {
            NavidromeDialog(
                initialConfig = navidromeConfig,
                onConnect = { url, user, pass -> viewModel.connectNavidrome(url, user, pass) },
                onDismiss = { viewModel.setNavidromeDialogVisible(false) }
            )
        }
    }
}

private fun tabIcons(tab: AppTab): Pair<ImageVector, ImageVector> = when (tab) {
    AppTab.HOME -> Icons.Rounded.Home to Icons.Outlined.Home
    AppTab.VIDEO_PLAYER -> Icons.Rounded.Movie to Icons.Outlined.Movie
    AppTab.WEB_MOVIES -> Icons.Rounded.Language to Icons.Outlined.Language
    AppTab.DOWNLOAD -> Icons.Rounded.Download to Icons.Outlined.Download
    AppTab.SEARCH -> Icons.Rounded.Search to Icons.Outlined.Search
}
