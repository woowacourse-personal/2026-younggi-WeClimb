package com.weclimb.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrimAndShareTest {
    @Test
    fun sendsValidTrimRequestWithEditListMode() {
        val gateway = FakeTrimGateway()
        val service = TrimService(gateway)

        val result = service.trim(
            TrimRequest(
                sourcePath = "cache/success.mp4",
                outputPath = "cache/trimmed.mp4",
                startMillis = 1_000,
                endMillis = 4_000,
                durationMillis = 6_000,
            ),
        )

        assertEquals(TrimResult.Started, result)
        assertEquals(TrimMode.EDIT_LIST, gateway.request?.mode)
    }

    @Test
    fun rejectsTrimRangeWithNonPositiveDuration() {
        val gateway = FakeTrimGateway()
        val service = TrimService(gateway)

        val result = service.trim(
            TrimRequest(
                sourcePath = "cache/success.mp4",
                outputPath = "cache/trimmed.mp4",
                startMillis = 4_000,
                endMillis = 4_000,
                durationMillis = 6_000,
            ),
        )

        assertEquals(TrimError.INVALID_RANGE, assertIs<TrimResult.Rejected>(result).error)
        assertNull(gateway.request)
    }

    @Test
    fun rejectsTrimRangeOutsideVideoDuration() {
        val gateway = FakeTrimGateway()
        val service = TrimService(gateway)

        val result = service.trim(
            TrimRequest(
                sourcePath = "cache/success.mp4",
                outputPath = "cache/trimmed.mp4",
                startMillis = 1_000,
                endMillis = 7_000,
                durationMillis = 6_000,
            ),
        )

        assertEquals(TrimError.OUT_OF_BOUNDS, assertIs<TrimResult.Rejected>(result).error)
        assertNull(gateway.request)
    }

    @Test
    fun createsSingleVideoShareRequestWithReadPermission() {
        val request = ShareRequestFactory().create("content://media/Movies/WeClimb/success.mp4")

        assertEquals("android.intent.action.SEND", request.action)
        assertEquals("video/*", request.mimeType)
        assertEquals("content://media/Movies/WeClimb/success.mp4", request.streamUri)
        assertTrue(request.grantsReadPermission)
    }
}

private class FakeTrimGateway : TrimGateway {
    var request: TrimRequest? = null
        private set

    override fun start(request: TrimRequest) {
        this.request = request
    }
}
