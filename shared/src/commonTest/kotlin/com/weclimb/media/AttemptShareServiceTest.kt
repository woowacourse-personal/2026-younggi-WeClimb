package com.weclimb.media

import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AttemptShareServiceTest {
    @Test
    fun createsVideoShareRequestForPersistedSuccessfulAttempt() {
        val attempt = Attempt(
            id = "attempt-1",
            sessionId = "session-1",
            color = "blue",
            recordedAtEpochMillis = 1L,
            outcome = AttemptOutcome.SUCCESS,
            videoUri = "content://media/external/video/media/42",
            cachePath = null,
        )

        val result = AttemptShareService().create(attempt)

        assertEquals(
            "content://media/external/video/media/42",
            assertIs<AttemptShareResult.Ready>(result).request.streamUri,
        )
    }

    @Test
    fun sharesTrimmedVideoInsteadOfRetainedOriginal() {
        val attempt = Attempt(
            id = "attempt-1",
            sessionId = "session-1",
            color = "blue",
            recordedAtEpochMillis = 1L,
            outcome = AttemptOutcome.SUCCESS,
            videoUri = "content://media/original",
            cachePath = null,
            media = AttemptMedia(
                state = AttemptMediaState.TRIMMED,
                originalVideoUri = "content://media/original",
                trimmedVideoUri = "content://media/trimmed",
            ),
        )

        val result = AttemptShareService().create(attempt)

        assertEquals("content://media/trimmed", assertIs<AttemptShareResult.Ready>(result).request.streamUri)
    }
}
