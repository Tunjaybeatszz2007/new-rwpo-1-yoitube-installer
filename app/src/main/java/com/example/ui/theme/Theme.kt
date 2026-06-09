package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    background = DarkBackground,
    onBackground = Color(0xFFE6E1E5),
    surface = DarkSurface,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = DarkOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = SecondaryPurple,
    onPrimaryContainer = Color(0xFF21005D),
    secondary = SecondaryPurple,
    onSecondary = Color(0xFF21005D),
    background = BackgroundClean,
    onBackground = OnBackgroundClean,
    surface = SurfaceClean,
    onSurface = OnBackgroundClean,
    surfaceVariant = SurfaceVariantClean,
    onSurfaceVariant = Color(0xFF49454F),
    outline = OutlineClean
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Custom "Clean Minimalism" palette gets priority
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
