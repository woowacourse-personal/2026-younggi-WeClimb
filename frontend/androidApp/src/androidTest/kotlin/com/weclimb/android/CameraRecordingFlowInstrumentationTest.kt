package com.weclimb.android

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.weclimb.media.AttemptMediaState
import com.weclimb.session.AttemptOutcome
import com.weclimb.session.displayVideoUri
import com.weclimb.session.originalVideoUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraRecordingFlowInstrumentationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        grantPermission("android.permission.CAMERA")
        grantPermission("android.permission.RECORD_AUDIO")
        context.deleteDatabase("we-climb.db")
        context.cacheDir.listFiles()?.forEach { it.delete() }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        completeOnboardingAndStartSession()
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun recordsAndClassifiesFailedVideoThroughCameraX() {
        recordVideo()

        text("실패").click()

        text("●  클라이밍 중")
        val repository = RoomSessionLoopRepository(SessionLoopDatabase.create(context).sessionLoopDao())
        val session = requireNotNull(repository.activeSession())
        val failure = repository.attempts(session.id).single()
        assertEquals(AttemptOutcome.FAILURE, failure.outcome)
        val failedCache = requireNotNull(failure.cachePath)
        assertTrue(java.io.File(failedCache).exists())

        text("클라이밍 종료").click()
        text("종료하기").click()
        text("클라이밍 시작")
        assertFalse(java.io.File(failedCache).exists())
        assertTrue(repository.attempts(session.id).isEmpty())
    }

    @Test
    fun recordsSuccessfulVideoAndExposesShareAction() {
        recordVideo()

        text("성공").click()

        text("지금 자르기")
        text("나중에")
        text("원본 그대로").click()
        text("●  클라이밍 중")
        text("클라이밍 종료").click()
        text("종료하기").click()
        text("영상").click()
        text("원본 유지")
        text("공유").click()
        device.wait(Until.hasObject(By.pkg("com.android.intentresolver")), 15_000)
        device.pressBack()
        text("재생").click()
        device.wait(Until.hasObject(By.clazz("androidx.media3.ui.PlayerView")), 15_000)
    }

    @Test
    fun defersSuccessfulVideoThenOpensArchiveAndTrim() {
        recordVideo()
        text("성공").click()

        text("나중에").click()
        text("blue 영상은 아카이브에서 나중에 자를 수 있습니다")
        text("클라이밍 종료").click()
        text("종료하기").click()
        text("영상").click()
        text("파랑 · Lv.5")
        text("이어서 자르기").click()
        text("앞뒤 자르기")
        text("원본 유지").click()
        text("클라이밍 시작")
    }

    @Test
    fun trimsSuccessfulVideoThroughTheAppAndKeepsOriginalAttempt() {
        recordVideo()
        text("성공").click()
        text("지금 자르기").click()

        text("자르고 저장").click()

        text("트리밍 완료")
        text("0:12로 저장했어요 · 아카이브에 추가됨")
        text("공유")
        text("아카이브로 이동").click()
        text("파랑 · Lv.5")

        val repository = RoomSessionLoopRepository(SessionLoopDatabase.create(context).sessionLoopDao())
        val attempt = repository.archiveAttempts().single().attempt
        assertTrue(isReadableVideoUri(context, attempt.originalVideoUri))
        assertTrue(isReadableVideoUri(context, requireNotNull(attempt.displayVideoUri)))
        assertNotEquals(attempt.originalVideoUri, attempt.displayVideoUri)
        assertTrue(attempt.media.state == AttemptMediaState.TRIMMED)
    }

    private fun completeOnboardingAndStartSession() {
        text("카메라·마이크 허용하기").click()
        text("시작하기").click()
        text("클라이밍 시작").click()
        text("클라이밍파크 성수점").click()
        text("클라이밍파크 성수점에서 시작").click()
        text("●  클라이밍 중")
        Thread.sleep(2_000)
    }

    private fun recordVideo() {
        text("▣  촬영하기").click()
        description("녹화 시작").click()
        description("녹화 중지")
        Thread.sleep(1_000)
        description("녹화 중지").click()
        text("방금 시도, 성공했나요?")
    }

    private fun grantPermission(permission: String) {
        instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} $permission").close()
    }

    private fun text(value: String): UiObject2 {
        device.wait(Until.findObject(By.text(value)), 2_000)?.let { return it }
        val scrollable = device.findObject(By.scrollable(true))
        repeat(4) {
            scrollable?.scroll(Direction.DOWN, 0.8f)
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 3, 16)
            device.wait(Until.findObject(By.text(value)), 2_000)?.let { return it }
        }
        throw AssertionError("text not found: $value")
    }

    private fun description(value: String): UiObject2 =
        device.wait(Until.findObject(By.desc(value)), 15_000)
            ?: throw AssertionError("content description not found: $value")
}
