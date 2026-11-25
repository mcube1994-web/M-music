package com.example.ui.screens

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SpatialCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "welcome-neon")
    val glow by transition.animateFloat(
        initialValue = 0.32f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "welcome-glow"
    )
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0xFF21103D), ObsidianBg, Color.Black),
                radius = 1100f
            )
        ).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().border(
                1.5.dp,
                Brush.linearGradient(listOf(SpatialCyan.copy(alpha = glow), ElectricViolet.copy(alpha = glow))),
                RoundedCornerShape(30.dp)
            ).background(Color(0xE80A0D18), RoundedCornerShape(30.dp)).padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier.size(104.dp).border(2.dp, SpatialCyan.copy(alpha = glow), CircleShape)
                    .background(Brush.radialGradient(listOf(ElectricViolet.copy(alpha = 0.42f), Color.Transparent)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("M³", color = TextPrimary, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
            }
            Text("M³ REBORN", color = SpatialCyan, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("Your world.
Your sound.
Your escape.", color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("One midnight studio for music, movies and mind.", color = TextSecondary, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = SpatialCyan, modifier = Modifier.size(25.dp))
                    Text("MUSIC", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Movie, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(25.dp))
                    Text("MOVIES", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Psychology, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(25.dp))
                    Text("BRAINWAVE", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SpatialCyan)
            ) {
                Text("ENTER M³ EXPERIENCE", color = ObsidianBg, fontWeight = FontWeight.Bold)
            }
            Text("Offline library • Web movie player • Live visualizer", color = TextSecondary, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}
