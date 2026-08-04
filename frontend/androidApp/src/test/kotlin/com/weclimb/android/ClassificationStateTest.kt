package com.weclimb.android

import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassificationStateTest {
    @Test
    fun acceptsOnlyTheFirstClassificationInput() {
        val initial = AppState(capturedFile = File("/cache/attempt.mp4"))

        val started = initial.beginClassification()

        assertTrue(requireNotNull(started).classificationInProgress)
        assertNull(started.beginClassification())
    }

    @Test
    fun ignoresClassificationWithoutACapturedVideo() {
        assertNull(AppState().beginClassification())
    }

    @Test
    fun exposesPendingSaveAsAnEndSessionBlocker() {
        val state = AppState(attempts = listOf(pendingAttempt()))

        assertTrue(state.hasPendingSave)
        assertFalse(AppState().hasPendingSave)
    }

    private fun pendingAttempt() = Attempt(
        id = "pending-1",
        sessionId = "session-1",
        color = "blue",
        recordedAtEpochMillis = 1L,
        outcome = AttemptOutcome.SAVE_PENDING,
        videoUri = null,
        cachePath = "/cache/pending.mp4",
    )
}
