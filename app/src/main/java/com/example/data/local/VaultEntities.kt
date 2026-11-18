package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.MediaTrack
import com.example.domain.model.MusicLanguage
import com.example.domain.model.StreamingBackend

@Entity(tableName = "vault_tracks")
data class VaultTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val languageName: String,
    val backendName: String,
    val localFilePath: String,
    val artworkUrl: String?,
    val durationMs: Long,
    val formatBadge: String,
    val isLossless: Boolean,
    val isVideo: Boolean,
    val fileSizeBytes: Long,
    val downloadedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): MediaTrack {
        val lang = runCatching { MusicLanguage.valueOf(languageName) }.getOrDefault(MusicLanguage.LOSSLESS)
        val backend = runCatching { StreamingBackend.valueOf(backendName) }.getOrDefault(StreamingBackend.LOCAL)
        return MediaTrack(
            id = id,
            title = title,
            artist = artist,
            album = album,
            language = lang,
            backend = backend,
            streamUrl = localFilePath,
            downloadUrl = localFilePath,
            artworkUrl = artworkUrl,
            durationMs = durationMs,
            formatBadge = formatBadge,
            isVideo = isVideo,
            isLossless = isLossless,
            isOfflineSaved = true
        )
    }
}

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
