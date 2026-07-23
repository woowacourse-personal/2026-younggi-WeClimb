package com.weclimb.media

import com.weclimb.session.Attempt
import com.weclimb.session.originalVideoUri

enum class AttemptMediaState {
    NONE,
    TRIM_PENDING,
    ORIGINAL_KEPT,
    TRIM_PROCESSING,
    TRIMMED,
    TRIM_FAILED,
}

data class AttemptMedia(
    val state: AttemptMediaState,
    val originalVideoUri: String,
    val trimmedVideoUri: String? = null,
    val errorMessage: String? = null,
) {
    val displayVideoUri: String
        get() = trimmedVideoUri ?: originalVideoUri

    companion object {
        fun none(): AttemptMedia = AttemptMedia(
            state = AttemptMediaState.NONE,
            originalVideoUri = "",
        )

        fun pending(originalVideoUri: String): AttemptMedia = AttemptMedia(
            state = AttemptMediaState.TRIM_PENDING,
            originalVideoUri = originalVideoUri,
        )

        fun originalKept(originalVideoUri: String): AttemptMedia = AttemptMedia(
            state = AttemptMediaState.ORIGINAL_KEPT,
            originalVideoUri = originalVideoUri,
        )
    }
}

sealed interface AttemptMediaResult {
    data class Updated(val attempt: Attempt) : AttemptMediaResult
    data class Rejected(val message: String) : AttemptMediaResult
}

class AttemptMediaService {
    fun keepOriginal(attempt: Attempt): Attempt = attempt.copy(
        media = AttemptMedia.originalKept(attempt.originalVideoUri),
    )

    fun startTrim(attempt: Attempt): AttemptMediaResult {
        if (attempt.media.state !in setOf(AttemptMediaState.TRIM_PENDING, AttemptMediaState.TRIM_FAILED)) {
            return AttemptMediaResult.Rejected("트리밍할 수 있는 영상이 아닙니다")
        }
        return AttemptMediaResult.Updated(
            attempt.copy(
                media = attempt.media.copy(
                    state = AttemptMediaState.TRIM_PROCESSING,
                    errorMessage = null,
                ),
            ),
        )
    }

    fun completeTrim(attempt: Attempt, trimmedVideoUri: String): AttemptMediaResult {
        if (attempt.media.state != AttemptMediaState.TRIM_PROCESSING || trimmedVideoUri.isBlank()) {
            return AttemptMediaResult.Rejected("트리밍 결과를 저장할 수 없습니다")
        }
        return AttemptMediaResult.Updated(
            attempt.copy(
                media = attempt.media.copy(
                    state = AttemptMediaState.TRIMMED,
                    trimmedVideoUri = trimmedVideoUri,
                    errorMessage = null,
                ),
            ),
        )
    }

    fun failTrim(attempt: Attempt, message: String): Attempt = attempt.copy(
        media = attempt.media.copy(
            state = AttemptMediaState.TRIM_FAILED,
            errorMessage = message.ifBlank { "트리밍에 실패했습니다" },
        ),
    )

    fun cancelTrim(attempt: Attempt): Attempt = if (attempt.media.state == AttemptMediaState.TRIM_PROCESSING) {
        attempt.copy(
            media = attempt.media.copy(
                state = AttemptMediaState.TRIM_PENDING,
                errorMessage = null,
            ),
        )
    } else {
        attempt
    }

    fun recoverInterrupted(attempt: Attempt): Attempt = if (attempt.media.state == AttemptMediaState.TRIM_PROCESSING) {
        failTrim(attempt, "트리밍이 중단되었습니다")
    } else {
        attempt
    }
}
