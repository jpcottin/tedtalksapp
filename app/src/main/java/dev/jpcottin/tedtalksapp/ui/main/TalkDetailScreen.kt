package dev.jpcottin.tedtalksapp.ui.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import dev.jpcottin.tedtalksapp.data.TalkItem
import dev.jpcottin.tedtalksapp.theme.MyApplicationTheme

@Composable
fun TalkDetailPane(
    talk: TalkItem,
    showBackButton: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    getExoPlayer: ((String) -> ExoPlayer)? = null,
) {
    var isPlayerVisible by rememberSaveable(talk.id) { mutableStateOf(false) }

    // In picture-in-picture the window is tiny: show only the video, full-bleed.
    val isInPip = rememberIsInPipMode()
    if (isInPip && isPlayerVisible && talk.videoUrl != null && getExoPlayer != null) {
        VideoPlayerView(
            exoPlayer = getExoPlayer(talk.videoUrl),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        )
        return
    }

    if (isTabletopPosture()) {
        // Foldable half-opened with the hinge across the middle: the media sits
        // on the upper half and the readable content and its controls on the
        // lower half, so the fold never cuts through either.
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            HeroArea(
                talk = talk,
                isPlayerVisible = isPlayerVisible,
                onPlay = { isPlayerVisible = true },
                showBackButton = showBackButton,
                onBack = onBack,
                getExoPlayer = getExoPlayer,
                fillParent = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            DetailContent(
                talk = talk,
                isPlayerVisible = isPlayerVisible,
                onPlay = { isPlayerVisible = true },
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        HeroArea(
            talk = talk,
            isPlayerVisible = isPlayerVisible,
            onPlay = { isPlayerVisible = true },
            showBackButton = showBackButton,
            onBack = onBack,
            getExoPlayer = getExoPlayer,
            fillParent = false,
            modifier = Modifier.fillMaxWidth(),
        )
        DetailContent(
            talk = talk,
            isPlayerVisible = isPlayerVisible,
            onPlay = { isPlayerVisible = true },
        )
    }
}

/**
 * Hero image or inline player, drawn edge-to-edge under the status bar. The
 * overlaid M3 TopAppBar applies its own status-bar inset. With [fillParent]
 * the media fills whatever box it is given (tabletop upper half); otherwise it
 * keeps a 16:9 band at the top of the scrolling page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeroArea(
    talk: TalkItem,
    isPlayerVisible: Boolean,
    onPlay: () -> Unit,
    showBackButton: Boolean,
    onBack: () -> Unit,
    getExoPlayer: ((String) -> ExoPlayer)?,
    fillParent: Boolean,
    modifier: Modifier = Modifier,
) {
    val hasVideo = talk.videoUrl != null
    val mediaModifier = if (fillParent) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f)

    // Target size follows the input, not the form factor label: bigger when
    // viewed from across the room, smaller when a fine pointer is attached.
    val playButtonSize: Dp = when {
        isFarViewing() -> 88.dp
        hasFinePointer() -> 56.dp
        else -> 72.dp
    }

    Box(modifier = modifier) {
        if (isPlayerVisible && talk.videoUrl != null && getExoPlayer != null) {
            VideoPlayerView(
                exoPlayer = getExoPlayer(talk.videoUrl),
                modifier = mediaModifier,
            )
        } else {
            Box(modifier = mediaModifier) {
                AsyncImage(
                    model = talk.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                startY = 120f,
                            )
                        ),
                )
                if (hasVideo) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val playInteraction = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = onPlay,
                            interactionSource = playInteraction,
                            modifier = Modifier
                                .size(playButtonSize)
                                // Background first so the ring draws over its rim
                                // instead of shrinking the disc.
                                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                .focusRing(playInteraction, CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier.size(playButtonSize - 28.dp),
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                ) {
                    Text(
                        text = talk.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }

        if (showBackButton) {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
    }
}

/**
 * Speaker, metadata, description and actions. Horizontal/bottom safe-drawing
 * insets keep the content clear of the nav bar, gesture bar, and any
 * horizontal display cutouts.
 */
@Composable
private fun DetailContent(
    talk: TalkItem,
    isPlayerVisible: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hasVideo = talk.videoUrl != null
    val isTV = isTelevision()

    Column(
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
            .padding(16.dp),
    ) {
        if (isPlayerVisible) {
            Text(
                text = talk.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = talk.speaker,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (talk.duration.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = talk.duration,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (talk.pubDate.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = talk.pubDate,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = talk.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (hasVideo && !isPlayerVisible) {
            val playInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = onPlay,
                interactionSource = playInteraction,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRing(playInteraction, ButtonDefaults.shape),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play video", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Leanback devices have no browser; a web link is a dead end there.
        if (!isTV) {
            val linkInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(talk.link)))
                    } catch (_: ActivityNotFoundException) {
                        // No browser installed; nothing sensible to do.
                    }
                },
                interactionSource = linkInteraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRing(linkInteraction, ButtonDefaults.outlinedShape),
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open on TED.com")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun EmptyDetailPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Text(
                text = "Select a talk",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Choose a talk from the list to explore it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 600, name = "Light Mode")
@Preview(showBackground = true, widthDp = 600, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Preview(showBackground = true, widthDp = 800, heightDp = 600, name = "Tablet")
@Composable
fun TalkDetailPanePreview() {
    MyApplicationTheme {
        TalkDetailPane(
            talk = TalkItem("1", "The future of media", "Hamish McKenzie", "Hamish McKenzie discusses the future of media.", "May 21, 2025", "10:58", "", "https://ted.com", "https://example.com/video.mp4"),
            showBackButton = true,
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 600, name = "No Video")
@Composable
fun TalkDetailPaneNoVideoPreview() {
    MyApplicationTheme {
        TalkDetailPane(
            talk = TalkItem("2", "The catastrophic risks of AI", "Yoshua Bengio", "A very long and detailed description about the catastrophic risks of Artificial Intelligence and what we must do to stop them. ".repeat(10), "May 20, 2025", "14:49", "", "https://ted.com", null),
            showBackButton = false,
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 400, name = "Missing Meta (Mobile)")
@Composable
fun TalkDetailPaneMissingMetaPreview() {
    MyApplicationTheme {
        TalkDetailPane(
            talk = TalkItem("3", "Mysterious Talk", "Unknown Speaker", "Description is available but other meta is missing.", "", "", "", "https://ted.com", null),
            showBackButton = true,
            onBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun EmptyDetailPlaceholderPreview() {
    MyApplicationTheme {
        EmptyDetailPlaceholder()
    }
}
