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
internal fun SessionEndPreviewUi(openHome: () -> Unit) = Column(
    Modifier.fillMaxSize().semantics { testTag = "screen-static-session-end" },
) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
        listOf(true, true, false).forEach { active ->
            Box(Modifier.padding(horizontal = 3.dp).width(30.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant))
        }
    }
    Column(
        Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 14.dp, end = 20.dp, bottom = 16.dp),
    ) {
        Text("✓  세션 종료", color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
        Text("닻 클라이밍 · 1시간 24분 · 7월 24일", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.padding(vertical = 20.dp), verticalAlignment = Alignment.Bottom) {
            Text("26", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("완등", style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(13.dp).clip(RoundedCornerShape(4.dp)).background(WeClimbHoldColors.blue))
                    Spacer(Modifier.width(6.dp))
                    Text("최고 Lv.5 파랑", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        StaticLabelWithBadge("오늘의 한 컷")
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
        ) {
            Box(Modifier.height(88.dp), contentAlignment = Alignment.Center) {
                Text("▣  오늘의 한 컷 찍기", fontWeight = FontWeight.Bold)
                InactiveBadge("비활성", Modifier.align(Alignment.TopEnd).padding(10.dp))
            }
        }
        StaticLabel("미뤄둔 트리밍", "2개")
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .28f)),
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("✂", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("트리밍 대기 2", fontWeight = FontWeight.Bold)
                    Text("아카이브에서 이어서 자르기", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Text("정리하기 ›", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        StaticLabelWithBadge("실패 영상")
        InactivePanel("실패 영상 47개\n일괄 삭제 · 복구함(예정)", "비활성")
    }
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp)) { Text("리포트 만들기") }
        OutlinedButton(onClick = openHome, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) { Text("⌂  홈으로") }
    }
}

