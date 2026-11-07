package dev.jpcottin.tedtalksapp.ui.main

import android.content.Context
import android.util.Log
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.platform.LocalSpatialConfiguration
import androidx.xr.compose.platform.SpatialConfiguration
import androidx.xr.compose.platform.requestFullSpace
import androidx.xr.compose.platform.requestHomeSpace
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterPosition
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.runtime.DisplayBlendMode
import androidx.xr.runtime.XrDevice
import androidx.xr.scenecore.scene
import dev.jpcottin.tedtalksapp.theme.LocalAdditiveDisplay
import kotlinx.coroutines.launch

/*
 * Android XR. On a headset or wired XR glasses the app opens in Home Space as one flat
 * panel, laid out as on any large screen. Full Space, which the app bar's toggle asks
 * for, spatializes it: the window becomes a panel the user can move and resize, the
 * app bar floats above it as an orbiter, and fullscreen video opens in a cinema panel
 * in front of it with the room dimmed. Off XR every function here answers false or
 * does nothing.
 *
 * Nothing here moves composed content between the window and a spatial panel: a switch
 * of space disposes what one space shows and composes what the other shows. Content
 * moved out of a spatial panel back into the window, the way the XR Material overrides
 * and the library's own Orbiter fallback do it, crashed the app on the way back to Home
 * Space ("Cannot insert LayoutNode ... already has a parent or an owner").
 */

/** An Android XR device (headset, wired XR glasses), whether in Home Space or Full Space. */
@Composable
fun isXrDevice(): Boolean = LocalSpatialConfiguration.current.hasXrSpatialFeature

/** Spatial panels and orbiters can be shown right now: an Android XR device in Full Space. */
@Composable
fun isSpatialUi(): Boolean = LocalSpatialCapabilities.current.isSpatialUiEnabled

/**
 * In Full Space, places the activity's own window in the scene as a panel the user can
 * move and resize. The window keeps its composition, so nothing in it starts over on a
 * switch between Home Space and Full Space.
 */
@Composable
fun SpatialMainWindow() {
    if (!isSpatialUi()) return
    Subspace {
        SpatialMainPanel(modifier = SubspaceModifier.movable().resizable())
    }
}

/**
 * [bar] as an orbiter above the window's top-start corner in Full Space, and in place
 * everywhere else. The two are separate compositions on purpose (see above): a switch
 * builds the bar again rather than moving it.
 */
@Composable
fun SpatialAppBar(bar: @Composable () -> Unit) {
    if (isSpatialUi()) {
        Orbiter(
            position = OrbiterPosition.TopStart(
                horizontalEdgeAlignment = OrbiterPosition.EdgeAlignment.Inside,
                verticalEdgeAlignment = OrbiterPosition.EdgeAlignment.Outside,
            ),
            shape = SpatialRoundedCornerShape(CornerSize(50)),
        ) {
            bar()
        }
    } else {
        bar()
    }
}

/**
 * The Home Space / Full Space switch, for an Android XR device. Its label says where it
 * takes the app, the way the system's own switch does: "Full Space", then "Home Space".
 */
@Composable
fun SpaceToggle(modifier: Modifier = Modifier) {
    val activity = LocalContext.current.findActivity() ?: return
    val isFullSpace = isSpatialUi()
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    IconToggleButton(
        checked = isFullSpace,
        onCheckedChange = {
            scope.launch { if (isFullSpace) activity.requestHomeSpace() else activity.requestFullSpace() }
        },
        interactionSource = interactionSource,
        modifier = modifier.focusRing(interactionSource, CircleShape),
    ) {
        Icon(
            imageVector = if (isFullSpace) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
            contentDescription = spaceToggleLabel(isFullSpace),
        )
    }
}

/** What the space switch is called: where a tap takes the app. */
internal fun spaceToggleLabel(isFullSpace: Boolean): String = if (isFullSpace) "Home Space" else "Full Space"

