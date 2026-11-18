package com.example.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.domain.model.MediaTrack
import com.example.domain.model.MusicLanguage
import com.example.domain.model.StreamingBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class OfflineVaultRepository(
    private val context: Context,
    private val vaultDao: VaultDao,
    private val okHttpClient: OkHttpClient
) {
    private val vaultDir: File by lazy {
        File(context.filesDir, "m3_offline_vault").apply { mkdirs() }
    }

    val vaultTracks: Flow<List<MediaTrack>> = vaultDao.observeVaultTracks().map { list ->
        list.map { it.toDomain() }
    }

    val recentSearches: Flow<List<String>> = vaultDao.observeRecentSearches().map { list ->
        list.map { it.query }
    }

    suspend fun recordSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            vaultDao.upsertSearch(SearchHistoryEntity(query = trimmed))
        }
    }

    suspend fun removeSearch(query: String) {
        vaultDao.deleteSearch(query)
    }

    suspend fun downloadToVault(
        track: MediaTrack,
        onProgress: (Float) -> Unit
    ): Result<MediaTrack> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceUrl = track.downloadUrl?.takeIf { it.startsWith("http") }
                ?: track.streamUrl.takeIf { it.startsWith("http") }
                ?: error("No downloadable URL available for ${track.title}")

            val ext = when {
                sourceUrl.contains(".flac", ignoreCase = true) || track.formatBadge.contains("FLAC", true) -> "flac"
                sourceUrl.contains(".wav", ignoreCase = true) || track.formatBadge.contains("WAV", true) -> "wav"
                sourceUrl.contains(".m4a", ignoreCase = true) -> "m4a"
                else -> "mp3"
            }

            val safeName = track.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val destFile = File(vaultDir, "${safeName}.$ext")

            val request = Request.Builder()
                .url(sourceUrl)
                .header("User-Agent", "m3-music-player/1.0 (OpenSource Android)")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("HTTP ${response.code} downloading ${track.title}")
                }
                val body = response.body ?: error("Empty download response body")
                val totalBytes = body.contentLength()
                var copied = 0L
                val buffer = ByteArray(16 * 1024)

                body.byteStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            copied += read
                            if (totalBytes > 0) {
                                onProgress((copied.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }

            onProgress(1f)
            val fileUri = Uri.fromFile(destFile).toString()
            val badge = when (ext) {
                "flac" -> "FLAC Lossless"
                "wav" -> "WAV Master"
                "m4a" -> "AAC 320k Offline"
                else -> "MP3 Offline"
            }

            val entity = VaultTrackEntity(
                id = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                languageName = track.language.name,
                backendName = track.backend.name,
                localFilePath = fileUri,
                artworkUrl = track.artworkUrl,
                durationMs = track.durationMs,
                formatBadge = badge,
                isLossless = ext == "flac" || ext == "wav",
                isVideo = track.isVideo,
                fileSizeBytes = destFile.length()
            )
            vaultDao.upsertTrack(entity)
            entity.toDomain()
        }
    }

    suspend fun importLocalUri(uri: Uri, isVideo: Boolean): Result<MediaTrack> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            var displayName = "Local Track"
            var sizeBytes = 0L
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                }
            }

            runCatching {
                resolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            val lower = displayName.lowercase()
            val ext = lower.substringAfterLast('.', if (isVideo) "mp4" else "flac")
            val isLossless = ext == "flac" || ext == "wav" || ext == "alac"
            val badge = when {
                ext == "flac" -> "Local FLAC"
                ext == "wav" -> "Local WAV"
                ext == "mkv" -> "Local MKV"
                ext == "webm" -> "Local WebM"
                ext == "mp4" -> "Local MP4"
                else -> "Local ${ext.uppercase()}"
            }

            val cleanTitle = displayName.substringBeforeLast('.').ifBlank { displayName }
            val entity = VaultTrackEntity(
                id = "local_${uri.toString().hashCode()}",
                title = cleanTitle,
                artist = if (isVideo) "Local Cinema" else "Local Device Library",
                album = "Device Storage",
                languageName = if (isLossless) MusicLanguage.LOSSLESS.name else MusicLanguage.ENGLISH_GLOBAL.name,
                backendName = StreamingBackend.LOCAL.name,
                localFilePath = uri.toString(),
                artworkUrl = null,
                durationMs = 0L,
                formatBadge = badge,
                isLossless = isLossless,
                isVideo = isVideo,
                fileSizeBytes = sizeBytes
            )
            vaultDao.upsertTrack(entity)
            entity.toDomain()
        }
    }

    suspend fun deleteFromVault(trackId: String) = withContext(Dispatchers.IO) {
        val existing = vaultDao.getTrackById(trackId)
        if (existing != null && existing.localFilePath.startsWith("file://")) {
            val file = Uri.parse(existing.localFilePath).path?.let { File(it) }
            if (file != null && file.exists()) {
                file.delete()
            }
        }
        vaultDao.deleteTrackById(trackId)
    }
}
