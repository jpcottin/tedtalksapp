package dev.jpcottin.tedtalksapp.ui.main

import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer

internal tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun Context.supportsPip(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

/** Observes the hosting activity's picture-in-picture state. */
@Composable
fun rememberIsInPipMode(): Boolean {
    val activity = LocalContext.current.findActivity()
    if (activity == null || !activity.supportsPip()) return false
    var inPipMode by remember { mutableStateOf(activity.isInPictureInPictureMode) }
    DisposableEffect(activity) {
        val listener = Consumer<PictureInPictureModeChangedInfo> { info ->
            inPipMode = info.isInPictureInPictureMode
        }
        activity.addOnPictureInPictureModeChangedListener(listener)
        onDispose { activity.removeOnPictureInPictureModeChangedListener(listener) }
    }
    return inPipMode
}

/**
 * Keeps the activity's picture-in-picture params in sync with playback so the app
 * auto-enters PiP when the user leaves while a video is playing. On Android 12+
 * this uses [PictureInPictureParams.Builder.setAutoEnterEnabled]; on Android 8–11
 * it falls back to an explicit enter on the user-leave hint.
 *
 * Call only when [Context.supportsPip] is true.
 */
@Composable
internal fun PipOnLeaveEffect(exoPlayer: ExoPlayer?, shouldEnterPip: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        LaunchedEffect(shouldEnterPip) {
            activity.setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setAutoEnterEnabled(shouldEnterPip)
                    .setAspectRatio(pipAspectRatio(exoPlayer))
                    .build()
            )
        }
        DisposableEffect(activity) {
            onDispose {
                // The player UI left composition; stop auto-entering PiP.
                activity.setPictureInPictureParams(
                    PictureInPictureParams.Builder().setAutoEnterEnabled(false).build()
                )
            }
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val currentShouldEnterPip by rememberUpdatedState(shouldEnterPip)
        val currentPlayer by rememberUpdatedState(exoPlayer)
        DisposableEffect(activity) {
            val onUserLeave = Runnable {
                if (currentShouldEnterPip) {
                    activity.enterPictureInPictureMode(
                        PictureInPictureParams.Builder()
                            .setAspectRatio(pipAspectRatio(currentPlayer))
                            .build()
                    )
                }
            }
            activity.addOnUserLeaveHintListener(onUserLeave)
            onDispose { activity.removeOnUserLeaveHintListener(onUserLeave) }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun pipAspectRatio(player: ExoPlayer?): Rational {
    val size = player?.videoSize
    val rational = if (size != null && size != VideoSize.UNKNOWN && size.height > 0) {
        Rational(size.width, size.height)
    } else {
        Rational(16, 9)
    }
    // The system only accepts PiP aspect ratios between 1:2.39 and 2.39:1.
    return when {
        rational.toFloat() > 2.39f -> Rational(239, 100)
        rational.toFloat() < 1f / 2.39f -> Rational(100, 239)
        else -> rational
    }
}
