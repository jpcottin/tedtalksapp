package com.jpcexample.tedtalks.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TedColorScheme = darkColorScheme(
    primary = TedRed,
    onPrimary = OnSurfaceWhite,
    primaryContainer = TedRedContainer,
    onPrimaryContainer = OnSurfaceWhite,
    secondary = OnSurfaceMuted,
    onSecondary = OnSurfaceWhite,
    background = NearBlack,
    onBackground = OnSurfaceWhite,
    surface = SurfaceDark,
    onSurface = OnSurfaceWhite,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceMuted,
    error = TedDarkRed,
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TedColorScheme,
        typography = Typography,
        content = content,
    )
}
