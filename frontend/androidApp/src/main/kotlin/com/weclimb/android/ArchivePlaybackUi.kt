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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.weclimb.session.summarizeAttempts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun ArchiveUi(
    archive: List<ArchiveAttempt>,
    unavailableId: String?,
    play: (Attempt) -> Unit,
    trim: (Attempt) -> Unit,
    share: (Attempt) -> Unit,
    classify: (Attempt) -> Unit,
    startClimbing: () -> Unit,
) {
    Column(Modifier.fillMaxSize().semantics { testTag = "screen-archive" }) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 8.dp)) {
            Text("영상 아카이브", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                if (archive.isEmpty()) "성공 영상을 모아둬요" else "영상과 미분류 시도 ${archive.size}개",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 6.dp, bottom = 12.dp).semantics { testTag = "archive-static-filters" },
        ) {
            items(listOf("전체", "트리밍 완료", "대기", "닻 클라이밍")) { label ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (label == "전체") MaterialTheme.colorScheme.primary.copy(alpha = .16f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (label == "전체") MaterialTheme.colorScheme.primary.copy(alpha = .4f) else Color.Transparent),
                ) {
                    Text(label, Modifier.padding(horizontal = 13.dp, vertical = 7.dp), color = if (label == "전체") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (archive.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().weight(1f).padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(Modifier.size(78.dp), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
                    Box(contentAlignment = Alignment.Center) { Text("▷", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF64748B)) }
                }
                Spacer(Modifier.height(16.dp))
                Text("아직 영상이 없어요", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "클라이밍에서 성공한 순간을 찍으면\n여기에 자동으로 쌓여요",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(18.dp))
                Button(startClimbing, shape = RoundedCornerShape(15.dp), modifier = Modifier.height(50.dp)) { Text("▷  클라이밍 시작") }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
            ) {
                itemsIndexed(archive) { index, item ->
                    val previewLevel = listOf("Lv.5", "Lv.4", "Lv.3", "Lv.1").getOrElse(index) { "Lv.5" }
                    ArchiveCard(item, previewLevel, item.attempt.id == unavailableId, play, trim, share, classify)
                }
            }
        }
    }
}

