package com.weclimb.media

import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome
import com.weclimb.session.displayVideoUri
import com.weclimb.session.originalVideoUri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AttemptMediaServiceTest {
    private val pendingAttempt = Attempt(
        id = "attempt-1",
        sessionId = "session-1",
        color = "blue",
        recordedAtEpochMillis = 1L,
        outcome = AttemptOutcome.SUCCESS,
        videoUri = "content://media/original",
        cachePath = null,
        media = AttemptMedia.pending("content://media/original"),
    )

    @Test
    fun keepsOriginalAsTheDisplayVideoWhenOriginalIsSelected() {
        val result = AttemptMediaService().keepOriginal(pendingAttempt)

        assertEquals(AttemptMediaState.ORIGINAL_KEPT, result.media.state)
        assertEquals("content://media/original", result.displayVideoUri)
        assertEquals("content://media/original", result.originalVideoUri)
    }

    @Test
    fun startsTrimOnlyForPendingAttempts() {
        val result = AttemptMediaService().startTrim(pendingAttempt)

        assertIs<AttemptMediaResult.Updated>(result)
        assertEquals(AttemptMediaState.TRIM_PROCESSING, result.attempt.media.state)
    }

    @Test
    fun completesTrimWithoutReplacingOriginal() {
        val processing = AttemptMediaService().startTrim(pendingAttempt)
        val attempt = assertIs<AttemptMediaResult.Updated>(processing).attempt

        val result = AttemptMediaService().completeTrim(attempt, "content://media/trimmed")

        assertIs<AttemptMediaResult.Updated>(result)
        assertEquals(AttemptMediaState.TRIMMED, result.attempt.media.state)
        assertEquals("content://media/original", result.attempt.originalVideoUri)
        assertEquals("content://media/trimmed", result.attempt.displayVideoUri)
    }

    @Test
    fun recoversInterruptedTrimAsRetryableFailure() {
        val processing = assertIs<AttemptMediaResult.Updated>(AttemptMediaService().startTrim(pendingAttempt)).attempt

        val recovered = AttemptMediaService().recoverInterrupted(processing)

        assertEquals(AttemptMediaState.TRIM_FAILED, recovered.media.state)
        assertEquals("트리밍이 중단되었습니다", recovered.media.errorMessage)
        assertEquals("content://media/original", recovered.displayVideoUri)
    }

    @Test
    fun keepsOriginalWhenTrimFails() {
        val processing = assertIs<AttemptMediaResult.Updated>(AttemptMediaService().startTrim(pendingAttempt)).attempt

        val failed = AttemptMediaService().failTrim(processing, "출력 저장 실패")

        assertEquals(AttemptMediaState.TRIM_FAILED, failed.media.state)
        assertEquals("content://media/original", failed.originalVideoUri)
        assertEquals("content://media/original", failed.displayVideoUri)
        assertEquals("출력 저장 실패", failed.media.errorMessage)
    }

    @Test
    fun restartsTrimFromRetryableFailureWithoutReplacingOriginal() {
        val processing = assertIs<AttemptMediaResult.Updated>(AttemptMediaService().startTrim(pendingAttempt)).attempt
        val failed = AttemptMediaService().failTrim(processing, "출력 저장 실패")

        val retried = assertIs<AttemptMediaResult.Updated>(AttemptMediaService().startTrim(failed)).attempt

        assertEquals(AttemptMediaState.TRIM_PROCESSING, retried.media.state)
        assertEquals("content://media/original", retried.originalVideoUri)
    }

    @Test
    fun returnsToPendingWhenUserCancelsBeforeTrimStarts() {
        val processing = assertIs<AttemptMediaResult.Updated>(AttemptMediaService().startTrim(pendingAttempt)).attempt

        val cancelled = AttemptMediaService().cancelTrim(processing)

        assertEquals(AttemptMediaState.TRIM_PENDING, cancelled.media.state)
        assertEquals("content://media/original", cancelled.originalVideoUri)
    }

    @Test
    fun returnsFailedTrimToPendingWhenUserDefersIt() {
        val processing = assertIs<AttemptMediaResult.Updated>(AttemptMediaService().startTrim(pendingAttempt)).attempt
        val failed = AttemptMediaService().failTrim(processing, "출력 저장 실패")

        val deferred = AttemptMediaService().cancelTrim(failed)

        assertEquals(AttemptMediaState.TRIM_PENDING, deferred.media.state)
        assertEquals(null, deferred.media.errorMessage)
        assertEquals("content://media/original", deferred.originalVideoUri)
    }
}
