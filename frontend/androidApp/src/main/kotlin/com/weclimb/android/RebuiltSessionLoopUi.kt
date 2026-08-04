package com.weclimb.android

import androidx.camera.view.PreviewView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.weclimb.media.AttemptMediaState
import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome
import com.weclimb.session.Gym
import com.weclimb.session.GymCatalog
import com.weclimb.session.GymSource
import com.weclimb.session.originalVideoUri
import com.weclimb.session.displayVideoUri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RebuiltSessionLoopApp(
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
    val bottomScreen = state.playingVideoUri == null &&
        (state.screen == Screen.Home || state.screen == Screen.Archive || state.screen == Screen.RecordsPreview)
    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (bottomScreen) {
                BottomTabs(
                    selected = state.screen,
                    openHome = { openStaticScreen(Screen.Home) },
                    openArchive = openArchive,
                    openRecords = { openStaticScreen(Screen.RecordsPreview) },
                )
            }
        },
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            when (state.screen) {
                Screen.Loading -> LoadingUi()
                Screen.Onboarding -> OnboardingUi(state, requestPermissions, openAppSettings, completeOnboarding)
                Screen.Home -> HomeUi(openGyms, openArchive)
                Screen.Gyms -> GymPickerUi(state.gyms, startSession, addGym, renameGym, hideGym, backToBoard)
                Screen.Board -> BoardUi(state, openCapture, recordSuccessWithoutVideo, requestEndSession)
                Screen.Capture -> CaptureUi(
                    state,
                    attachCameraPreview,
                    toggleRecording,
                    classifySuccess,
                    classifyFailure,
                    retryStatusAction,
                    openAppSettings,
                    captureSystemBack,
                )
                Screen.Trim -> TrimUi(state, submitTrim, keepOriginal, cancelTrim, playAttempt, shareAttempt, openArchive, backToBoard)
                Screen.Archive -> ArchiveUi(
                    state.archive,
                    state.unavailableVideoAttemptId,
                    playAttempt,
                    openTrim,
                    shareAttempt,
                    openClassification,
                    openGyms,
                )
                Screen.SessionEndPreview -> SessionEndPreviewUi { openStaticScreen(Screen.Home) }
                Screen.ReportPreview -> ReportPreviewUi()
                Screen.RecordsPreview -> RecordsPreviewUi()
            }
            state.message
                ?.takeUnless { state.screen == Screen.Capture }
                ?.takeUnless { state.screen == Screen.Trim }
                ?.let {
                    StatusBanner(
                        message = it,
                        error = state.statusIsError,
                        retry = retryStatusAction.takeIf { state.statusIsError },
                    )
                }
            state.playingVideoUri?.let { uri -> PlaybackOverlay(uri, state.selectedAttempt, closePlayback, state.selectedAttempt?.let { attempt -> { shareAttempt(attempt) } }) }
        }
    }
    state.mediaChoiceAttempt?.let { attempt ->
        ModalBottomSheet(onDismissRequest = { deferTrim(attempt) }, containerColor = MaterialTheme.colorScheme.surface) {
            MediaChoiceSheet(attempt, { openTrim(attempt) }, { deferTrim(attempt) }, { keepOriginal(attempt) })
        }
    }
    if (state.confirmEnd) {
        EndSessionDialog(state, endSession, backToBoard, retryPendingAttempt, discardPendingAttempt)
    }
}
