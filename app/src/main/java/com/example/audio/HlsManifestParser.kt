package com.example.audio

import com.example.domain.model.HlsAudioRendition
import com.example.domain.model.HlsManifestInfo
import com.example.domain.model.HlsVideoVariant

object HlsManifestParser {

    val DEFAULT_DOLBY_HLS_MANIFEST = """
        #EXTM3U
        #EXT-X-VERSION:7
        # Audio Renditions
        #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="dolby",NAME="Atmos AC-4",DEFAULT=YES,AUTOSELECT=YES,URI="ac4_atmos.m3u8"
        #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="dolby",NAME="Atmos DD+",DEFAULT=NO,AUTOSELECT=YES,CHANNELS="16/JOC",URI="ec3_atmos.m3u8"
        #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="dolby",NAME="Surround DD+",DEFAULT=NO,AUTOSELECT=YES,CHANNELS="6",URI="ec3_51.m3u8"
        #EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID="dolby",NAME="Stereo AAC",DEFAULT=NO,AUTOSELECT=YES,CHANNELS="2",URI="aac_20.m3u8"
        # Video Variants (Dolby Vision Profile 8 with HDR10 fallback + AVC SDR fallback)
        #EXT-X-STREAM-INF:BANDWIDTH=16000000,RESOLUTION=3840x2160,CODECS="hvc1.2.4.L150.90,ac-4.02.01.03",SUPPLEMENTAL-CODECS="dvh1.08.06/db4h",AUDIO="dolby"
        dv_p8_4k.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=16000000,RESOLUTION=3840x2160,CODECS="hvc1.2.4.L150.90,ec-3",SUPPLEMENTAL-CODECS="dvh1.08.06/db4h",AUDIO="dolby"
        dv_p8_4k.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=6000000,RESOLUTION=1920x1080,CODECS="avc1.640028,mp4a.40.2",AUDIO="dolby"
        sdr_1080p.m3u8
    """.trimIndent()

    fun parse(manifest: String = DEFAULT_DOLBY_HLS_MANIFEST): HlsManifestInfo {
        val lines = manifest.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var version = 1
        val audioRenditions = mutableListOf<HlsAudioRendition>()
        val videoVariants = mutableListOf<HlsVideoVariant>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.startsWith("#EXT-X-VERSION:") -> {
                    version = line.substringAfter(":").toIntOrNull() ?: 1
                }
                line.startsWith("#EXT-X-MEDIA:") -> {
                    val attrs = parseAttributes(line.substringAfter(":"))
                    if (attrs["TYPE"] == "AUDIO") {
                        audioRenditions.add(
                            HlsAudioRendition(
                                name = attrs["NAME"] ?: "Audio",
                                groupId = attrs["GROUP-ID"] ?: "default",
                                channels = attrs["CHANNELS"] ?: "Multi",
                                uri = attrs["URI"] ?: "",
                                isDefault = attrs["DEFAULT"] == "YES"
                            )
                        )
                    }
                }
                line.startsWith("#EXT-X-STREAM-INF:") -> {
                    val attrs = parseAttributes(line.substringAfter(":"))
                    val nextUri = if (i + 1 < lines.size && !lines[i + 1].startsWith("#")) {
                        i++
                        lines[i]
                    } else ""
                    videoVariants.add(
                        HlsVideoVariant(
                            bandwidth = attrs["BANDWIDTH"]?.toLongOrNull() ?: 0L,
                            resolution = attrs["RESOLUTION"] ?: "Auto",
                            codecs = attrs["CODECS"] ?: "",
                            supplementalCodecs = attrs["SUPPLEMENTAL-CODECS"],
                            audioGroup = attrs["AUDIO"],
                            uri = nextUri
                        )
                    )
                }
            }
            i++
        }

        return HlsManifestInfo(
            version = version,
            audioRenditions = audioRenditions,
            videoVariants = videoVariants,
            rawManifest = manifest
        )
    }

    private fun parseAttributes(raw: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val regex = Regex("""([A-Z0-9\-]+)=("([^"]*)"|([^,]*))""")
        regex.findAll(raw).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[3].ifEmpty { match.groupValues[4] }
            map[key] = value
        }
        return map
    }
}
