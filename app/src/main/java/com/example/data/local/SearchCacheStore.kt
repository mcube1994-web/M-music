package com.example.data.local

import android.content.Context
import com.example.R
import com.example.domain.model.MediaTrack
import com.example.domain.model.MusicLanguage
import com.example.domain.model.StreamingBackend
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent query-keyed search cache (Android equivalent of localStorage query cache)
 * so repeated searches load instantly without extra network requests or API quota use.
 */
class SearchCacheStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val memoryCache = LinkedHashMap<String, List<MediaTrack>>(MAX_ENTRIES, 0.75f, true)

    @Synchronized
    fun getCachedResults(rawQuery: String, nowMs: Long = System.currentTimeMillis()): List<MediaTrack>? {
        val key = normalizeKey(rawQuery) ?: return null
        memoryCache[key]?.let { return it }

        val rawJson = prefs.getString(key, null) ?: return null
        return runCatching {
            val obj = JSONObject(rawJson)
            val savedAt = obj.optLong("ts", 0L)
            if (nowMs - savedAt > CACHE_TTL_MS) {
                prefs.edit().remove(key).apply()
                return null
            }
            val arr = obj.optJSONArray("tracks") ?: return null
            val parsed = ArrayList<MediaTrack>(arr.length())
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                parsed.add(jsonToTrack(item))
            }
            if (parsed.isNotEmpty()) {
                memoryCache[key] = parsed
                parsed
            } else {
                null
            }
        }.getOrNull()
    }

    @Synchronized
    fun saveResults(rawQuery: String, tracks: List<MediaTrack>, nowMs: Long = System.currentTimeMillis()) {
        val key = normalizeKey(rawQuery) ?: return
        if (tracks.isEmpty()) return

        memoryCache[key] = tracks
        val arr = JSONArray()
        tracks.take(40).forEach { arr.put(trackToJson(it)) }
        val payload = JSONObject()
            .put("ts", nowMs)
            .put("tracks", arr)
            .toString()
        prefs.edit().putString(key, payload).apply()
    }

    @Synchronized
    fun clear() {
        memoryCache.clear()
        prefs.edit().clear().apply()
    }

    private fun normalizeKey(query: String): String? {
        val q = query.trim().lowercase()
        return if (q.isBlank()) null else "q_$q"
    }

    private fun trackToJson(t: MediaTrack): JSONObject = JSONObject().apply {
        put("id", t.id)
        put("title", t.title)
        put("artist", t.artist)
        put("album", t.album)
        put("language", t.language.name)
        put("backend", t.backend.name)
        put("streamUrl", t.streamUrl)
        put("downloadUrl", t.downloadUrl ?: JSONObject.NULL)
        put("artworkUrl", t.artworkUrl ?: JSONObject.NULL)
        put("durationMs", t.durationMs)
        put("formatBadge", t.formatBadge)
        put("isVideo", t.isVideo)
        put("isLossless", t.isLossless)
        put("isOfflineSaved", t.isOfflineSaved)
        put("isLiveStream", t.isLiveStream)
        put("hdrTag", t.hdrTag ?: JSONObject.NULL)
        put("subtitle", t.subtitle ?: JSONObject.NULL)
    }

    private fun jsonToTrack(o: JSONObject): MediaTrack {
        val lang = runCatching { MusicLanguage.valueOf(o.optString("language")) }
            .getOrDefault(MusicLanguage.ENGLISH_GLOBAL)
        val backend = runCatching { StreamingBackend.valueOf(o.optString("backend")) }
            .getOrDefault(StreamingBackend.JIOSAAVN)
        return MediaTrack(
            id = o.optString("id"),
            title = o.optString("title"),
            artist = o.optString("artist"),
            album = o.optString("album"),
            language = lang,
            backend = backend,
            streamUrl = o.optString("streamUrl"),
            downloadUrl = o.optString("downloadUrl").takeIf { it.isNotBlank() && it != "null" },
            artworkUrl = o.optString("artworkUrl").takeIf { it.isNotBlank() && it != "null" },
            fallbackDrawableRes = if (backend == StreamingBackend.ARCHIVE) {
                R.drawable.img_lossless_vault_art
            } else {
                R.drawable.img_album_regional_neon
            },
            durationMs = o.optLong("durationMs", 0L),
            formatBadge = o.optString("formatBadge", "AAC 320k"),
            isVideo = o.optBoolean("isVideo", false),
            isLossless = o.optBoolean("isLossless", false),
            isOfflineSaved = o.optBoolean("isOfflineSaved", false),
            isLiveStream = o.optBoolean("isLiveStream", false),
            hdrTag = o.optString("hdrTag").takeIf { it.isNotBlank() && it != "null" },
            subtitle = o.optString("subtitle").takeIf { it.isNotBlank() && it != "null" }
        )
    }

    companion object {
        private const val PREFS_NAME = "m3_search_query_cache"
        private const val MAX_ENTRIES = 24
        private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L
    }
}
