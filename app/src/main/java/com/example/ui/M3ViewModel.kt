package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.R
import com.example.audio.AiAutoEqEngine
import com.example.audio.AiEqState
import com.example.audio.DolbySpatializerEngine
import com.example.audio.HlsManifestParser
import com.example.audio.OpeningSpatialSweepEngine
import com.example.audio.VisualizerTelemetry
import com.example.data.local.M3Database
import com.example.data.local.OfflineVaultRepository
import com.example.data.local.PreferencesStore
import com.example.data.local.SearchCacheStore
import com.example.data.remote.AudiusClient
import com.example.data.remote.InternetArchiveClient
import com.example.data.remote.JioSaavnClient
import com.example.data.remote.LrclibClient
import com.example.data.remote.NavidromeClient
import com.example.data.remote.StreamingAggregatorRepository
import com.example.domain.model.AiEqMode
import com.example.domain.model.AppTab
import com.example.domain.model.AppearanceSkin
import com.example.domain.model.AudioTrackOption
import com.example.domain.model.DolbyCapabilitiesState
import com.example.domain.model.DownloadItemState
import com.example.domain.model.HlsManifestInfo
import com.example.domain.model.MediaTrack
import com.example.domain.model.MusicLanguage
import com.example.domain.model.NavidromeConfig
import com.example.domain.model.StreamingBackend
import com.example.domain.model.SyncedLyricLine
import com.example.domain.model.VisualizerMode
import com.example.domain.model.VisualizerPalette
import com.example.player.M3EngineRegistry
import com.example.player.M3PlayerController
import com.example.player.PlaybackUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

data class HomeUiState(
    val selectedLanguage: MusicLanguage = MusicLanguage.ALL,
    val songSearchQuery: String = "",
    val isLoading: Boolean = true,
    val songs: List<MediaTrack> = emptyList(),
    val statusBanner: String? = null
)

data class SearchUiState(
    val query: String = "",
    val selectedBackendFilter: StreamingBackend? = null,
    val isSearching: Boolean = false,
    val isFromCache: Boolean = false,
    val results: List<MediaTrack> = emptyList()
)

data class DownloadUiState(
    val query: String = "acoustic concert",
    val isSearching: Boolean = false,
    val candidates: List<MediaTrack> = emptyList(),
    val activeDownloads: Map<String, DownloadItemState> = emptyMap(),
    val statusMessage: String? = null
)

data class WebMoviesUiState(
    val currentUrl: String = "https://www.allsitehub.site/",
    val addressBarInput: String = "https://www.allsitehub.site/",
    val pageTitle: String = "AllSiteHub Portal",
    val detectedStreamUrls: List<String> = emptyList(),
    val isLoadingPage: Boolean = false,
    val frameBlockedHint: Boolean = false
)

