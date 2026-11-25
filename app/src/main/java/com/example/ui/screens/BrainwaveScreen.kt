package com.example.ui.screens

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.SpatialCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin

private enum class BrainPreset(val title: String, val hz: Double, val note: String, val accent: Color) {
    DELTA("Delta", 2.0, "2 Hz • Deep rest ambience", Color(0xFF7C6BFF)),
    THETA("Theta", 6.0, "6 Hz • Calm focus ambience", Color(0xFF22D3EE)),
    ALPHA("Alpha", 10.0, "10 Hz • Relaxed alertness", Color(0xFFEC4899)),
    BETA("Beta", 18.0, "18 Hz • Gentle concentration", Color(0xFFF59E0B))
}

@Composable
fun BrainwaveScreen(modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf(BrainPreset.ALPHA) }
    var playing by remember { mutableStateOf(false) }
    val transition = rememberInfiniteTransition(label = "brainwave-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "brainwave-glow"
    )

    LaunchedEffect(playing, selected) {
        if (!playing) return@LaunchedEffect
        val sampleRate = 44_100
        val frames = 1024
        val minBuffer = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuffer <= 0) { playing = false; return@LaunchedEffect }
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minBuffer, frames * 4))
            .build()
        var leftPhase = 0.0
        var rightPhase = 0.0
        val carrierHz = 220.0
        val rightHz = carrierHz + selected.hz
        val samples = ShortArray(frames * 2)
        try {
            track.setVolume(0.18f)
            track.play()
            withContext(Dispatchers.IO) {
            while (currentCoroutineContext().isActive) {
                for (i in 0 until frames) {
                    samples[i * 2] = (sin(leftPhase) * 5200).toInt().toShort()
                    samples[i * 2 + 1] = (sin(rightPhase) * 5200).toInt().toShort()
                    leftPhase += 2.0 * PI * carrierHz / sampleRate
                    rightPhase += 2.0 * PI * rightHz / sampleRate
                    if (leftPhase >= 2.0 * PI) leftPhase -= 2.0 * PI
                    if (rightPhase >= 2.0 * PI) rightPhase -= 2.0 * PI
                }
                track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            }
            }
        } finally {
            runCatching { track.pause() }
            runCatching { track.flush() }
            track.release()
        }
    }

    DisposableEffect(Unit) { onDispose { playing = false } }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("M³ BRAINWAVE", color = SpatialCyan, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("Tune your atmosphere.", color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Binaural tone experiments and quiet listening spaces.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Surface(color = ObsidianCard, shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth().border(1.dp, Brush.linearGradient(listOf(SpatialCyan.copy(alpha = pulse), ElectricViolet.copy(alpha = 0.55f))), RoundedCornerShape(26.dp))) {
                Column(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(SpatialCyan.copy(alpha = 0.10f), ElectricViolet.copy(alpha = 0.08f), ObsidianBg))).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(154.dp).clip(CircleShape).background(Brush.radialGradient(listOf(selected.accent.copy(alpha = 0.32f), ElectricViolet.copy(alpha = 0.08f), Color.Transparent))).border(2.dp, selected.accent.copy(alpha = pulse), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = selected.accent, modifier = Modifier.size(66.dp))
                    }
                    Text(selected.title.uppercase(), color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(selected.hz.toInt().toString() + " Hz binaural difference", color = selected.accent, style = MaterialTheme.typography.titleMedium)
                    Text(selected.note, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { playing = !playing }, colors = ButtonDefaults.buttonColors(containerColor = selected.accent), shape = RoundedCornerShape(18.dp)) {
                        Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
                        Text(if (playing) "Pause session" else "Start listening", modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(if (playing) "Playing softly • use comfortable volume" else "Ready when you are", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item { Text("Choose a frequency", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
        items(BrainPreset.entries) { preset ->
            val active = selected == preset
            Surface(onClick = { selected = preset; playing = false }, color = if (active) preset.accent.copy(alpha = 0.14f) else ObsidianCard, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, preset.accent.copy(alpha = if (active) 0.8f else 0.28f)), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = preset.accent, modifier = Modifier.size(26.dp))
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(preset.title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(preset.note, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(preset.hz.toInt().toString() + " Hz", color = preset.accent, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Surface(color = ObsidianCard, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Headphones, contentDescription = null, tint = SpatialCyan)
                        Text(" Headphones recommended", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Binaural tones send slightly different pitches to the left and right channels. Use stereo headphones and keep the volume low and comfortable. These sounds are for relaxation or listening experiments, not medical treatment or a guaranteed brain-state change. Do not listen while driving.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
