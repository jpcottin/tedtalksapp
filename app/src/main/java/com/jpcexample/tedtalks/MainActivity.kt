package com.jpcexample.tedtalks

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
import com.jpcexample.tedtalks.theme.MyApplicationTheme
import com.jpcexample.tedtalks.ui.main.TedTalksViewModel

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
      val viewModel: TedTalksViewModel = viewModel()
      MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(viewModel)
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
