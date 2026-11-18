package com.example.data.remote

import com.example.R
import com.example.domain.model.MediaTrack
import com.example.domain.model.MusicLanguage
import com.example.domain.model.StreamingBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class InternetArchiveClient(private val client: OkHttpClient) {

    suspend fun searchLosslessFlac(query: String): List<MediaTrack> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanQuery = query.trim().ifBlank { "acoustic" }
            val encodedQ = URLEncoder.encode("($cleanQuery) AND mediatype:audio", "UTF-8")
            val searchUrl =
                "https://archive.org/advancedsearch.php?q=$encodedQ&fl[]=identifier,title,creator&rows=6&output=json"
            val req = Request.Builder()
                .url(searchUrl)
                .header("Accept", "application/json")
                .build()

            val identifiers = mutableListOf<Triple<String, String, String>>()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use
                val body = resp.body?.string().orEmpty()
                val docs = JSONObject(body).optJSONObject("response")?.optJSONArray("docs")
                    ?: return@use
                for (i in 0 until docs.length()) {
                    val doc = docs.optJSONObject(i) ?: continue
                    val id = doc.optString("identifier")
                    val title = doc.optString("title", id)
                    val creator = doc.optString("creator", "Archive Master")
                    if (id.isNotBlank()) identifiers.add(Triple(id, title, creator))
                }
            }

            coroutineScope {
                identifiers.map { (identifier, albumTitle, creator) ->
                    async { fetchItemAudioFiles(identifier, albumTitle, creator) }
                }.awaitAll().flatten().take(18)
            }
        }.getOrDefault(emptyList())
    }

    private fun fetchItemAudioFiles(
        identifier: String,
        albumTitle: String,
        creator: String
    ): List<MediaTrack> {
        return runCatching {
            val metaUrl = "https://archive.org/metadata/$identifier"
            val req = Request.Builder().url(metaUrl).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList()
                val root = JSONObject(resp.body?.string().orEmpty())
                val server = root.optString("server")
                val dir = root.optString("dir")
                val files = root.optJSONArray("files") ?: return@use emptyList()
                if (server.isBlank() || dir.isBlank()) return@use emptyList()

                val results = mutableListOf<MediaTrack>()
                for (i in 0 until files.length()) {
                    val f = files.optJSONObject(i) ?: continue
                    val fileName = f.optString("name")
                    val format = f.optString("format")
                    val lower = fileName.lowercase()
                    val isFlac = lower.endsWith(".flac") || format.equals("Flac", ignoreCase = true)
                    val isWav = lower.endsWith(".wav")
                    val isMp3 = lower.endsWith(".mp3")

                    if (!isFlac && !isWav && !isMp3) continue
                    // Prefer lossless FLAC/WAV tracks first; cap per item to avoid overwhelming list
                    val encodedName = fileName.split("/").joinToString("/") {
                        URLEncoder.encode(it, "UTF-8").replace("+", "%20")
                    }
                    val streamUrl = "https://$server$dir/$encodedName"
                    val cleanTitle = f.optString("title").ifBlank {
                        fileName.substringAfterLast("/").substringBeforeLast(".")
                    }
                    val durationSec = f.optString("length").toDoubleOrNull()?.toLong() ?: 240L
                    val badge = when {
                        isFlac -> "FLAC Lossless"
                        isWav -> "WAV Master"
                        else -> "Archive MP3"
                    }

                    results.add(
                        MediaTrack(
                            id = "ia_${identifier}_${fileName.hashCode()}",
                            title = cleanTitle,
                            artist = f.optString("artist").ifBlank { creator },
                            album = albumTitle,
                            language = MusicLanguage.LOSSLESS,
                            backend = StreamingBackend.ARCHIVE,
                            streamUrl = streamUrl,
                            downloadUrl = streamUrl,
                            artworkUrl = "https://archive.org/services/img/$identifier",
                            fallbackDrawableRes = R.drawable.img_lossless_vault_art,
                            durationMs = durationSec * 1000L,
                            formatBadge = badge,
                            isLossless = isFlac || isWav
                        )
                    )
                    if (results.size >= 4) break
                }
                results
            }
        }.getOrDefault(emptyList())
    }
}
