package com.example.data.remote

import com.example.domain.model.SyncedLyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder

class LrclibClient(private val client: OkHttpClient) {

    suspend fun fetchSyncedLyrics(title: String, artist: String): List<SyncedLyricLine> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanTitle = title.replace(Regex("\\(.*?\\)|\\[.*?]"), "").trim()
                val q = URLEncoder.encode("$cleanTitle $artist", "UTF-8")
                val url = "https://lrclib.net/api/search?q=$q"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "m3-music-player/1.0")
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use emptyList()
                    val arr = JSONArray(resp.body?.string().orEmpty())
                    if (arr.length() == 0) return@use emptyList()
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val synced = obj.optString("syncedLyrics")
                        if (synced.isNotBlank()) {
                            return@use parseLrc(synced)
                        }
                        val plain = obj.optString("plainLyrics")
                        if (plain.isNotBlank()) {
                            return@use plain.lines()
                                .filter { it.isNotBlank() }
                                .mapIndexed { idx, line -> SyncedLyricLine(idx * 4000L, line) }
                        }
                    }
                    emptyList()
                }
            }.getOrDefault(emptyList())
        }

    private fun parseLrc(lrc: String): List<SyncedLyricLine> {
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)")
        return lrc.lines().mapNotNull { line ->
            val match = regex.find(line.trim()) ?: return@mapNotNull null
            val min = match.groupValues[1].toLongOrNull() ?: 0L
            val sec = match.groupValues[2].toLongOrNull() ?: 0L
            val msPart = match.groupValues[3]
            val ms = if (msPart.length == 2) (msPart.toLongOrNull() ?: 0L) * 10L
            else (msPart.toLongOrNull() ?: 0L)
            val text = match.groupValues[4].trim()
            if (text.isBlank()) null else SyncedLyricLine((min * 60 + sec) * 1000L + ms, text)
        }
    }
}
