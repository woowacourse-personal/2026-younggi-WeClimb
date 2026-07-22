package com.weclimb.android

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var repository: RoomSessionLoopRepository
    private lateinit var recorder: CameraRecordingController
    private var state by mutableStateOf(AppState())
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { onPermissionsUpdated() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = RoomSessionLoopRepository(SessionLoopDatabase.create(this).sessionLoopDao())
        recorder = CameraRecordingController(this, this)
        setContent {
            MaterialTheme {
                SessionLoopApp(
                    state,
                    ::requestPermissions,
                    ::openAppSettings,
                    ::completeOnboarding,
                    ::openGyms,
                    ::startSession,
                    ::addGym,
                    ::renameGym,
                    ::hideGym,
                    ::toggleRecording,
                    ::classifySuccess,
                    ::classifyFailure,
                    ::retryPendingAttempt,
                    ::requestEndSession,
                    ::endSession,
                )
            }
        }
        loadInitialState()
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private fun loadInitialState() = background {
        runCatching {
            repository.importSeedGyms(loadSeedGyms(this))
            val destination = if (repository.hasGuestProfile()) SessionNavigator().initialDestination(repository.activeSession()) else null
            val activeSession = repository.activeSession()
            AppState(
                screen = destination.toScreen(),
                gyms = repository.gyms(),
                activeSession = activeSession,
                attempts = activeSession?.let { session -> repository.attempts(session.id) }.orEmpty(),
                cameraReady = state.cameraReady,
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
            is com.weclimb.session.GymAddResult.Added -> repository.saveGym(result.gym).fold({ loadInitialState() }, ::showError)
            com.weclimb.session.GymAddResult.InvalidName -> render(state.copy(message = "암장 이름을 입력하세요"))
        }
    }

    private fun renameGym(gym: Gym, name: String) = background {
        if (gym.source != GymSource.USER_ADDED || name.isBlank()) {
            render(state.copy(message = "개인 암장 이름을 입력하세요"))
            return@background
        }
        repository.saveGym(GymCatalog().rename(gym, name)).fold({ loadInitialState() }, ::showError)
    }

    private fun hideGym(gym: Gym) = background {
        if (gym.source != GymSource.USER_ADDED) {
            render(state.copy(message = "시드 암장은 숨길 수 없습니다"))
            return@background
        }
        repository.saveGym(GymCatalog().hide(gym)).fold({ loadInitialState() }, ::showError)
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
        recorder.start(
            output = output,
            onFinalized = { file -> render(state.copy(recording = false, capturedFile = file, message = "성공 또는 실패를 선택하세요")) },
            onError = { message -> render(state.copy(recording = false, message = message)) },
        )
        render(state.copy(recording = true, capturedFile = null, message = "녹화 중"))
    }

    private fun classifySuccess(color: String) {
        val session = state.activeSession ?: return
        val file = state.capturedFile ?: return
        background {
            val result = AttemptService(AndroidMediaStoreGateway(this)).recordSuccess(
                session,
                color,
                file.absolutePath,
                System.currentTimeMillis(),
            )
            repository.saveAttempt(result.attempt).fold(
                onSuccess = { loadInitialState() },
                onFailure = ::showError,
            )
        }
    }

    private fun classifyFailure(color: String) {
        val session = state.activeSession ?: return
        val file = state.capturedFile ?: return
        background {
            val recordedAt = System.currentTimeMillis()
            val attempt = Attempt(
                id = "${session.id}-$recordedAt",
                sessionId = session.id,
                color = color,
                recordedAtEpochMillis = recordedAt,
                outcome = AttemptOutcome.FAILURE,
                videoUri = null,
                cachePath = file.absolutePath,
            )
            repository.saveAttempt(attempt).fold(
                onSuccess = { loadInitialState() },
                onFailure = ::showError,
            )
        }
    }

    private fun retryPendingAttempt(attempt: Attempt) = background {
        val retried = AttemptService(AndroidMediaStoreGateway(this)).retrySave(attempt)
        repository.saveAttempt(retried).fold({ loadInitialState() }, ::showError)
    }

    private fun background(block: () -> Unit) { executor.execute { runCatching(block).onFailure(::showError) } }
    private fun render(value: AppState) { runOnUiThread { state = value } }
    private fun showError(error: Throwable) { render(state.copy(message = error.message ?: "처리하지 못했습니다")) }
    private fun hasPermission(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    private fun newId() = UUID.randomUUID().toString()
}

private data class AppState(
    val screen: Screen = Screen.Loading,
    val gyms: List<Gym> = emptyList(),
    val activeSession: Session? = null,
    val attempts: List<Attempt> = emptyList(),
    val capturedFile: File? = null,
    val cameraReady: Boolean = false,
    val recording: Boolean = false,
    val confirmEnd: Boolean = false,
    val permissionsGranted: Boolean = false,
    val permissionRequested: Boolean = false,
    val settingsRequired: Boolean = false,
    val message: String? = null,
)
private enum class Screen { Loading, Onboarding, Home, Gyms, Board }
private fun AppDestination?.toScreen() = when (this) { AppDestination.Home -> Screen.Home; is AppDestination.SessionBoard -> Screen.Board; null -> Screen.Onboarding }

@Composable
private fun SessionLoopApp(
    state: AppState,
    requestPermissions: () -> Unit,
    openAppSettings: () -> Unit,
    completeOnboarding: () -> Unit,
    openGyms: () -> Unit,
    startSession: (Gym) -> Unit,
    addGym: (String) -> Unit,
    renameGym: (Gym, String) -> Unit,
    hideGym: (Gym) -> Unit,
    toggleRecording: () -> Unit,
    classifySuccess: (String) -> Unit,
    classifyFailure: (String) -> Unit,
    retryPendingAttempt: (Attempt) -> Unit,
    requestEndSession: () -> Unit,
    endSession: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.message?.let { message -> Text(message) }
        when (state.screen) {
            Screen.Loading -> Text("준비 중")
            Screen.Onboarding -> {
                Text("We-Climb 시작하기")
                Button(if (state.settingsRequired) openAppSettings else requestPermissions) { Text(if (state.settingsRequired) "설정 열기" else "권한 요청") }
                Button(completeOnboarding, enabled = state.permissionsGranted) { Text("다음") }
            }
            Screen.Home -> { Text("오늘 어디서 클라이밍할까요?"); Button(openGyms) { Text("암장 선택") } }
            Screen.Gyms -> GymPicker(state.gyms, name, { name = it }, startSession, addGym, renameGym, hideGym)
            Screen.Board -> SessionBoard(state, toggleRecording, classifySuccess, classifyFailure, retryPendingAttempt, requestEndSession, endSession)
        }
    }
}

@Composable
private fun GymPicker(
    gyms: List<Gym>,
    name: String,
    updateName: (String) -> Unit,
    startSession: (Gym) -> Unit,
    addGym: (String) -> Unit,
    renameGym: (Gym, String) -> Unit,
    hideGym: (Gym) -> Unit,
) {
    val results = GymCatalog().search(gyms, name)
    OutlinedTextField(name, updateName, label = { Text("암장 이름 또는 검색어") }, modifier = Modifier.fillMaxWidth())
    if (name.isNotBlank() && results.isEmpty()) {
        Button({ addGym(name) }) { Text("개인 암장 추가") }
    }
    LazyColumn {
        items(results) { gym ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button({ startSession(gym) }, Modifier.fillMaxWidth()) { Text(gym.name) }
                if (gym.source == GymSource.USER_ADDED) {
                    Button({ renameGym(gym, name) }) { Text("입력한 이름으로 수정") }
                    Button({ hideGym(gym) }) { Text("목록에서 숨기기") }
                }
            }
        }
    }
}

