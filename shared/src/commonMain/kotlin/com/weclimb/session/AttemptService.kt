package com.weclimb.session

import com.weclimb.media.CacheGateway
import com.weclimb.media.MediaStoreGateway

enum class AttemptOutcome {
    SUCCESS,
    FAILURE,
    SAVE_PENDING,
}

data class Attempt(
    val id: String,
    val sessionId: String,
    val color: String,
    val recordedAtEpochMillis: Long,
    val outcome: AttemptOutcome,
    val videoUri: String?,
    val cachePath: String?,
)

data class SuccessAttemptResult(
    val attempt: Attempt,
    val successCount: Int,
)

class AttemptService(
    private val mediaStore: MediaStoreGateway,
) {
    fun recordSuccess(
        session: Session,
        color: String,
        cachePath: String,
        recordedAtEpochMillis: Long,
    ): SuccessAttemptResult = mediaStore.save(cachePath).fold(
        onSuccess = { uri ->
            SuccessAttemptResult(
                attempt = Attempt(
                    id = "${session.id}-$recordedAtEpochMillis",
                    sessionId = session.id,
                    color = color,
                    recordedAtEpochMillis = recordedAtEpochMillis,
                    outcome = AttemptOutcome.SUCCESS,
                    videoUri = uri,
                    cachePath = null,
                ),
                successCount = 1,
            )
        },
        onFailure = {
            SuccessAttemptResult(
                attempt = Attempt(
                    id = "${session.id}-$recordedAtEpochMillis",
                    sessionId = session.id,
                    color = color,
                    recordedAtEpochMillis = recordedAtEpochMillis,
                    outcome = AttemptOutcome.SAVE_PENDING,
                    videoUri = null,
                    cachePath = cachePath,
                ),
                successCount = 0,
            )
        },
    )

    fun retrySave(attempt: Attempt): Attempt {
        require(attempt.outcome == AttemptOutcome.SAVE_PENDING)
        val cachePath = requireNotNull(attempt.cachePath)
        return mediaStore.save(cachePath).fold(
            onSuccess = { uri -> attempt.copy(outcome = AttemptOutcome.SUCCESS, videoUri = uri, cachePath = null) },
            onFailure = { attempt },
        )
    }
}

data class FinishedSession(
    val session: Session,
    val attempts: List<Attempt>,
    val destination: AppDestination,
)

class SessionFinisher(
    private val cache: CacheGateway,
) {
    fun finish(session: Session, attempts: List<Attempt>, endedAtEpochMillis: Long): Result<FinishedSession> = runCatching {
        attempts.filter { it.outcome == AttemptOutcome.FAILURE }
            .mapNotNull(Attempt::cachePath)
            .forEach { path -> cache.delete(path).getOrThrow() }
        FinishedSession(
            session = session.copy(status = SessionStatus.ENDED, endedAtEpochMillis = endedAtEpochMillis),
            attempts = attempts.filterNot { it.outcome == AttemptOutcome.FAILURE },
            destination = AppDestination.Home,
        )
    }
}