/**
 * True on a display that adds light to the real world (optical see-through XR glasses).
 * Black is transparent there, so dark surfaces disappear and dim text fades into the room.
 */
@Composable
fun rememberIsAdditiveDisplay(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.isAdditiveDisplay() }
}

private fun Context.isAdditiveDisplay(): Boolean {
    if (!SpatialConfiguration.hasXrSpatialFeature(this)) return false
    return try {
        // The application context: the display does not change with the activity.
        XrDevice.getCurrentDevice(applicationContext).getPreferredDisplayBlendMode() == DisplayBlendMode.ADDITIVE
    } catch (e: RuntimeException) {
        Log.w(TAG, "No display blend mode on this XR device", e)
        false
    }
}

/**
 * The box a cinema panel fits the video into, 16:9. A dialog panel sits closer to the
 * user than the window it rises from, so this already looks a fifth larger than the
 * 1024dp Home Space window and still fits in the field of view without a head turn.
 */
internal val CinemaBox = DpSize(1120.dp, 630.dp)

/**
 * The cinema panel for a video of [videoWidth] x [videoHeight] pixels shown with
 * [pixelRatio]: the video's own shape at the largest size that fits [box]. Its shape is
 * kept between 1:2.39 and 2.39:1 so an odd stream never becomes a sliver, and an
 * unknown size (an audio talk, whose cover fills the frame) gets the box's own shape.
 */
internal fun cinemaPanelSize(
    videoWidth: Int,
    videoHeight: Int,
    pixelRatio: Float = 1f,
    box: DpSize = CinemaBox,
): DpSize {
    val boxRatio = box.width / box.height
    val ratio = if (videoWidth > 0 && videoHeight > 0 && pixelRatio > 0f) {
        (videoWidth * pixelRatio / videoHeight).coerceIn(1f / 2.39f, 2.39f)
    } else {
        boxRatio
    }
    return if (ratio >= boxRatio) DpSize(box.width, box.width / ratio) else DpSize(box.height * ratio, box.height)
}

/**
 * Dims the lights for the cinema: while it is open the app's passthrough preference is
 * 0 (the real room fades out behind the video), and on the way out the app gets back the
 * preference it had before, usually none, which hands passthrough back to the system.
 */
internal class CinemaLights(
    private val preference: () -> Float,
    private val setPreference: (Float) -> Unit,
) {
    private var saved: Float? = null

    fun dim() {
        if (saved != null) return
        saved = preference()
        setPreference(0f)
    }

    fun restore() {
        val previous = saved ?: return
        saved = null
        setPreference(previous)
    }
}

/** Keeps the room dimmed for as long as this is composed, where the app may control passthrough. */
@Composable
internal fun DimPassthroughWhileComposed() {
    val session = LocalSession.current ?: return
    if (!LocalSpatialCapabilities.current.isPassthroughControlEnabled) return
    DisposableEffect(session) {
        val environment = session.scene.spatialEnvironment
        val lights = CinemaLights(
            preference = { environment.preferredPassthroughOpacity },
            setPreference = { environment.preferredPassthroughOpacity = it },
        )
        lights.dim()
        Log.d(TAG, "Cinema open: passthrough preference 0")
        onDispose {
            lights.restore()
            Log.d(TAG, "Cinema closed: passthrough preference ${environment.preferredPassthroughOpacity}")
        }
    }
}

/** Logs the XR facts the UI adapts to, next to [LogDeviceTraits]; silent off XR. */
@Composable
fun LogSpatialTraits() {
    if (!isXrDevice()) return
    val spatialUi = isSpatialUi()
    val additiveDisplay = LocalAdditiveDisplay.current
    LaunchedEffect(spatialUi, additiveDisplay) {
        Log.d("DeviceTraits", "xr=true spatialUi=$spatialUi additiveDisplay=$additiveDisplay")
    }
}

private const val TAG = "SpatialSupport"
