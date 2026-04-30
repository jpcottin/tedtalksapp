package com.jpcexample.tedtalks

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.jpcexample.tedtalks.ui.main.MainScreen
import com.jpcexample.tedtalks.ui.main.TedTalksViewModel

@Composable
fun MainNavigation(viewModel: TedTalksViewModel) {
    val backStack = rememberNavBackStack(Main)
    val isTV = LocalConfiguration.current.uiMode and
            Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                MainScreen(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        // TV overscan safe area: 48dp horizontal, 27dp vertical
                        .then(
                            if (isTV) Modifier.padding(horizontal = 48.dp, vertical = 27.dp)
                            else Modifier
                        )
                )
            }
        },
    )
}
