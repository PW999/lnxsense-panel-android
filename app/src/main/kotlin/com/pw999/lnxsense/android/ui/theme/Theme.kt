package com.pw999.lnxsense.android.ui.theme

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
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = LightBlue80
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = LightBlue40
)

// Pure black surfaces for AMOLED screens — pixels are fully off, saving battery
private val AmoledColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = LightBlue80,
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF1A1A1A),
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
    surfaceContainerLow = Color.Black,
    surfaceContainerLowest = Color.Black,
)

// Theme mode constants matching DataStore values
object ThemeMode {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"
    const val AMOLED = "amoled"

    val entries = listOf(SYSTEM, LIGHT, DARK, AMOLED)

    fun label(mode: String): String = when (mode) {
        SYSTEM -> "System"
        LIGHT -> "Light"
        DARK -> "Dark"
        AMOLED -> "AMOLED"
        else -> mode
    }
}

@Composable
fun LnxSenseTheme(
    themeMode: String = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        else -> systemDark
    }
    val isAmoled = themeMode == ThemeMode.AMOLED

    val colorScheme = when {
        isAmoled -> AmoledColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
