package com.weclimb.android

import com.weclimb.media.TrimRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidEditListTrimGatewayTest {
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
