package dev.jpcottin.tedtalksapp.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.animate
import androidx.compose.foundation.style.border
import androidx.compose.foundation.style.contentPadding
import androidx.compose.foundation.style.focused
import androidx.compose.foundation.style.hovered
import androidx.compose.foundation.style.pressed
import androidx.compose.foundation.style.selected
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Compose Styles API definitions for the app's own components. Interaction
 * states (focused, hovered, pressed, selected) are declared here instead of
 * being tracked by hand with onFocusChanged / mutableStateOf in each composable.
 *
 * The theme is a fixed dark palette, so the styles reference its colour tokens
 * directly rather than going through a CompositionLocal.
 */
object TedTalksStyles {

    /**
     * Invisible until the element is focused, then the border animates to the
     * brand red. A mouse hover shows a fainter ring so pointer users get the
     * same affordance. Styles follow the CSS box model, so the 3dp border
     * occupies layout space between the external and content padding; keeping
     * it transparent at rest means focus never shifts the layout.
     */
    val focusRing: Style = Style {
        border(3.dp, Color.Transparent)
        hovered { borderColor(TedRed.copy(alpha = 0.5f)) }
        focused { animate { borderColor(TedRed) } }
    }

    /**
     * A row in the talk list. The selected row (the one open in the detail
     * pane) is highlighted; hover and press give lighter feedback in place of
     * a ripple.
     */
    val talkListItem: Style = Style {
        shape(RoundedCornerShape(8.dp))
        // 16dp / 12dp gutters, of which 3dp is the focusRing border.
        contentPadding(horizontal = 13.dp, vertical = 9.dp)
        hovered { background(SurfaceVariantDark.copy(alpha = 0.6f)) }
        pressed { background(SurfaceVariantDark.copy(alpha = 0.8f)) }
        selected { background(SurfaceVariantDark) }
    }
}
