package com.weclimb.android

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.camera.view.PreviewView
import java.io.File
import android.media.MediaMetadataRetriever
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import com.weclimb.session.AppDestination
import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome
import com.weclimb.session.AttemptService
import com.weclimb.session.Gym
import com.weclimb.session.GymCatalog
import com.weclimb.session.GymSource
import com.weclimb.session.OnboardingResult
import com.weclimb.session.OnboardingService
import com.weclimb.session.PermissionState
import com.weclimb.session.Session
import com.weclimb.session.SessionFinisher
import com.weclimb.session.SessionNavigator
import com.weclimb.session.SuccessAttemptResult
import com.weclimb.session.displayVideoUri
import com.weclimb.session.originalVideoUri
import com.weclimb.media.AttemptShareResult
import com.weclimb.media.AttemptShareService
import com.weclimb.media.AttemptMediaResult
import com.weclimb.media.AttemptMediaService
import com.weclimb.media.TrimRequest
import java.util.UUID
import java.util.concurrent.Executors

@UnstableApi
@ExperimentalApi
class MainActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var repository: RoomSessionLoopRepository
    private lateinit var recorder: CameraRecordingController
    private val trimExports = TrimExportCoordinator { Media3EditListExporter(this) }
    private var state by mutableStateOf(AppState())
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { onPermissionsUpdated() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = RoomSessionLoopRepository(SessionLoopDatabase.create(this).sessionLoopDao())
        recorder = CameraRecordingController(this, this)
        setContent {
            WeClimbTheme {
                SessionLoopApp(
                    state,
                    ::requestPermissions,
                    ::openAppSettings,
                    ::completeOnboarding,
                    ::openGyms,
                    ::openCapture,
                    ::recordSuccessWithoutVideo,
                    ::startSession,
                    ::addGym,
                    ::renameGym,
                    ::hideGym,
                    ::toggleRecording,
                    ::classifySuccess,
                    ::classifyFailure,
                    ::captureSystemBack,
                    ::openTrim,
                    ::deferTrim,
                    ::keepOriginal,
                    ::openArchive,
                    ::openClassification,
                    ::playAttempt,
                    ::closePlayback,
                    ::submitTrim,
                    ::cancelTrim,
                    ::backToBoard,
                    ::shareAttempt,
                    ::retryPendingAttempt,
                    ::discardPendingAttempt,
                    ::retryStatusAction,
                    ::requestEndSession,
                    ::endSession,
                    ::openStaticScreen,
                    ::attachCameraPreview,
                )
            }
        }
        loadInitialState()
    }

    override fun onDestroy() {
        recorder.release()
        executor.shutdown()
        super.onDestroy()
    }

    private fun loadInitialState(
        message: String? = null,
        mediaChoiceAttemptId: String? = null,
        statusIsError: Boolean = false,
        statusRetryAttemptId: String? = null,
    ) = background {
        runCatching {
            repository.importSeedGyms(loadSeedGyms(this))
            repository.recoverInterruptedTrims().getOrThrow()
            val destination = if (repository.hasGuestProfile()) SessionNavigator().initialDestination(repository.activeSession()) else null
            val activeSession = repository.activeSession()
            val attempts = activeSession?.let { session -> repository.attempts(session.id) }.orEmpty()
            AppState(
                screen = destination.toScreen(),
                gyms = repository.gyms(),
                activeSession = activeSession,
                attempts = attempts,
                mediaChoiceAttempt = attempts.firstOrNull { it.id == mediaChoiceAttemptId },
                cameraReady = state.cameraReady,
                message = message,
                statusIsError = statusIsError,
                statusRetryAttemptId = statusRetryAttemptId,
            )
        }.fold(::render, ::showError)
    }

    private fun requestPermissions() {
        render(state.copy(permissionRequested = true))
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun onPermissionsUpdated() {
        val granted = hasPermission(Manifest.permission.CAMERA) && hasPermission(Manifest.permission.RECORD_AUDIO)
        if (granted) {
            recorder.bind(
                onReady = { render(state.copy(cameraReady = true, permissionsGranted = true, settingsRequired = false)) },
                onError = { message -> render(state.copy(message = message)) },
            )
        } else {
            val settingsRequired = state.permissionRequested && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) &&
                !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
            render(state.copy(permissionsGranted = false, settingsRequired = settingsRequired, message = "카메라와 마이크 권한이 필요합니다"))
        }
    }

    private fun completeOnboarding() {
        val permissions = PermissionState(hasPermission(Manifest.permission.CAMERA), hasPermission(Manifest.permission.RECORD_AUDIO))
        when (OnboardingService().complete(permissions)) {
            is OnboardingResult.Completed -> background {
                repository.saveGuestProfile().fold(
                    onSuccess = {
                        onPermissionsUpdated()
                        loadInitialState()
                    },
                    onFailure = ::showError,
                )
            }
            is OnboardingResult.PermissionRequired -> requestPermissions()
        }
    }

    private fun openGyms() { state = state.copy(screen = Screen.Gyms) }

    private fun openCapture() {
        render(state.copy(screen = Screen.Capture, capturedFile = null, message = null))
    }

    private fun recordSuccessWithoutVideo(color: String) {
        val session = state.activeSession ?: return
        background {
            val result = AttemptService(AndroidMediaStoreGateway(this)).recordSuccessWithoutVideo(
                session = session,
                color = color,
                recordedAtEpochMillis = System.currentTimeMillis(),
                attemptId = newId(),
            )
            repository.saveAttempt(result.attempt).fold(
                onSuccess = {
                    render(
                        state.copy(
                            screen = Screen.Board,
                            attempts = repository.attempts(session.id),
                            message = null,
                        ),
                    )
                },
                onFailure = ::showError,
            )
        }
    }

    private fun openStaticScreen(screen: Screen) {
        render(state.copy(screen = screen, message = null))
    }

    private fun attachCameraPreview(view: PreviewView) {
        if (!hasPermission(Manifest.permission.CAMERA) || !hasPermission(Manifest.permission.RECORD_AUDIO)) return
        recorder.bindPreview(
            view = view,
            onReady = { render(state.copy(cameraReady = true, permissionsGranted = true, settingsRequired = false)) },
            onError = { message -> render(state.copy(message = message)) },
        )
    }

    private fun openArchive() = background {
        render(
            state.copy(
                screen = Screen.Archive,
                archive = repository.archiveAttempts(),
                capturedFile = null,
                classificationAttempt = null,
                classificationInProgress = false,
            ),
        )
    }

    private fun openClassification(attempt: Attempt) {
        val cachePath = attempt.cachePath ?: return showError(IllegalStateException("분류할 영상을 찾을 수 없습니다"))
        val file = File(cachePath)
        if (!file.isFile) return showError(IllegalStateException("분류할 영상을 찾을 수 없습니다"))
        render(
            state.copy(
                screen = Screen.Capture,
                capturedFile = file,
                classificationAttempt = attempt,
                classificationInProgress = false,
                cameraReady = true,
                message = "성공 또는 실패를 선택하세요",
            ),
        )
    }

    private fun openTrim(attempt: Attempt) {
        render(
            state.copy(
                screen = Screen.Trim,
                selectedAttempt = attempt,
                selectedVideoDurationMillis = videoDurationMillis(attempt.originalVideoUri) ?: 19_000L,
                mediaChoiceAttempt = null,
                message = null,
            ),
        )
    }

    private fun keepOriginal(attempt: Attempt) = background {
        repository.saveAttempt(AttemptMediaService().keepOriginal(attempt)).fold({ loadInitialState() }, ::showError)
    }

    private fun deferTrim(attempt: Attempt) {
        if (!shouldDeferTrim(state.mediaChoiceAttempt, attempt)) return
        loadInitialState("${holdLabel(attempt.color)} 영상은 아카이브에서 나중에 자를 수 있습니다")
    }

    private fun backToBoard() = loadInitialState()

    private fun cancelTrim(attempt: Attempt) {
        trimExports.cancel(attempt.id)
        background {
            repository.saveAttempt(AttemptMediaService().cancelTrim(attempt)).fold({ loadInitialState() }, ::showError)
        }
    }

    private fun closePlayback() {
        render(state.copy(playingVideoUri = null))
    }

    private fun playAttempt(attempt: Attempt) {
        val uri = attempt.displayVideoUri
        if (uri == null || !isReadableVideoUri(this, uri)) {
            render(state.copy(unavailableVideoAttemptId = attempt.id, message = "기기에서 영상을 읽을 수 없습니다"))
        } else {
            render(state.copy(playingVideoUri = uri, selectedAttempt = attempt, unavailableVideoAttemptId = null))
        }
    }

    private fun submitTrim(startMillis: Long, endMillis: Long) {
        val attempt = state.selectedAttempt ?: return
        val submittedState = state.beginTrimSubmission(startMillis, endMillis) ?: return
        val sourceUri = attempt.originalVideoUri
        val duration = videoDurationMillis(sourceUri)
        if (duration == null) {
            failTrim(attempt, "트리밍 원본 영상을 읽을 수 없습니다")
            return
        }
        val request = TrimRequest(
            sourcePath = sourceUri,
            outputPath = File(cacheDir, "trim-${attempt.id}-${System.currentTimeMillis()}.mp4").absolutePath,
            startMillis = startMillis,
            endMillis = endMillis,
            durationMillis = duration,
        )
        if (!isValidTrimRange(startMillis, endMillis, duration)) {
            failTrim(attempt, "트리밍 구간을 확인하세요")
            return
        }
        render(submittedState)
        background {
            when (val started = AttemptMediaService().startTrim(attempt)) {
                is AttemptMediaResult.Updated -> repository.saveAttempt(started.attempt).fold(
                    onSuccess = {
                        render(state.copy(trimInProgress = true, selectedAttempt = started.attempt, message = "영상을 자르는 중입니다"))
                        runOnUiThread {
                            val accepted = trimExports.start(
                                attemptId = started.attempt.id,
                                request = request,
                                onCompleted = { path -> promoteTrim(started.attempt, path) },
                                onError = { message -> failTrim(started.attempt, message) },
                            )
                            if (!accepted) {
                                failTrim(started.attempt, "이미 영상을 자르고 있습니다")
                            }
                        }
                    },
                    onFailure = { error ->
                        render(state.copy(trimInProgress = false, message = error.message ?: "트리밍을 시작하지 못했습니다"))
                    },
                )
                is AttemptMediaResult.Rejected -> render(state.copy(trimInProgress = false, message = started.message))
            }
        }
    }

    private fun promoteTrim(attempt: Attempt, cachePath: String) = background {
        AndroidMediaStoreGateway(this).save(cachePath).fold(
            onSuccess = { uri ->
                File(cachePath).delete()
                val updated = AttemptMediaService().completeTrim(attempt, uri)
                if (updated is AttemptMediaResult.Updated) {
                    repository.saveAttempt(updated.attempt).fold(
                        onSuccess = {
                            val attempts = state.activeSession
                                ?.let { session -> repository.attempts(session.id) }
                                ?: state.attempts
                            render(state.afterTrimCompleted(updated.attempt, attempts))
                        },
                        onFailure = {
                            contentResolver.delete(Uri.parse(uri), null, null)
                            failTrim(attempt, "트리밍 결과를 저장하지 못했습니다")
                        },
                    )
                } else {
                    contentResolver.delete(Uri.parse(uri), null, null)
                    failTrim(attempt, "트리밍 결과를 저장하지 못했습니다")
                }
            },
            onFailure = { error ->
                File(cachePath).delete()
                failTrim(attempt, error.message ?: "트리밍 결과를 저장하지 못했습니다")
            },
        )
    }

    private fun failTrim(attempt: Attempt, message: String) = background {
        repository.saveAttempt(AttemptMediaService().failTrim(attempt, message)).fold(
            onSuccess = { render(state.copy(screen = Screen.Trim, trimInProgress = false, selectedAttempt = AttemptMediaService().failTrim(attempt, message), message = message)) },
            onFailure = ::showError,
        )
    }

    private fun videoDurationMillis(uri: String): Long? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, Uri.parse(uri))
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
        } finally {
            retriever.release()
        }
    }.getOrNull()

    private fun startSession(gym: Gym) = background {
        val result = SessionNavigator().startSession(gym, repository.activeSession(), ::newId, System.currentTimeMillis())
        val session = when (result) {
            is com.weclimb.session.SessionStartResult.Started -> result.session.also { repository.saveSession(it).getOrThrow() }
            is com.weclimb.session.SessionStartResult.AlreadyActive -> result.session
        }
        render(state.copy(screen = Screen.Board, activeSession = session))
    }

    private fun addGym(name: String) = background {
        when (val result = GymCatalog().addUserGym(name, ::newId)) {
            is com.weclimb.session.GymAddResult.Added -> repository.saveGym(result.gym).fold({ refreshGymPicker() }, ::showError)
            com.weclimb.session.GymAddResult.InvalidName -> render(state.copy(message = "암장 이름을 입력하세요"))
        }
    }

    private fun renameGym(gym: Gym, name: String) = background {
        if (gym.source != GymSource.USER_ADDED || name.isBlank()) {
            render(state.copy(message = "개인 암장 이름을 입력하세요"))
            return@background
        }
        repository.saveGym(GymCatalog().rename(gym, name)).fold({ refreshGymPicker() }, ::showError)
    }

    private fun hideGym(gym: Gym) = background {
        if (gym.source != GymSource.USER_ADDED) {
            render(state.copy(message = "시드 암장은 숨길 수 없습니다"))
            return@background
        }
        repository.saveGym(GymCatalog().hide(gym)).fold({ refreshGymPicker() }, ::showError)
    }

    private fun refreshGymPicker() = background {
        runCatching { repository.gyms() }
            .fold(
                onSuccess = { gyms -> render(state.copy(screen = Screen.Gyms, gyms = gyms, message = null)) },
                onFailure = ::showError,
            )
    }

    private fun endSession() = background {
        val session = repository.activeSession() ?: return@background
        val finished = SessionFinisher(AndroidCacheGateway()).finish(
            session,
            repository.attempts(session.id),
            System.currentTimeMillis(),
        ).getOrThrow()
        val removedIds = repository.attempts(session.id)
            .filter { it.outcome == AttemptOutcome.FAILURE }
            .map(Attempt::id)
        repository.deleteAttempts(removedIds).getOrThrow()
        repository.saveSession(finished.session).fold({ loadInitialState() }, ::showError)
    }

    private fun requestEndSession() {
        render(state.copy(confirmEnd = true))
    }

    private fun toggleRecording() {
        if (recorder.isRecording) {
            recorder.stop()
            render(state.copy(recording = false, message = "녹화를 정리하고 있습니다"))
            return
        }
        if (!state.cameraReady) {
            onPermissionsUpdated()
            return
        }
        val output = File(cacheDir, "attempt-${System.currentTimeMillis()}.mp4")
        val started = recorder.start(
            output = output,
            onFinalized = { file -> render(state.copy(screen = Screen.Capture, recording = false, capturedFile = file, message = "성공 또는 실패를 선택하세요")) },
            onError = { message -> render(state.copy(recording = false, message = message)) },
        )
        if (started) {
            render(state.copy(recording = true, capturedFile = null, message = "녹화 중"))
        }
    }

    private fun captureSystemBack(color: String) {
        if (state.classificationInProgress) return
        if (state.recording) {
            toggleRecording()
            return
        }
        if (state.classificationAttempt != null) {
            openArchive()
            return
        }
        val session = state.activeSession ?: return backToBoard()
        val file = state.capturedFile ?: return backToBoard()
        val attempt = AttemptService(AndroidMediaStoreGateway(this)).recordUnclassified(
            session = session,
            color = color,
            cachePath = file.absolutePath,
            recordedAtEpochMillis = System.currentTimeMillis(),
            attemptId = newId(),
        )
        render(state.copy(classificationInProgress = true))
        background {
            repository.saveAttempt(attempt).fold(
                onSuccess = { loadInitialState("미분류 시도를 보관했어요") },
                onFailure = ::showError,
            )
        }
    }

    private fun classifySuccess(color: String) {
        val submitted = state.beginClassification() ?: return
        val file = state.capturedFile ?: return
        val existing = state.classificationAttempt
        val session = state.activeSession
        if (existing == null && session == null) return
        render(submitted)
        background {
            val service = AttemptService(AndroidMediaStoreGateway(this))
            val result = if (existing == null) {
                service.recordSuccess(
                    requireNotNull(session),
                    color,
                    file.absolutePath,
                    System.currentTimeMillis(),
                )
            } else {
                service.classifyUnclassifiedSuccess(existing, color)
            }
            val presentation = attemptSavePresentation(result)
            repository.saveAttempt(result.attempt).fold(
                onSuccess = {
                    if (existing == null) {
                        loadInitialState(
                            message = presentation.message,
                            mediaChoiceAttemptId = presentation.mediaChoiceAttemptId,
                            statusIsError = presentation.isError,
                            statusRetryAttemptId = presentation.retryAttemptId,
                        )
                    } else if (result.attempt.outcome == AttemptOutcome.UNCLASSIFIED) {
                        render(
                            state.copy(
                                classificationAttempt = result.attempt,
                                classificationInProgress = false,
                                message = result.saveErrorMessage,
                                statusIsError = true,
                            ),
                        )
                    } else {
                        openArchive()
                    }
                },
                onFailure = ::showError,
            )
        }
    }

    private fun classifyFailure(color: String) {
        val submitted = state.beginClassification() ?: return
        val file = state.capturedFile ?: return
        val existing = state.classificationAttempt
        val session = state.activeSession
        if (existing == null && session == null) return
        render(submitted)
        background {
            val attempt = if (existing == null) {
                Attempt(
                    id = newId(),
                    sessionId = requireNotNull(session).id,
                    color = color,
                    recordedAtEpochMillis = System.currentTimeMillis(),
                    outcome = AttemptOutcome.FAILURE,
                    videoUri = null,
                    cachePath = file.absolutePath,
                )
            } else {
                AttemptService(AndroidMediaStoreGateway(this))
                    .classifyUnclassifiedFailure(existing, color, AndroidCacheGateway())
                    .getOrThrow()
            }
            repository.saveAttempt(attempt).fold(
                onSuccess = { if (existing == null) loadInitialState() else openArchive() },
                onFailure = ::showError,
            )
        }
    }

    private fun shareAttempt(attempt: Attempt) = background {
        when (val result = AttemptShareService().create(attempt)) {
            is AttemptShareResult.Ready -> {
                if (isReadableVideoUri(this, result.request.streamUri)) {
                    runOnUiThread { AndroidShareLauncher(this).launch(result.request) }
                } else {
                    render(state.copy(message = "기기에서 공유할 영상을 읽을 수 없습니다"))
                }
            }
            AttemptShareResult.VideoUnavailable -> render(state.copy(message = "공유할 영상을 찾을 수 없습니다"))
        }
    }

    private fun retryPendingAttempt(attempt: Attempt) = background {
        val retried = AttemptService(AndroidMediaStoreGateway(this)).retrySave(attempt)
        val presentation = attemptSavePresentation(
            SuccessAttemptResult(
                attempt = retried,
                successCount = if (retried.outcome == AttemptOutcome.SUCCESS) 1 else 0,
                saveErrorMessage = if (retried.outcome == AttemptOutcome.SAVE_PENDING) {
                    "영상을 저장하지 못했습니다"
                } else {
                    "영상을 저장했어요"
                },
            ),
        )
        repository.saveAttempt(retried).fold(
            onSuccess = {
                loadInitialState(
                    message = presentation.message,
                    mediaChoiceAttemptId = presentation.mediaChoiceAttemptId,
                    statusIsError = presentation.isError,
                    statusRetryAttemptId = presentation.retryAttemptId,
                )
            },
            onFailure = ::showError,
        )
    }

    private fun discardPendingAttempt(attempt: Attempt) = background {
        AttemptService(AndroidMediaStoreGateway(this))
            .discardPendingVideo(attempt, AndroidCacheGateway())
            .fold(
                onSuccess = { discarded ->
                    repository.saveAttempt(discarded).fold(
                        onSuccess = { loadInitialState("영상은 폐기하고 성공 기록은 유지했어요") },
                        onFailure = ::showError,
                    )
                },
                onFailure = ::showError,
            )
    }

    private fun retryStatusAction() {
        state.retryableSaveAttempt()?.let {
            retryPendingAttempt(it)
            return
        }
        when (state.screen) {
            Screen.Trim -> {
                val start = state.lastTrimStartMillis
                val end = state.lastTrimEndMillis
                if (start != null && end != null) submitTrim(start, end) else render(state.copy(message = null))
            }
            Screen.Capture -> onPermissionsUpdated()
            Screen.Archive -> openArchive()
            else -> loadInitialState()
        }
    }

    private fun background(block: () -> Unit) { executor.execute { runCatching(block).onFailure(::showError) } }
    private fun render(value: AppState) { runOnUiThread { state = value } }
    private fun showError(error: Throwable) {
        render(
            state.copy(
                message = error.message ?: "처리하지 못했습니다",
                statusIsError = true,
                statusRetryAttemptId = null,
                classificationInProgress = false,
            ),
        )
    }
    private fun hasPermission(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    private fun newId() = UUID.randomUUID().toString()
}

