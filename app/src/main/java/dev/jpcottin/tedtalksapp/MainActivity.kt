package dev.jpcottin.tedtalksapp

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jpcottin.tedtalksapp.theme.TedTalksTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.jpcottin.tedtalksapp.data.DefaultTedTalksRepository
import dev.jpcottin.tedtalksapp.data.PreferencesTalkSourceSettings
import dev.jpcottin.tedtalksapp.ui.main.CinemaHostScope
import dev.jpcottin.tedtalksapp.ui.main.SpatialMainWindow
import dev.jpcottin.tedtalksapp.ui.main.TedTalksViewModel
import dev.jpcottin.tedtalksapp.ui.main.rememberIsAdditiveDisplay

@SuppressLint("Instantiatable")
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    // App forces dark Compose theme regardless of system setting; tell the system
    // to render dark scrims so status/nav bar icons are light and legible.
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    super.onCreate(savedInstanceState)
    Log.d("MainActivityLifecycle", "onCreate")

    setContent {
      // The bundled video feed comes from the app's assets, the audio choice from its preferences.
      val appContext = LocalContext.current.applicationContext
      val viewModel: TedTalksViewModel = viewModel(
        factory = remember(appContext) {
          TedTalksViewModel.factory(
            DefaultTedTalksRepository(bundledFeed = { appContext.assets.open(DefaultTedTalksRepository.BUNDLED_VIDEO_FEED) }),
            PreferencesTalkSourceSettings(appContext),
          )
        },
      )
      TedTalksTheme(additiveDisplay = rememberIsAdditiveDisplay()) {
        // Android XR Full Space only: the window becomes a panel in the scene, and the
        // cinema a player asks for opens from it. Elsewhere, and in Home Space, neither
        // changes anything.
        SpatialMainWindow()
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          CinemaHostScope {
            MainNavigation(viewModel)
          }
        }
      }
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    Log.d("MainActivityLifecycle", "onConfigurationChanged: $newConfig")
  }

  override fun onDestroy() {
    super.onDestroy()
    Log.d("MainActivityLifecycle", "onDestroy")
  }
}
