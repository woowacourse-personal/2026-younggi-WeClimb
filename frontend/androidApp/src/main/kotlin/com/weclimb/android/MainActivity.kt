package com.weclimb.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
                    ::completeOnboarding,
                    ::openGyms,
                    ::startSession,
                    ::addGym,
                    ::toggleRecording,
                    ::classifySuccess,
                    ::classifyFailure,
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
                attempts = activeSession?.let(repository::attempts).orEmpty(),
                cameraReady = state.cameraReady,
            )
        }.fold(::render, ::showError)
    }

    private fun requestPermissions() {
        permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    private fun onPermissionsUpdated() {
        if (hasPermission(Manifest.permission.CAMERA) && hasPermission(Manifest.permission.RECORD_AUDIO)) {
            recorder.bind(
                onReady = { render(state.copy(cameraReady = true)) },
                onError = { message -> render(state.copy(message = message)) },
            )
        } else {
            render(state.copy(message = "카메라와 마이크 권한이 필요합니다"))
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

    private fun endSession() = background {
        val session = repository.activeSession() ?: return@background
        val finished = SessionFinisher(AndroidCacheGateway()).finish(
            session,
            repository.attempts(session.id),
            System.currentTimeMillis(),
        )
        repository.saveSession(finished.session).fold({ loadInitialState() }, ::showError)
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
    val message: String? = null,
)
private enum class Screen { Loading, Onboarding, Home, Gyms, Board }
private fun AppDestination?.toScreen() = when (this) { AppDestination.Home -> Screen.Home; is AppDestination.SessionBoard -> Screen.Board; null -> Screen.Onboarding }

@Composable
private fun SessionLoopApp(
    state: AppState,
    requestPermissions: () -> Unit,
    completeOnboarding: () -> Unit,
    openGyms: () -> Unit,
    startSession: (Gym) -> Unit,
    addGym: (String) -> Unit,
    toggleRecording: () -> Unit,
    classifySuccess: (String) -> Unit,
    classifyFailure: (String) -> Unit,
    endSession: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.message?.let { message -> Text(message) }
        when (state.screen) {
            Screen.Loading -> Text("준비 중")
            Screen.Onboarding -> { Text("We-Climb 시작하기"); Button(requestPermissions) { Text("권한 요청") }; Button(completeOnboarding) { Text("다음") } }
            Screen.Home -> { Text("오늘 어디서 클라이밍할까요?"); Button(openGyms) { Text("암장 선택") } }
            Screen.Gyms -> { OutlinedTextField(name, { name = it }, label = { Text("개인 암장 이름") }, modifier = Modifier.fillMaxWidth()); Button({ addGym(name) }) { Text("개인 암장 추가") }; LazyColumn { items(GymCatalog().search(state.gyms, name)) { gym -> Button({ startSession(gym) }, Modifier.fillMaxWidth()) { Text(gym.name) } } } }
            Screen.Board -> SessionBoard(state, toggleRecording, classifySuccess, classifyFailure, endSession)
        }
    }
}

@Composable
private fun SessionBoard(
    state: AppState,
    toggleRecording: () -> Unit,
    classifySuccess: (String) -> Unit,
    classifyFailure: (String) -> Unit,
    endSession: () -> Unit,
) {
    Text("세션 진행 중")
    Text("완등 ${state.attempts.count { it.outcome == AttemptOutcome.SUCCESS }}개")
    state.attempts.filter { it.outcome == AttemptOutcome.SUCCESS }
        .groupingBy(Attempt::color)
        .eachCount()
        .toSortedMap()
        .forEach { (color, count) -> Text("$color $count개") }
    Button(toggleRecording) { Text(if (state.recording) "녹화 중지" else "촬영 시작") }
    if (state.capturedFile != null) {
        Button({ classifySuccess("blue") }) { Text("파랑 성공") }
        Button({ classifyFailure("blue") }) { Text("파랑 실패") }
    }
    Button(endSession) { Text("운동 종료") }
}
