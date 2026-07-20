package com.weclimb.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RecordingCoordinatorTest {
    private val coordinator = RecordingCoordinator()

    @Test
    fun startsRecordingWithCacheOutputWhenPermissionsAreGranted() {
        val result = coordinator.start(
            snapshot = RecordingSnapshot(),
            cameraGranted = true,
            microphoneGranted = true,
            outputPath = "cache/attempt-1.mp4",
            recorderStarts = true,
        )

        assertEquals("cache/attempt-1.mp4", assertIs<RecordingState.Recording>(result.state).outputPath)
    }

    @Test
    fun successfulRecordingReturnsToReadyStateAndKeepsCandidate() {
        val recording = recordingSnapshot()

        val result = coordinator.markSuccessful(recording)

        assertEquals(RecordingState.Ready, result.state)
        assertEquals(listOf("cache/attempt-1.mp4"), result.inventory.successfulPaths)
        assertTrue(result.inventory.failedPaths.isEmpty())
    }

    @Test
    fun failedRecordingReturnsToReadyStateAndQueuesOnlyFailedCandidate() {
        val recording = recordingSnapshot()

        val result = coordinator.markFailed(recording)

        assertEquals(RecordingState.Ready, result.state)
        assertEquals(listOf("cache/attempt-1.mp4"), result.inventory.failedPaths)
        assertTrue(result.inventory.successfulPaths.isEmpty())
    }

    @Test
    fun missingCameraPermissionReturnsCameraPermissionErrorWithoutRecording() {
        val result = coordinator.start(
            snapshot = RecordingSnapshot(),
            cameraGranted = false,
            microphoneGranted = true,
            outputPath = "cache/attempt-1.mp4",
            recorderStarts = true,
        )

        assertEquals(RecordingError.CAMERA_PERMISSION_DENIED, assertIs<RecordingState.Error>(result.state).reason)
        assertTrue(result.inventory.successfulPaths.isEmpty())
        assertTrue(result.inventory.failedPaths.isEmpty())
    }

    @Test
    fun missingMicrophonePermissionReturnsMicrophonePermissionErrorWithoutRecording() {
        val result = coordinator.start(
            snapshot = RecordingSnapshot(),
            cameraGranted = true,
            microphoneGranted = false,
            outputPath = "cache/attempt-1.mp4",
            recorderStarts = true,
        )

        assertEquals(RecordingError.MICROPHONE_PERMISSION_DENIED, assertIs<RecordingState.Error>(result.state).reason)
        assertTrue(result.inventory.successfulPaths.isEmpty())
        assertTrue(result.inventory.failedPaths.isEmpty())
    }

    private fun recordingSnapshot(): RecordingSnapshot = coordinator.start(
        snapshot = RecordingSnapshot(),
        cameraGranted = true,
        microphoneGranted = true,
        outputPath = "cache/attempt-1.mp4",
        recorderStarts = true,
    )
}
