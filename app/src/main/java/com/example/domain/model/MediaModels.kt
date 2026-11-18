package com.example.domain.model

import androidx.annotation.DrawableRes
import com.example.R

enum class AppTab(val label: String) {
    HOME("Home"),
    VIDEO_PLAYER("Video Player"),
    WEB_MOVIES("Web Movies"),
    DOWNLOAD("Download"),
    SEARCH("Search")
}

enum class MusicLanguage(val label: String, val queryHint: String) {
    ALL("All Mix", "bollywood hindi punjabi bengali english"),
    HINDI("Hindi", "hindi bollywood arijit shreya"),
    BANGLA("Bangla", "bengali bangla rabindra sangeet"),
    PUNJABI("Punjabi", "punjabi hits diljit ap dhillon"),
    TAMIL("Tamil", "tamil anirudh ar rahman"),
    TELUGU("Telugu", "telugu tollywood dsp thaman"),
    ENGLISH_GLOBAL("English Global", "english pop synthwave spatial"),
    LOSSLESS("Lossless FLAC", "flac lossless acoustic")
}

enum class StreamingBackend(val displayName: String) {
    JIOSAAVN("JioSaavn"),
    ITUNES("iTunes"),
    AUDIUS("Audius"),
    ARCHIVE("Internet Archive"),
    NAVIDROME("Navidrome"),
    LOCAL("Local File"),
    HLS("HLS Stream")
}

enum class VisualizerMode(val label: String, val badge: String) {
    VIZZ_MESH_GRID("Mesh Grid", "vizz.fm 3D"),
    VIZZ_SPHERICAL_SHADER("Spherical Shader", "vizz.fm Shader"),
    VIZZ_AUDIO_TUNNEL("Audio Tunnel", "vizz.fm 3D"),
    VIZZ_CYCLONE_EYES("Cyclone Eyes", "vizz.fm Vortex"),
    VIZZ_EMBER_ROSE("Ember Rose", "vizz.fm Shader"),
    VIZZ_GLACIER_SPIRAL("Glacier Spiral", "vizz.fm Spiral"),
    VIZZ_LIVE_WEB("vizz.fm/app Live", "WebGL Live")
}

enum class VisualizerPalette(
    val label: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val tertiaryHex: Long
) {
    CYBER_CYAN("Cyber Cyan", 0xFF22D3EE, 0xFF7C6BFF, 0xFFF59E0B),
    DEEPSOUND_VIOLET("DeepSound Violet", 0xFF9E92FF, 0xFFEC4899, 0xFF22D3EE),
    ACID_PSYCHEDELIC("Acid Psychedelic", 0xFFF43F5E, 0xFFA855F7, 0xFF10B981),
    FIRESTORM_GOLD("Firestorm Gold", 0xFFF59E0B, 0xFFFB7185, 0xFFFDE047),
    EMERALD_MATRIX("Emerald Matrix", 0xFF10B981, 0xFF22D3EE, 0xFFA3E635)
}

enum class AppearanceSkin(
    val label: String,
    val tagline: String,
    val bgHex: Long,
    val surfaceHex: Long,
    val cardHex: Long,
    val primaryHex: Long,
    val secondaryHex: Long,
    val tertiaryHex: Long
) {
    LEVYRA_DEEPSOUND(
        label = "Levyra DeepSound",
        tagline = "Obsidian audiophile depth with electric violet & cyan",
        bgHex = 0xFF06080E,
        surfaceHex = 0xFF0D121E,
        cardHex = 0xCC131A2B,
        primaryHex = 0xFF7C6BFF,
        secondaryHex = 0xFF22D3EE,
        tertiaryHex = 0xFFF59E0B
    ),
    APPLE_MUSIC_GLASS(
        label = "Apple Frosted Glass",
        tagline = "Translucent slate with vivid crimson-coral accents",
        bgHex = 0xFF0B0B10,
        surfaceHex = 0xFF16161F,
        cardHex = 0xCC1F1F2B,
        primaryHex = 0xFFFA2D48,
        secondaryHex = 0xFFFF6482,
        tertiaryHex = 0xFF64D2FF
    ),
    YOUTUBE_MOOD_DARK(
        label = "Deep Mood Studio",
        tagline = "Pure midnight AMOLED with vibrant emerald & amber tiles",
        bgHex = 0xFF030305,
        surfaceHex = 0xFF0E1015,
        cardHex = 0xCC181B22,
        primaryHex = 0xFF10B981,
        secondaryHex = 0xFF38BDF8,
        tertiaryHex = 0xFFF59E0B
    ),
    NEON_VIZZ_STUDIO(
        label = "Vizz.fm Neon Prism",
        tagline = "Cyber-stage magenta, ultraviolet & laser cyan",
        bgHex = 0xFF080514,
        surfaceHex = 0xFF120B26,
        cardHex = 0xCC1D133A,
        primaryHex = 0xFFD946EF,
        secondaryHex = 0xFF06B6D4,
        tertiaryHex = 0xFFFACC15
    )
}