private fun AppDestination?.toScreen() = when (this) { AppDestination.Home -> Screen.Home; is AppDestination.SessionBoard -> Screen.Board; null -> Screen.Onboarding }

@Composable
private fun SessionLoopApp(
    state: AppState,
    requestPermissions: () -> Unit,
    openAppSettings: () -> Unit,
    completeOnboarding: () -> Unit,
    openGyms: () -> Unit,
    openCapture: () -> Unit,
    recordSuccessWithoutVideo: (String) -> Unit,
    startSession: (Gym) -> Unit,
    addGym: (String) -> Unit,
    renameGym: (Gym, String) -> Unit,
    hideGym: (Gym) -> Unit,
    toggleRecording: () -> Unit,
    classifySuccess: (String) -> Unit,
    classifyFailure: (String) -> Unit,
    captureSystemBack: (String) -> Unit,
    openTrim: (Attempt) -> Unit,
    deferTrim: (Attempt) -> Unit,
    keepOriginal: (Attempt) -> Unit,
    openArchive: () -> Unit,
    openClassification: (Attempt) -> Unit,
    playAttempt: (Attempt) -> Unit,
    closePlayback: () -> Unit,
    submitTrim: (Long, Long) -> Unit,
    cancelTrim: (Attempt) -> Unit,
    backToBoard: () -> Unit,
    shareAttempt: (Attempt) -> Unit,
    retryPendingAttempt: (Attempt) -> Unit,
    discardPendingAttempt: (Attempt) -> Unit,
    retryStatusAction: () -> Unit,
    requestEndSession: () -> Unit,
    endSession: () -> Unit,
    openStaticScreen: (Screen) -> Unit,
    attachCameraPreview: (PreviewView) -> Unit,
) {
    RebuiltSessionLoopApp(
        state, requestPermissions, openAppSettings, completeOnboarding, openGyms, openCapture,
        recordSuccessWithoutVideo, startSession, addGym, renameGym, hideGym, toggleRecording, classifySuccess,
        classifyFailure, captureSystemBack, openTrim, deferTrim, keepOriginal, openArchive, openClassification, playAttempt,
        closePlayback, submitTrim, cancelTrim, backToBoard, shareAttempt, retryPendingAttempt, discardPendingAttempt,
        retryStatusAction, requestEndSession, endSession, openStaticScreen, attachCameraPreview,
    )
}
