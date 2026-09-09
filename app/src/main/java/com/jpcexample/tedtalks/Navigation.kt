package com.jpcexample.tedtalks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.jpcexample.tedtalks.ui.main.EmptyDetailPlaceholder
import com.jpcexample.tedtalks.ui.main.LogDeviceTraits
import com.jpcexample.tedtalks.ui.main.TalkDetailPane
import com.jpcexample.tedtalks.ui.main.isFarViewing
import com.jpcexample.tedtalks.ui.main.TalkListPane
import com.jpcexample.tedtalks.ui.main.TedTalksUiState
import com.jpcexample.tedtalks.ui.main.TedTalksViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainNavigation(viewModel: TedTalksViewModel) {
    val backStack = rememberNavBackStack(TalksList)
    val context = LocalContext.current
    LogDeviceTraits()
    // Viewed from a distance (TV): keep content inside the overscan-safe area.
    val isFarViewing = isFarViewing()

    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val isMultiPane = directive.maxHorizontalPartitions > 1
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTalkId by viewModel.selectedTalkId.collectAsStateWithLifecycle()

    // In a two-pane scene ListDetailSceneStrategy reports no previous entries, which
    // disables NavDisplay's built-in back handling — BACK would close the activity
    // instead of dismissing the detail pane. This outer fallback pops the stack; in
    // single-pane scenes NavDisplay's own (more recently registered) handler wins,
    // keeping the predictive-back animation.
    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
        viewModel.clearSelection()
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            backStack.removeLastOrNull()
            viewModel.clearSelection()
        },
        sceneStrategies = listOf(listDetailStrategy),
        entryProvider = entryProvider {
            entry<TalksList>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { EmptyDetailPlaceholder() }
                )
            ) {
                TalkListPane(
                    uiState = uiState,
                    selectedTalkId = selectedTalkId,
                    onTalkClick = { talk ->
                        viewModel.selectTalk(talk.id)
                        backStack.add(TalkDetail(talk.id))
                    },
                    onRetry = viewModel::loadTalks,
                )
            }
            entry<TalkDetail>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { detail ->
                val talks = (uiState as? TedTalksUiState.Success)?.talks ?: emptyList()
                val talk = talks.find { it.id == detail.talkId }
                if (talk != null) {
                    TalkDetailPane(
                        talk = talk,
                        showBackButton = !isMultiPane,
                        onBack = {
                            backStack.removeLastOrNull()
                            viewModel.clearSelection()
                        },
                        getExoPlayer = { url -> viewModel.getExoPlayer(context, url) },
                    )
                } else {
                    EmptyDetailPlaceholder()
                }
            }
        },
        modifier = Modifier
            .fillMaxSize()
            // Overscan safe area: 48dp horizontal, 27dp vertical
            .then(
                if (isFarViewing) Modifier.padding(horizontal = 48.dp, vertical = 27.dp)
                else Modifier
            ),
    )
}
