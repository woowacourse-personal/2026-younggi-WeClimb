package com.weclimb.android

import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.weclimb.media.AttemptMedia
import com.weclimb.media.AttemptMediaState
import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome
import com.weclimb.session.Gym
import com.weclimb.session.GymSource
import com.weclimb.session.Session
import com.weclimb.session.SessionStatus
import java.io.File
import java.util.regex.Pattern
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiRebuildInstrumentationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private var scenario: ActivityScenario<*>? = null
    private lateinit var database: SessionLoopDatabase
    private lateinit var sessionRepository: RoomSessionLoopRepository

    @Before
    fun setUp() {
        grantPermission("android.permission.CAMERA")
        grantPermission("android.permission.RECORD_AUDIO")
        context.deleteDatabase("we-climb.db")
        database = SessionLoopDatabase.create(context)
        sessionRepository = RoomSessionLoopRepository(database.sessionLoopDao())
    }

    @After
    fun tearDown() {
        scenario?.close()
        database.close()
    }

    @Test
    fun reproducesApprovedStatesOnThe390DpCatalogSurface() {
        val approvedStates = linkedMapOf(
            "Loading" to "불러오는 중…",
            "LoadingSuccess" to "영상을 저장했어요",
            "LoadingError" to "저장에 실패했어요",
            "PermissionSettings" to "설정 열기",
            "OnboardingRequest" to "카메라·마이크 허용하기",
            "OnboardingDenied" to "다시 요청하기",
            "OnboardingGranted" to "시작하기",
            "Home" to "클라이밍 시작",
            "Gyms" to "어디서 클라이밍해요?",
            "Board" to "색을 탭해서 완등",
            "BoardDialog" to "클라이밍을 종료할까요?",
            "CapturePreparing" to "카메라를 준비하고 있어요",
            "CaptureError" to "카메라를 열 수 없어요",
            "CaptureReady" to "파랑",
            "CaptureRecording" to "파랑",
            "CaptureClassify" to "방금 시도, 성공했나요?",
            "MediaChoice" to "이 영상, 어떻게 할까요?",
            "Trim" to "앞뒤 자르기",
            "TrimInvalid" to "선택 구간을 확인해 주세요",
            "TrimProcessing" to "영상을 저장하고 있어요",
            "TrimFailed" to "저장에 실패했어요 — 원본은 그대로 있어요",
            "TrimCompleted" to "트리밍 완료",
            "Archive" to "영상 아카이브",
            "Playback" to "영상 공유",
            "SessionEndPreview" to "▣  오늘의 한 컷 찍기",
            "ReportPreview" to "이번 세션 리포트",
            "RecordsPreview" to "내 기록",
        )

        approvedStates.forEach { (state, expectedText) ->
            launchCatalog(state)
            assertEquals(
                390f,
                device.displayWidth / targetCatalogDensity(device.displayWidth),
                0.01f,
            )
            text(expectedText)
            val screenshot = File(context.cacheDir, "catalog-$state.png")
            assertTrue("screenshot failed for $state", device.takeScreenshot(screenshot))
            assertTrue("empty screenshot for $state", screenshot.length() > 0L)
            screenshot.delete()
            scenario?.close()
            scenario = null
        }
    }

    @Test
    fun trimTimelineUsesFramedSelectionWithEdgeHandles() {
        launchCatalog("Trim")

        val timeline = description("트리밍 프레임 타임라인")
        val selection = description("트리밍 선택 구간")
        val startControl = description("범위 시작")
        val startHandle = description("범위 시작 손잡이")
        val endHandle = description("범위 끝 손잡이")
        text("선택 0:04 – 0:16")

        assertTrue(selection.visibleBounds.left > timeline.visibleBounds.left)
        assertTrue(selection.visibleBounds.right < timeline.visibleBounds.right)
        assertTrue(startHandle.visibleBounds.width() < startHandle.visibleBounds.height())
        assertTrue(endHandle.visibleBounds.width() < endHandle.visibleBounds.height())
        assertTrue(startHandle.visibleBounds.centerX() <= selection.visibleBounds.left)
        assertTrue(endHandle.visibleBounds.centerX() >= selection.visibleBounds.right)

        val actionArguments = Bundle().apply {
            putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, 0.4f)
        }
        assertTrue(
            accessibilityNode("범위 시작").performAction(
                AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,
                actionArguments,
            ),
        )

        assertNotNull(
            device.wait(
                Until.findObject(By.text(Pattern.compile("선택 (?!0:04 – 0:16).*"))),
                5_000,
            ),
        )
    }

    @Test
    fun trimProcessingBlocksDuplicateSubmissionAndFailureOffersRetry() {
        launchCatalog("TrimProcessing")

        assertNull(device.findObject(By.text("자르고 저장")))
        text("완료 전까지 중복 실행할 수 없어요")
        device.pressBack()
        resource("screen-board")

        launchCatalog("TrimFailed")
        text("다시 시도")
        val retry = resource("cta-submit-trim")
        assertTrue("failed trim retry must be enabled", retry.isEnabled)
        assertTrue("failed trim retry must be clickable", retry.isClickable)
    }

    @Test
    fun statusBannersExposeExplicitSeverityAndRetry() {
        launchCatalog("LoadingSuccess")

        resource("status-banner")
        text("영상을 저장했어요")
        text("아카이브에서 다시 볼 수 있어요")
        assertNull(device.findObject(By.text("다시 시도")))

        launchCatalog("LoadingError")

        resource("status-banner")
        text("저장에 실패했어요")
        text("원본은 그대로 있어요")
        text("다시 시도")
        val retry = resource("cta-retry-status")
        assertTrue(retry.isEnabled)
        assertTrue(retry.isClickable)
    }

    @Test
    fun addsSelectsRenamesAndHidesAPersonalGym() {
        saveGuestProfile()
        launchMain()

        text("클라이밍 시작").click()
        val search = editable()
        search.text = "새 암장"
        textContains("찾는 암장이 없어요").click()
        text("추가하고 선택").click()
        text("새 암장에서 시작")

        assertTrue(repository().gyms().any { it.name == "새 암장" && it.source == GymSource.USER_ADDED })

        text("⋮").click()
        text("이름 수정").click()
        val nameInput = editableWithText("새 암장")
        nameInput.text = "고친 암장"
        text("이름 저장").click()
        text("고친 암장")
        text("고친 암장에서 시작")
        assertTrue(repository().gyms().any { it.name == "고친 암장" && !it.hidden })

        text("⋮").click()
        text("목록에서 숨기기").click()
        assertTrue(device.wait(Until.gone(By.text("고친 암장")), 5_000))
        assertTrue(device.wait(Until.gone(By.text("고친 암장에서 시작")), 5_000))
        assertTrue(repository().gyms().any { it.name == "고친 암장" && it.hidden })
    }

    @Test
    fun recordsVideoLessSuccessFromTheBoardAndRefreshesTheCount() {
        val session = saveActiveSession()
        launchMain()

        val blue = device.wait(Until.findObject(By.desc("파랑 완등 0개")), 5_000)
            ?: throw AssertionError("blue success row not found")
        blue.click()
        assertNotNull(device.wait(Until.findObject(By.desc("파랑 완등 1개")), 5_000))

        val attempts = repository().attempts(session.id)
        assertEquals(1, attempts.size)
        assertEquals("blue", attempts.single().color)
        assertEquals(AttemptOutcome.SUCCESS, attempts.single().outcome)
        assertNull(attempts.single().videoUri)
        assertNull(attempts.single().cachePath)
        assertEquals(AttemptMediaState.NONE, attempts.single().media.state)
    }

    @Test
    fun endsTheSessionOnlyAfterExplicitConfirmation() {
        saveActiveSession()
        launchMain()

        text("클라이밍 종료").click()
        text("클라이밍을 종료할까요?")
        assertNotNull(repository().activeSession())

        text("계속 클라이밍").click()
        text("●  클라이밍 중")
        assertNotNull(repository().activeSession())

        text("클라이밍 종료").click()
        text("종료하기").click()
        text("클라이밍 시작")
        assertNull(repository().activeSession())
    }

    @Test
    fun endSessionDialogSummarizesActualSuccessAndPendingTrimCounts() {
        val session = saveActiveSession()
        repository().saveAttempt(
            Attempt(
                id = "success-without-video",
                sessionId = session.id,
                color = "red",
                recordedAtEpochMillis = 1L,
                outcome = AttemptOutcome.SUCCESS,
                videoUri = null,
                cachePath = null,
                media = AttemptMedia.none(),
            ),
        ).getOrThrow()
        repository().saveAttempt(
            Attempt(
                id = "success-pending-trim",
                sessionId = session.id,
                color = "blue",
                recordedAtEpochMillis = 2L,
                outcome = AttemptOutcome.SUCCESS,
                videoUri = "content://video/original",
                cachePath = null,
                media = AttemptMedia.pending("content://video/original"),
            ),
        ).getOrThrow()
        repository().saveAttempt(
            Attempt(
                id = "failed-attempt",
                sessionId = session.id,
                color = "green",
                recordedAtEpochMillis = 3L,
                outcome = AttemptOutcome.FAILURE,
                videoUri = null,
                cachePath = "/cache/failed.mp4",
            ),
        ).getOrThrow()
        repository().saveAttempt(
            Attempt(
                id = "unclassified-attempt",
                sessionId = session.id,
                color = "yellow",
                recordedAtEpochMillis = 4L,
                outcome = AttemptOutcome.UNCLASSIFIED,
                videoUri = null,
                cachePath = "/cache/unclassified.mp4",
            ),
        ).getOrThrow()
        repository().saveAttempt(
            Attempt(
                id = "failed-trim",
                sessionId = session.id,
                color = "white",
                recordedAtEpochMillis = 5L,
                outcome = AttemptOutcome.SUCCESS,
                videoUri = "content://video/failed",
                cachePath = null,
                media = AttemptMedia(AttemptMediaState.TRIM_FAILED, "content://video/failed"),
            ),
        ).getOrThrow()
        repository().saveAttempt(
            Attempt(
                id = "processing-trim",
                sessionId = session.id,
                color = "purple",
                recordedAtEpochMillis = 6L,
                outcome = AttemptOutcome.SUCCESS,
                videoUri = "content://video/processing",
                cachePath = null,
                media = AttemptMedia(AttemptMediaState.TRIM_PROCESSING, "content://video/processing"),
            ),
        ).getOrThrow()
        launchMain()

        text("클라이밍 종료").click()

        description("완등 4")
        description("전체 시도 6")
        description("정리 필요 3")
        assertNull(device.findObject(By.text("Lv.5")))
    }

    @Test
    fun blocksSessionEndUntilPendingSaveIsRetriedOrDiscarded() {
        val session = saveActiveSession()
        val cache = File(context.cacheDir, "pending-save.mp4").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        repository().saveAttempt(
            Attempt(
                id = "pending-save",
                sessionId = session.id,
                color = "blue",
                recordedAtEpochMillis = 1L,
                outcome = AttemptOutcome.SAVE_PENDING,
                videoUri = null,
                cachePath = cache.absolutePath,
            ),
        ).getOrThrow()
        launchMain()

        text("클라이밍 종료").click()

        text("저장하지 못한 영상이 있어요")
        text("다시 저장")
        text("영상만 폐기하고 기록 유지").click()
        assertFalse(cache.exists())
        assertNotNull(repository().activeSession())
        val retained = repository().attempts(session.id).single()
        assertEquals(AttemptOutcome.SUCCESS, retained.outcome)
        assertNull(retained.videoUri)

        text("클라이밍 종료").click()
        text("종료하기").click()
        text("클라이밍 시작")
        assertNull(repository().activeSession())
    }

    @Test
    fun disablesClassificationActionsAfterTheFirstInput() {
        launchCatalog("CaptureClassifying")

        assertFalse(resource("cta-classify-success").isEnabled)
        assertFalse(resource("cta-classify-failure").isEnabled)
    }

    @Test
    fun keepsExampleFiltersAndPhaseThreeActionsInactive() {
        val gymsBefore = repository().gyms()
        val activeSessionBefore = repository().activeSession()

        launchCatalog("Archive")
        val filter = text("트리밍 완료")
        assertFalse("archive example filter must stay inert", filter.isClickable)
        text("초록 · Lv.4")
        text("노랑 · Lv.3")
        text("빨강 · Lv.1")

        launchCatalog("SessionEndPreview")
        text("리포트 만들기").click()
        text("✓  세션 종료")
        assertTrue(repository().gyms().isEmpty())

        launchCatalog("ReportPreview")
        text("인스타 스토리로 공유").click()
        text("이번 세션 리포트")

        launchCatalog("RecordsPreview")
        assertFalse(text("전체 보기 ›").isClickable)
        text("통계·성장 곡선은 Phase 3에서 활성화 · 지금은 표시만")

        assertEquals(gymsBefore, repository().gyms())
        assertEquals(activeSessionBefore, repository().activeSession())
        assertTrue(repository().archiveAttempts().isEmpty())
    }

    @Test
    fun exposesStableSemanticResourceIdsForApprovedSurfaces() {
        val screenTags = linkedMapOf(
            "Loading" to "screen-loading",
            "OnboardingGranted" to "screen-onboarding",
            "Home" to "screen-home",
            "Gyms" to "screen-gyms",
            "Board" to "screen-board",
            "CaptureReady" to "screen-capture",
            "Trim" to "screen-trim",
            "Archive" to "screen-archive",
            "SessionEndPreview" to "screen-static-session-end",
            "ReportPreview" to "screen-static-report",
            "RecordsPreview" to "screen-static-records",
        )

        screenTags.forEach { (state, tag) ->
            launchCatalog(state)
            resource(tag)
        }

        listOf(
            "LoadingError" to "status-banner",
            "MediaChoice" to "sheet-media-choice",
            "BoardDialog" to "dialog-end-session",
            "Trim" to "player-trim-preview",
            "Playback" to "screen-playback",
        ).forEach { (state, tag) ->
            launchCatalog(state)
            resource(tag)
        }
    }

    private fun saveGuestProfile() {
        repository().saveGuestProfile().getOrThrow()
    }

    private fun saveActiveSession(): Session {
        val repository = repository()
        val gym = Gym("active-gym", "닻 클라이밍", GymSource.USER_ADDED)
        val session = Session(
            id = "active-session",
            gymId = gym.id,
            startedAtEpochMillis = System.currentTimeMillis() - 60_000L,
            status = SessionStatus.ACTIVE,
        )
        repository.saveGuestProfile().getOrThrow()
        repository.saveGym(gym).getOrThrow()
        repository.saveSession(session).getOrThrow()
        return session
    }

    private fun repository() = sessionRepository

    private fun launchMain() {
        scenario?.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun launchCatalog(state: String) {
        scenario?.close()
        val intent = Intent(context, UiCatalogActivity::class.java).putExtra("state", state)
        scenario = ActivityScenario.launch<UiCatalogActivity>(intent)
    }

    private fun grantPermission(permission: String) {
        instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} $permission").close()
    }

    private fun text(value: String): UiObject2 {
        device.wait(Until.findObject(By.text(value)), 3_000)?.let { return it }
        repeat(5) {
            device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 0.8f)
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 3 / 4,
                device.displayWidth / 2,
                device.displayHeight / 3,
                16,
            )
            device.wait(Until.findObject(By.text(value)), 2_000)?.let { return it }
        }
        throw AssertionError("text not found: $value")
    }

    private fun textContains(value: String): UiObject2 =
        device.wait(Until.findObject(By.textContains(value)), 5_000)
            ?: throw AssertionError("text containing value not found: $value")

    private fun description(value: String): UiObject2 =
        device.wait(Until.findObject(By.desc(value)), 5_000)
            ?: throw AssertionError("content description not found: $value")

    private fun resource(value: String): UiObject2 =
        device.wait(Until.findObject(By.res(Pattern.compile(Pattern.quote(value)))), 5_000)
            ?: throw AssertionError("semantic resource id not found: $value")

    private fun accessibilityNode(description: String): AccessibilityNodeInfo {
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(instrumentation.uiAutomation.rootInActiveWindow)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            if (node.contentDescription?.toString() == description) {
                return node
            }
            repeat(node.childCount) { index ->
                node.getChild(index)?.let(pending::add)
            }
        }
        throw AssertionError("accessibility node not found: $description")
    }

    private fun editable(): UiObject2 =
        device.wait(Until.findObject(By.clazz("android.widget.EditText")), 5_000)
            ?: throw AssertionError("editable field not found")

    private fun editableWithText(value: String): UiObject2 =
        device.wait(Until.findObject(By.clazz("android.widget.EditText").text(value)), 5_000)
            ?: throw AssertionError("editable field not found with text: $value")
}