@Composable
internal fun StaticLabelWithBadge(title: String) = Row(
    Modifier.fillMaxWidth().padding(start = 4.dp, top = 20.dp, end = 4.dp, bottom = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(title.uppercase(), color = Color(0xFF64748B), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    InactiveBadge("Phase 3 · 비활성")
}

@Composable
internal fun ReportPreviewUi() = Column(
    Modifier.fillMaxSize().semantics { testTag = "screen-static-report" },
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, top = 6.dp, end = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = {}, enabled = false, modifier = Modifier.size(48.dp)) { Text("×", style = MaterialTheme.typography.headlineSmall) }
        Text("이번 세션 리포트", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        InactiveBadge("비활성")
    }
    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Surface(
            Modifier.fillMaxWidth().height(548.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF201C24),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
        ) {
            Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("닻 클라이밍", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("2026. 7. 24 · 1시간 24분", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                    Row {
                        Text("✦ WE-", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("CLIMB", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                Column {
                    Text("26", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                    Text("완등", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
                ReportDistributionBars()
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {}, enabled = false, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(15.dp)) { Text("인스타 스토리로 공유") }
            Surface(Modifier.size(54.dp), shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(contentAlignment = Alignment.Center) { Text("↓", color = Color.White, style = MaterialTheme.typography.titleLarge) }
            }
        }
        Text("공유와 저장은 Phase 3에서 활성화 예정", Modifier.fillMaxWidth().padding(top = 8.dp), color = Color(0xFF64748B), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun ReportDistributionBars() = Row(
    Modifier.fillMaxWidth().height(104.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Bottom,
) {
    listOf(
        Triple("0", 6, Color(0xFFE2E8F0)),
        Triple("5", 52, WeClimbHoldColors.yellow),
        Triple("12", 94, WeClimbHoldColors.green),
        Triple("7", 66, WeClimbHoldColors.blue),
        Triple("2", 30, Color(0xFFE2E8F0)),
        Triple("0", 6, WeClimbHoldColors.purple),
    ).forEach { (value, height, color) ->
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape(6.dp)).background(color))
        }
    }
}

@Composable
internal fun RecordsPreviewUi() = Column(
    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 20.dp).semantics { testTag = "screen-static-records" },
    verticalArrangement = Arrangement.spacedBy(14.dp),
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("내 기록", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        InactiveBadge("비활성")
    }
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFC084FC).copy(alpha = .1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA877D2).copy(alpha = .35f)),
    ) {
        Text(
            "통계·성장 곡선은 Phase 3에서 활성화 · 지금은 표시만",
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            color = Color(0xFFC9A9E8),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatTile("87", "총 완등", Modifier.weight(1f)); StatTile("12", "총 방문", Modifier.weight(1f)); StatTile("4회", "이번 달", Modifier.weight(1f)) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("지난 6개월 완등", color = Color(0xFF64748B), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text("전체 보기 ›", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp)) {
            ColorDistributionBars()
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                listOf("2월", "3월", "4월", "5월", "6월", "7월").forEach { month ->
                    Text(month, Modifier.weight(1f), color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
    StaticLabel("등급별 완등")
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            listOf("Lv.5 파랑" to Color(0xFF5A8FDB), "Lv.4 초록" to Color(0xFF46AE72), "Lv.3 노랑" to Color(0xFFDDBB44), "Lv.2 주황" to Color(0xFFE68A4C)).forEachIndexed { index, (label, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
                    Spacer(Modifier.width(9.dp))
                    Text(label, Modifier.width(76.dp), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Surface(Modifier.weight(1f).height(9.dp), shape = RoundedCornerShape(5.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Box(
                            Modifier
                                .fillMaxWidth(listOf(.18f, .45f, .85f, .70f)[index])
                                .fillMaxSize()
                                .background(color),
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(listOf("7", "18", "34", "28")[index], Modifier.width(26.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "암장 색을 앱 레벨로 환산해 합산 · 세션 상세에선 실제 색으로 표시",
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
    StaticLabel("지난 클라이밍")
    listOf(
        Triple("14", "닻 클라이밍", "7월 22일 · 최고 Lv.5"),
        Triple("", "더클라임 양재", "7월 19일 · 최고 Lv.4"),
    ).forEach { (day, gym, detail) ->
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(58.dp), shape = RoundedCornerShape(12.dp), color = if (day.isEmpty()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary.copy(alpha = .18f)) { Box(contentAlignment = Alignment.Center) { Text(if (day.isEmpty()) "영상 삭제됨\n기록은 남음" else day, color = if (day.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) } }
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(gym, fontWeight = FontWeight.Bold); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
internal fun StaticLabel(title: String, value: String? = null) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    value?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
}

@Composable
internal fun InactivePanel(text: String, badge: String) = Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text(text, Modifier.weight(1f), fontWeight = FontWeight.Bold); Text(badge, color = Color(0xFFC084FC), style = MaterialTheme.typography.labelSmall) } }

@Composable
internal fun StatTile(value: String, label: String, modifier: Modifier = Modifier) = Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) { Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) } }

@Composable
internal fun ColorDistributionBars() = Row(
    Modifier.fillMaxWidth().height(96.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.Bottom,
) {
    listOf(
        listOf(8 to WeClimbHoldColors.red, 4 to WeClimbHoldColors.orange),
        listOf(12 to WeClimbHoldColors.red, 7 to WeClimbHoldColors.orange, 4 to WeClimbHoldColors.yellow),
        listOf(16 to WeClimbHoldColors.red, 9 to WeClimbHoldColors.orange, 7 to WeClimbHoldColors.yellow),
        listOf(20 to WeClimbHoldColors.red, 12 to WeClimbHoldColors.orange, 9 to WeClimbHoldColors.yellow, 5 to WeClimbHoldColors.green),
        listOf(24 to WeClimbHoldColors.red, 15 to WeClimbHoldColors.orange, 12 to WeClimbHoldColors.yellow, 8 to WeClimbHoldColors.green),
        listOf(28 to WeClimbHoldColors.red, 18 to WeClimbHoldColors.orange, 14 to WeClimbHoldColors.yellow, 11 to WeClimbHoldColors.green, 6 to WeClimbHoldColors.blue),
    ).forEach { month ->
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.Bottom,
        ) {
            month.asReversed().forEachIndexed { index, (height, color) ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(height.dp)
                        .clip(
                            if (index == 0) {
                                RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                            } else {
                                RoundedCornerShape(0.dp)
                            },
                        )
                        .background(color),
                )
            }
        }
    }
}

@Composable
internal fun BottomTabs(selected: Screen, openHome: () -> Unit, openArchive: () -> Unit, openRecords: () -> Unit) = Column {
    NavigationBar(
        modifier = Modifier.height(66.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        NavigationBarItem(selected = selected == Screen.Home, onClick = openHome, icon = { TabGlyph("⌂") }, label = { Text("홈") })
        NavigationBarItem(selected = selected == Screen.Archive, onClick = openArchive, icon = { TabGlyph("▷") }, label = { Text("영상") })
        NavigationBarItem(selected = selected == Screen.RecordsPreview, onClick = openRecords, icon = { TabGlyph("▥") }, label = { Text("기록") })
    }
    Spacer(Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars).background(MaterialTheme.colorScheme.surface))
}

@Composable
internal fun TabGlyph(glyph: String) = Text(glyph, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

@Composable
internal fun holdColor(color: String): Color = when (color.lowercase()) {
    "red", "빨강" -> WeClimbHoldColors.red
    "orange", "주황" -> WeClimbHoldColors.orange
    "yellow", "노랑" -> WeClimbHoldColors.yellow
    "green", "초록" -> WeClimbHoldColors.green
    "white", "흰색" -> Color(0xFFE2E8F0)
    else -> WeClimbHoldColors.blue
}
