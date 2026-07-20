package com.weclimb.media

sealed interface RecordingState {
    data object Ready : RecordingState

    data class Recording(val outputPath: String) : RecordingState

    data class Error(val reason: RecordingError) : RecordingState
}

enum class RecordingError {
    CAMERA_PERMISSION_DENIED,
    MICROPHONE_PERMISSION_DENIED,
    CACHE_OUTPUT_UNAVAILABLE,
    RECORDER_START_FAILED,
}
