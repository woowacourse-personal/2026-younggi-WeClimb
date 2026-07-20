package com.weclimb.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SessionRepositoryTest {
    @Test
    fun savesAndFindsSessionForAuthenticatedUser() {
        val gateway = FakeSessionGateway()
        val repository = SessionRepository(gateway)
        val session = SessionMetadata(
            id = "session-1",
            userId = "user-1",
            startedAtEpochMillis = 1_000,
        )

        val saved = repository.save(session)
        val found = repository.find("user-1", "session-1")

        assertEquals(SessionWriteResult.Saved, saved)
        assertEquals(session, assertIs<SessionReadResult.Found>(found).session)
    }

    @Test
    fun rejectsWriteWhenUserIsNotAuthenticated() {
        val repository = SessionRepository(FakeSessionGateway())
        val session = SessionMetadata("session-1", "", 1_000)

        val result = repository.save(session)

        assertEquals(SessionWriteResult.Rejected(SessionError.UNAUTHENTICATED), result)
    }

    @Test
    fun returnsNetworkErrorWithoutPersistingSession() {
        val gateway = FakeSessionGateway(shouldFail = true)
        val repository = SessionRepository(gateway)
        val session = SessionMetadata("session-1", "user-1", 1_000)

        val result = repository.save(session)

        assertEquals(SessionWriteResult.Rejected(SessionError.NETWORK), result)
        assertEquals(SessionReadResult.Missing, repository.find("user-1", "session-1"))
    }
}

private class FakeSessionGateway(
    private val shouldFail: Boolean = false,
) : SessionGateway {
    private var session: SessionMetadata? = null

    override fun insert(session: SessionMetadata): Result<Unit> {
        if (shouldFail) {
            return Result.failure(IllegalStateException("network failure"))
        }
        this.session = session
        return Result.success(Unit)
    }

    override fun find(userId: String, sessionId: String): Result<SessionMetadata?> = Result.success(
        session?.takeIf { it.userId == userId && it.id == sessionId },
    )
}
