package dev.jpcottin.tedtalksapp

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.LocalUiMediaScope
import androidx.compose.ui.UiMediaScope
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import dev.jpcottin.tedtalksapp.data.TalkItem
import dev.jpcottin.tedtalksapp.theme.MyApplicationTheme
import dev.jpcottin.tedtalksapp.ui.main.EmptyDetailPlaceholder
import dev.jpcottin.tedtalksapp.ui.main.TalkDetailPane
import dev.jpcottin.tedtalksapp.ui.main.TalkListPane
import dev.jpcottin.tedtalksapp.ui.main.TedTalksUiState

// Compose reads this flag once per composition root, so it must be on before
// the screenshot runner composes the first preview. A file-level val is
// initialised when this class loads, which is before any preview function runs.

@Suppress("unused")
private val mediaQueryEnabled: Boolean = run {
    ComposeUiFlags.isMediaQueryIntegrationEnabled = true
    true
}

/** A UiMediaScope describing a half-opened foldable lying flat (tabletop). */

private class TabletopMediaScope(
    override val windowWidth: Dp,
    override val windowHeight: Dp,
) : UiMediaScope {
    override val windowPosture = UiMediaScope.Posture.Tabletop
    override val pointerPrecision = UiMediaScope.PointerPrecision.Coarse
    override val keyboardKind = UiMediaScope.KeyboardKind.Virtual
    override val hasMicrophone = true
    override val hasCamera = true
    override val viewingDistance = UiMediaScope.ViewingDistance.Near
}

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
@Preview(name = "Detail Tabletop", widthDp = 841, heightDp = 673, showBackground = true)
@Composable
fun TalkDetailPaneTabletopScreenshot() {
    // Same override technique the MediaQuery docs recommend for previews: swap
    // the scope so the detail pane lays out for a horizontal hinge.
    CompositionLocalProvider(LocalUiMediaScope provides TabletopMediaScope(841.dp, 673.dp)) {
        MyApplicationTheme {
            TalkDetailPane(
                talk = sampleTalk,
                showBackButton = false,
                onBack = {},
            )
        }
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
