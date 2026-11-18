package com.example.data.remote

import com.example.R
import com.example.data.local.SearchCacheStore
import com.example.domain.model.MediaTrack
import com.example.domain.model.MusicLanguage
import com.example.domain.model.NavidromeConfig
import com.example.domain.model.StreamingBackend
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class StreamingAggregatorRepository(
    private val jioSaavnClient: JioSaavnClient,
    private val audiusClient: AudiusClient,
    private val archiveClient: InternetArchiveClient,
    private val navidromeClient: NavidromeClient,
    private val searchCacheStore: SearchCacheStore? = null
) {

    suspend fun loadHomeFeed(
        language: MusicLanguage,
        query: String,
        navidromeConfig: NavidromeConfig
    ): List<MediaTrack> = coroutineScope {
        val effectiveQuery = query.trim().ifBlank {
            when (language) {
                MusicLanguage.ALL -> "arijit singh"
                MusicLanguage.HINDI -> "hindi bollywood hits"
                MusicLanguage.BANGLA -> "bengali rabindra sangeet"
                MusicLanguage.PUNJABI -> "punjabi diljit hits"
                MusicLanguage.TAMIL -> "tamil anirudh hits"
                MusicLanguage.TELUGU -> "telugu hits"
                MusicLanguage.ENGLISH_GLOBAL -> "electronic synthwave"
                MusicLanguage.LOSSLESS -> "acoustic live concert"
            }
        }

        val saavnDeferred = async {
            if (language == MusicLanguage.ENGLISH_GLOBAL || language == MusicLanguage.LOSSLESS) {
                emptyList()
            } else {
                jioSaavnClient.searchSongs(effectiveQuery, language)
            }
        }

        val audiusDeferred = async {
            if (language == MusicLanguage.ALL || language == MusicLanguage.ENGLISH_GLOBAL) {
                if (query.isBlank()) audiusClient.getTrendingTracks()
                else audiusClient.searchTracks(effectiveQuery)
            } else emptyList()
        }

        val archiveDeferred = async {
            if (language == MusicLanguage.LOSSLESS || language == MusicLanguage.ALL) {
                archiveClient.searchLosslessFlac(
                    if (query.isBlank() && language == MusicLanguage.ALL) "classical acoustic" else effectiveQuery
                )
            } else emptyList()
        }

        val navidromeDeferred = async {
            if (navidromeConfig.isConnected) {
                navidromeClient.pingAndFetchSongs(navidromeConfig, query).second
            } else emptyList()
        }

        buildList {
            addAll(navidromeDeferred.await())
            addAll(saavnDeferred.await())
            addAll(audiusDeferred.await())
            addAll(archiveDeferred.await())
        }.distinctBy { "${it.title.lowercase()}_${it.artist.lowercase()}" }
    }

    suspend fun searchLosslessForDownload(query: String): List<MediaTrack> = coroutineScope {
        val q = query.trim().ifBlank { "concert acoustic" }
        val archiveDeferred = async { archiveClient.searchLosslessFlac(q) }
        val audiusDeferred = async { audiusClient.searchTracks(q) }
        val saavnDeferred = async { jioSaavnClient.searchSongs(q, MusicLanguage.HINDI) }

        buildList {
            addAll(archiveDeferred.await())
            addAll(audiusDeferred.await())
            addAll(saavnDeferred.await())
        }.distinctBy { it.id }
    }

    suspend fun searchAllSources(
        query: String,
        navidromeConfig: NavidromeConfig
    ): Pair<List<MediaTrack>, Boolean> = coroutineScope {
        val q = query.trim().ifBlank { return@coroutineScope emptyList<MediaTrack>() to false }

        val cacheKey = "${q}_${navidromeConfig.isConnected}"
        searchCacheStore?.getCachedResults(cacheKey)?.let { cached ->
            return@coroutineScope cached to true
        }

        val saavn = async { jioSaavnClient.searchSongs(q, MusicLanguage.ALL) }
        val audius = async { audiusClient.searchTracks(q) }
        val archive = async { archiveClient.searchLosslessFlac(q) }
        val navidrome = async {
            if (navidromeConfig.isConnected) navidromeClient.pingAndFetchSongs(navidromeConfig, q).second
            else emptyList()
        }

        val combined = buildList {
            addAll(navidrome.await())
            addAll(saavn.await())
            addAll(audius.await())
            addAll(archive.await())
        }.distinctBy { it.id }

        if (combined.isNotEmpty()) {
            searchCacheStore?.saveResults(cacheKey, combined)
        }
        combined to false
    }

    fun getCuratedVideoCatalog(): List<MediaTrack> = listOf(
        MediaTrack(
            id = "vid_tears_of_steel_hls",
            title = "Tears of Steel (4K Multi-Audio HLS)",
            artist = "Blender Foundation • Sci-Fi Cinema",
            album = "HLS Adaptive Master",
            language = MusicLanguage.ENGLISH_GLOBAL,
            backend = StreamingBackend.HLS,
            streamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            artworkUrl = "https://mango.blender.org/wp-content/uploads/2013/05/01_thom_celia_bridge.jpg",
            fallbackDrawableRes = R.drawable.img_poster_cinema_atmos,
            durationMs = 734_000L,
            formatBadge = "HLS • Multi-Audio",
            isVideo = true,
            hdrTag = "DV/HDR Fallback Ready",
            subtitle = "Live adaptive .m3u8 with multi-track audio switching"
        ),
        MediaTrack(
            id = "vid_big_buck_bunny",
            title = "Big Buck Bunny (60fps Animated)",
            artist = "Blender Open Movie • Animation",
            album = "Open Cinema Collection",
            language = MusicLanguage.ENGLISH_GLOBAL,
            backend = StreamingBackend.HLS,
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            artworkUrl = "https://peach.blender.org/wp-content/uploads/title_anouncement.jpg",
            fallbackDrawableRes = R.drawable.img_poster_cinema_atmos,
            durationMs = 596_000L,
            formatBadge = "HLS 1080p",
            isVideo = true,
            hdrTag = "Cinema Surround EQ",
            subtitle = "Vibrant animated feature streamed via Mux HLS"
        ),
        MediaTrack(
            id = "vid_sintel_animation",
            title = "Sintel — Fantasy Animated Film",
            artist = "Durian Open Movie • Animation",
            album = "Open Cinema Collection",
            language = MusicLanguage.ENGLISH_GLOBAL,
            backend = StreamingBackend.HLS,
            streamUrl = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            artworkUrl = "https://durian.blender.org/wp-content/uploads/2010/06/05.8b_comp_000272.jpg",
            fallbackDrawableRes = R.drawable.img_poster_cinema_atmos,
            durationMs = 888_000L,
            formatBadge = "HLS Multi-Track",
            isVideo = true,
            hdrTag = "Atmos Cinema EQ",
            subtitle = "Epic fantasy short film with multi-language audio"
        ),
        MediaTrack(
            id = "vid_bipbop_dolby_test",
            title = "Apple fMP4 HEVC / Multi-Audio Reference",
            artist = "Dolby / HEVC Adaptive Test Channel",
            album = "HLS v7 Engineering Stream",
            language = MusicLanguage.ENGLISH_GLOBAL,
            backend = StreamingBackend.HLS,
            streamUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
            artworkUrl = null,
            fallbackDrawableRes = R.drawable.img_hero_spatial_banner,
            durationMs = 600_000L,
            formatBadge = "fMP4 HLS v7",
            isVideo = true,
            hdrTag = "HEVC + Multi-Audio",
            subtitle = "Multi-rendition fMP4 reference channel"
        ),
        MediaTrack(
            id = "vid_live_nasa_hls",
            title = "NASA TV Public Live Channel",
            artist = "Live HLS Broadcast • Space & Science",
            album = "Live Channels",
            language = MusicLanguage.ENGLISH_GLOBAL,
            backend = StreamingBackend.HLS,
            streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
            artworkUrl = null,
            fallbackDrawableRes = R.drawable.img_poster_cinema_atmos,
            durationMs = 0L,
            formatBadge = "LIVE .m3u8",
            isVideo = true,
            isLiveStream = true,
            hdrTag = "Live Broadcast",
            subtitle = "24/7 live orbital feed"
        ),
        MediaTrack(
            id = "vid_elephants_dream",
            title = "Elephant's Dream (MP4 Cinema Master)",
            artist = "Orange Open Movie • Experimental",
            album = "Open Cinema Collection",
            language = MusicLanguage.ENGLISH_GLOBAL,
            backend = StreamingBackend.ARCHIVE,
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            artworkUrl = null,
            fallbackDrawableRes = R.drawable.img_hero_spatial_banner,
            durationMs = 653_000L,
            formatBadge = "MP4 Direct",
            isVideo = true,
            hdrTag = "Spatial Cinema",
            subtitle = "Direct MP4 streaming with AI Auto Cinema EQ"
        )
    )
}