@Composable
private fun SessionBoard(
    state: AppState,
    toggleRecording: () -> Unit,
    classifySuccess: (String) -> Unit,
    classifyFailure: (String) -> Unit,
    retryPendingAttempt: (Attempt) -> Unit,
    requestEndSession: () -> Unit,
    endSession: () -> Unit,
) {
    var color by remember { mutableStateOf("blue") }
    Text("세션 진행 중")
    Text("완등 ${state.attempts.count { it.outcome == AttemptOutcome.SUCCESS }}개")
    state.attempts.filter { it.outcome == AttemptOutcome.SUCCESS }
        .groupingBy(Attempt::color)
        .eachCount()
        .toSortedMap()
        .forEach { (color, count) -> Text("$color ${count}개") }
    Button(toggleRecording) { Text(if (state.recording) "녹화 중지" else "촬영 시작") }
    if (state.capturedFile != null) {
        OutlinedTextField(color, { color = it }, label = { Text("홀드 색상") })
        Button({ classifySuccess(color) }) { Text("성공") }
        Button({ classifyFailure(color) }) { Text("실패") }
    }
    state.attempts.filter { it.outcome == AttemptOutcome.SAVE_PENDING }
        .forEach { attempt -> Button({ retryPendingAttempt(attempt) }) { Text("${attempt.color} 저장 재시도") } }
    if (state.confirmEnd) {
        Text("실패 영상을 삭제하고 운동을 종료할까요?")
        Button(endSession) { Text("운동 종료 확정") }
    } else {
        Button(requestEndSession) { Text("운동 종료") }
    }
}
