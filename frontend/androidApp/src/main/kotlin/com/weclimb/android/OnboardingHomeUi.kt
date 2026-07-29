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
internal fun LoadingUi() = Box(Modifier.fillMaxSize().semantics { testTag = "screen-loading" }, Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row {
            Text("✦ WE-", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text("CLIMB", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        CircularProgressIndicator(
            modifier = Modifier.size(46.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 4.dp,
        )
        Text("불러오는 중…", color = Color(0xFF64748B), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun OnboardingUi(state: AppState, requestPermissions: () -> Unit, openAppSettings: () -> Unit, complete: () -> Unit) {
    val granted = state.permissionsGranted
    val denied = state.settingsRequired
    val title = when {
        granted -> "준비됐어요"
        denied -> "권한이 꺼져 있어요"
        else -> "붙는 순간을\n바로 남겨요"
    }
    val description = when {
        granted -> "카메라·마이크가 모두 허용됐어요. 이제 암장을 고르고 첫 세션을 시작할 수 있어요."
        denied -> "카메라·마이크 권한을 거부해서 촬영을 시작할 수 없어요. 설정에서 직접 켜주시면 바로 이어갈 수 있어요."
        else -> "촬영부터 성공/실패 정리, 자르기, 공유까지 한 흐름으로. 시작하려면 카메라와 마이크 권한이 필요해요."
    }
    Column(
        Modifier.fillMaxSize().semantics { testTag = "screen-onboarding" },
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            Modifier.padding(start = 26.dp, top = 18.dp, end = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row {
                Text("✦ WE-", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text("CLIMB", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            OnboardingArt(denied = denied, granted = granted)
            Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(2.dp))
            PermissionRow("카메라", when { denied -> "거부됨"; granted -> "허용됨"; else -> "클라이밍 영상 촬영" }, granted, denied)
            PermissionRow("마이크", when { denied -> "거부됨"; granted -> "허용됨"; else -> "현장 소리 함께 기록" }, granted, denied)
        }
        Column(
            Modifier.padding(start = 26.dp, end = 26.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                when {
                    granted -> "기록이 쌓이면 리포트가 자동으로 채워져요"
                    denied -> "설정 › 권한 › 카메라·마이크 › 허용"
                    else -> "권한은 촬영에만 쓰이고 기기에만 저장돼요"
                },
                color = if (denied) Color(0xFFF0A878) else Color(0xFF64748B),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            when {
                granted -> Button(
                    onClick = complete,
                    modifier = Modifier.fillMaxWidth().height(54.dp).semantics { testTag = "cta-complete-onboarding" },
                    shape = RoundedCornerShape(16.dp),
                ) { Text("시작하기") }
                denied -> {
                    Button(
                        onClick = openAppSettings,
                        modifier = Modifier.fillMaxWidth().height(54.dp).semantics { testTag = "cta-open-settings" },
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("설정 열기") }
                    OutlinedButton(
                        onClick = requestPermissions,
                        modifier = Modifier.fillMaxWidth().height(48.dp).semantics { testTag = "cta-request-permissions-again" },
                        shape = RoundedCornerShape(16.dp),
                    ) { Text("다시 요청하기") }
                }
                else -> Button(
                    onClick = requestPermissions,
                    modifier = Modifier.fillMaxWidth().height(54.dp).semantics { testTag = "cta-request-permissions" },
                    shape = RoundedCornerShape(16.dp),
                ) { Text("카메라·마이크 허용하기") }
            }
        }
    }
}

@Composable
internal fun OnboardingArt(denied: Boolean, granted: Boolean) {
    val accent = when {
        denied -> MaterialTheme.colorScheme.error
        granted -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    Box(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(Color(0xFF181A22))
                .border(1.dp, accent.copy(alpha = .35f), RoundedCornerShape(34.dp)),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(WeClimbHoldColors.red.copy(alpha = .7f), 8.dp.toPx(), Offset(46.dp.toPx(), 45.dp.toPx()))
                drawCircle(WeClimbHoldColors.blue.copy(alpha = .7f), 7.dp.toPx(), Offset(103.dp.toPx(), 60.dp.toPx()))
                drawCircle(WeClimbHoldColors.yellow.copy(alpha = .7f), 7.dp.toPx(), Offset(68.dp.toPx(), 105.dp.toPx()))
                if (granted) {
                    drawCircle(accent, 34.dp.toPx(), center)
                    drawLine(Color.White, Offset(58.dp.toPx(), 75.dp.toPx()), Offset(70.dp.toPx(), 87.dp.toPx()), 3.dp.toPx())
                    drawLine(Color.White, Offset(70.dp.toPx(), 87.dp.toPx()), Offset(94.dp.toPx(), 61.dp.toPx()), 3.dp.toPx())
                } else {
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(45.dp.toPx(), 52.dp.toPx()),
                        size = Size(60.dp.toPx(), 44.dp.toPx()),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(2.dp.toPx()),
                    )
                    drawCircle(accent, 12.dp.toPx(), Offset(75.dp.toPx(), 74.dp.toPx()), style = Stroke(2.dp.toPx()))
                    if (denied) {
                        drawLine(accent, Offset(42.dp.toPx(), 42.dp.toPx()), Offset(108.dp.toPx(), 108.dp.toPx()), 3.dp.toPx())
                    }
                }
            }
        }
    }
}

@Composable
internal fun PermissionRow(label: String, detail: String, granted: Boolean, denied: Boolean) {
    val accent = when {
        granted -> MaterialTheme.colorScheme.secondary
        denied -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = if (granted || denied) .35f else .12f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(36.dp), shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (label == "카메라") "▣" else "│", color = accent, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
            Text(
                when {
                    granted -> "✓"
                    denied -> "⊗"
                    else -> "필요"
                },
                color = accent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
internal fun HomeUi(openGyms: () -> Unit, openArchive: () -> Unit) = Column(
    Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 20.dp)
        .semantics { testTag = "screen-home" },
) {
    Text("오늘도 붙어볼까요,", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    Text("기영 님", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Button(
        onClick = openGyms,
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(104.dp).semantics { testTag = "cta-start-session" },
        shape = RoundedCornerShape(22.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(50.dp), shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = .16f)) {
                Box(contentAlignment = Alignment.Center) { Text("▶", color = Color.White) }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("바로 촬영 시작", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .85f))
                Text("클라이밍 시작", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Text("›", style = MaterialTheme.typography.headlineMedium)
        }
    }
    Spacer(Modifier.height(16.dp))
    DisabledPreviewCard()
    SectionTitle("내 영상", "전체 보기", openArchive)
    StaticVideoStrip()
    SectionLabel("최근 리포트", "Phase 3")
    StaticReportCard()
}

@Composable
internal fun SectionTitle(title: String, action: String, onClick: () -> Unit) = Row(
    Modifier.fillMaxWidth().padding(start = 2.dp, top = 22.dp, end = 2.dp, bottom = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(title.uppercase(), color = Color(0xFF64748B), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    TextButton(onClick = onClick) { Text("$action ›", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
}

@Composable
internal fun SectionLabel(title: String, badge: String) = Row(
    Modifier.fillMaxWidth().padding(start = 2.dp, top = 22.dp, end = 2.dp, bottom = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(title.uppercase(), color = Color(0xFF64748B), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    InactiveBadge(badge)
}

@Composable
internal fun DisabledPreviewCard() = Surface(
    Modifier.fillMaxWidth().alpha(.6f),
    shape = RoundedCornerShape(20.dp),
    color = Color(0xFF111827),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
) {
    Box {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(82.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { .78f },
                    modifier = Modifier.fillMaxSize(),
                    color = WeClimbHoldColors.blue,
                    trackColor = Color.White.copy(alpha = .08f),
                    strokeWidth = 6.dp,
                )
                Surface(Modifier.size(68.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("5", style = MaterialTheme.typography.headlineMedium, color = WeClimbHoldColors.blue, fontWeight = FontWeight.Bold)
                            Text("LEVEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("현재 실력", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(15.dp).clip(RoundedCornerShape(5.dp)).background(WeClimbHoldColors.blue))
                    Spacer(Modifier.width(7.dp))
                    Text("Lv.5 파랑", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Text("Lv.6 남색까지 78%", color = Color(0xFF46AE72), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
            Surface(Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(10.dp), color = Color(0xFF0F1420)) {
                Row(Modifier.padding(7.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                    listOf(12, 18, 24, 32, 38, 42).forEachIndexed { index, height ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(height.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(
                                    listOf(
                                        WeClimbHoldColors.red,
                                        WeClimbHoldColors.orange,
                                        WeClimbHoldColors.yellow,
                                        WeClimbHoldColors.green,
                                        WeClimbHoldColors.blue,
                                        WeClimbHoldColors.purple,
                                    )[index].copy(alpha = .78f),
                                ),
                        )
                    }
                }
            }
        }
        InactiveBadge("Phase 3 · 비활성", Modifier.align(Alignment.TopEnd).padding(10.dp))
    }
}

@Composable
internal fun StaticVideoStrip() = LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().semantics { testTag = "home-static-videos" }) {
    items(
        listOf(
            Triple("파랑", "닻 클라이밍", WeClimbHoldColors.blue),
            Triple("초록", "닻 클라이밍", WeClimbHoldColors.green),
            Triple("노랑", "더클라임", WeClimbHoldColors.yellow),
        ),
    ) { (label, gym, color) ->
        Surface(
            Modifier.width(118.dp),
            shape = RoundedCornerShape(15.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
        ) {
            Column {
                Box(
                    Modifier.fillMaxWidth().height(150.dp).background(color.copy(alpha = .18f)),
                ) {
                    Surface(Modifier.align(Alignment.TopStart).padding(8.dp), shape = RoundedCornerShape(20.dp), color = Color.Black.copy(alpha = .55f)) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(color))
                            Spacer(Modifier.width(4.dp))
                            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(Modifier.align(Alignment.BottomEnd).padding(8.dp).size(26.dp), shape = CircleShape, color = Color.Black.copy(alpha = .5f)) {
                        Box(contentAlignment = Alignment.Center) { Text("▶", color = Color.White, style = MaterialTheme.typography.labelSmall) }
                    }
                }
                Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(gym, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Text(if (label == "노랑") "7월 19일" else "7월 22일", color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
internal fun StaticReportCard() = Surface(
    Modifier.fillMaxWidth().alpha(.6f),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .1f)),
) {
    Box {
        Column {
            Box(
                Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF263247)),
                contentAlignment = Alignment.BottomStart,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("14", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text("닻 클라이밍 · 7월 22일", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("1시간 24분 · 최고 Lv.5 파랑", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(WeClimbHoldColors.yellow, WeClimbHoldColors.green, WeClimbHoldColors.blue).forEach {
                        Box(Modifier.width(16.dp).height(8.dp).clip(RoundedCornerShape(3.dp)).background(it))
                    }
                }
            }
        }
        InactiveBadge("비활성", Modifier.align(Alignment.TopEnd).padding(10.dp))
    }
}

@Composable
internal fun InactiveBadge(text: String, modifier: Modifier = Modifier) = Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = Color(0xFFA877D2).copy(alpha = .14f),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA877D2).copy(alpha = .4f)),
) {
    Text(
        text,
        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        color = Color(0xFFC084FC),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
    )
}
