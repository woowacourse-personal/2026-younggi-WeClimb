package com.weclimb.android

import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome
import com.weclimb.session.Gym
import com.weclimb.session.Session
import com.weclimb.session.SuccessAttemptResult
import java.io.File

internal fun isValidTrimRange(startMillis: Long, endMillis: Long, durationMillis: Long): Boolean =
    startMillis >= 0L && endMillis > startMillis && endMillis <= durationMillis

internal fun shouldDeferTrim(currentChoice: Attempt?, dismissedChoice: Attempt): Boolean =
    currentChoice?.id == dismissedChoice.id

internal fun AppState.afterTrimCompleted(
    attempt: Attempt,
    refreshedAttempts: List<Attempt>,
): AppState = copy(
    screen = Screen.Trim,
    attempts = refreshedAttempts,
    selectedAttempt = attempt,
    trimInProgress = false,
    message = null,
)

internal fun AppState.beginTrimSubmission(startMillis: Long, endMillis: Long): AppState? =
    if (trimInProgress) {
        null
    } else {
        copy(
            trimInProgress = true,
            lastTrimStartMillis = startMillis,
            lastTrimEndMillis = endMillis,
            message = "영상을 자르는 중입니다",
        )
    }

internal data class AttemptSavePresentation(
    val message: String?,
    val isError: Boolean,
    val mediaChoiceAttemptId: String?,
    val retryAttemptId: String?,
)

internal fun attemptSavePresentation(result: SuccessAttemptResult): AttemptSavePresentation =
    if (result.attempt.outcome == AttemptOutcome.SAVE_PENDING) {
        AttemptSavePresentation(
            message = result.saveErrorMessage ?: "영상을 저장하지 못했습니다",
            isError = true,
            mediaChoiceAttemptId = null,
            retryAttemptId = result.attempt.id,
        )
    } else {
        AttemptSavePresentation(
            message = result.saveErrorMessage,
            isError = false,
            mediaChoiceAttemptId = result.attempt.id,
            retryAttemptId = null,
        )
    }

internal fun AppState.retryableSaveAttempt(): Attempt? = statusRetryAttemptId
    ?.let { id -> attempts.firstOrNull { it.id == id && it.outcome == AttemptOutcome.SAVE_PENDING } }

internal fun AppState.beginClassification(): AppState? = if (capturedFile == null || classificationInProgress) {
    null
} else {
    copy(classificationInProgress = true)
}

internal val AppState.hasPendingSave: Boolean
    get() = attempts.any { it.outcome == AttemptOutcome.SAVE_PENDING }

internal data class AppState(
    val screen: Screen = Screen.Loading,
    val gyms: List<Gym> = emptyList(),
    val activeSession: Session? = null,
    val attempts: List<Attempt> = emptyList(),
    val archive: List<ArchiveAttempt> = emptyList(),
    val selectedAttempt: Attempt? = null,
    val classificationAttempt: Attempt? = null,
    val selectedVideoDurationMillis: Long = 19_000L,
    val mediaChoiceAttempt: Attempt? = null,
    val playingVideoUri: String? = null,
    val unavailableVideoAttemptId: String? = null,
    val trimInProgress: Boolean = false,
    val lastTrimStartMillis: Long? = null,
    val lastTrimEndMillis: Long? = null,
    val capturedFile: File? = null,
    val classificationInProgress: Boolean = false,
    val cameraReady: Boolean = false,
    val recording: Boolean = false,
    val confirmEnd: Boolean = false,
    val permissionsGranted: Boolean = false,
    val permissionRequested: Boolean = false,
    val settingsRequired: Boolean = false,
    val message: String? = null,
    val statusIsError: Boolean = false,
    val statusRetryAttemptId: String? = null,
)

internal enum class Screen {
    Loading,
    Onboarding,
    Home,
    Gyms,
    Board,
    Capture,
    Trim,
    Archive,
    SessionEndPreview,
    ReportPreview,
    RecordsPreview,
}
