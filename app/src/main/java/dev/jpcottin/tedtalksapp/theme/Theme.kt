package dev.jpcottin.tedtalksapp.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

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

/** The same palette for an additive display: the dark background stays (it is see-through there). */
private val TedAdditiveColorScheme = TedColorScheme.copy(
    secondary = OnSurfaceMutedBright,
    surfaceVariant = SurfaceVariantBright,
    onSurfaceVariant = OnSurfaceMutedBright,
    error = TedErrorBright,
)

internal fun tedColorScheme(additiveDisplay: Boolean): ColorScheme =
    if (additiveDisplay) TedAdditiveColorScheme else TedColorScheme

/**
 * Whether the UI is drawn on an additive display (optical see-through XR glasses), for
 * the colours that do not come from the colour scheme, such as the list row's Style.
 */
val LocalAdditiveDisplay = staticCompositionLocalOf { false }

@Composable
fun TedTalksTheme(
    /** See [LocalAdditiveDisplay]. */
    additiveDisplay: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAdditiveDisplay provides additiveDisplay) {
        MaterialTheme(
            colorScheme = tedColorScheme(additiveDisplay),
            typography = Typography,
            content = content,
        )
    }
}
