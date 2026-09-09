package dev.jpcottin.tedtalksapp.ui.main

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import dev.jpcottin.tedtalksapp.theme.TedTalksStyles

/**
 * Draws a highly visible ring while the element is focused, so D-pad and
 * keyboard users can always tell where focus is. Material's default focus
 * indication (a faint state layer) is invisible on image-heavy TV layouts.
 *
 * The ring is a Compose [Style] (see [TedTalksStyles.focusRing]). Material
 * components don't take a Style parameter, so it is attached with
 * `Modifier.styleable`, driven by the same [MutableInteractionSource] the
 * component reports its focus and hover interactions to.
 */
@Composable
fun Modifier.focusRing(interactionSource: MutableInteractionSource, shape: Shape): Modifier {
    val styleState = rememberUpdatedStyleState(interactionSource) {}
    val shapeStyle = remember(shape) { Style { shape(shape) } }
    return styleable(styleState, TedTalksStyles.focusRing, shapeStyle)
}
