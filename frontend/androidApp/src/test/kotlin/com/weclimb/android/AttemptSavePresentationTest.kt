package com.weclimb.android

import com.weclimb.media.AttemptMedia
import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome
import com.weclimb.session.SuccessAttemptResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AttemptSavePresentationTest {
    @Test
    fun saveFailureShowsAnErrorAndRetriesThePendingAttempt() {
        val pending = savePendingAttempt()
        val result = SuccessAttemptResult(
            attempt = pending,
            successCount = 0,
            saveErrorMessage = "MediaStore write failed",
        )

        val presentation = attemptSavePresentation(result)
        val state = AppState(
            attempts = listOf(pending),
            statusRetryAttemptId = presentation.retryAttemptId,
        )

        assertEquals(true, presentation.isError)
        assertEquals("MediaStore write failed", presentation.message)
        assertNull(presentation.mediaChoiceAttemptId)
        assertEquals(pending.id, state.retryableSaveAttempt()?.id)
    }

    @Test
    fun saveSuccessOpensTheMediaChoiceWithoutAStatusRetry() {
        val saved = savePendingAttempt().copy(
            outcome = AttemptOutcome.SUCCESS,
            videoUri = "content://video/original",
            cachePath = null,
            media = AttemptMedia.pending("content://video/original"),
        )

        val presentation = attemptSavePresentation(
            SuccessAttemptResult(attempt = saved, successCount = 1),
        )

        assertEquals(false, presentation.isError)
        assertEquals(saved.id, presentation.mediaChoiceAttemptId)
        assertNull(presentation.retryAttemptId)
    }

    @Test
    fun ignoresRetryIdWhenTheAttemptIsNoLongerPending() {
        val saved = savePendingAttempt().copy(
            outcome = AttemptOutcome.SUCCESS,
            videoUri = "content://video/original",
            cachePath = null,
            media = AttemptMedia.pending("content://video/original"),
        )

        val state = AppState(
            attempts = listOf(saved),
            statusRetryAttemptId = saved.id,
        )

        assertNull(state.retryableSaveAttempt())
    }
}

private fun savePendingAttempt() = Attempt(
    id = "attempt-1",
    sessionId = "session-1",
    color = "blue",
    recordedAtEpochMillis = 1L,
    outcome = AttemptOutcome.SAVE_PENDING,
    videoUri = null,
    cachePath = "/cache/attempt.mp4",
)
