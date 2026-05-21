package com.jpcexample.tedtalks

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.jpcexample.tedtalks.data.TalkItem
import com.jpcexample.tedtalks.theme.MyApplicationTheme
import com.jpcexample.tedtalks.ui.main.EmptyDetailPlaceholder
import com.jpcexample.tedtalks.ui.main.TalkDetailPane
import com.jpcexample.tedtalks.ui.main.TalkListPane
import com.jpcexample.tedtalks.ui.main.TedTalksUiState

private val sampleTalks = listOf(
    TalkItem("1", "The future of media", "Hamish McKenzie", "Desc", "May 21, 2025", "10:58", "", "", null),
    TalkItem("2", "The catastrophic risks of AI", "Yoshua Bengio", "Desc", "May 20, 2025", "14:49", "", "", null),
    TalkItem("3", "How to make climate stories impossible to ignore", "Katherine Dunn", "Desc", "May 19, 2025", "09:46", "", "", null),
    TalkItem("4", "What if the climate movement felt like a house party?", "Matthew Phillips", "Desc", "May 16, 2025", "08:34", "", "", null),
)

private val sampleTalk = TalkItem(
    id = "1",
    title = "The future of media",
    speaker = "Hamish McKenzie",
    description = "Hamish McKenzie discusses the future of media in this engaging talk.",
    pubDate = "May 21, 2025",
    duration = "10:58",
    imageUrl = "",
    link = "https://ted.com",
    videoUrl = "https://example.com/video.mp4",
)

@PreviewTest
@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Composable
fun TalkListPaneSuccessScreenshot() {
    MyApplicationTheme {
        TalkListPane(
            uiState = TedTalksUiState.Success(sampleTalks),
            selectedTalkId = "1",
            onTalkClick = {},
            onRetry = {},
        )
    }
}

@PreviewTest
@Preview(name = "Loading", widthDp = 400, heightDp = 600, showBackground = true)
@Composable
fun TalkListPaneLoadingScreenshot() {
    MyApplicationTheme {
        TalkListPane(
            uiState = TedTalksUiState.Loading,
            selectedTalkId = null,
            onTalkClick = {},
            onRetry = {},
        )
    }
}

@PreviewTest
@Preview(name = "Error", widthDp = 400, heightDp = 600, showBackground = true)
@Composable
fun TalkListPaneErrorScreenshot() {
    MyApplicationTheme {
        TalkListPane(
            uiState = TedTalksUiState.Error("Network timeout."),
            selectedTalkId = null,
            onTalkClick = {},
            onRetry = {},
        )
    }
}

@PreviewTest
@Preview(name = "Detail Phone", widthDp = 400, heightDp = 800, showBackground = true)
@Preview(name = "Detail Phone Dark", widthDp = 400, heightDp = 800, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Detail Tablet", widthDp = 800, heightDp = 600, showBackground = true)
@Composable
fun TalkDetailPaneScreenshot() {
    MyApplicationTheme {
        TalkDetailPane(
            talk = sampleTalk,
            showBackButton = true,
            onBack = {},
        )
    }
}

@PreviewTest
@Preview(name = "Empty Detail", widthDp = 400, heightDp = 600, showBackground = true)
@Composable
fun EmptyDetailPlaceholderScreenshot() {
    MyApplicationTheme {
        EmptyDetailPlaceholder()
    }
}
