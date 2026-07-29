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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GymPickerUi(gyms: List<Gym>, startSession: (Gym) -> Unit, addGym: (String) -> Unit, renameGym: (Gym, String) -> Unit, hideGym: (Gym) -> Unit, back: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var showAddSheet by remember { mutableStateOf(false) }
    var managedGym by remember { mutableStateOf<Gym?>(null) }
    var selectedGym by remember { mutableStateOf<Gym?>(null) }
    var pendingSelectionName by remember { mutableStateOf<String?>(null) }
    val results = GymCatalog().search(gyms, name)
    LaunchedEffect(gyms, pendingSelectionName) {
        val pendingName = pendingSelectionName ?: return@LaunchedEffect
        gyms.firstOrNull {
            it.source == GymSource.USER_ADDED && it.name.equals(pendingName, ignoreCase = true)
        }?.let { addedGym ->
            selectedGym = addedGym
            pendingSelectionName = null
            name = ""
        }
    }
    LaunchedEffect(gyms, selectedGym?.id) {
        selectedGym = selectedGym?.let { selected ->
            gyms.firstOrNull { it.id == selected.id && !it.hidden }
        }
    }
    Column(Modifier.fillMaxSize().semantics { testTag = "screen-gyms" }) {
        Row(
            Modifier.fillMaxWidth().padding(start = 10.dp, top = 6.dp, end = 20.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(back, Modifier.size(48.dp).semantics { testTag = "cta-close-gyms" }) {
                Text("‹", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(4.dp))
            Text("어디서 클라이밍해요?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth().semantics { testTag = "input-gym-search" },
                placeholder = { Text("암장 이름 검색") },
                leadingIcon = { Text("⌕", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = if (name.isNotEmpty()) {
                    { TextButton({ name = "" }) { Text("×") } }
                } else {
                    null
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            Text(
                when {
                    selectedGym != null -> "선택됨"
                    name.isBlank() -> "최근"
                    else -> "검색 결과 ${results.size}"
                },
                modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp),
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        selectedGym?.let { gym ->
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1B1715),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .5f)),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GymMarker(highlighted = true)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(gym.name, fontWeight = FontWeight.Bold)
                            Text("서울 성동구 · 2.4km", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        }
                        Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = .08f))
                    Text("기록으로 보면, 여기선", color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GymInsight("평균 완등", "파랑", WeClimbHoldColors.blue, Modifier.weight(1f))
                        GymInsight("도전해볼 만한", "빨강", WeClimbHoldColors.red, Modifier.weight(1f))
                    }
                }
            }
            Text(
                "다른 암장",
                modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 4.dp),
                color = Color(0xFF64748B),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
            items(results) { gym ->
                Row(
                    Modifier.fillMaxWidth().clickable { selectedGym = gym }.padding(horizontal = 4.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GymMarker()
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        HighlightedGymName(gym.name, name)
                        Text(
                            if (gym.source == GymSource.USER_ADDED) "직접 추가" else gymLocation(gym),
                            color = Color(0xFF64748B),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    if (gym.source == GymSource.USER_ADDED) {
                        InactiveBadge("내 암장")
                        TextButton({ managedGym = gym }, Modifier.size(48.dp)) { Text("⋮") }
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = .06f))
            }
            if (name.isNotBlank() && results.isEmpty()) {
                item {
                    TextButton(
                        onClick = { showAddSheet = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).semantics { testTag = "cta-add-gym" },
                    ) { Text("+  찾는 암장이 없어요 - 직접 추가", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                }
            }
        }
        selectedGym?.let { gym ->
            Button(
                onClick = { startSession(gym) },
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp).height(56.dp).semantics { testTag = "cta-start-selected-gym" },
                shape = RoundedCornerShape(16.dp),
            ) { Text("${gym.name}에서 시작", fontWeight = FontWeight.Bold) }
        }
    }
    if (showAddSheet) {
        var newGymName by remember { mutableStateOf(name) }
        var memo by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 30.dp).semantics { testTag = "sheet-add-gym" }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("개인 암장 추가", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("목록에 없는 암장을 직접 만들어요. 나에게만 보여요.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(newGymName, { newGymName = it }, Modifier.fillMaxWidth().semantics { testTag = "input-new-gym-name" }, label = { Text("암장 이름") }, singleLine = true)
                OutlinedTextField(memo, { memo = it }, Modifier.fillMaxWidth(), label = { Text("메모 (선택)") }, placeholder = { Text("지점·위치 등") }, singleLine = true)
                Button(
                    onClick = {
                        pendingSelectionName = newGymName.trim()
                        addGym(newGymName)
                        showAddSheet = false
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp).semantics { testTag = "cta-confirm-add-gym" },
                    shape = RoundedCornerShape(14.dp),
                ) { Text("추가하고 선택") }
            }
        }
    }
    managedGym?.let { gym ->
        var editName by remember(gym.id) { mutableStateOf(gym.name) }
        var editing by remember(gym.id) { mutableStateOf(false) }
        ModalBottomSheet(onDismissRequest = { managedGym = null }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(start = 22.dp, end = 22.dp, bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(gym.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("내가 추가한 암장이에요", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                if (editing) {
                    OutlinedTextField(editName, { editName = it }, Modifier.fillMaxWidth().semantics { testTag = "input-gym-name" }, label = { Text("암장 이름") }, singleLine = true)
                    Button(
                        onClick = { renameGym(gym, editName); managedGym = null },
                        modifier = Modifier.fillMaxWidth().semantics { testTag = "cta-save-gym-name" },
                    ) { Text("이름 저장") }
                } else {
                    GymManagementAction("✎", "이름 수정", { editing = true })
                    GymManagementAction("⊘", "목록에서 숨기기", {
                        hideGym(gym)
                        managedGym = null
                    }, Modifier.semantics { testTag = "cta-hide-gym" })
                    GymManagementAction("×", "삭제", {}, enabled = false, danger = true)
                }
            }
        }
    }
}

@Composable
internal fun HighlightedGymName(name: String, query: String) {
    val start = name.indexOf(query, ignoreCase = true).takeIf { query.isNotBlank() } ?: -1
    if (start < 0) {
        Text(name, fontWeight = FontWeight.SemiBold)
        return
    }
    Text(
        buildAnnotatedString {
            append(name.substring(0, start))
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                append(name.substring(start, start + query.length))
            }
            append(name.substring(start + query.length))
        },
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
internal fun GymMarker(highlighted: Boolean = false) = Surface(
    Modifier.size(38.dp),
    shape = RoundedCornerShape(11.dp),
    color = if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = .15f) else MaterialTheme.colorScheme.surfaceVariant,
) {
    Box(contentAlignment = Alignment.Center) { Text("⌖", color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
internal fun GymInsight(label: String, value: String, color: Color, modifier: Modifier) = Surface(
    modifier,
    shape = RoundedCornerShape(11.dp),
    color = Color.Black.copy(alpha = .25f),
) {
    Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
            Spacer(Modifier.width(6.dp))
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun GymManagementAction(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
) = Surface(
    modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(12.dp))
        Text(label, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

internal fun gymLocation(gym: Gym): String = when {
    gym.name.contains("닻") -> "서울 성동구 · 2.4km"
    gym.name.contains("양재") -> "서울 서초구 · 6.1km"
    gym.name.contains("강남") -> "서울 강남구 · 8.3km"
    gym.name.contains("클라임") -> "서울 마포구 · 11.2km"
    else -> "서울 · 추천 암장"
}

@Composable
internal fun BoardUi(
    state: AppState,
    openCapture: () -> Unit,
    recordSuccessWithoutVideo: (String) -> Unit,
    end: () -> Unit,
) {
    val successes = state.attempts.filter { it.outcome == AttemptOutcome.SUCCESS }
    val gymName = state.gyms.firstOrNull { it.id == state.activeSession?.gymId }?.name ?: "클라이밍 세션"
    Column(Modifier.fillMaxSize().semantics { testTag = "screen-board" }) {
        Column(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(gymName, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(formatElapsed(state.activeSession?.startedAtEpochMillis), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text("●  클라이밍 중", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, top = 8.dp, end = 20.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("색을 탭해서 완등", color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text("어려움 ↑", color = Color(0xFF64748B), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            HoldRows(successes, recordSuccessWithoutVideo)
            Button(
                onClick = openCapture,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(60.dp).semantics { testTag = "cta-open-capture" },
                shape = RoundedCornerShape(18.dp),
            ) { Text("▣  촬영하기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        }
        Row(
            Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = .08f)).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text("${successes.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(" 완등 · 최고 ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text("Lv.5", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Surface(
                Modifier.clickable(onClick = end),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) { Text("클라이밍 종료", Modifier.padding(horizontal = 18.dp, vertical = 12.dp), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
internal fun HoldRows(
    successes: List<Attempt>,
    recordSuccess: (String) -> Unit,
) = Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        listOf("red", "blue", "green", "yellow", "white").forEach { color ->
            val count = successes.count { it.color.equals(color, ignoreCase = true) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { recordSuccess(color) }
                    .background(Brush.verticalGradient(listOf(Color(0xFF1C2434), MaterialTheme.colorScheme.surface)))
                    .border(1.dp, Color.White.copy(alpha = .11f), RoundedCornerShape(16.dp))
                    .semantics {
                    contentDescription = "${holdLabel(color)} 완등 ${count}개"
                    testTag = "hold-row-$color"
                },
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(holdColor(color)))
                Spacer(Modifier.width(14.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(22.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D111A)),
                ) {
                    if (count > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth(if (color == "white") .4f else 1f)
                                .fillMaxHeight()
                                .background(if (color == "green") Color(0xFF2E7A52) else holdColor(color)),
                        )
                        if (color == "blue" || color == "green") {
                            Box(
                                Modifier
                                    .fillMaxWidth(.4f)
                                    .fillMaxHeight()
                                    .background(if (color == "blue") Color(0xFF3E6CB5) else Color(0xFF20573A)),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                    Text("$count", Modifier.width(32.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
}

internal fun formatElapsed(startedAtEpochMillis: Long?): String {
    val elapsed = ((System.currentTimeMillis() - (startedAtEpochMillis ?: System.currentTimeMillis())).coerceAtLeast(0L)) / 1000L
    val hours = elapsed / 3600L
    val minutes = (elapsed % 3600L) / 60L
    val seconds = elapsed % 60L
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

internal fun holdLabel(color: String): String = when (color) {
    "red" -> "빨강"
    "blue" -> "파랑"
    "green" -> "초록"
    "yellow" -> "노랑"
    "white" -> "흰색"
    else -> color
}
