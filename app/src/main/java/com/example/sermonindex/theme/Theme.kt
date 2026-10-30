package com.example.sermonindex.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = NavyDeep,
    primaryContainer = NavyCard,
    onPrimaryContainer = GoldPrimary,
    secondary = AmberAccent,
    onSecondary = Color.Black,
    tertiary = SageTealDark,
    onTertiary = NavyDeep,
    tertiaryContainer = SageTealContainerDark,
    onTertiaryContainer = SageTealOnContainerDark,
    background = NavyDeep,
    onBackground = Color(0xFFF1F5F9),
    surface = NavySurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = NavyCard,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDE8D3),
    onPrimaryContainer = LightPrimary,
    secondary = AmberAccent,
    onSecondary = Color.White,
    tertiary = SageTeal,
    onTertiary = Color.White,
    tertiaryContainer = SageTealContainerLight,
    onTertiaryContainer = SageTealOnContainerLight,
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun SermonIndexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
