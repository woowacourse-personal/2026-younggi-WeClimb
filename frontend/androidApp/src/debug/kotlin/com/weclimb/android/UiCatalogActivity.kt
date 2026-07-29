package com.weclimb.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import com.weclimb.media.AttemptMedia
import com.weclimb.media.AttemptMediaState
import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome
import com.weclimb.session.Gym
import com.weclimb.session.GymSource
import com.weclimb.session.Session
import com.weclimb.session.SessionStatus
import java.time.LocalDateTime
import java.time.ZoneId

internal class UiCatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stateName = intent.getStringExtra(EXTRA_STATE)
        val catalogState = catalogState(stateName)
        setContent {
            val targetDensity = targetCatalogDensity(resources.displayMetrics.widthPixels)
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = targetDensity,
                    fontScale = resources.configuration.fontScale,
                ),
            ) {
                WeClimbTheme {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .semantics {
                                contentDescription = "catalog-390dp-${stateName ?: "Home"}"
                            },
                    ) {
                        if (stateName == "PermissionSettings") {
                            PermissionSettingsCardUi(openSettings = {})
                        } else {
                            RebuiltSessionLoopApp(
                                state = catalogState,
                                requestPermissions = {},
                                openAppSettings = {},
                                completeOnboarding = {},
                                openGyms = {},
                                openCapture = {},
                                recordSuccessWithoutVideo = {},
                                startSession = {},
                                addGym = {},
                                renameGym = { _, _ -> },
                                hideGym = {},
                                toggleRecording = {},
                                classifySuccess = {},
                                classifyFailure = {},
                                openTrim = {},
                                deferTrim = {},
                                keepOriginal = {},
                                openArchive = {},
                                playAttempt = {},
                                closePlayback = {},
                                submitTrim = { _, _ -> },
                                cancelTrim = {},
                                backToBoard = {},
                                shareAttempt = {},
                                retryPendingAttempt = {},
                                retryStatusAction = {},
                                requestEndSession = {},
                                endSession = {},
                                openStaticScreen = {},
                                attachCameraPreview = {},
                            )
                        }
                    }
                }
            }
        }
    }

    private fun catalogState(stateName: String?): AppState {
        val gym = Gym("catalog-gym", "닻 클라이밍", GymSource.SEEDED)
        val gyms = listOf(
            gym,
            Gym("catalog-yangjae", "더클라임 양재점", GymSource.SEEDED),
            Gym("catalog-gangnam", "더클라임 강남점", GymSource.SEEDED),
            Gym("catalog-piclimb", "피클라임", GymSource.SEEDED),
            Gym("catalog-personal", "클라이밍 파크 성수", GymSource.USER_ADDED),
        )
        val session = Session(
            id = "catalog-session",
            gymId = gym.id,
            startedAtEpochMillis = System.currentTimeMillis() - 5_044_000L,
            status = SessionStatus.ACTIVE,
        )
        val baseAttempt = Attempt(
            id = "catalog-attempt",
            sessionId = session.id,
            color = "blue",
            recordedAtEpochMillis = System.currentTimeMillis() - 120_000L,
            outcome = AttemptOutcome.SUCCESS,
            videoUri = "content://com.weclimb.android.catalog/video",
            cachePath = null,
            media = AttemptMedia.pending("content://com.weclimb.android.catalog/video"),
        )
        val screen = when (stateName) {
            "BoardDialog" -> Screen.Board
            "OnboardingRequest", "OnboardingDenied", "OnboardingGranted" -> Screen.Onboarding
            "CapturePreparing", "CaptureError", "CaptureReady", "CaptureRecording", "CaptureClassify", "MediaChoice" -> Screen.Capture
            "TrimInvalid", "TrimProcessing", "TrimFailed", "TrimCompleted" -> Screen.Trim
            "LoadingSuccess", "LoadingError" -> Screen.Home
            "Playback" -> Screen.Archive
            else -> runCatching { Screen.valueOf(stateName.orEmpty()) }.getOrDefault(Screen.Home)
        }
        val media = when (stateName) {
            "TrimProcessing" -> baseAttempt.media.copy(state = AttemptMediaState.TRIM_PROCESSING)
            "TrimFailed" -> baseAttempt.media.copy(state = AttemptMediaState.TRIM_FAILED, errorMessage = "영상을 자르지 못했어요")
            "TrimCompleted" -> baseAttempt.media.copy(
                state = AttemptMediaState.TRIMMED,
                trimmedVideoUri = "content://com.weclimb.android.catalog/trimmed",
            )
            else -> baseAttempt.media
        }
        val attempt = baseAttempt.copy(media = media)
        val boardAttempts = listOf(attempt) + listOf(
            "blue" to 6,
            "green" to 12,
            "yellow" to 5,
            "white" to 2,
        ).flatMap { (color, count) ->
            List(count) { index ->
                Attempt(
                    id = "catalog-$color-$index",
                    sessionId = session.id,
                    color = color,
                    recordedAtEpochMillis = System.currentTimeMillis() - (index + 1) * 1_000L,
                    outcome = AttemptOutcome.SUCCESS,
                    videoUri = null,
                    cachePath = null,
                    media = AttemptMedia.none(),
                )
            }
        }
        val archive = listOf(
            ArchiveAttempt(
                attempt = baseAttempt.copy(
                    id = "archive-blue",
                    recordedAtEpochMillis = catalogEpoch(22, 20, 14),
                    media = baseAttempt.media.copy(
                        state = AttemptMediaState.TRIMMED,
                        trimmedVideoUri = "content://com.weclimb.android.catalog/trimmed-blue",
                    ),
                ),
                gymName = "닻 클라이밍",
            ),
            ArchiveAttempt(
                attempt = baseAttempt.copy(
                    id = "archive-green",
                    color = "green",
                    recordedAtEpochMillis = catalogEpoch(22, 19, 55),
                ),
                gymName = "닻 클라이밍",
            ),
            ArchiveAttempt(
                attempt = baseAttempt.copy(
                    id = "archive-yellow",
                    color = "yellow",
                    recordedAtEpochMillis = catalogEpoch(19, 21, 2),
                    media = AttemptMedia.originalKept("content://com.weclimb.android.catalog/video-yellow"),
                ),
                gymName = "더클라임 양재점",
            ),
            ArchiveAttempt(
                attempt = baseAttempt.copy(
                    id = "archive-red",
                    color = "red",
                    recordedAtEpochMillis = catalogEpoch(13, 20, 40),
                ),
                gymName = "닻 클라이밍",
            ),
        )
        return AppState(
            screen = screen,
            gyms = gyms,
            activeSession = session,
            attempts = boardAttempts,
            archive = archive,
            selectedAttempt = attempt,
            selectedVideoDurationMillis = 19_000L,
            cameraReady = stateName != "CapturePreparing" && stateName != "CaptureError",
            capturedFile = cacheDir.resolve("catalog-attempt.mp4").takeIf { stateName == "CaptureClassify" },
            recording = stateName == "CaptureRecording",
            permissionsGranted = stateName != "OnboardingRequest" && stateName != "OnboardingDenied",
            permissionRequested = stateName == "OnboardingDenied",
            settingsRequired = stateName == "OnboardingDenied",
            confirmEnd = stateName == "BoardDialog",
            trimInProgress = stateName == "TrimProcessing",
            message = when (stateName) {
                "LoadingSuccess" -> "영상을 저장했어요\n아카이브에서 다시 볼 수 있어요"
                "LoadingError" -> "저장에 실패했어요\n원본은 그대로 있어요"
                "CaptureError" -> "카메라 권한을 확인해 주세요"
                "TrimInvalid" -> "선택 구간을 확인해 주세요"
                else -> null
            },
            unavailableVideoAttemptId = "archive-red",
            mediaChoiceAttempt = attempt.takeIf { stateName == "MediaChoice" },
            playingVideoUri = "content://com.weclimb.android.catalog/video".takeIf { stateName == "Playback" },
        )
    }

    private companion object {
        const val EXTRA_STATE = "state"
    }
}

internal fun targetCatalogDensity(widthPixels: Int): Float = widthPixels / 390f

private fun catalogEpoch(day: Int, hour: Int, minute: Int): Long =
    LocalDateTime
        .of(2026, 7, day, hour, minute)
        .atZone(ZoneId.of("Asia/Seoul"))
        .toInstant()
        .toEpochMilli()
