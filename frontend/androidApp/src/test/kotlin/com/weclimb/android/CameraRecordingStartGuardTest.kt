package com.weclimb.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraRecordingStartGuardTest {
    @Test
    fun reportsCameraStartFailureWithoutCreatingRecording() {
        var error: String? = null

        val recording = CameraRecordingStartGuard().start(
            start = { throw IllegalStateException("camera unavailable") },
            onError = { error = it },
        )

        assertNull(recording)
        assertEquals("녹화를 시작하지 못했습니다", error)
    }
}
