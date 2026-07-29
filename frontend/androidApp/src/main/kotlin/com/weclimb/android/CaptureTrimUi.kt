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

@Composable
internal fun CaptureUi(
    state: AppState,
    attachPreview: (PreviewView) -> Unit,
    toggle: () -> Unit,
    success: (String) -> Unit,
    failure: (String) -> Unit,
    retry: () -> Unit,
    openSettings: () -> Unit,
    back: () -> Unit,
) {
    BackHandler(onBack = back)
    val color = "blue"
    val cameraError = !state.cameraReady && state.message != null
    Box(Modifier.fillMaxSize().semantics { testTag = "screen-capture" }) {
        CameraPreviewSurface(attachPreview, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = .5f)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)).background(WeClimbHoldColors.blue))
                        Spacer(Modifier.width(8.dp))
                        Text("파랑", color = Color.White, fontWeight = FontWeight.Bold)
                        if (state.capturedFile == null) {
                            Spacer(Modifier.width(6.dp))
                            Text("⌄", color = Color.White)
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = .5f)) {
                    Text(
                        when {
                            state.recording -> "●  0:08"
                            state.capturedFile != null -> "■  0:19"
                            else -> "촬영 준비"
                        },
                        Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            when {
                cameraError -> CameraErrorOverlay(state.message.orEmpty(), retry, openSettings)
                !state.cameraReady && state.capturedFile == null -> CameraPreparingOverlay()
                state.capturedFile == null -> {
                    Column(
                        Modifier.fillMaxWidth().padding(bottom = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            if (state.recording) "완등 순간까지 찍고 정지하세요" else "촬영을 시작할 준비가 됐어요",
                            Modifier.background(Color.Black.copy(alpha = .35f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 5.dp),
                            color = Color.White.copy(alpha = .75f),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(
                            Modifier
                                .size(78.dp)
                                .clickable(onClick = toggle)
                                .semantics {
                                    contentDescription = if (state.recording) "녹화 중지" else "녹화 시작"
                                    testTag = "cta-toggle-recording"
                                },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = .25f),
                            border = androidx.compose.foundation.BorderStroke(5.dp, Color.White.copy(alpha = .85f)),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    Modifier
                                        .size(if (state.recording) 28.dp else 50.dp)
                                        .clip(if (state.recording) RoundedCornerShape(7.dp) else CircleShape)
                                        .background(MaterialTheme.colorScheme.error),
                                )
                            }
                        }
                    }
                }
                else -> {
                    Column(
                        Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .55f)).padding(start = 18.dp, end = 18.dp, bottom = 34.dp, top = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("방금 시도, 성공했나요?", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { failure(color) },
                                modifier = Modifier.weight(1f).height(100.dp).semantics { testTag = "cta-classify-failure" },
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("×", style = MaterialTheme.typography.headlineMedium)
                                    Text("실패", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("영상 삭제", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Button(
                                onClick = { success(color) },
                                modifier = Modifier.weight(1f).height(100.dp).semantics { testTag = "cta-classify-success" },
                                shape = RoundedCornerShape(22.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("✓", style = MaterialTheme.typography.headlineMedium)
                                    Text("성공", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("영상 보관", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CameraPreparingOverlay() = Column(
    Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 80.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
) {
    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, trackColor = Color.White.copy(alpha = .16f), strokeWidth = 4.dp)
    Text("카메라를 준비하고 있어요", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text("잠시만요, 곧 촬영할 수 있어요", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun CameraErrorOverlay(message: String, retry: () -> Unit, openSettings: () -> Unit) = Column(
    Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 80.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    Surface(Modifier.size(58.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.error.copy(alpha = .16f)) {
        Box(contentAlignment = Alignment.Center) { Text("!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
    }
    Text("카메라를 열 수 없어요", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(
        message.ifBlank { "다른 앱이 카메라를 쓰고 있거나 일시적인 문제일 수 있어요" },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(retry, shape = RoundedCornerShape(13.dp)) { Text("다시 시도") }
        OutlinedButton(openSettings, shape = RoundedCornerShape(13.dp)) { Text("설정 열기") }
    }
}

@Composable
internal fun CameraPreviewSurface(attachPreview: (PreviewView) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    AndroidView(modifier = modifier, factory = { PreviewView(context).also(attachPreview) })
}

@Composable
internal fun TrimUi(
    state: AppState,
    submit: (Long, Long) -> Unit,
    keep: (Attempt) -> Unit,
    cancel: (Attempt) -> Unit,
    play: (Attempt) -> Unit,
    share: (Attempt) -> Unit,
    openArchive: () -> Unit,
    back: () -> Unit,
) {
    val attempt = state.selectedAttempt ?: return Column(Modifier.padding(20.dp)) {
        Text("트리밍할 영상을 찾을 수 없습니다")
        TextButton(back) { Text("세션으로 돌아가기") }
    }
    BackHandler(onBack = back)
    val durationMillis = state.selectedVideoDurationMillis.coerceAtLeast(1_000L)
    var range by remember(attempt.id, durationMillis) { mutableStateOf(.2f..minOf(.84f, 1f)) }
    val startMillis = (durationMillis * range.start).toLong()
    val endMillis = (durationMillis * range.endInclusive).toLong()
    val completed = attempt.media.state == AttemptMediaState.TRIMMED
    val failed = attempt.media.state == AttemptMediaState.TRIM_FAILED
    Box(Modifier.fillMaxSize().semantics { testTag = "screen-trim" }) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, top = 6.dp, end = 20.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(back, Modifier.size(48.dp)) { Text("×", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall) }
                Box(Modifier.size(15.dp).clip(RoundedCornerShape(5.dp)).background(holdColor(attempt.color)))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (completed) "트리밍 완료" else "앞뒤 자르기",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text("${holdLabel(attempt.color)} · 8트", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
            if (failed) {
                TrimStatusBanner("저장에 실패했어요 — 원본은 그대로 있어요", success = false)
            } else if (completed) {
                TrimStatusBanner("0:12로 저장했어요 · 아카이브에 추가됨", success = true)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(396.dp)
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF10141D))
                    .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(20.dp))
                    .semantics { testTag = "player-trim-preview" },
            ) {
                AndroidVideoPlayer(
                    if (completed) attempt.displayVideoUri.orEmpty() else attempt.originalVideoUri,
                    Modifier.fillMaxSize(),
                    useController = false,
                    controllerColor = if (completed) MaterialTheme.colorScheme.primary.copy(alpha = .9f) else Color.Black.copy(alpha = .5f),
                    controllerSize = if (completed) 52.dp else 46.dp,
                )
                Text(
                    if (completed) "Media3 Player · 결과" else "Media3 Player",
                    Modifier.align(Alignment.TopCenter).padding(top = 10.dp).border(1.dp, Color.White.copy(alpha = .25f), RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 3.dp),
                    color = Color.White.copy(alpha = .5f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (completed) "0:00 / 0:12" else "${formatSeconds(startMillis)} / ${formatSeconds(durationMillis)}",
                    Modifier.align(Alignment.BottomEnd).padding(12.dp).background(Color.Black.copy(alpha = .55f), RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (!completed) {
                Column(Modifier.padding(start = 20.dp, top = 14.dp, end = 20.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(56.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(Modifier.fillMaxSize().padding(7.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(10) { index ->
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (index in 2..7) Color(0xFF3A4351) else Color(0xFF262D39)),
                                )
                            }
                        }
                        RangeSlider(
                            value = range,
                            onValueChange = { range = it },
                            modifier = Modifier.fillMaxWidth().semantics { testTag = "trim-range" },
                            valueRange = 0f..1f,
                            enabled = !state.trimInProgress,
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0:00", color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall)
                        Text("선택 ${formatSeconds(startMillis)} - ${formatSeconds(endMillis)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        Text(formatSeconds(durationMillis), color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall)
                    }
                    if (state.message?.contains("구간") == true) {
                        Text(
                            "선택 구간을 확인해 주세요",
                            Modifier.fillMaxWidth().padding(top = 8.dp).background(MaterialTheme.colorScheme.error.copy(alpha = .12f), RoundedCornerShape(11.dp)).border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .35f), RoundedCornerShape(11.dp)).padding(horizontal = 12.dp, vertical = 9.dp),
                            color = Color(0xFFFCA5A5),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (completed) {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton({ play(attempt) }, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("▶  다시 보기") }
                        Button({ share(attempt) }, Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("공유") }
                    }
                    TextButton(openArchive, Modifier.fillMaxWidth()) { Text("아카이브로 이동") }
                }
            } else {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton({ keep(attempt) }, Modifier.weight(1f).height(52.dp), enabled = !state.trimInProgress, shape = RoundedCornerShape(14.dp)) { Text("원본 유지") }
                        Button(
                            onClick = { submit(startMillis, endMillis) },
                            modifier = Modifier.weight(1f).height(52.dp).semantics { testTag = "cta-submit-trim" },
                            enabled = !state.trimInProgress,
                            shape = RoundedCornerShape(14.dp),
                        ) { Text(if (failed) "다시 시도" else "자르고 저장") }
                    }
                    TextButton({ cancel(attempt) }, Modifier.fillMaxWidth(), enabled = !state.trimInProgress) { Text("나중에 할래요") }
                }
            }
        }
        if (state.trimInProgress) {
            Surface(Modifier.fillMaxSize(), color = Color(0xE605070D)) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(progress = { .45f }, modifier = Modifier.size(92.dp), color = MaterialTheme.colorScheme.primary, trackColor = Color.White.copy(alpha = .16f), strokeWidth = 7.dp)
                    Spacer(Modifier.height(18.dp))
                    Text("45%", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("영상을 저장하고 있어요", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("완료 전까지 중복 실행할 수 없어요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun TrimStatusBanner(text: String, success: Boolean) {
    val accent = if (success) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
    val base = if (success) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
    Surface(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
        shape = RoundedCornerShape(12.dp),
        color = base.copy(alpha = .12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, base.copy(alpha = .4f)),
    ) {
        Text(text, Modifier.padding(horizontal = 13.dp, vertical = 11.dp), color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

internal fun formatSeconds(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
