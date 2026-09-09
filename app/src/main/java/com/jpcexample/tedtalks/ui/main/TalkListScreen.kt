package com.jpcexample.tedtalks.ui.main

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.rememberUpdatedStyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jpcexample.tedtalks.data.TalkItem
import com.jpcexample.tedtalks.theme.MyApplicationTheme
import com.jpcexample.tedtalks.theme.TedTalksStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalkListPane(
    uiState: TedTalksUiState,
    selectedTalkId: String?,
    onTalkClick: (TalkItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The app bar slides away as the list scrolls down and returns as soon as
    // the user scrolls up, giving the grid the full height on small windows.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TED Talks",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is TedTalksUiState.Loading -> LoadingPane(Modifier.padding(innerPadding))
            is TedTalksUiState.Error -> ErrorPane(
                message = uiState.message,
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding),
            )
            is TedTalksUiState.Success -> TalkList(
                talks = uiState.talks,
                selectedTalkId = selectedTalkId,
                onTalkClick = onTalkClick,
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun LoadingPane(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorPane(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Couldn't load talks",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun TalkList(
    talks: List<TalkItem>,
    selectedTalkId: String?,
    onTalkClick: (TalkItem) -> Unit,
    contentPadding: PaddingValues,
) {
    // Without a pointing device (TV remote, keyboard-only desktop) focus is the
    // only cursor, so the first item takes it as soon as the list appears.
    val isDpadOnly = hasNoPointer()

    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isDpadOnly, talks.isNotEmpty()) {
        if (isDpadOnly && talks.isNotEmpty()) {
            try { firstItemFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    // Adaptive cells: a single column on a phone-width list pane, multiple columns
    // when the list pane is wider (e.g. tablet primary pane, desktop, TV).
    LazyVerticalGrid(
        columns = GridCells.Adaptive(360.dp),
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            // The Scaffold insets are applied as contentPadding above so items
            // scroll under the bars; consume them so nothing below pads twice.
            .consumeWindowInsets(contentPadding)
            // When focus comes back from the detail pane / player, return it to
            // the item that was focused before instead of the top of the list.
            .focusRestorer(firstItemFocusRequester),
    ) {
        items(talks, key = { it.id }) { talk ->
            val isFirst = talk === talks.first()
            TalkListItem(
                talk = talk,
                isSelected = talk.id == selectedTalkId,
                onClick = { onTalkClick(talk) },
                modifier = if (isFirst) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
            )
        }
    }
}

@Composable
private fun TalkListItem(
    talk: TalkItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // One InteractionSource feeds both the click handling and the Style, so
    // focus, hover, press and selection are all rendered by TedTalksStyles.
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = rememberUpdatedStyleState(interactionSource) { it.isSelected = isSelected }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .styleable(styleState, TedTalksStyles.focusRing, TedTalksStyles.talkListItem),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = talk.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Text(
                    text = talk.duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = talk.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = talk.speaker,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = talk.pubDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val sampleTalks = listOf(
    TalkItem("1", "The future of media", "Hamish McKenzie", "Desc", "May 21, 2025", "10:58", "", "", null),
    TalkItem("2", "The catastrophic risks of AI", "Yoshua Bengio", "Desc", "May 20, 2025", "14:49", "", "", null),
    TalkItem("3", "How to make climate stories impossible to ignore", "Katherine Dunn", "Desc", "May 19, 2025", "09:46", "", "", null),
    TalkItem("4", "What if the climate movement felt like a house party?", "Matthew Phillips", "Desc", "May 16, 2025", "08:34", "", "", null),
    TalkItem("5", "The AI revolution is underhyped", "Eric Schmidt", "Desc", "May 15, 2025", "25:34", "", "", null),
    TalkItem("6", "The delicious potential of rescuing wasted food", "Arash Derambarsh", "Desc", "May 14, 2025", "11:21", "", "", null),
    TalkItem("7", "A new era of medicine", "Dr. Jane Smith", "Desc", "May 13, 2025", "18:05", "", "", null),
)

@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Phone Dark", device = Devices.PHONE, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
annotation class FormFactorPreviews

@FormFactorPreviews
@Composable
fun TalkListPanePreview() {
    MyApplicationTheme {
        TalkListPane(
            uiState = TedTalksUiState.Success(sampleTalks),
            selectedTalkId = "1",
            onTalkClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun TalkListItemPreview() {
    MyApplicationTheme {
        TalkListItem(
            talk = sampleTalks[0],
            isSelected = false,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 480, name = "Loading")
@Composable
fun TalkListPaneLoadingPreview() {
    MyApplicationTheme {
        TalkListPane(
            uiState = TedTalksUiState.Loading,
            selectedTalkId = null,
            onTalkClick = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 480, name = "Error")
@Composable
fun TalkListPaneErrorPreview() {
    MyApplicationTheme {
        TalkListPane(
            uiState = TedTalksUiState.Error("Network timeout. Please check your connection."),
            selectedTalkId = null,
            onTalkClick = {},
            onRetry = {},
        )
    }
}
