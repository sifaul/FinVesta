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

enum class AppThemeMode(val title: String) {
    SYSTEM("Otomatis (Sistem)"),
    LIGHT("Mode Terang"),
    DARK("Mode Gelap (Malam)")
}

private val DarkColorScheme = darkColorScheme(
    primary = Emerald400,
    onPrimary = Color(0xFF064E3B),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Emerald100,
    secondary = Cyan400,
    onSecondary = Color(0xFF164E63),
    secondaryContainer = Color(0xFF155E75),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = Amber400,
    onTertiary = Color(0xFF78350F),
    tertiaryContainer = Color(0xFF92400E),
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = Rose400,
    onError = Color(0xFF450A0A),
    background = DarkBg,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF263345),
    surfaceContainer = Color(0xFF131D2E),
    surfaceContainerHigh = Color(0xFF1A263B),
    surfaceContainerLow = Color(0xFF0F1724)
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = Color.White,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald700,
    secondary = Cyan600,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF155E75),
    tertiary = Amber600,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),
    error = Rose600,
    onError = Color.White,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = Color(0xFFE2E8F0),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerLow = Color(0xFFF8FAFC)
)

@Composable
fun MyApplicationTheme(
    appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = false, // Use our rich custom financial branding
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appThemeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
