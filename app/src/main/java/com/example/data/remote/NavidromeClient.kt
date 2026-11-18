package com.example.data.remote

import com.example.R
import com.example.domain.model.MediaTrack
import com.example.domain.model.MusicLanguage
import com.example.domain.model.NavidromeConfig
import com.example.domain.model.StreamingBackend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.security.MessageDigest
import kotlin.random.Random

class NavidromeClient(private val client: OkHttpClient) {

    suspend fun pingAndFetchSongs(config: NavidromeConfig, query: String = ""): Pair<Boolean, List<MediaTrack>> =
        withContext(Dispatchers.IO) {
            if (config.serverUrl.isBlank() || config.username.isBlank()) {
                return@withContext false to emptyList()
            }
            val authParams = buildAuthParams(config.username, config.password)
            val base = config.serverUrl.trimEnd('/')

            val pingOk = runCatching {
                val pingReq = Request.Builder().url("$base/rest/ping.view?$authParams").build()
                client.newCall(pingReq).execute().use { resp ->
                    if (!resp.isSuccessful) return@use false
                    val json = JSONObject(resp.body?.string().orEmpty())
                    json.optJSONObject("subsonic-response")?.optString("status") == "ok"
                }
            }.getOrDefault(false)

            if (!pingOk) return@withContext false to emptyList()

            val songsUrl = if (query.isNotBlank()) {
                val encoded = URLEncoder.encode(query.trim(), "UTF-8")
                "$base/rest/search3.view?$authParams&query=$encoded&songCount=20"
            } else {
                "$base/rest/getRandomSongs.view?$authParams&size=20"
            }

            val tracks = runCatching {
                val req = Request.Builder().url(songsUrl).build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use emptyList()
                    val subsonic = JSONObject(resp.body?.string().orEmpty())
                        .optJSONObject("subsonic-response") ?: return@use emptyList()
                    val songs = if (query.isNotBlank()) {
                        subsonic.optJSONObject("searchResult3")?.optJSONArray("song")
                    } else {
                        subsonic.optJSONObject("randomSongs")?.optJSONArray("song")
                    } ?: return@use emptyList()

                    val list = mutableListOf<MediaTrack>()
                    for (i in 0 until songs.length()) {
                        val s = songs.optJSONObject(i) ?: continue
                        val id = s.optString("id")
                        if (id.isBlank()) continue
                        val coverId = s.optString("coverArt")
                        val suffix = s.optString("suffix", "flac").uppercase()
                        val bitRate = s.optInt("bitRate", 0)
                        val streamUrl = "$base/rest/stream.view?$authParams&id=$id"
                        val artUrl = if (coverId.isNotBlank()) {
                            "$base/rest/getCoverArt.view?$authParams&id=$coverId&size=500"
                        } else null

                        list.add(
                            MediaTrack(
                                id = "navidrome_$id",
                                title = s.optString("title", "Navidrome Track"),
                                artist = s.optString("artist", "Self-Hosted"),
                                album = s.optString("album", "Navidrome Library"),
                                language = MusicLanguage.LOSSLESS,
                                backend = StreamingBackend.NAVIDROME,
                                streamUrl = streamUrl,
                                downloadUrl = streamUrl,
                                artworkUrl = artUrl,
                                fallbackDrawableRes = R.drawable.img_lossless_vault_art,
                                durationMs = s.optLong("duration", 180L) * 1000L,
                                formatBadge = if (bitRate > 0) "$suffix ${bitRate}k" else suffix,
                                isLossless = suffix == "FLAC" || suffix == "WAV" || suffix == "ALAC"
                            )
                        )
                    }
                    list
                }
            }.getOrDefault(emptyList())

            true to tracks
        }

    private fun buildAuthParams(user: String, password: String): String {
        val salt = Random.nextInt(100000, 999999).toString()
        val token = md5(password + salt)
        val u = URLEncoder.encode(user, "UTF-8")
        return "u=$u&t=$token&s=$salt&v=1.16.1&c=m3musicplayer&f=json"
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
