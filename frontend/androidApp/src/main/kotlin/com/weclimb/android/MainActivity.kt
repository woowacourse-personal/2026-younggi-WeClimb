package com.weclimb.android

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import java.text.DateFormat
import java.util.Date
import java.io.File
import android.media.MediaMetadataRetriever
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.weclimb.session.displayVideoUri
import com.weclimb.session.originalVideoUri
import com.weclimb.media.AttemptShareResult
import com.weclimb.media.AttemptShareService
import com.weclimb.media.AttemptMediaResult
import com.weclimb.media.AttemptMediaService
import com.weclimb.media.AttemptMediaState
import com.weclimb.media.TrimRequest
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
            WeClimbTheme {
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
                    ::openTrim,
                    ::deferTrim,
                    ::keepOriginal,
                    ::openArchive,
                    ::playAttempt,
                    ::closePlayback,
                    ::submitTrim,
                    ::cancelTrim,
                    ::backToBoard,
                    ::shareAttempt,
                    ::retryPendingAttempt,
                    ::requestEndSession,
                    ::endSession,
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

    private fun loadInitialState(message: String? = null) = background {
        runCatching {
            repository.importSeedGyms(loadSeedGyms(this))
            repository.recoverInterruptedTrims().getOrThrow()
            val destination = if (repository.hasGuestProfile()) SessionNavigator().initialDestination(repository.activeSession()) else null
            val activeSession = repository.activeSession()
            AppState(
                screen = destination.toScreen(),
                gyms = repository.gyms(),
                activeSession = activeSession,
                attempts = activeSession?.let { session -> repository.attempts(session.id) }.orEmpty(),
                cameraReady = state.cameraReady,
                message = message,
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

    private fun openArchive() = background {
        render(state.copy(screen = Screen.Archive, archive = repository.archiveAttempts()))
    }

    private fun openTrim(attempt: Attempt) {
        render(state.copy(screen = Screen.Trim, selectedAttempt = attempt, message = null))
    }

    private fun keepOriginal(attempt: Attempt) = background {
        repository.saveAttempt(AttemptMediaService().keepOriginal(attempt)).fold({ loadInitialState() }, ::showError)
    }

    private fun deferTrim(attempt: Attempt) {
        render(state.copy(message = "${attempt.color} 영상은 아카이브에서 나중에 자를 수 있습니다"))
    }

    private fun backToBoard() = loadInitialState()

    private fun cancelTrim(attempt: Attempt) = background {
        repository.saveAttempt(AttemptMediaService().cancelTrim(attempt)).fold({ loadInitialState() }, ::showError)
    }

    private fun closePlayback() {
        render(state.copy(playingVideoUri = null))
    }

    private fun playAttempt(attempt: Attempt) {
        val uri = attempt.displayVideoUri
        if (uri == null || !isReadableVideoUri(this, uri)) {
            render(state.copy(unavailableVideoAttemptId = attempt.id, message = "기기에서 영상을 읽을 수 없습니다"))
        } else {
            render(state.copy(playingVideoUri = uri, unavailableVideoAttemptId = null))
        }
    }

    private fun submitTrim(startMillis: Long, endMillis: Long) {
        val attempt = state.selectedAttempt ?: return
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
        if (endMillis <= startMillis || startMillis < 0L || endMillis > duration) {
            failTrim(attempt, "트리밍 구간을 확인하세요")
            return
        }
        background {
            when (val started = AttemptMediaService().startTrim(attempt)) {
                is AttemptMediaResult.Updated -> repository.saveAttempt(started.attempt).fold(
                    onSuccess = {
                        render(state.copy(trimInProgress = true, selectedAttempt = started.attempt, message = "영상을 자르는 중입니다"))
                        runOnUiThread {
                            Media3EditListExporter(this).export(
                                request = request,
                                onCompleted = { path -> promoteTrim(started.attempt, path) },
                                onError = { message -> failTrim(started.attempt, message) },
                            )
                        }
                    },
                    onFailure = ::showError,
                )
                is AttemptMediaResult.Rejected -> render(state.copy(message = started.message))
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
                        onSuccess = { loadInitialState("트리밍 영상을 저장했습니다") },
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
        val started = recorder.start(
            output = output,
            onFinalized = { file -> render(state.copy(recording = false, capturedFile = file, message = "성공 또는 실패를 선택하세요")) },
            onError = { message -> render(state.copy(recording = false, message = message)) },
        )
        if (started) {
            render(state.copy(recording = true, capturedFile = null, message = "녹화 중"))
        }
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
                onSuccess = { loadInitialState(result.saveErrorMessage) },
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
    val archive: List<ArchiveAttempt> = emptyList(),
    val selectedAttempt: Attempt? = null,
    val playingVideoUri: String? = null,
    val unavailableVideoAttemptId: String? = null,
    val trimInProgress: Boolean = false,
    val capturedFile: File? = null,
    val cameraReady: Boolean = false,
    val recording: Boolean = false,
    val confirmEnd: Boolean = false,
    val permissionsGranted: Boolean = false,
    val permissionRequested: Boolean = false,
    val settingsRequired: Boolean = false,
    val message: String? = null,
)
private enum class Screen { Loading, Onboarding, Home, Gyms, Board, Trim, Archive }
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
    openTrim: (Attempt) -> Unit,
    deferTrim: (Attempt) -> Unit,
    keepOriginal: (Attempt) -> Unit,
    openArchive: () -> Unit,
    playAttempt: (Attempt) -> Unit,
    closePlayback: () -> Unit,
    submitTrim: (Long, Long) -> Unit,
    cancelTrim: (Attempt) -> Unit,
    backToBoard: () -> Unit,
    shareAttempt: (Attempt) -> Unit,
    retryPendingAttempt: (Attempt) -> Unit,
    requestEndSession: () -> Unit,
    endSession: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val contentModifier = if (state.screen == Screen.Gyms || state.screen == Screen.Archive) {
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)
    } else {
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp).verticalScroll(rememberScrollState())
    }
    Surface(Modifier.fillMaxSize()) {
        Column(contentModifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.message?.let { message -> StatusMessage(message) }
            state.playingVideoUri?.let { uri ->
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AndroidVideoPlayer(uri, Modifier.fillMaxWidth())
                        OutlinedButton(closePlayback, Modifier.fillMaxWidth()) { Text("재생 닫기") }
                    }
                }
            }
            when (state.screen) {
            Screen.Loading -> LoadingScreen()
            Screen.Onboarding -> {
                OnboardingScreen(
                    state.settingsRequired,
                    state.permissionsGranted,
                    if (state.settingsRequired) openAppSettings else requestPermissions,
                    completeOnboarding,
                )
            }
            Screen.Home -> HomeScreen(openGyms, openArchive)
            Screen.Gyms -> GymPicker(state.gyms, name, { name = it }, startSession, addGym, renameGym, hideGym)
            Screen.Board -> SessionBoard(state, toggleRecording, classifySuccess, classifyFailure, openTrim, deferTrim, keepOriginal, openArchive, playAttempt, shareAttempt, retryPendingAttempt, requestEndSession, endSession)
            Screen.Trim -> TrimScreen(state.selectedAttempt, state.trimInProgress, submitTrim, keepOriginal, cancelTrim, shareAttempt, backToBoard)
            Screen.Archive -> ArchiveScreen(state.archive, state.unavailableVideoAttemptId, playAttempt, openTrim, shareAttempt, backToBoard)
            }
        }
    }
}

@Composable
private fun LoadingScreen() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text("WE-CLIMB", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun StatusMessage(message: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f)) {
        Text(message, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun OnboardingScreen(
    settingsRequired: Boolean,
    permissionsGranted: Boolean,
    requestPermissions: () -> Unit,
    completeOnboarding: () -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { index ->
                    Surface(Modifier.width(30.dp).height(5.dp), shape = RoundedCornerShape(3.dp), color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {}
                }
            }
            Text("WE-CLIMB · 시작하기", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("오늘의 완등을\n영상과 함께 남겨요", style = MaterialTheme.typography.headlineLarge)
            Text("카메라와 마이크 권한은 시도 영상을 기록할 때만 사용합니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SkillGaugePreview()
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(if (settingsRequired) "설정 열기" else "권한 요청", requestPermissions)
            OutlinedButton(completeOnboarding, Modifier.fillMaxWidth(), enabled = permissionsGranted) { Text("다음") }
        }
    }
}

@Composable
private fun SkillGaugePreview() {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("평소 완등하는 색", style = MaterialTheme.typography.titleMedium)
            Text("기록이 쌓이면 나만의 난이도를 더 정확하게 보여드려요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(WeClimbHoldColors.red, WeClimbHoldColors.orange, WeClimbHoldColors.yellow, WeClimbHoldColors.green, WeClimbHoldColors.blue).forEach { color ->
                Surface(Modifier.fillMaxWidth().height(12.dp), shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.85f)) {}
            }
        }
    }
}