enum class AiEqMode(
    val label: String,
    val description: String,
    val defaultGainsDb: List<Float>
) {
    AUTO_DETECT(
        label = "AI Auto",
        description = "Adapts dynamically between Music & Cinema",
        defaultGainsDb = listOf(1.5f, 1.0f, 0.5f, 0f, 0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 2.0f)
    ),
    BASS_BOOST(
        label = "Bass Boost",
        description = "Deep sub-bass punch for EDM, Punjabi & hip-hop",
        defaultGainsDb = listOf(6.0f, 5.2f, 3.8f, 1.5f, 0f, -0.5f, 0.5f, 1.5f, 2.0f, 2.5f)
    ),
    VOCAL_CLARITY(
        label = "Vocal Clarity",
        description = "Forward midrange presence for Bollywood & acoustic",
        defaultGainsDb = listOf(-1.0f, -0.5f, 0.5f, 1.5f, 3.0f, 4.2f, 4.0f, 2.5f, 1.5f, 1.0f)
    ),
    SPATIAL_ATMOS(
        label = "Spatial Atmos",
        description = "Wide 7.1.4 soundstage & shimmering height harmonics",
        defaultGainsDb = listOf(3.0f, 2.2f, 1.0f, 0f, -0.5f, 1.5f, 3.0f, 4.0f, 5.0f, 4.5f)
    ),
    CINEMA_SURROUND(
        label = "Cinema Surround",
        description = "LFE rumble + crisp dialogue center channel for movies",
        defaultGainsDb = listOf(5.0f, 4.0f, 2.0f, -1.0f, 1.5f, 3.5f, 3.0f, 2.5f, 3.5f, 4.0f)
    ),
    FLAT_HIFI(
        label = "Flat Hi-Fi",
        description = "Bit-transparent audiophile reference curve",
        defaultGainsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    )
}

data class MediaTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val language: MusicLanguage = MusicLanguage.ENGLISH_GLOBAL,
    val backend: StreamingBackend,
    val streamUrl: String,
    val downloadUrl: String? = null,
    val artworkUrl: String? = null,
    @DrawableRes val fallbackDrawableRes: Int = R.drawable.img_album_regional_neon,
    val durationMs: Long = 0L,
    val formatBadge: String = "AAC 320k",
    val isVideo: Boolean = false,
    val isLossless: Boolean = false,
    val isOfflineSaved: Boolean = false,
    val isLiveStream: Boolean = false,
    val hdrTag: String? = null,
    val subtitle: String? = null
)

data class DownloadItemState(
    val track: MediaTrack,
    val progress: Float = 0f,
    val isDownloading: Boolean = false,
    val isCompleted: Boolean = false,
    val localPath: String? = null,
    val error: String? = null
)

data class HlsAudioRendition(
    val name: String,
    val groupId: String,
    val channels: String,
    val uri: String,
    val isDefault: Boolean
)

data class HlsVideoVariant(
    val bandwidth: Long,
    val resolution: String,
    val codecs: String,
    val supplementalCodecs: String?,
    val audioGroup: String?,
    val uri: String
)

data class HlsManifestInfo(
    val version: Int,
    val audioRenditions: List<HlsAudioRendition>,
    val videoVariants: List<HlsVideoVariant>,
    val rawManifest: String
)

data class DolbyFormatSupport(
    val key: String,
    val label: String,
    val mimeOrCodec: String,
    val hardwareSupported: Boolean,
    val webMediaCapabilitiesSupported: Boolean? = null
)

data class DolbyCapabilitiesState(
    val spatializerAvailable: Boolean = false,
    val spatializerEnabled: Boolean = false,
    val userSpatializerEnabled: Boolean = true,
    val compatibleHeadphonesConnected: Boolean = false,
    val connectedHeadphoneName: String? = null,
    val canBeSpatialized: Boolean = false,
    val headTrackerAvailable: Boolean = false,
    val immersiveLevelLabel: String = "Checking...",
    val maxOutputChannelCount: Int = 2,
    val hdrTypesLabel: String = "SDR",
    val formats: List<DolbyFormatSupport> = emptyList(),
    val lastPcmChannelsSampled: Int = 2,
    val lastPcmPeakDb: Float = -96f
)

data class NavidromeConfig(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isConnected: Boolean = false,
    val statusMessage: String = "Not configured"
)

data class SyncedLyricLine(
    val timeMs: Long,
    val text: String
)

data class AudioTrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val codecs: String,
    val channels: Int,
    val isSelected: Boolean
)
