package com.example.data.remote

import android.util.Base64
import com.example.R
import com.example.domain.model.MediaTrack
import com.example.domain.model.MusicLanguage
import com.example.domain.model.StreamingBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec

class JioSaavnClient(private val client: OkHttpClient) {

    private val mirrors = listOf(
        "https://saavn.sumit.co/api/search/songs?query=%s&limit=15",
        "https://saavn.dev/api/search/songs?query=%s&limit=15",
        "https://jiosaavn-api-privatecvc2.vercel.app/search/songs?query=%s&limit=15",
        "https://jiosaavn-api-2-harsh-patel.vercel.app/api/search/songs?query=%s&limit=15"
    )

    suspend fun searchSongs(
        query: String,
        languageHint: MusicLanguage
    ): List<MediaTrack> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")

        for (template in mirrors) {
            val url = String.format(template, encoded)
            val parsed = runCatching { fetchFromMirror(url, languageHint) }.getOrNull()
            if (!parsed.isNullOrEmpty()) return@withContext parsed
        }

        // Direct JioSaavn Web API fallback with DES decryption
        val directUrl =
            "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&n=15&p=1&q=$encoded"
        runCatching { fetchFromDirectApi(directUrl, languageHint) }.getOrDefault(emptyList())
    }

    private fun fetchFromMirror(url: String, languageHint: MusicLanguage): List<MediaTrack> {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Android 15; Mobile) m3player/1.0")
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string().orEmpty()
            if (body.isBlank()) return emptyList()

            val root = JSONObject(body)
            val dataObj = root.optJSONObject("data") ?: root
            val results: JSONArray = dataObj.optJSONArray("results") ?: return emptyList()

            val tracks = mutableListOf<MediaTrack>()
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val id = item.optString("id").ifBlank { "saavn_$i" }
                val title = decodeHtmlEntities(item.optString("name").ifBlank { item.optString("title") })
                if (title.isBlank()) continue

                val artistsObj = item.optJSONObject("artists")
                val primaryArtists = artistsObj?.optJSONArray("primary")
                val artistNames = buildString {
                    if (primaryArtists != null && primaryArtists.length() > 0) {
                        for (j in 0 until primaryArtists.length()) {
                            val name = primaryArtists.optJSONObject(j)?.optString("name").orEmpty()
                            if (name.isNotBlank()) {
                                if (isNotEmpty()) append(", ")
                                append(decodeHtmlEntities(name))
                            }
                        }
                    } else {
                        append(decodeHtmlEntities(item.optString("primaryArtists", "Indian Artist")))
                    }
                }.ifBlank { "JioSaavn Artist" }

                val albumName = decodeHtmlEntities(
                    item.optJSONObject("album")?.optString("name")
                        ?: item.optString("album", "Single")
                )

                val artwork = extractBestImage(item.optJSONArray("image"))
                val streamUrl = extractBestDownloadUrl(item.optJSONArray("downloadUrl"))
                    ?: continue

                val durationSec = item.optLong("duration", 210L)
                val langStr = item.optString("language", languageHint.label)
                val detectedLang = detectLanguage(langStr, languageHint)

                tracks.add(
                    MediaTrack(
                        id = "saavn_$id",
                        title = title,
                        artist = artistNames,
                        album = albumName,
                        language = detectedLang,
                        backend = StreamingBackend.JIOSAAVN,
                        streamUrl = streamUrl,
                        downloadUrl = streamUrl,
                        artworkUrl = artwork,
                        fallbackDrawableRes = R.drawable.img_album_regional_neon,
                        durationMs = durationSec * 1000L,
                        formatBadge = "AAC 320k",
                        isLossless = false
                    )
                )
            }
            return tracks
        }
    }

    private fun fetchFromDirectApi(url: String, languageHint: MusicLanguage): List<MediaTrack> {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Referer", "https://www.jiosaavn.com/")
            .header("Origin", "https://www.jiosaavn.com")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/126.0.0.0 Mobile Safari/537.36")
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string().orEmpty()
            val root = JSONObject(body)
            val results = root.optJSONArray("results") ?: return emptyList()

            val tracks = mutableListOf<MediaTrack>()
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val id = item.optString("id").ifBlank { "direct_$i" }
                val title = decodeHtmlEntities(item.optString("title"))
                val moreInfo = item.optJSONObject("more_info")
                val encryptedUrl = moreInfo?.optString("encrypted_media_url").orEmpty()
                val decrypted = decryptMediaUrl(encryptedUrl) ?: continue
                val artist = decodeHtmlEntities(
                    moreInfo?.optJSONObject("artistMap")
                        ?.optJSONArray("primary_artists")
                        ?.optJSONObject(0)
                        ?.optString("name")
                        ?: item.optString("subtitle", "Artist")
                )
                val album = decodeHtmlEntities(moreInfo?.optString("album").orEmpty())
                val image = item.optString("image").replace("150x150", "500x500").ifBlank { null }
                val durationSec = moreInfo?.optString("duration")?.toLongOrNull() ?: 200L
                val lang = detectLanguage(moreInfo?.optString("language").orEmpty(), languageHint)

                tracks.add(
                    MediaTrack(
                        id = "saavn_$id",
                        title = title,
                        artist = artist,
                        album = album,
                        language = lang,
                        backend = StreamingBackend.JIOSAAVN,
                        streamUrl = decrypted.replace("_96.", "_320."),
                        downloadUrl = decrypted.replace("_96.", "_320."),
                        artworkUrl = image,
                        fallbackDrawableRes = R.drawable.img_album_regional_neon,
                        durationMs = durationSec * 1000L,
                        formatBadge = "AAC 320k"
                    )
                )
            }
            return tracks
        }
    }

    private fun extractBestImage(arr: JSONArray?): String? {
        if (arr == null || arr.length() == 0) return null
        return arr.optJSONObject(arr.length() - 1)?.optString("url")
            ?.ifBlank { arr.optJSONObject(arr.length() - 1)?.optString("link") }
    }

    private fun extractBestDownloadUrl(arr: JSONArray?): String? {
        if (arr == null || arr.length() == 0) return null
        return arr.optJSONObject(arr.length() - 1)?.optString("url")
            ?.ifBlank { arr.optJSONObject(arr.length() - 1)?.optString("link") }
    }

    private fun decryptMediaUrl(encrypted: String): String? {
        if (encrypted.isBlank()) return null
        return runCatching {
            val keySpec = DESKeySpec("38346591".toByteArray(Charsets.UTF_8))
            val keyFactory = SecretKeyFactory.getInstance("DES")
            val secretKey = keyFactory.generateSecret(keySpec)
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decoded = Base64.decode(encrypted, Base64.DEFAULT)
            String(cipher.doFinal(decoded), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun detectLanguage(raw: String, fallback: MusicLanguage): MusicLanguage {
        val lower = raw.lowercase()
        return when {
            lower.contains("hindi") -> MusicLanguage.HINDI
            lower.contains("bengali") || lower.contains("bangla") -> MusicLanguage.BANGLA
            lower.contains("punjabi") -> MusicLanguage.PUNJABI
            lower.contains("tamil") -> MusicLanguage.TAMIL
            lower.contains("telugu") -> MusicLanguage.TELUGU
            lower.contains("english") -> MusicLanguage.ENGLISH_GLOBAL
            else -> if (fallback == MusicLanguage.ALL) MusicLanguage.HINDI else fallback
        }
    }

    private fun decodeHtmlEntities(input: String): String =
        input.replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
}