@Composable
private fun HomeScreen(openGyms: () -> Unit, openArchive: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Spacer(Modifier.height(12.dp))
        Text("오늘 어디서 클라이밍할까요?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("오늘도 붙어볼까요,", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("기영 님", style = MaterialTheme.typography.displaySmall)
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(Modifier.size(72.dp), shape = RoundedCornerShape(36.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)) {
                        Box(contentAlignment = Alignment.Center) { Text("+", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary) }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("첫 기록을 남겨보세요", style = MaterialTheme.typography.titleLarge)
                        Text("암장과 완등을 쌓으면 나만의 레벨이 보여요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                SessionSnapshot()
            }
        }
        PrimaryButton("암장 선택", openGyms)
        Text("최근 클라이밍", style = MaterialTheme.typography.titleMedium)
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text("첫 세션을 시작하면 여기에 기록이 쌓입니다", Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(openArchive, Modifier.fillMaxWidth()) { Text("영상 아카이브") }
    }
}

@Composable
private fun SessionSnapshot() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SnapshotMetric("이번 주", "0회", Modifier.weight(1f))
        SnapshotMetric("완등", "0개", Modifier.weight(1f))
        SnapshotMetric("보관 영상", "0개", Modifier.weight(1f))
    }
}

@Composable
private fun SnapshotMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) { Text(label, style = MaterialTheme.typography.titleMedium) }
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
    Text("어디서 클라이밍할까요?", style = MaterialTheme.typography.headlineMedium)
    Text("기록을 시작할 암장을 선택하세요", color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedTextField(name, updateName, label = { Text("암장 이름 또는 검색어") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
    if (name.isNotBlank() && results.isEmpty()) {
        PrimaryButton("개인 암장 추가", { addGym(name) })
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(results) { gym ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(Modifier.size(42.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)) {
                            Box(contentAlignment = Alignment.Center) { Text("●", color = MaterialTheme.colorScheme.primary) }
                        }
                        Button(
                            { startSession(gym) },
                            Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        ) {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(gym.name, style = MaterialTheme.typography.titleMedium)
                                Text(if (gym.source == GymSource.USER_ADDED) "내가 추가한 암장" else "추천 암장", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (gym.source == GymSource.USER_ADDED) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton({ renameGym(gym, name) }) { Text("이름 수정") }
                            OutlinedButton({ hideGym(gym) }) { Text("숨기기") }
                        }
                    }
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
    openTrim: (Attempt) -> Unit,
    deferTrim: (Attempt) -> Unit,
    keepOriginal: (Attempt) -> Unit,
    openArchive: () -> Unit,
    playAttempt: (Attempt) -> Unit,
    shareAttempt: (Attempt) -> Unit,
    retryPendingAttempt: (Attempt) -> Unit,
    requestEndSession: () -> Unit,
    endSession: () -> Unit,
) {
    var color by remember { mutableStateOf("blue") }
    val successfulAttempts = state.attempts.filter { it.outcome == AttemptOutcome.SUCCESS }
    val completeCount = successfulAttempts.size
    val successCounts = successfulAttempts.groupingBy(Attempt::color).eachCount()
    Text(
        state.gyms.firstOrNull { it.id == state.activeSession?.gymId }?.name ?: "클라이밍 세션",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text("세션 진행 중", style = MaterialTheme.typography.headlineLarge)
    Text("클라이밍 중", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("색을 탭해서 완등", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("완등 ${completeCount}개", style = MaterialTheme.typography.titleMedium)
            }
            listOf("red", "orange", "yellow", "green", "blue").forEach { attemptColor ->
                DifficultyRow(attemptColor, successCounts[attemptColor] ?: 0)
            }
        }
    }
    if (state.recording) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(Modifier.size(10.dp), shape = RoundedCornerShape(5.dp), color = MaterialTheme.colorScheme.primary) {}
                    Text("녹화 중", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text("방금 시도를 끝까지 담고 있어요", style = MaterialTheme.typography.titleLarge)
                Text("완료되면 성공 또는 실패를 선택할 수 있습니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                PrimaryButton("녹화 중지", toggleRecording)
            }
        }
    } else {
        PrimaryButton("촬영 시작", toggleRecording)
    }
    if (state.capturedFile != null) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("방금 시도, 성공했나요?", style = MaterialTheme.typography.headlineSmall)
                Text("색을 고르고 결과를 기록하면 영상도 함께 보관됩니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(color, { color = it }, label = { Text("홀드 색상") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton({ classifyFailure(color) }, Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp)) { Text("실패") }
                    Button({ classifySuccess(color) }, Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("성공") }
                }
            }
        }
    }
    state.attempts.filter { it.outcome == AttemptOutcome.SAVE_PENDING }
        .forEach { attempt -> Button({ retryPendingAttempt(attempt) }) { Text("${attempt.color} 저장 재시도") } }
    OutlinedButton(openArchive, Modifier.fillMaxWidth()) { Text("영상 아카이브") }
    state.attempts.filter { it.outcome == AttemptOutcome.SUCCESS && it.videoUri != null }
        .forEach { attempt ->
            when (attempt.media.state) {
                AttemptMediaState.TRIM_PENDING, AttemptMediaState.TRIM_FAILED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ openTrim(attempt) }, Modifier.weight(1f).height(48.dp)) { Text("${attempt.color} 영상 자르기") }
                        OutlinedButton({ deferTrim(attempt) }, Modifier.weight(1f).height(48.dp)) { Text("${attempt.color} 나중에") }
                    }
                    OutlinedButton({ keepOriginal(attempt) }, Modifier.fillMaxWidth().height(48.dp)) { Text("${attempt.color} 원본 유지") }
                }
                else -> Unit
            }
            OutlinedButton({ playAttempt(attempt) }) { Text("${attempt.color} 영상 재생") }
            OutlinedButton({ shareAttempt(attempt) }) { Text("${attempt.color} 영상 공유") }
        }
    if (state.confirmEnd) {
        Text("실패 영상을 삭제하고 운동을 종료할까요?")
        PrimaryButton("운동 종료 확정", endSession)
    } else {
        OutlinedButton(requestEndSession, Modifier.fillMaxWidth()) { Text("운동 종료") }
    }
}

