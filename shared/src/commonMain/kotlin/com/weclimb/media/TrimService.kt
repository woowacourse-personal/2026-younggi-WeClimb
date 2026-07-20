package com.weclimb.media

data class TrimRequest(
    val sourcePath: String,
    val outputPath: String,
    val startMillis: Long,
    val endMillis: Long,
    val durationMillis: Long,
    val mode: TrimMode = TrimMode.EDIT_LIST,
)

enum class TrimMode {
    EDIT_LIST,
}

enum class TrimError {
    INVALID_RANGE,
    OUT_OF_BOUNDS,
}

sealed interface TrimResult {
    data object Started : TrimResult

    data class Rejected(val error: TrimError) : TrimResult
}

interface TrimGateway {
    fun start(request: TrimRequest)
}

class TrimService(
    private val gateway: TrimGateway,
) {
    fun trim(request: TrimRequest): TrimResult {
        val error = validationError(request)
        if (error != null) {
            return TrimResult.Rejected(error)
        }
        gateway.start(request)
        return TrimResult.Started
    }

    private fun validationError(request: TrimRequest): TrimError? = when {
        request.endMillis <= request.startMillis -> TrimError.INVALID_RANGE
        request.startMillis < 0 || request.endMillis > request.durationMillis -> TrimError.OUT_OF_BOUNDS
        else -> null
    }
}
