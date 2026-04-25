package com.jpcexample.tedtalks.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val viewModel: TedTalksViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTalkId by viewModel.selectedTalkId.collectAsStateWithLifecycle()

    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()

    BackHandler(navigator.canNavigateBack()) {
        scope.launch {
            navigator.navigateBack()
            viewModel.clearSelection()
        }
    }

    val talks = (uiState as? TedTalksUiState.Success)?.talks ?: emptyList()
    val selectedTalk = talks.find { it.id == (navigator.currentDestination?.contentKey ?: selectedTalkId) }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        modifier = modifier,
        listPane = {
            AnimatedPane {
                TalkListPane(
                    uiState = uiState,
                    selectedTalkId = navigator.currentDestination?.contentKey ?: selectedTalkId,
                    onTalkClick = { talk ->
                        viewModel.selectTalk(talk.id)
                        scope.launch {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, talk.id)
                        }
                    },
                    onRetry = viewModel::loadTalks,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                if (selectedTalk != null) {
                    TalkDetailPane(
                        talk = selectedTalk,
                        showBackButton = navigator.canNavigateBack(),
                        onBack = {
                            scope.launch {
                                navigator.navigateBack()
                                viewModel.clearSelection()
                            }
                        },
                    )
                } else {
                    EmptyDetailPlaceholder()
                }
            }
        },
    )
}
