package com.weclimb.session

import com.weclimb.media.AttemptMediaState

data class AttemptSummary(
    val successCount: Int,
    val totalCount: Int,
    val mediaActionCount: Int,
)

fun summarizeAttempts(attempts: List<Attempt>): AttemptSummary = AttemptSummary(
    successCount = attempts.count { it.outcome == AttemptOutcome.SUCCESS },
    totalCount = attempts.size,
    mediaActionCount = attempts.count {
        it.media.state in setOf(
            AttemptMediaState.TRIM_PENDING,
            AttemptMediaState.TRIM_FAILED,
            AttemptMediaState.TRIM_PROCESSING,
        )
    },
)
