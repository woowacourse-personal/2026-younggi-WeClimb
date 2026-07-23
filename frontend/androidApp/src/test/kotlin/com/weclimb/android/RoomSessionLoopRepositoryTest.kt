package com.weclimb.android

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome
import com.weclimb.session.Gym
import com.weclimb.session.GymSource
import com.weclimb.session.Session
import com.weclimb.session.SessionStatus
import com.weclimb.media.AttemptMedia
import com.weclimb.media.AttemptMediaState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomSessionLoopRepositoryTest {
    private lateinit var database: SessionLoopDatabase
    private lateinit var repository: RoomSessionLoopRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SessionLoopDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomSessionLoopRepository(database.sessionLoopDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun persistsGuestGymActiveSessionAndAttempts() {
        val gym = Gym("gym-1", "개인 암장", GymSource.USER_ADDED)
        val session = Session("session-1", gym.id, 10L, status = SessionStatus.ACTIVE)
        val attempt = Attempt("attempt-1", session.id, "blue", 20L, AttemptOutcome.SUCCESS, "content://video/1", null)

        repository.saveGuestProfile().getOrThrow()
        repository.saveGym(gym).getOrThrow()
        repository.saveSession(session).getOrThrow()
        repository.saveAttempt(attempt).getOrThrow()

        assertEquals(true, repository.hasGuestProfile())
        assertEquals(listOf(gym), repository.gyms())
        assertEquals(session, repository.activeSession())
        assertEquals(listOf(attempt), repository.attempts(session.id))
    }

    @Test
    fun deletesFailedAttemptsAndDoesNotRestoreEndedSession() {
        val session = Session("session-1", "gym-1", 10L, status = SessionStatus.ACTIVE)
        val failure = Attempt("attempt-1", session.id, "red", 20L, AttemptOutcome.FAILURE, null, "/cache/failure.mp4")

        repository.saveSession(session).getOrThrow()
        repository.saveAttempt(failure).getOrThrow()
        repository.deleteAttempts(listOf(failure.id)).getOrThrow()
        repository.saveSession(session.copy(status = SessionStatus.ENDED, endedAtEpochMillis = 30L)).getOrThrow()

        assertEquals(emptyList<Attempt>(), repository.attempts(session.id))
        assertEquals(null, repository.activeSession())
    }

    @Test
    fun returnsOnlySuccessfulArchiveAttemptsInNewestFirstOrder() {
        val gym = Gym("gym-1", "테스트 암장", GymSource.USER_ADDED)
        val session = Session("session-1", gym.id, 10L, status = SessionStatus.ACTIVE)
        val oldSuccess = Attempt("success-1", session.id, "blue", 20L, AttemptOutcome.SUCCESS, "content://video/old", null, AttemptMedia.pending("content://video/old"))
        val newSuccess = Attempt("success-2", session.id, "red", 30L, AttemptOutcome.SUCCESS, "content://video/new", null, AttemptMedia.originalKept("content://video/new"))
        val failure = Attempt("failure", session.id, "green", 40L, AttemptOutcome.FAILURE, null, "/cache/failure.mp4")

        repository.saveGym(gym).getOrThrow()
        repository.saveSession(session).getOrThrow()
        repository.saveAttempt(oldSuccess).getOrThrow()
        repository.saveAttempt(newSuccess).getOrThrow()
        repository.saveAttempt(failure).getOrThrow()

        assertEquals(listOf(newSuccess.id, oldSuccess.id), repository.archiveAttempts().map { it.attempt.id })
        assertEquals("테스트 암장", repository.archiveAttempts().first().gymName)
    }

    @Test
    fun recoversInterruptedTrimWithoutRemovingOriginalVideo() {
        val session = Session("session-1", "gym-1", 10L, status = SessionStatus.ACTIVE)
        val processing = Attempt(
            "success-1", session.id, "blue", 20L, AttemptOutcome.SUCCESS, "content://video/original", null,
            AttemptMedia(AttemptMediaState.TRIM_PROCESSING, "content://video/original", "content://video/orphan"),
        )

        repository.saveSession(session).getOrThrow()
        repository.saveAttempt(processing).getOrThrow()
        repository.recoverInterruptedTrims().getOrThrow()

        val recovered = repository.attempts(session.id).single()
        assertEquals(AttemptMediaState.TRIM_FAILED, recovered.media.state)
        assertEquals("content://video/original", recovered.media.originalVideoUri)
        assertEquals(null, recovered.media.trimmedVideoUri)
    }
}
