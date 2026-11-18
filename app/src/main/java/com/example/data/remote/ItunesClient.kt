package com.example.data.remote

import com.example.R
import com.example.domain.model.MediaTrack
import com.example.domain.model.MusicLanguage
import com.example.domain.model.StreamingBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class ItunesClient(private val client: OkHttpClient) {

    suspend fun searchTracks(
        query: String,
        countryCode: String = "IN",
        languageHint: MusicLanguage = MusicLanguage.HINDI
    ): List<MediaTrack> = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url =
                "https://itunes.apple.com/search?term=$encoded&country=$countryCode&media=music&entity=song&limit=12"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList()
                val body = resp.body?.string().orEmpty()
                val root = JSONObject(body)
                val results = root.optJSONArray("results") ?: return@use emptyList()
                val list = mutableListOf<MediaTrack>()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val streamUrl = item.optString("previewUrl")
                    if (streamUrl.isBlank()) continue
                    val trackId = item.optLong("trackId", i.toLong())
                    val title = item.optString("trackName")
                    val artist = item.optString("artistName")
                    val album = item.optString("collectionName")
                    val art = item.optString("artworkUrl100").replace("100x100", "600x600").ifBlank { null }
                    val durationMs = item.optLong("trackTimeMillis", 30_000L)
                    val genre = item.optString("primaryGenreName")
                    val lang = inferLanguage(genre, countryCode, languageHint)

                    list.add(
                        MediaTrack(
                            id = "itunes_${countryCode}_$trackId",
                            title = title,
                            artist = artist,
                            album = album,
                            language = lang,
                            backend = StreamingBackend.ITUNES,
                            streamUrl = streamUrl,
                            downloadUrl = streamUrl,
                            artworkUrl = art,
                            fallbackDrawableRes = R.drawable.img_album_regional_neon,
                            durationMs = durationMs,
                            formatBadge = "iTunes $countryCode • M4A",
                            subtitle = genre
                        )
                    )
                }
                list
            }
        }.getOrDefault(emptyList())
    }

    private fun inferLanguage(genre: String, country: String, fallback: MusicLanguage): MusicLanguage {
        val g = genre.lowercase()
        return when {
            g.contains("bollywood") || g.contains("hindi") -> MusicLanguage.HINDI
            g.contains("bengali") || g.contains("bangla") -> MusicLanguage.BANGLA
            g.contains("punjabi") -> MusicLanguage.PUNJABI
            g.contains("tamil") -> MusicLanguage.TAMIL
            g.contains("telugu") -> MusicLanguage.TELUGU
            country.equals("US", ignoreCase = true) -> MusicLanguage.ENGLISH_GLOBAL
            else -> if (fallback == MusicLanguage.ALL) MusicLanguage.HINDI else fallback
        }
    }
}
