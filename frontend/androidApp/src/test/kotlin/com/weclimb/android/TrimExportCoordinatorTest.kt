package com.weclimb.android

import com.weclimb.media.TrimRequest
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrimExportCoordinatorTest {
    @Test
    fun cancelsActiveExportDeletesPartialOutputAndIgnoresLateCompletion() {
        val output = File.createTempFile("trim-export", ".mp4").apply { delete() }
        val exporter = ControllableEditListExporter()
        val coordinator = TrimExportCoordinator { exporter }
        var completedPath: String? = null
        var errorMessage: String? = null
        val request = TrimRequest(
            sourcePath = "cache/source.mp4",
            outputPath = output.absolutePath,
            startMillis = 1_000L,
            endMillis = 4_000L,
            durationMillis = 6_000L,
        )

        coordinator.start(
            attemptId = "attempt-1",
            request = request,
            onCompleted = { completedPath = it },
            onError = { errorMessage = it },
        )
        output.writeText("partial")

        assertTrue(coordinator.cancel("attempt-1"))
        exporter.complete(request.outputPath)
        exporter.fail("late failure")

        assertTrue(exporter.cancelled)
        assertFalse(output.exists())
        assertEquals(null, completedPath)
        assertEquals(null, errorMessage)
    }

    @Test
    fun doesNotCancelAnotherAttemptsExport() {
        val exporter = ControllableEditListExporter()
        val coordinator = TrimExportCoordinator { exporter }
        val request = TrimRequest(
            sourcePath = "cache/source.mp4",
            outputPath = File.createTempFile("trim-export", ".mp4").apply { delete() }.absolutePath,
            startMillis = 1_000L,
            endMillis = 4_000L,
            durationMillis = 6_000L,
        )

        coordinator.start("attempt-1", request, {}, {})

        assertFalse(coordinator.cancel("attempt-2"))
        assertFalse(exporter.cancelled)
    }
}

private class ControllableEditListExporter : EditListExporter {
    var cancelled = false
        private set
    private var onCompleted: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null

    override fun export(
        request: TrimRequest,
        onCompleted: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        this.onCompleted = onCompleted
        this.onError = onError
    }

    override fun cancel() {
        cancelled = true
    }

    fun complete(outputPath: String) {
        requireNotNull(onCompleted)(outputPath)
    }

    fun fail(message: String) {
        requireNotNull(onError)(message)
    }
}
