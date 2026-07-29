package com.weclimb.android

import com.weclimb.media.AttemptMedia
import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome
import com.weclimb.media.TrimRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidEditListTrimGatewayTest {
    @Test
    fun keepsCompletedTrimOnTheTrimResultScreen() {
        val completed = Attempt(
            id = "attempt-1",
            sessionId = "session-1",
            color = "blue",
            recordedAtEpochMillis = 10L,
            outcome = AttemptOutcome.SUCCESS,
            videoUri = "content://video/original",
            cachePath = null,
            media = AttemptMedia(
                state = com.weclimb.media.AttemptMediaState.TRIMMED,
                originalVideoUri = "content://video/original",
                trimmedVideoUri = "content://video/trimmed",
            ),
        )

        val result = AppState(
            screen = Screen.Board,
            selectedAttempt = completed.copy(media = AttemptMedia.pending("content://video/original")),
            trimInProgress = true,
            message = "영상을 자르는 중입니다",
        ).afterTrimCompleted(completed, listOf(completed))

        assertEquals(Screen.Trim, result.screen)
        assertEquals(completed, result.selectedAttempt)
        assertEquals(listOf(completed), result.attempts)
        assertEquals(false, result.trimInProgress)
        assertEquals(null, result.message)
    }

    @Test
    fun rejectsInvalidTrimRangesBeforeStartingMedia3Export() {
        assertEquals(false, isValidTrimRange(500L, 0L, 1_000L))
        assertEquals(false, isValidTrimRange(-1L, 500L, 1_000L))
        assertEquals(false, isValidTrimRange(0L, 1_001L, 1_000L))
        assertEquals(true, isValidTrimRange(0L, 500L, 1_000L))
    }

    @Test
    fun startsEditListExportAndForwardsCompletion() {
        val exporter = FakeEditListExporter()
        var completedPath: String? = null
        val request = TrimRequest(
            sourcePath = "cache/source.mp4",
            outputPath = "cache/trimmed.mp4",
            startMillis = 1_000,
            endMillis = 4_000,
            durationMillis = 6_000,
        )
        val gateway = AndroidEditListTrimGateway(
            exporter = exporter,
            onCompleted = { completedPath = it },
            onError = { throw AssertionError(it) },
        )

        gateway.start(request)
        exporter.complete(request.outputPath)

        assertEquals(request, exporter.request)
        assertEquals(request.outputPath, completedPath)
    }
}

private class FakeEditListExporter : EditListExporter {
    var request: TrimRequest? = null
        private set
    private var onCompleted: ((String) -> Unit)? = null

    override fun export(
        request: TrimRequest,
        onCompleted: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        this.request = request
        this.onCompleted = onCompleted
    }

    fun complete(outputPath: String) {
        requireNotNull(onCompleted)(outputPath)
    }
}
