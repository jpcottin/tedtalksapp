package com.jpcexample.tedtalks.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import com.jpcexample.tedtalks.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.view.KeyEvent as AndroidKeyEvent

import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview

import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerView(
    videoUrl: String,
    modifier: Modifier = Modifier,
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

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFullscreen by remember { mutableStateOf(false) }

    // Create a permissive OkHttpClient to handle emulator SSL issues (Demo Only)
    // In case images lack the latest root CA certificates required for TED's CDN
    val okHttpClient = remember {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            OkHttpClient()
        }
    }

    val exoPlayer = remember(videoUrl) {
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        
        val mediaSource = androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))

        ExoPlayer.Builder(context).build().apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }

    val playerView = remember(context) {
        val view = LayoutInflater.from(context).inflate(R.layout.view_player, null) as PlayerView
        view.apply {
            player = exoPlayer
            keepScreenOn = true
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
    }

    val focusRequester = remember { FocusRequester() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) exoPlayer.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    val viewFactory: (Context) -> PlayerView = {
        (playerView.parent as? ViewGroup)?.removeView(playerView)
        playerView
    }

    if (isFullscreen) {
        FullscreenPlayerDialog(
            playerView = playerView,
            onDismiss = { isFullscreen = false }
        )
    } else {
        AndroidView(
            factory = viewFactory,
            update = { pv ->
                pv.setFullscreenButtonState(false)
                pv.setFullscreenButtonClickListener { isFullscreen = true }
            },
            modifier = modifier
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_BACK) false
                    else playerView.dispatchKeyEvent(event.nativeKeyEvent)
                },
        )
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun FullscreenPlayerDialog(
    playerView: PlayerView,
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AndroidView(
                factory = {
                    (playerView.parent as? ViewGroup)?.removeView(playerView)
                    playerView
                },
                update = { pv ->
                    pv.setFullscreenButtonState(true)
                    pv.setFullscreenButtonClickListener { onDismiss() }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_BACK) false
                        else playerView.dispatchKeyEvent(event.nativeKeyEvent)
                    },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 225, name = "Video Player (Inline)")
@Composable
fun VideoPlayerViewPreview() {
    com.jpcexample.tedtalks.theme.MyApplicationTheme {
        VideoPlayerView(videoUrl = "", modifier = Modifier.fillMaxSize())
    }
}
