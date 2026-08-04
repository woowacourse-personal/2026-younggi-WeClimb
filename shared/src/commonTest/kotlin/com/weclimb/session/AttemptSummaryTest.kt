package com.weclimb.session

import com.weclimb.media.AttemptMedia
import com.weclimb.media.AttemptMediaState
import kotlin.test.Test
import kotlin.test.assertEquals

class AttemptSummaryTest {
    @Test
    fun summarizesTruthfulCountsWithoutInventingALevel() {
        val attempts = listOf(
            attempt("success", AttemptOutcome.SUCCESS, AttemptMedia.none()),
            attempt("unclassified", AttemptOutcome.UNCLASSIFIED, AttemptMedia.none()),
            attempt("failure", AttemptOutcome.FAILURE, AttemptMedia.none()),
            attempt("pending-save", AttemptOutcome.SAVE_PENDING, AttemptMedia.none()),
        )

        val summary = summarizeAttempts(attempts)

        assertEquals(1, summary.successCount)
        assertEquals(4, summary.totalCount)
    }

    @Test
    fun countsEveryUnfinishedTrimStateAsRequiringAction() {
        val attempts = listOf(
            attempt("pending", media = media(AttemptMediaState.TRIM_PENDING)),
            attempt("failed", media = media(AttemptMediaState.TRIM_FAILED)),
            attempt("processing", media = media(AttemptMediaState.TRIM_PROCESSING)),
            attempt("kept", media = media(AttemptMediaState.ORIGINAL_KEPT)),
            attempt("trimmed", media = media(AttemptMediaState.TRIMMED)),
        )

        assertEquals(3, summarizeAttempts(attempts).mediaActionCount)
    }

    private fun attempt(
        id: String,
        outcome: AttemptOutcome = AttemptOutcome.SUCCESS,
        media: AttemptMedia,
    ) = Attempt(id, "session-1", "blue", 1L, outcome, null, null, media)

    private fun media(state: AttemptMediaState) = AttemptMedia(state, "content://video/original")
}