@Composable
private fun DifficultyRow(color: String, count: Int) {
    val swatch = when (color.lowercase()) {
        "blue", "파랑" -> WeClimbHoldColors.blue
        "green", "초록" -> WeClimbHoldColors.green
        "red", "빨강" -> WeClimbHoldColors.red
        "orange", "주황" -> WeClimbHoldColors.orange
        "yellow", "노랑" -> WeClimbHoldColors.yellow
        else -> MaterialTheme.colorScheme.outline
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(Modifier.size(28.dp), shape = RoundedCornerShape(8.dp), color = swatch) {}
        Surface(Modifier.weight(1f).height(18.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(Modifier.fillMaxSize().padding(end = 0.dp), contentAlignment = Alignment.CenterStart) {
                Surface(Modifier.fillMaxWidth(fraction = (count.coerceAtMost(10) / 10f)), color = swatch) {}
            }
        }
        Text(count.toString(), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun TrimScreen(
    attempt: Attempt?,
    trimInProgress: Boolean,
    submitTrim: (Long, Long) -> Unit,
    keepOriginal: (Attempt) -> Unit,
    cancelTrim: (Attempt) -> Unit,
    shareAttempt: (Attempt) -> Unit,
    backToBoard: () -> Unit,
) {
    var startMillis by remember { mutableStateOf("0") }
    var endMillis by remember { mutableStateOf("0") }
    Text("앞뒤 자르기", style = MaterialTheme.typography.headlineMedium)
    Text("영상 자르기", color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (attempt == null) {
        Text("트리밍할 영상을 찾을 수 없습니다")
        Button(backToBoard) { Text("세션으로 돌아가기") }
        return
    }
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AndroidVideoPlayer(attempt.originalVideoUri, Modifier.fillMaxWidth().height(180.dp))
            Text("원본은 그대로 남고 새 영상이 저장됩니다", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(startMillis, { startMillis = it }, label = { Text("시작 시간(ms)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(endMillis, { endMillis = it }, label = { Text("종료 시간(ms)") }, modifier = Modifier.fillMaxWidth())
        }
    }
    PrimaryButton(if (trimInProgress) "트리밍 중" else "트리밍 완료", { submitTrim(startMillis.toLongOrNull() ?: -1L, endMillis.toLongOrNull() ?: -1L) }, enabled = !trimInProgress)
    OutlinedButton({ keepOriginal(attempt) }, Modifier.fillMaxWidth(), enabled = !trimInProgress) { Text("원본 유지") }
    OutlinedButton({ shareAttempt(attempt) }, Modifier.fillMaxWidth(), enabled = !trimInProgress) { Text("영상 공유") }
    OutlinedButton({ cancelTrim(attempt) }, Modifier.fillMaxWidth(), enabled = !trimInProgress) { Text("나중에 할래요") }
}

@Composable
private fun ArchiveScreen(
    archive: List<ArchiveAttempt>,
    unavailableVideoAttemptId: String?,
    playAttempt: (Attempt) -> Unit,
    openTrim: (Attempt) -> Unit,
    shareAttempt: (Attempt) -> Unit,
    backToBoard: () -> Unit,
) {
    Text("영상 아카이브", style = MaterialTheme.typography.headlineMedium)
    Text("완등한 시도만 최신순으로 보관합니다", color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (archive.isEmpty()) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Text("아직 보관한 성공 영상이 없습니다", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(archive, key = { it.attempt.id }) { item ->
            val attempt = item.attempt
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(item.gymName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${attempt.color} 완등", style = MaterialTheme.typography.titleLarge)
                    Text(attempt.media.state.label(), color = MaterialTheme.colorScheme.secondary)
                    Text("${item.gymName} · ${attempt.color} · ${attempt.media.state.label()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatArchiveDate(attempt.recordedAtEpochMillis), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (attempt.id == unavailableVideoAttemptId) {
                        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f)) {
                            Text("재생 불가 · 기기에서 영상을 읽을 수 없습니다", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ playAttempt(attempt) }, Modifier.weight(1f)) { Text("재생") }
                        OutlinedButton({ shareAttempt(attempt) }, Modifier.weight(1f)) { Text("공유") }
                    }
                    if (attempt.media.state == AttemptMediaState.TRIM_PENDING || attempt.media.state == AttemptMediaState.TRIM_FAILED) {
                        PrimaryButton("자르기", { openTrim(attempt) })
                    }
                }
            }
        }
    }
    OutlinedButton(backToBoard, Modifier.fillMaxWidth()) { Text("세션으로 돌아가기") }
}

private fun formatArchiveDate(epochMillis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis))
