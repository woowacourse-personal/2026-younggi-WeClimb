package com.weclimb.session

import com.weclimb.media.CacheGateway
import com.weclimb.media.MediaStoreGateway
import com.weclimb.media.AttemptMediaState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttemptServiceTest {
    private val activeSession = Session("session-1", "gym-1", 1L, status = SessionStatus.ACTIVE)

    @Test
    fun recordsSuccessfulAttemptWithoutCreatingVideoMedia() {
        val result = AttemptService(FailingMediaStore()).recordSuccessWithoutVideo(
            session = activeSession,
            color = "green",
            recordedAtEpochMillis = 2L,
            attemptId = "attempt-without-video",
        )

        assertEquals("attempt-without-video", result.attempt.id)
        assertEquals(AttemptOutcome.SUCCESS, result.attempt.outcome)
        assertEquals("green", result.attempt.color)
        assertEquals(null, result.attempt.videoUri)
        assertEquals(null, result.attempt.cachePath)
        assertEquals(AttemptMediaState.NONE, result.attempt.media.state)
        assertEquals(1, result.successCount)
    }

    @Test
    fun savesSuccessfulAttemptWithMediaStoreUriAndUpdatesCount() {
        val result = AttemptService(SuccessfulMediaStore()).recordSuccess(activeSession, "blue", "/cache/a.mp4", 2L)

        assertEquals(AttemptOutcome.SUCCESS, result.attempt.outcome)
        assertEquals("content://video/1", result.attempt.videoUri)
        assertEquals(AttemptMediaState.TRIM_PENDING, result.attempt.media.state)
        assertEquals(1, result.successCount)
    }

    @Test
    fun preservesCachePathWhenMediaStoreSaveFails() {
        val result = AttemptService(FailingMediaStore()).recordSuccess(activeSession, "blue", "/cache/a.mp4", 2L)

        assertEquals(AttemptOutcome.SAVE_PENDING, result.attempt.outcome)
        assertEquals("/cache/a.mp4", result.attempt.cachePath)
    }

    @Test
    fun returnsSaveFailureReasonWhenMediaStoreSaveFails() {
        val result = AttemptService(FailingMediaStore()).recordSuccess(activeSession, "blue", "/cache/a.mp4", 2L)

        assertEquals("MediaStore write failed", result.saveErrorMessage)
    }

    @Test
    fun retriesSavePendingAttemptWithoutChangingItsIdentity() {
        val pending = Attempt("attempt-1", activeSession.id, "blue", 2L, AttemptOutcome.SAVE_PENDING, null, "/cache/a.mp4")

        val result = AttemptService(SuccessfulMediaStore()).retrySave(pending)

        assertEquals(pending.id, result.id)
        assertEquals(AttemptOutcome.SUCCESS, result.outcome)
        assertEquals("content://video/1", result.videoUri)
        assertEquals(null, result.cachePath)
        assertEquals(AttemptMediaState.TRIM_PENDING, result.media.state)
    }

    @Test
    fun endsSessionAndDeletesOnlyFailedCacheAttempts() {
        val cache = RecordingCache()
        val attempts = listOf(
            Attempt("success", activeSession.id, "blue", 2L, AttemptOutcome.SUCCESS, "content://video/1", null),
            Attempt("failure", activeSession.id, "red", 3L, AttemptOutcome.FAILURE, null, "/cache/failure.mp4"),
        )

        val result = SessionFinisher(cache).finish(activeSession, attempts, 4L).getOrThrow()

        assertEquals(SessionStatus.ENDED, result.session.status)
        assertEquals(AppDestination.Home, result.destination)
        assertEquals(listOf("/cache/failure.mp4"), cache.deleted)
        assertTrue(result.attempts.none { it.outcome == AttemptOutcome.FAILURE && it.cachePath != null })
    }

    @Test
    fun keepsSessionActiveWhenFailedCacheCannotBeDeleted() {
        val attempts = listOf(Attempt("failure", activeSession.id, "red", 3L, AttemptOutcome.FAILURE, null, "/cache/failure.mp4"))

        val result = SessionFinisher(FailingCache()).finish(activeSession, attempts, 4L)

        assertTrue(result.isFailure)
    }

    private class SuccessfulMediaStore : MediaStoreGateway {
        override fun save(path: String): Result<String> = Result.success("content://video/1")
    }

    private class FailingMediaStore : MediaStoreGateway {
        override fun save(path: String): Result<String> = Result.failure(IllegalStateException("MediaStore write failed"))
    }

    private class RecordingCache : CacheGateway {
        val deleted = mutableListOf<String>()

        override fun delete(path: String): Result<Unit> {
            deleted += path
            return Result.success(Unit)
        }
    }

    private class FailingCache : CacheGateway {
        override fun delete(path: String): Result<Unit> = Result.failure(IllegalStateException("delete failed"))
    }
}
