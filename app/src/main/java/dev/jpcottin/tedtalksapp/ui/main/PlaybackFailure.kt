package dev.jpcottin.tedtalksapp.ui.main

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource

/**
 * What to tell the user when a talk will not play. The bundled videos live on TED's
 * CDN under a fixed key and the audio on Acast's: either can stop serving a file
 * long after the feed listed it, and "Source error" says nothing about that.
 */
object PlaybackFailure {
    @OptIn(UnstableApi::class)
    fun describe(error: PlaybackException, isAudio: Boolean): String =
        describe(error.errorCode, (error.cause as? HttpDataSource.InvalidResponseCodeException)?.responseCode, isAudio)

    /**
     * @param errorCode one of [PlaybackException]'s `ERROR_CODE_*`.
     * @param httpStatus the status the server answered, when the failure was an HTTP one.
     */
    fun describe(errorCode: Int, httpStatus: Int?, isAudio: Boolean): String {
        val file = if (isAudio) "audio" else "video"
        val name = PlaybackException.getErrorCodeName(errorCode)
        return when (errorCode) {
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                "TED no longer serves this talk's $file" +
                    (if (httpStatus != null) " (its server answered HTTP $httpStatus)" else "") +
                    ". The talk is still listed, but there is nothing left to play."
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "The $file could not be reached. Check your connection and try again."
            PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED ->
                "This device is not allowed to fetch the $file from where it is kept ($name)."
            in PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED..PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            in PlaybackException.ERROR_CODE_DECODER_INIT_FAILED..PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
                "This $file cannot be played on this device ($name)."
            else -> "Playback failed ($name)."
        }
    }
}
