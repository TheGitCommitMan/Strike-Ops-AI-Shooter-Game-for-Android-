package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF00FFCC),
    secondary = Color(0xFF00A383),
    tertiary = Color(0xFFFF6600),
    background = Color(0xFF020907),
    surface = Color(0xFF081511),
    onPrimary = Color(0xFF020907),
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
  )

private val LightColorScheme = DarkColorScheme // Enforce dark theme for military simulator aesthetic

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force DarkTheme
  dynamicColor: Boolean = false, // Disable dynamic content scheme to preserve beautiful military green brand colors
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
