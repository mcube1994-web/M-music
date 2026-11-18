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

class AudiusClient(private val client: OkHttpClient) {

    private val baseUrls = listOf(
        "https://api.audius.co/v1",
        "https://discoveryprovider.audius.co/v1"
    )

    suspend fun searchTracks(query: String): List<MediaTrack> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        for (base in baseUrls) {
            val res = fetchTracks(base, "$base/tracks/search?query=$encoded&app_name=m3musicplayer")
            if (res.isNotEmpty()) return@withContext res
        }
        emptyList()
    }

    suspend fun getTrendingTracks(): List<MediaTrack> = withContext(Dispatchers.IO) {
        for (base in baseUrls) {
            val res = fetchTracks(base, "$base/tracks/trending?app_name=m3musicplayer&limit=12")
            if (res.isNotEmpty()) return@withContext res
        }
        emptyList()
    }

    private fun fetchTracks(activeBaseUrl: String, url: String): List<MediaTrack> {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList()
                val body = resp.body?.string().orEmpty()
                val root = JSONObject(body)
                val data = root.optJSONArray("data") ?: return@use emptyList()
                val list = mutableListOf<MediaTrack>()
                for (i in 0 until minOf(data.length(), 15)) {
                    val item = data.optJSONObject(i) ?: continue
                    val id = item.optString("id")
                    if (id.isBlank()) continue
                    val title = item.optString("title")
                    val user = item.optJSONObject("user")
                    val artist = user?.optString("name").orEmpty().ifBlank { "Audius Creator" }
                    val artworkObj = item.optJSONObject("artwork")
                    val artUrl = artworkObj?.optString("480x480")
                        ?.ifBlank { artworkObj.optString("150x150") }
                    val durationSec = item.optLong("duration", 180L)
                    val genre = item.optString("genre", "Electronic")
                    val streamUrl = "$activeBaseUrl/tracks/$id/stream?app_name=m3musicplayer"

                    list.add(
                        MediaTrack(
                            id = "audius_$id",
                            title = title,
                            artist = artist,
                            album = genre,
                            language = MusicLanguage.ENGLISH_GLOBAL,
                            backend = StreamingBackend.AUDIUS,
                            streamUrl = streamUrl,
                            downloadUrl = streamUrl,
                            artworkUrl = artUrl,
                            fallbackDrawableRes = R.drawable.img_hero_spatial_banner,
                            durationMs = durationSec * 1000L,
                            formatBadge = "Audius 320k",
                            subtitle = genre
                        )
                    )
                }
                list
            }
        }.getOrDefault(emptyList())
    }
}