@OptIn(UnstableApi::class)
class M3ViewModel(application: Application) : AndroidViewModel(application) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val database = M3Database.getInstance(application)
    private val prefsStore = PreferencesStore(application)
    private val searchCacheStore = SearchCacheStore(application)
    private val vaultRepo = OfflineVaultRepository(application, database.vaultDao(), okHttpClient)

    private val jioSaavnClient = JioSaavnClient(okHttpClient)
    private val audiusClient = AudiusClient(okHttpClient)
    private val archiveClient = InternetArchiveClient(okHttpClient)
    private val navidromeClient = NavidromeClient(okHttpClient)
    private val lrclibClient = LrclibClient(okHttpClient)

    private val aggregator = StreamingAggregatorRepository(
        jioSaavnClient, audiusClient, archiveClient, navidromeClient, searchCacheStore
    )

    val playerController = M3PlayerController(application, viewModelScope)
    val openingSweepEngine = OpeningSpatialSweepEngine()
    val aiEqEngine: AiAutoEqEngine = M3EngineRegistry.aiAutoEqEngine

    val playbackState: StateFlow<PlaybackUiState> = playerController.playbackState
    val visualizerTelemetry: StateFlow<VisualizerTelemetry> = M3EngineRegistry.pcmProcessor.telemetry
    val aiEqState: StateFlow<AiEqState> = aiEqEngine.state
    val isPlayingOpeningSweep: StateFlow<Boolean> = openingSweepEngine.isPlayingSweep

    val openingSweepEnabled: StateFlow<Boolean> = prefsStore.openingSweepEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _visualizerMode = MutableStateFlow(VisualizerMode.VIZZ_MESH_GRID)
    val visualizerMode: StateFlow<VisualizerMode> = _visualizerMode.asStateFlow()

    private val _visualizerPalette = MutableStateFlow(VisualizerPalette.CYBER_CYAN)
    val visualizerPalette: StateFlow<VisualizerPalette> = _visualizerPalette.asStateFlow()

    private val _appearanceSkin = MutableStateFlow(AppearanceSkin.LEVYRA_DEEPSOUND)
    val appearanceSkin: StateFlow<AppearanceSkin> = _appearanceSkin.asStateFlow()

    private val _spatializerEffectEnabled = MutableStateFlow(true)
    val spatializerEffectEnabled: StateFlow<Boolean> = _spatializerEffectEnabled.asStateFlow()

    val navidromeConfig: StateFlow<NavidromeConfig> = prefsStore.navidromeConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NavidromeConfig())

    val vaultTracks: StateFlow<List<MediaTrack>> = vaultRepo.vaultTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<String>> = vaultRepo.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeTab = MutableStateFlow(AppTab.HOME)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _isStudioExpanded = MutableStateFlow(false)
    val isStudioExpanded: StateFlow<Boolean> = _isStudioExpanded.asStateFlow()

    private val _showDolbySheet = MutableStateFlow(false)
    val showDolbySheet: StateFlow<Boolean> = _showDolbySheet.asStateFlow()

    private val _showNavidromeDialog = MutableStateFlow(false)
    val showNavidromeDialog: StateFlow<Boolean> = _showNavidromeDialog.asStateFlow()

    private val _showAppearanceDialog = MutableStateFlow(false)
    val showAppearanceDialog: StateFlow<Boolean> = _showAppearanceDialog.asStateFlow()

    private val _showEqualizerDialog = MutableStateFlow(false)
    val showEqualizerDialog: StateFlow<Boolean> = _showEqualizerDialog.asStateFlow()

    private val _showQueueSheet = MutableStateFlow(false)
    val showQueueSheet: StateFlow<Boolean> = _showQueueSheet.asStateFlow()

    private val _isVideoFullscreen = MutableStateFlow(false)
    val isVideoFullscreen: StateFlow<Boolean> = _isVideoFullscreen.asStateFlow()

    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _downloadState = MutableStateFlow(DownloadUiState())
    val downloadState: StateFlow<DownloadUiState> = _downloadState.asStateFlow()

    private val _webMoviesState = MutableStateFlow(WebMoviesUiState())
    val webMoviesState: StateFlow<WebMoviesUiState> = _webMoviesState.asStateFlow()

    private val _curatedVideos = MutableStateFlow(aggregator.getCuratedVideoCatalog())
    val curatedVideos: StateFlow<List<MediaTrack>> = _curatedVideos.asStateFlow()

    private val _dolbyCapabilities = MutableStateFlow(DolbySpatializerEngine.probeDeviceCapabilities(application))
    val dolbyCapabilities: StateFlow<DolbyCapabilitiesState> = _dolbyCapabilities.asStateFlow()

    private val _hlsManifestInfo = MutableStateFlow(HlsManifestParser.parse())
    val hlsManifestInfo: StateFlow<HlsManifestInfo> = _hlsManifestInfo.asStateFlow()

    private val _lyrics = MutableStateFlow<List<SyncedLyricLine>>(emptyList())
    val lyrics: StateFlow<List<SyncedLyricLine>> = _lyrics.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private var homeSearchJob: Job? = null
    private var unifiedSearchJob: Job? = null
    private var unregisterSpatializerCallbacks: (() -> Unit)? = null

    init {
        viewModelScope.launch { prefsStore.visualizerMode.collect { _visualizerMode.value = it } }
        viewModelScope.launch { prefsStore.visualizerPalette.collect { _visualizerPalette.value = it } }
        viewModelScope.launch { prefsStore.appearanceSkin.collect { _appearanceSkin.value = it } }
        viewModelScope.launch {
            prefsStore.spatializerEffectEnabled.collect { enabled ->
                _spatializerEffectEnabled.value = enabled
                playerController.applySpatializerSettings(enabled)
                refreshSpatializerCapabilities(enabled)
            }
        }
        unregisterSpatializerCallbacks = DolbySpatializerEngine.registerSpatializerCallbacks(application) {
            refreshSpatializerCapabilities()
        }
        // Non-blocking launch: immediately load Home feed & trigger 7.1.4 spatial sweep if enabled
        viewModelScope.launch {
            val sweepOn = prefsStore.openingSweepEnabled.first()
            if (sweepOn) {
                openingSweepEngine.playOpeningSweep(viewModelScope)
            }
        }
        loadHomeFeed()
        searchDownloadCandidates(_downloadState.value.query)
    }

    override fun onCleared() {
        unregisterSpatializerCallbacks?.invoke()
        unregisterSpatializerCallbacks = null
        super.onCleared()
    }

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
        if (tab != AppTab.VIDEO_PLAYER) {
            _isVideoFullscreen.value = false
        }
    }

    fun setVideoFullscreen(fullscreen: Boolean) {
        _isVideoFullscreen.value = fullscreen
        if (fullscreen) {
            _isStudioExpanded.value = false
        }
    }

    fun setStudioExpanded(expanded: Boolean) {
        _isStudioExpanded.value = expanded
        if (expanded) {
            playbackState.value.currentTrack?.let { loadLyricsForTrack(it) }
        }
    }

    fun setDolbySheetVisible(visible: Boolean) {
        if (visible) {
            refreshSpatializerCapabilities()
        }
        _showDolbySheet.value = visible
    }

    fun refreshSpatializerCapabilities(userEnabled: Boolean = _spatializerEffectEnabled.value) {
        val currentWebMap = _dolbyCapabilities.value.formats.associate { it.key to it.webMediaCapabilitiesSupported }
        val probed = DolbySpatializerEngine.probeDeviceCapabilities(getApplication(), userEnabled)
        _dolbyCapabilities.value = probed.copy(
            formats = probed.formats.map { fmt ->
                fmt.copy(webMediaCapabilitiesSupported = currentWebMap[fmt.key])
            }
        )
    }

    fun setSpatializerEffectEnabled(enabled: Boolean) {
        _spatializerEffectEnabled.value = enabled
        playerController.applySpatializerSettings(enabled)
        refreshSpatializerCapabilities(enabled)
        viewModelScope.launch { prefsStore.setSpatializerEffectEnabled(enabled) }
        val caps = _dolbyCapabilities.value
        val msg = when {
            !enabled -> "Android Spatializer effect disabled (Stereo Direct)"
            caps.compatibleHeadphonesConnected && caps.spatializerAvailable && !caps.spatializerEnabled ->
                "Spatializer enabled in app — turn on Spatial Audio in System Sound Settings for ${caps.connectedHeadphoneName ?: "headphones"}"
            caps.compatibleHeadphonesConnected ->
                "Android Spatializer enabled for ${caps.connectedHeadphoneName ?: "compatible headphones"}"
            else -> "Android Spatializer enabled — connect headphones to activate 3D audio"
        }
        showToast(msg)
    }

    fun updateWebMediaCapabilitiesResults(jsonMap: Map<String, Boolean>) {
        val current = _dolbyCapabilities.value
        val updatedFormats = current.formats.map { fmt ->
            fmt.copy(webMediaCapabilitiesSupported = jsonMap[fmt.key])
        }
        _dolbyCapabilities.value = current.copy(formats = updatedFormats)
    }

    fun setNavidromeDialogVisible(visible: Boolean) {
        _showNavidromeDialog.value = visible
    }

    fun setAppearanceDialogVisible(visible: Boolean) {
        _showAppearanceDialog.value = visible
    }

    fun setEqualizerDialogVisible(visible: Boolean) {
        _showEqualizerDialog.value = visible
    }

    fun setQueueSheetVisible(visible: Boolean) {
        _showQueueSheet.value = visible
    }

    fun triggerOpeningSweep() {
        if (openingSweepEngine.isPlayingSweep.value) {
            openingSweepEngine.stopSweep()
        } else {
            openingSweepEngine.playOpeningSweep(viewModelScope)
        }
    }

    fun toggleOpeningSweepPref(enabled: Boolean) {
        viewModelScope.launch { prefsStore.setOpeningSweepEnabled(enabled) }
    }

    fun setVisualizerMode(mode: VisualizerMode) {
        _visualizerMode.value = mode
        viewModelScope.launch { prefsStore.setVisualizerMode(mode) }
    }

    fun setVisualizerPalette(palette: VisualizerPalette) {
        _visualizerPalette.value = palette
        viewModelScope.launch { prefsStore.setVisualizerPalette(palette) }
    }

    fun setAppearanceSkin(skin: AppearanceSkin) {
        _appearanceSkin.value = skin
        viewModelScope.launch { prefsStore.setAppearanceSkin(skin) }
        showToast("Applied ${skin.label} appearance")
    }

    fun setEqEnabled(enabled: Boolean) {
        aiEqEngine.setEqEnabled(enabled)
        viewModelScope.launch { prefsStore.setAiAutoEqEnabled(enabled && aiEqState.value.isAutoEnabled) }
    }

    fun selectEqMode(mode: AiEqMode) {
        aiEqEngine.selectMode(mode)
        viewModelScope.launch {
            prefsStore.setAiAutoEqEnabled(mode == AiEqMode.AUTO_DETECT)
            prefsStore.setEqMode(mode)
        }
    }

    fun setEqBandGain(index: Int, gainDb: Float) {
        aiEqEngine.setBandGain(index, gainDb)
    }

    fun resetEqFlat() {
        aiEqEngine.resetToFlat()
        viewModelScope.launch {
            prefsStore.setAiAutoEqEnabled(false)
            prefsStore.setEqMode(AiEqMode.FLAT_HIFI)
        }
        showToast("10-Band Equalizer reset to 0.0 dB")
    }

    // --- Home Songs Feed & Dedicated Search ---
    fun selectHomeLanguage(language: MusicLanguage) {
        _homeState.value = _homeState.value.copy(selectedLanguage = language)
        loadHomeFeed()
    }

    fun updateHomeSearchQuery(query: String) {
        _homeState.value = _homeState.value.copy(songSearchQuery = query)
        homeSearchJob?.cancel()
        homeSearchJob = viewModelScope.launch {
            delay(350L)
            loadHomeFeed()
        }
    }

    fun clearHomeSearch() {
        _homeState.value = _homeState.value.copy(songSearchQuery = "")
        loadHomeFeed()
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            val st = _homeState.value
            _homeState.value = st.copy(isLoading = true, statusBanner = null)
            val navConfig = navidromeConfig.value
            val results = runCatching {
                aggregator.loadHomeFeed(st.selectedLanguage, st.songSearchQuery, navConfig)
            }.getOrDefault(emptyList())

            _homeState.value = _homeState.value.copy(
                isLoading = false,
                songs = results,
                statusBanner = if (results.isEmpty()) {
                    "No online tracks returned for '${st.songSearchQuery.ifBlank { st.selectedLanguage.label }}'. Try another query or import local FLAC/MP3."
                } else null
            )
        }
    }

    // --- Playback & Queue Controls ---
    fun playTrack(track: MediaTrack, queue: List<MediaTrack> = listOf(track)) {
        playerController.playTrack(track, queue)
        if (!track.isVideo && _isStudioExpanded.value) {
            loadLyricsForTrack(track)
        }
    }

    fun addToQueue(track: MediaTrack) {
        playerController.addToQueue(track)
        showToast("Added '${track.title}' to queue")
    }

    fun playNext(track: MediaTrack) {
        playerController.playNext(track)
        showToast("'${track.title}' will play next")
    }

    fun jumpToQueueIndex(index: Int) {
        playerController.jumpToQueueIndex(index)
    }

    fun removeFromQueue(index: Int) {
        playerController.removeFromQueue(index)
    }

    fun clearQueue() {
        playerController.clearQueue()
        showToast("Cleared upcoming tracks")
    }

    fun toggleShuffle() {
        playerController.toggleShuffle()
        val enabled = playbackState.value.shuffleEnabled
        showToast(if (enabled) "Shuffle On" else "Shuffle Off")
    }

    fun cycleRepeatMode() {
        playerController.cycleRepeatMode()
        showToast(playbackState.value.repeatMode.label)
    }

    fun playVideoUrl(rawUrl: String, title: String = "Direct Stream Video") {
        val url = rawUrl.trim()
        if (url.isBlank()) {
            showToast("Enter a valid .m3u8 or .mp4 stream URL")
            return
        }
        val isHls = url.contains(".m3u8", ignoreCase = true)
        val item = MediaTrack(
            id = "stream_${url.hashCode()}",
            title = title,
            artist = if (isHls) "HLS Adaptive Stream" else "Direct Cinema Stream",
            album = "m³ Video Player",
            language = MusicLanguage.ENGLISH_GLOBAL,
            backend = StreamingBackend.HLS,
            streamUrl = url,
            fallbackDrawableRes = R.drawable.img_poster_cinema_atmos,
            formatBadge = if (isHls) "HLS .m3u8" else "Direct Video",
            isVideo = true,
            hdrTag = "AI Cinema EQ Active"
        )
        _activeTab.value = AppTab.VIDEO_PLAYER
        playerController.playTrack(item, listOf(item) + _curatedVideos.value)
        showToast("Loaded into m³ Video Player with AI Cinema EQ")
    }

    fun selectAudioTrack(option: AudioTrackOption) {
        playerController.selectAudioTrack(option)
        showToast("Switched audio to ${option.label}")
    }

    private fun loadLyricsForTrack(track: MediaTrack) {
        viewModelScope.launch {
            _lyrics.value = emptyList()
            val lines = lrclibClient.fetchSyncedLyrics(track.title, track.artist)
            _lyrics.value = lines
        }
    }

    // --- Unified Search Tab ---
    fun updateUnifiedSearchQuery(query: String) {
        _searchState.value = _searchState.value.copy(query = query)
        unifiedSearchJob?.cancel()
        if (query.isBlank()) {
            _searchState.value = _searchState.value.copy(results = emptyList(), isSearching = false)
            return
        }
        unifiedSearchJob = viewModelScope.launch {
            delay(400L)
            executeUnifiedSearch(query)
        }
    }

    fun executeUnifiedSearch(query: String = _searchState.value.query) {
        val q = query.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _searchState.value = _searchState.value.copy(query = q, isSearching = true)
            vaultRepo.recordSearch(q)
            val (remoteResults, fromCache) = aggregator.searchAllSources(q, navidromeConfig.value)
            val localMatches = vaultTracks.value.filter {
                it.title.contains(q, true) || it.artist.contains(q, true)
            }
            val videoMatches = _curatedVideos.value.filter {
                it.title.contains(q, true) || it.artist.contains(q, true)
            }
            _searchState.value = _searchState.value.copy(
                isSearching = false,
                isFromCache = fromCache,
                results = (localMatches + videoMatches + remoteResults).distinctBy { it.id }
            )
        }
    }

    fun setSearchBackendFilter(backend: StreamingBackend?) {
        _searchState.value = _searchState.value.copy(selectedBackendFilter = backend)
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch { vaultRepo.removeSearch(query) }
    }

    // --- Download Tab & Offline Vault ---
    fun updateDownloadQuery(query: String) {
        _downloadState.value = _downloadState.value.copy(query = query)
    }

    fun searchDownloadCandidates(query: String = _downloadState.value.query) {
        viewModelScope.launch {
            _downloadState.value = _downloadState.value.copy(isSearching = true, statusMessage = null)
            val list = aggregator.searchLosslessForDownload(query)
            _downloadState.value = _downloadState.value.copy(
                isSearching = false,
                candidates = list,
                statusMessage = if (list.isEmpty()) "No lossless tracks matched '$query'" else null
            )
        }
    }

    fun downloadTrackToVault(track: MediaTrack) {
        viewModelScope.launch {
            val currentMap = _downloadState.value.activeDownloads.toMutableMap()
            currentMap[track.id] = DownloadItemState(track = track, progress = 0.05f, isDownloading = true)
            _downloadState.value = _downloadState.value.copy(activeDownloads = currentMap)

            val result = vaultRepo.downloadToVault(track) { progress ->
                val map = _downloadState.value.activeDownloads.toMutableMap()
                map[track.id] = DownloadItemState(track = track, progress = progress, isDownloading = true)
                _downloadState.value = _downloadState.value.copy(activeDownloads = map)
            }

            val finalMap = _downloadState.value.activeDownloads.toMutableMap()
            result.onSuccess { saved ->
                finalMap[track.id] = DownloadItemState(
                    track = saved,
                    progress = 1f,
                    isDownloading = false,
                    isCompleted = true,
                    localPath = saved.streamUrl
                )
                _downloadState.value = _downloadState.value.copy(activeDownloads = finalMap)
                showToast("Saved ${saved.title} (${saved.formatBadge}) to Offline Vault")
            }.onFailure { err ->
                finalMap[track.id] = DownloadItemState(
                    track = track,
                    progress = 0f,
                    isDownloading = false,
                    error = err.message ?: "Download failed"
                )
                _downloadState.value = _downloadState.value.copy(activeDownloads = finalMap)
                showToast("Download failed: ${err.message}")
            }
        }
    }

    fun deleteFromVault(trackId: String) {
        viewModelScope.launch {
            vaultRepo.deleteFromVault(trackId)
            showToast("Removed from Offline Vault")
        }
    }

    fun importLocalMedia(uri: Uri, isVideo: Boolean) {
        viewModelScope.launch {
            vaultRepo.importLocalUri(uri, isVideo)
                .onSuccess { imported ->
                    showToast("Imported ${imported.title}")
                    if (isVideo) {
                        _activeTab.value = AppTab.VIDEO_PLAYER
                    }
                    playTrack(imported, listOf(imported))
                }
                .onFailure {
                    showToast("Could not open local file: ${it.message}")
                }
        }
    }

    // --- Navidrome Self-Hosted Server ---
    fun connectNavidrome(url: String, username: String, password: String) {
        viewModelScope.launch {
            val testConfig = NavidromeConfig(url.trim().trimEnd('/'), username.trim(), password)
            val (ok, songs) = navidromeClient.pingAndFetchSongs(testConfig)
            if (ok) {
                prefsStore.saveNavidromeConfig(testConfig.serverUrl, testConfig.username, testConfig.password)
                _showNavidromeDialog.value = false
                showToast("Connected to Navidrome (${songs.size} tracks ready)")
                loadHomeFeed()
            } else {
                showToast("Navidrome ping failed — verify server URL & credentials")
            }
        }
    }

    // --- Web Movies Browser & Bridge ---
    fun updateWebUrl(url: String, pageTitle: String? = null, isLoading: Boolean? = null) {
        _webMoviesState.value = _webMoviesState.value.copy(
            currentUrl = url,
            addressBarInput = url,
            pageTitle = pageTitle ?: _webMoviesState.value.pageTitle,
            isLoadingPage = isLoading ?: _webMoviesState.value.isLoadingPage
        )
    }

    fun updateWebAddressInput(input: String) {
        _webMoviesState.value = _webMoviesState.value.copy(addressBarInput = input)
    }

    fun registerDetectedWebStream(streamUrl: String) {
        val clean = streamUrl.trim()
        if (clean.isBlank()) return
        val currentList = _webMoviesState.value.detectedStreamUrls
        if (!currentList.contains(clean)) {
            _webMoviesState.value = _webMoviesState.value.copy(
                detectedStreamUrls = (listOf(clean) + currentList).take(8)
            )
        }
    }

    fun setWebFrameBlockedHint(blocked: Boolean) {
        _webMoviesState.value = _webMoviesState.value.copy(frameBlockedHint = blocked)
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
