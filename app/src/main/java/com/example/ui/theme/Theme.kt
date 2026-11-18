package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppearanceSkin

val LocalAppearanceSkin = staticCompositionLocalOf { AppearanceSkin.LEVYRA_DEEPSOUND }

val M3Shapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun M3MusicPlayerTheme(
    skin: AppearanceSkin = AppearanceSkin.LEVYRA_DEEPSOUND,
    content: @Composable () -> Unit
) {
    val bg = Color(skin.bgHex)
    val surface = Color(skin.surfaceHex)
    val card = Color(skin.cardHex)
    val primary = Color(skin.primaryHex)
    val secondary = Color(skin.secondaryHex)
    val tertiary = Color(skin.tertiaryHex)

    val colorScheme = darkColorScheme(
        primary = primary,
        onPrimary = TextPrimary,
        primaryContainer = card,
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = bg,
        secondaryContainer = card,
        onSecondaryContainer = secondary,
        tertiary = tertiary,
        onTertiary = bg,
        background = bg,
        onBackground = TextPrimary,
        surface = surface,
        onSurface = TextPrimary,
        surfaceVariant = card,
        onSurfaceVariant = TextSecondary,
        outline = GlassBorder,
        error = RoseLive,
        onError = bg
    )

    CompositionLocalProvider(LocalAppearanceSkin provides skin) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = M3Typography,
            shapes = M3Shapes,
            content = content
        )
    }
}
