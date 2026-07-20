package com.weclimb.session

data class SessionMetadata(
    val id: String,
    val userId: String,
    val startedAtEpochMillis: Long,
)

interface SessionGateway {
    fun insert(session: SessionMetadata): Result<Unit>

    fun find(userId: String, sessionId: String): Result<SessionMetadata?>
}

enum class SessionError {
    UNAUTHENTICATED,
    NETWORK,
}

sealed interface SessionWriteResult {
    data object Saved : SessionWriteResult

    data class Rejected(val error: SessionError) : SessionWriteResult
}

sealed interface SessionReadResult {
    data class Found(val session: SessionMetadata) : SessionReadResult

    data object Missing : SessionReadResult
}

class SessionRepository(
    private val gateway: SessionGateway,
) {
    fun save(session: SessionMetadata): SessionWriteResult {
        if (session.userId.isBlank()) {
            return SessionWriteResult.Rejected(SessionError.UNAUTHENTICATED)
        }
        return gateway.insert(session).fold(
            onSuccess = { SessionWriteResult.Saved },
            onFailure = { SessionWriteResult.Rejected(SessionError.NETWORK) },
        )
    }

    fun find(userId: String, sessionId: String): SessionReadResult = gateway.find(userId, sessionId)
        .getOrNull()
        ?.let(SessionReadResult::Found)
        ?: SessionReadResult.Missing
}
