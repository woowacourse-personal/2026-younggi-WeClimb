package com.weclimb.media

import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome

sealed interface AttemptShareResult {
    data class Ready(val request: ShareRequest) : AttemptShareResult

    data object VideoUnavailable : AttemptShareResult
}

class AttemptShareService(
    private val shareRequestFactory: ShareRequestFactory = ShareRequestFactory(),
) {
    fun create(attempt: Attempt): AttemptShareResult {
        val videoUri = attempt.videoUri
        return if (attempt.outcome == AttemptOutcome.SUCCESS && videoUri != null) {
            AttemptShareResult.Ready(shareRequestFactory.create(videoUri))
        } else {
            AttemptShareResult.VideoUnavailable
        }
    }
}
