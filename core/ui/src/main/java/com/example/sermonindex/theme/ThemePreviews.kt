package com.example.sermonindex.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multipreview annotation that renders Composable UI components
 * in both Light and Dark Material 3 themes.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
@Preview(
    name = "1. Light Theme",
    group = "Themes",
    showBackground = true
)
@Preview(
    name = "2. Dark Theme",
    group = "Themes",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
annotation class ThemePreviews
