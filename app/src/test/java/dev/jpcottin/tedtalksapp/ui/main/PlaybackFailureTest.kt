package dev.jpcottin.tedtalksapp.ui.main

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackFailureTest {
    @Test
    fun aFileGoneFromItsServer_saysSoWithTheStatus() {
        assertEquals(
            "TED no longer serves this talk's video (its server answered HTTP 404). The talk is still listed, but there is nothing left to play.",
            PlaybackFailure.describe(PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS, 404, isAudio = false),
        )
        assertEquals(
            "TED no longer serves this talk's audio. The talk is still listed, but there is nothing left to play.",
            PlaybackFailure.describe(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND, null, isAudio = true),
        )
    }

    @Test
    fun noNetwork_asksToCheckTheConnection() {
        assertEquals(
            "The audio could not be reached. Check your connection and try again.",
            PlaybackFailure.describe(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, null, isAudio = true),
        )
        assertEquals(
            "The video could not be reached. Check your connection and try again.",
            PlaybackFailure.describe(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT, null, isAudio = false),
        )
    }

    @Test
    fun anUnplayableStream_namesTheCode() {
        assertEquals(
            "This video cannot be played on this device (ERROR_CODE_DECODER_INIT_FAILED).",
            PlaybackFailure.describe(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, null, isAudio = false),
        )
        assertEquals(
            "Playback failed (ERROR_CODE_UNSPECIFIED).",
            PlaybackFailure.describe(PlaybackException.ERROR_CODE_UNSPECIFIED, null, isAudio = false),
        )
    }
}
