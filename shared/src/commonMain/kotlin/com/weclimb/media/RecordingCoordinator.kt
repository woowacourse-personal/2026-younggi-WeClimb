package com.weclimb.media

data class RecordingInventory(
    val successfulPaths: List<String> = emptyList(),
    val failedPaths: List<String> = emptyList(),
)

data class RecordingSnapshot(
    val state: RecordingState = RecordingState.Ready,
    val inventory: RecordingInventory = RecordingInventory(),
)

class RecordingCoordinator {
    fun start(
        snapshot: RecordingSnapshot,
        cameraGranted: Boolean,
        microphoneGranted: Boolean,
        outputPath: String?,
        recorderStarts: Boolean,
    ): RecordingSnapshot {
        val error = startError(cameraGranted, microphoneGranted, outputPath, recorderStarts)
        return if (error == null) {
            snapshot.copy(state = RecordingState.Recording(outputPath.orEmpty()))
        } else {
            snapshot.copy(state = RecordingState.Error(error))
        }
    }

    fun markSuccessful(snapshot: RecordingSnapshot): RecordingSnapshot {
        val path = (snapshot.state as? RecordingState.Recording)?.outputPath ?: return snapshot
        return snapshot.copy(
            state = RecordingState.Ready,
            inventory = snapshot.inventory.copy(
                successfulPaths = snapshot.inventory.successfulPaths + path,
            ),
        )
    }

    fun markFailed(snapshot: RecordingSnapshot): RecordingSnapshot {
        val path = (snapshot.state as? RecordingState.Recording)?.outputPath ?: return snapshot
        return snapshot.copy(
            state = RecordingState.Ready,
            inventory = snapshot.inventory.copy(
                failedPaths = snapshot.inventory.failedPaths + path,
            ),
        )
    }

    private fun startError(
        cameraGranted: Boolean,
        microphoneGranted: Boolean,
        outputPath: String?,
        recorderStarts: Boolean,
    ): RecordingError? = when {
        !cameraGranted -> RecordingError.CAMERA_PERMISSION_DENIED
        !microphoneGranted -> RecordingError.MICROPHONE_PERMISSION_DENIED
        outputPath.isNullOrBlank() -> RecordingError.CACHE_OUTPUT_UNAVAILABLE
        !recorderStarts -> RecordingError.RECORDER_START_FAILED
        else -> null
    }
}
