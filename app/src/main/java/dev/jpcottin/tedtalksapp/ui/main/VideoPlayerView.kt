package dev.jpcottin.tedtalksapp.ui.main

import android.app.PictureInPictureParams
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import dev.jpcottin.tedtalksapp.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.toRect
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.xr.compose.spatial.SpatialDialog
import androidx.xr.compose.spatial.SpatialDialogProperties
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import android.view.KeyEvent as AndroidKeyEvent

import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview

import android.util.Log

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerView(
    exoPlayer: ExoPlayer?,
    modifier: Modifier = Modifier,
    /** Cover art for a talk that is audio only: the player shows it in place of the frames. */
    artworkUrl: String? = null,
    /** The player gave up on the media: a file gone from its server, no network, an unplayable stream. */
    onPlaybackError: (PlaybackException) -> Unit = {},
) {
    if (LocalInspectionMode.current) {
        // Preview placeholder
        Box(
            modifier = modifier.background(Color.DarkGray),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = "Video Player Placeholder",
                color = Color.White
            )
        }
        return
    }

    Log.d("VideoPlayerView", "VideoPlayerView Composed. Player state: ${exoPlayer?.playbackState}, position: ${exoPlayer?.currentPosition}")

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFullscreen by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val isDpadOnly = hasNoPointer()
    val isSpatialUi = isSpatialUi()
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

    // Without a pointing device (TV remote, keyboard only) the player must own
    // focus so the D-pad drives the playback controls (show controller,
    // pause/seek). Key events are forwarded to the PlayerView below; BACK falls
    // through when the controller is hidden, popping navigation.
    LaunchedEffect(isDpadOnly, exoPlayer, isFullscreen) {
        if (isDpadOnly && exoPlayer != null && !isFullscreen) {
            try { focusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    val supportsPip = remember(context) { context.supportsPip() }
    val isInPip = rememberIsInPipMode()
    var shouldEnterPip by remember { mutableStateOf(exoPlayer?.isPlaying == true) }

    val currentOnPlaybackError by rememberUpdatedState(onPlaybackError)
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                shouldEnterPip = isPlaying
            }
            override fun onPlayerError(error: PlaybackException) {
                Log.w("VideoPlayerView", "Playback failed: ${error.errorCodeName}", error)
                currentOnPlaybackError(error)
            }
        }
        exoPlayer?.addListener(listener)
        onDispose { exoPlayer?.removeListener(listener) }
    }

    if (supportsPip) {
        PipOnLeaveEffect(exoPlayer = exoPlayer, shouldEnterPip = shouldEnterPip)
        // The PiP window hosts the inline player, not the fullscreen dialog.
        LaunchedEffect(isInPip) {
            if (isInPip) isFullscreen = false
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            Log.d("VideoPlayerView", "Lifecycle event: $event")
            if (event == Lifecycle.Event.ON_PAUSE &&
                context.findActivity()?.isInPictureInPictureMode != true
            ) {
                Log.d("VideoPlayerView", "Pausing ExoPlayer")
                exoPlayer?.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            Log.d("VideoPlayerView", "onDispose called")
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val viewFactory: (Context) -> PlayerView = { ctx ->
        Log.d("VideoPlayerView", "Creating PlayerView")
        (LayoutInflater.from(ctx).inflate(R.layout.view_player, null) as PlayerView).apply {
            // A screen that shows nothing but the cover may go to sleep.
            keepScreenOn = artworkUrl == null
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
    }

    if (isFullscreen && exoPlayer != null) {
        // The inline slot keeps its size under the dialog, so the page does not jump
        // (in Android XR the page stays in view, dimmed, behind the cinema panel).
        Box(modifier = modifier.background(Color.Black))
        val cinemaHost = LocalCinemaHost.current
        if (isSpatialUi && cinemaHost != null) {
            // The window shows it, so it rises from the window's centre, not this pane's.
            DisposableEffect(cinemaHost, exoPlayer, artworkUrl) {
                val request = CinemaRequest(exoPlayer, artworkUrl) { isFullscreen = false }
                cinemaHost.request = request
                onDispose { if (cinemaHost.request === request) cinemaHost.request = null }
            }
        } else if (isSpatialUi) {
            CinemaPlayerDialog(
                exoPlayer = exoPlayer,
                artworkUrl = artworkUrl,
                onDismiss = { isFullscreen = false },
            )
        } else {
            FullscreenPlayerDialog(
                exoPlayer = exoPlayer,
                artworkUrl = artworkUrl,
                onDismiss = { isFullscreen = false }
            )
        }
    } else {
        // Feed the player's on-screen bounds to the system so the enter/exit
        // PiP animation morphs from the actual video rect.
        val pipSourceRectModifier = if (supportsPip) {
            Modifier.onGloballyPositioned { coordinates ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.findActivity()?.setPictureInPictureParams(
                        PictureInPictureParams.Builder()
                            .setSourceRectHint(
                                coordinates.boundsInWindow().toAndroidRectF().toRect()
                            )
                            .build()
                    )
                }
            }
        } else {
            Modifier
        }

        AndroidView(
            factory = viewFactory,
            update = { pv ->
                Log.d("VideoPlayerView", "AndroidView update called. attaching player")
                playerViewRef = pv
                if (pv.player != exoPlayer) {
                    pv.player = exoPlayer
                }
                pv.showArtwork(artworkUrl)
                pv.useController = !isInPip
                pv.setFullscreenButtonState(false)
                pv.setFullscreenButtonClickListener { isFullscreen = true }
            },
            // The player outlives this view (it belongs to the view model): detach it, or
            // every view that leaves composition stays registered as a listener for good.
            onRelease = { pv ->
                pv.player = null
                if (playerViewRef === pv) playerViewRef = null
            },
            modifier = modifier
                .then(pipSourceRectModifier)
                .focusRequester(focusRequester)
                // D-pad/media keys go to the PlayerView (Media3 TV recipe). It
                // consumes them while the controller is visible and lets BACK
                // through once hidden, so navigation still works.
                .onKeyEvent { event ->
                    playerViewRef?.dispatchKeyEvent(event.nativeKeyEvent) ?: false
                }
                .focusable(),
        )
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun FullscreenPlayerDialog(
    exoPlayer: ExoPlayer,
    artworkUrl: String?,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
        ),
    ) {
        val dialogView = LocalView.current

        SideEffect {
            (dialogView.parent as? DialogWindowProvider)?.window?.apply {
                setLayout(MATCH_PARENT, MATCH_PARENT)
                WindowInsetsControllerCompat(this, decorView).apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }

        FullscreenPlayerSurface(
            exoPlayer = exoPlayer,
            artworkUrl = artworkUrl,
            onDismiss = onDismiss,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** A player asking for the cinema; [onDismiss] hands the video back to it. */
internal class CinemaRequest(
    val exoPlayer: ExoPlayer,
    val artworkUrl: String?,
    val onDismiss: () -> Unit,
)

/** Shows the cinema a player in one of the panes asks for, from the window it is hosted in. */
internal class CinemaHost {
    var request: CinemaRequest? by mutableStateOf(null)
}

internal val LocalCinemaHost = staticCompositionLocalOf<CinemaHost?> { null }

/**
 * Hosts the Android XR cinema for the players in [content]. In Full Space each list-detail
 * pane is a panel of its own, and a dialog rises from the panel it is shown in: from the
 * detail pane the cinema would be centred on that pane, half out of view. Shown here, in
 * the window, it is centred on the app. Off XR and in Home Space there is never a request.
 */
@Composable
fun CinemaHostScope(content: @Composable () -> Unit) {
    val host = remember { CinemaHost() }
    CompositionLocalProvider(LocalCinemaHost provides host, content = content)
    // Full Space only: leaving it removes the dialog rather than letting it fall back
    // to a 2D one, which would move its content out of the spatial panel.
    if (!isSpatialUi()) return
    host.request?.let { request ->
        CinemaPlayerDialog(
            exoPlayer = request.exoPlayer,
            artworkUrl = request.artworkUrl,
            onDismiss = request.onDismiss,
        )
    }
}

/**
 * Fullscreen in Android XR Full Space: a cinema panel elevated in front of the app, which
 * the system pushes back and dims, with the room dimmed as well. The panel takes the
 * video's own shape at a size well beyond the window's; the player's collapse button or
 * BACK closes it.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun CinemaPlayerDialog(
    exoPlayer: ExoPlayer,
    artworkUrl: String?,
    onDismiss: () -> Unit,
) {
    var videoSize by remember(exoPlayer) { mutableStateOf(exoPlayer.videoSize) }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(size: VideoSize) {
                videoSize = size
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }
    DimPassthroughWhileComposed()

    SpatialDialog(
        onDismissRequest = onDismiss,
        // Looking or pinching beside the video must not end it.
        properties = SpatialDialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        FullscreenPlayerSurface(
            exoPlayer = exoPlayer,
            artworkUrl = artworkUrl,
            onDismiss = onDismiss,
            modifier = Modifier.size(
                cinemaPanelSize(videoSize.width, videoSize.height, videoSize.pixelWidthHeightRatio)
            ),
        )
    }
}

/** The player view of both fullscreen forms: its collapse button calls [onDismiss]. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun FullscreenPlayerSurface(
    exoPlayer: ExoPlayer,
    artworkUrl: String?,
    onDismiss: () -> Unit,
    modifier: Modifier,
) {
    val fullscreenFocusRequester = remember { FocusRequester() }
    var fullscreenPlayerView by remember { mutableStateOf<PlayerView?>(null) }
    val isDpadOnly = hasNoPointer()

    LaunchedEffect(isDpadOnly) {
        if (isDpadOnly) {
            try { fullscreenFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                (LayoutInflater.from(ctx).inflate(R.layout.view_player, null) as PlayerView).apply {
                    // As inline: a screen that shows nothing but the cover may go to sleep.
                    keepScreenOn = artworkUrl == null
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
            },
            update = { pv ->
                fullscreenPlayerView = pv
                if (pv.player != exoPlayer) {
                    pv.player = exoPlayer
                }
                pv.showArtwork(artworkUrl)
                pv.setFullscreenButtonState(true)
                pv.setFullscreenButtonClickListener { onDismiss() }
            },
            onRelease = { pv ->
                pv.player = null
                if (fullscreenPlayerView === pv) fullscreenPlayerView = null
            },
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(fullscreenFocusRequester)
                .onKeyEvent { event ->
                    fullscreenPlayerView?.dispatchKeyEvent(event.nativeKeyEvent) ?: false
                }
                .focusable(),
        )
    }
}

/**
 * The cover of an audio talk, shown by the player view itself whenever the media has no
 * video track (its default artwork), cropped to fill like the hero image it replaces.
 * The view remembers the URL it was given so a recomposition does not fetch it again.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun PlayerView.showArtwork(url: String?) {
    if (tag == url) return
    tag = url
    if (url == null) {
        defaultArtwork = null
        return
    }
    artworkDisplayMode = PlayerView.ARTWORK_DISPLAY_MODE_FILL
    context.imageLoader.enqueue(
        ImageRequest.Builder(context)
            .data(url)
            .target(onSuccess = { image -> if (tag == url) defaultArtwork = image.asDrawable(context.resources) })
            .build()
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 225, name = "Video Player (Inline)")
@Composable
fun VideoPlayerViewPreview() {
    dev.jpcottin.tedtalksapp.theme.TedTalksTheme {
        VideoPlayerView(exoPlayer = null, modifier = Modifier.fillMaxSize())
    }
}