@Composable
internal fun ArchiveCard(
    item: ArchiveAttempt,
    previewLevel: String,
    unavailable: Boolean,
    play: (Attempt) -> Unit,
    trim: (Attempt) -> Unit,
    share: (Attempt) -> Unit,
    classify: (Attempt) -> Unit,
) = Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = if (unavailable) .14f else .08f)),
) {
    val attempt = item.attempt
    val unclassified = attempt.outcome == AttemptOutcome.UNCLASSIFIED
    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(96.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (unavailable) Color(0xFF0D111A) else holdColor(attempt.color).copy(alpha = .2f)),
        ) {
            if (unclassified) {
                Text("분류\n필요", Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            } else if (unavailable) {
                Text("이미지\n없음", Modifier.align(Alignment.Center), color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall)
            } else {
                Box(Modifier.align(Alignment.TopStart).padding(7.dp).size(12.dp).clip(RoundedCornerShape(4.dp)).background(holdColor(attempt.color)))
                Surface(Modifier.align(Alignment.Center).size(38.dp), shape = CircleShape, color = Color.Black.copy(alpha = .45f)) {
                    Box(contentAlignment = Alignment.Center) { Text("▶", color = Color.White, style = MaterialTheme.typography.labelSmall) }
                }
                Text("0:12", Modifier.align(Alignment.BottomEnd).padding(6.dp).background(Color.Black.copy(alpha = .6f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f).height(120.dp)) {
            Text(item.gymName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(formatArchiveDateTime(attempt.recordedAtEpochMillis), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(13.dp).clip(RoundedCornerShape(4.dp)).background(holdColor(attempt.color)))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (unclassified) "${holdLabel(attempt.color)} · 미분류" else "${holdLabel(attempt.color)} · $previewLevel",
                    color = Color(0xFFCBD5E1),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (unclassified) {
                ArchiveUnclassifiedPill()
            } else {
                ArchiveStatePill(if (unavailable) null else attempt.media.state)
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when {
                    unclassified -> ArchiveAction("분류하기", { classify(attempt) }, primary = true)
                    unavailable -> ArchiveAction("기록만 유지 · 재생 불가", {}, enabled = false)
                    attempt.media.state == AttemptMediaState.TRIM_PENDING || attempt.media.state == AttemptMediaState.TRIM_FAILED -> {
                        ArchiveAction("이어서 자르기", { trim(attempt) }, primary = true)
                        ArchiveAction("재생", { play(attempt) })
                    }
                    else -> {
                        ArchiveAction("재생", { play(attempt) }, primary = true)
                        if (attempt.media.state == AttemptMediaState.ORIGINAL_KEPT) {
                            ArchiveAction("자르기", { trim(attempt) })
                        }
                        ArchiveAction("공유", { share(attempt) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveUnclassifiedPill() {
    val color = MaterialTheme.colorScheme.primary
    Surface(
        Modifier.padding(top = 6.dp),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = .14f),
    ) {
        Text(
            "분류 필요",
            Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

internal fun formatArchiveDateTime(epochMillis: Long): String =
    SimpleDateFormat("M월 d일 · a h:mm", Locale.KOREAN).format(Date(epochMillis))

@Composable
internal fun ArchiveStatePill(state: AttemptMediaState?) {
    val text = when (state) {
        AttemptMediaState.TRIMMED -> "트리밍 완료"
        AttemptMediaState.TRIM_PENDING, AttemptMediaState.TRIM_FAILED, AttemptMediaState.TRIM_PROCESSING -> "트리밍 대기"
        AttemptMediaState.ORIGINAL_KEPT -> "원본 유지"
        AttemptMediaState.NONE -> "영상 없음"
        null -> "기기에서 삭제됨"
    }
    val color = when (state) {
        AttemptMediaState.TRIMMED -> Color(0xFF6EE7B7)
        AttemptMediaState.TRIM_PENDING, AttemptMediaState.TRIM_FAILED, AttemptMediaState.TRIM_PROCESSING -> Color(0xFFF0A878)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        Modifier.padding(top = 6.dp),
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = .14f),
    ) {
        Text(text, Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun ArchiveAction(label: String, onClick: () -> Unit, primary: Boolean = false, enabled: Boolean = true) = Surface(
    Modifier.clickable(enabled = enabled, onClick = onClick),
    shape = RoundedCornerShape(9.dp),
    color = if (primary) MaterialTheme.colorScheme.primary.copy(alpha = .16f) else Color.Transparent,
    border = androidx.compose.foundation.BorderStroke(1.dp, if (primary) MaterialTheme.colorScheme.primary.copy(alpha = .4f) else Color.White.copy(alpha = .12f)),
) {
    Text(
        label,
        Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun MediaChoiceSheet(attempt: Attempt, trim: () -> Unit, later: () -> Unit, original: () -> Unit) = Column(
    Modifier.padding(start = 22.dp, end = 22.dp, bottom = 30.dp).semantics {
        testTag = "sheet-media-choice"
        testTagsAsResourceId = true
    },
    verticalArrangement = Arrangement.spacedBy(10.dp),
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondary.copy(alpha = .16f)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(holdColor(attempt.color)))
            Spacer(Modifier.width(7.dp))
            Text("${holdLabel(attempt.color)} 완등 · 0:19", color = Color(0xFF34D399), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
    Text("이 영상, 어떻게 할까요?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("지금 자르면 바로 정리돼요. 나중에 해도 원본은 안전하게 보관돼요.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    MediaChoiceOption("✂", "지금 자르기", "앞뒤 컷 편집 후 저장", trim, primary = true)
    MediaChoiceOption("◷", "나중에", "트리밍 대기함에 보관 · 아카이브에서 이어서", later)
    MediaChoiceOption("▣", "원본 그대로", "자르지 않고 아카이브에", original)
}

@Composable
internal fun MediaChoiceOption(icon: String, title: String, detail: String, onClick: () -> Unit, primary: Boolean = false) = Surface(
    Modifier.fillMaxWidth().clickable(onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = if (primary) Color(0xFF1E1D20) else MaterialTheme.colorScheme.surfaceVariant,
    border = androidx.compose.foundation.BorderStroke(1.dp, if (primary) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = .07f)),
) {
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = if (primary) MaterialTheme.colorScheme.primary.copy(alpha = .18f) else Color(0xFF0D111A)) {
            Box(contentAlignment = Alignment.Center) { Text(icon, color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        Text("›", color = Color(0xFF475569))
    }
}

@Composable
internal fun EndSessionDialog(
    state: AppState,
    confirm: () -> Unit,
    dismiss: () -> Unit,
    retryPending: (Attempt) -> Unit,
    discardPending: (Attempt) -> Unit,
) {
    val summary = summarizeAttempts(state.attempts)
    val pendingSave = state.attempts.firstOrNull { it.outcome == AttemptOutcome.SAVE_PENDING }
    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).semantics {
                testTag = "dialog-end-session"
                testTagsAsResourceId = true
            },
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .1f)),
        ) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("클라이밍을 종료할까요?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "종료하면 이번 세션 기록이 저장돼요.\n나중에 처리한 영상은 아카이브에서 계속 정리할 수 있어요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EndRecap(summary.successCount.toString(), "완등", Modifier.weight(1f))
                    EndRecap(summary.totalCount.toString(), "전체 시도", Modifier.weight(1f), WeClimbHoldColors.blue)
                    EndRecap(summary.mediaActionCount.toString(), "정리 필요", Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(20.dp))
                if (pendingSave == null) {
                    Button(
                        onClick = confirm,
                        modifier = Modifier.fillMaxWidth().height(50.dp).semantics { testTag = "cta-confirm-end-session" },
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("종료하기") }
                } else {
                    Text(
                        "저장하지 못한 영상이 있어요",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { retryPending(pendingSave) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("다시 저장") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { discardPending(pendingSave) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text("영상만 폐기하고 기록 유지") }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = dismiss,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("계속 클라이밍") }
            }
        }
    }
}

@Composable
internal fun EndRecap(value: String, label: String, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onSurface) = Surface(
    modifier.semantics { contentDescription = "$label $value" },
    shape = RoundedCornerShape(12.dp),
    color = Color(0xFF0D111A),
) {
    Column(Modifier.padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun StatusBanner(message: String, error: Boolean, retry: (() -> Unit)?) {
    val accent = if (error) MaterialTheme.colorScheme.error else Color(0xFF059669)
    val lines = message.lines()
    val title = if (lines.size > 1) {
        lines.first()
    } else if (error) {
        "처리 상태를 확인해 주세요"
    } else {
        "완료됐어요"
    }
    val detail = if (lines.size > 1) lines.drop(1).joinToString("\n") else message
    Surface(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).semantics { testTag = "status-banner" },
        shape = RoundedCornerShape(14.dp),
        color = if (error) Color(0xFF2A1315) else Color(0xFF0E2A20),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = .5f)),
    ) {
    Row(Modifier.padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(30.dp), shape = RoundedCornerShape(9.dp), color = accent) { Box(contentAlignment = Alignment.Center) { Text(if (error) "!" else "✓", color = Color.White, fontWeight = FontWeight.Bold) } }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            if (detail.isNotEmpty()) {
                Text(detail, color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (retry != null) {
            Button(
                onClick = retry,
                modifier = Modifier.height(38.dp).semantics { testTag = "cta-retry-status" },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
            ) { Text("다시 시도", style = MaterialTheme.typography.labelMedium) }
        }
    }
}
}

@Composable
internal fun PlaybackOverlay(uri: String, attempt: Attempt?, close: () -> Unit, share: (() -> Unit)?) = Surface(
    Modifier.fillMaxSize().semantics { testTag = "screen-playback" },
    color = Color.Black,
) {
    BackHandler(onBack = close)
    Box(Modifier.fillMaxSize()) {
        AndroidVideoPlayer(
            uri,
            Modifier.fillMaxSize(),
            useController = false,
            controllerSize = 62.dp,
        )
        Column(
            Modifier.fillMaxWidth().align(Alignment.TopCenter).background(Color.Black.copy(alpha = .5f)).padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(close, Modifier.size(48.dp)) { Text("×", color = Color.White, style = MaterialTheme.typography.headlineSmall) }
                Spacer(Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(holdColor(attempt?.color ?: "blue")))
                        Spacer(Modifier.width(6.dp))
                        Text("닻 클라이밍 · ${holdLabel(attempt?.color ?: "blue")}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text("7월 22일 · Lv.5 · 0:12", color = Color.White.copy(alpha = .7f), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter).background(Color.Black.copy(alpha = .58f)).padding(start = 20.dp, end = 20.dp, bottom = 24.dp, top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(Modifier.fillMaxWidth().height(5.dp), shape = RoundedCornerShape(3.dp), color = Color.White.copy(alpha = .25f)) {
                Box(Modifier.fillMaxWidth(.42f).fillMaxSize().background(MaterialTheme.colorScheme.primary))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0:05", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Text("0:12", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            share?.let { action ->
                Button(action, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) { Text("영상 공유") }
            }
        }
    }
}
