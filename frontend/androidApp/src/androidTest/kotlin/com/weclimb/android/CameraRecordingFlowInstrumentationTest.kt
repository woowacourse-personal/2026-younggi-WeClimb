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
import com.weclimb.session.displayVideoUri
import com.weclimb.session.originalVideoUri
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

        text("세션 진행 중")
    }

    @Test
    fun recordsSuccessfulVideoAndExposesShareAction() {
        recordVideo()

        text("성공").click()

        text("완등 1개")
        text("blue 영상 자르기")
        text("blue 나중에")
        text("blue 원본 유지")
        text("blue 영상 공유").click()
        device.wait(Until.hasObject(By.pkg("com.android.intentresolver")), 15_000)
        device.pressBack()
        text("blue 영상 재생").click()
        device.wait(Until.hasObject(By.clazz("androidx.media3.ui.PlayerView")), 15_000)
    }

    @Test
    fun defersSuccessfulVideoThenOpensArchiveAndTrim() {
        recordVideo()
        text("성공").click()

        text("blue 나중에").click()
        scenario?.recreate()
        text("blue 영상 자르기")
        text("영상 아카이브").click()
        text("클라이밍파크 성수점 · blue · 나중에 자르기")
        text("자르기").click()
        text("영상 자르기")
        text("영상 공유").click()
        device.wait(Until.hasObject(By.pkg("com.android.intentresolver")), 15_000)
        device.pressBack()
        text("원본 유지").click()
        text("세션 진행 중")
    }

    @Test
    fun trimsSuccessfulVideoThroughTheAppAndKeepsOriginalAttempt() {
        recordVideo()
        text("성공").click()
        text("blue 영상 자르기").click()

        val fields = device.wait(Until.findObjects(By.clazz("android.widget.EditText")), 15_000)
        require(fields.size >= 2) { "트리밍 시간 입력칸을 찾을 수 없습니다" }
        fields[0].text = "0"
        fields[1].text = "500"
        text("트리밍 완료").click()

        text("트리밍 영상을 저장했습니다")
        text("영상 아카이브").click()
        text("클라이밍파크 성수점 · blue · 트리밍 완료")

        val repository = RoomSessionLoopRepository(SessionLoopDatabase.create(context).sessionLoopDao())
        val attempt = repository.archiveAttempts().single().attempt
        assertTrue(isReadableVideoUri(context, attempt.originalVideoUri))
        assertTrue(isReadableVideoUri(context, requireNotNull(attempt.displayVideoUri)))
        assertNotEquals(attempt.originalVideoUri, attempt.displayVideoUri)
        assertTrue(attempt.media.state == AttemptMediaState.TRIMMED)
    }

    @Test
    fun keepsRetryAndOriginalActionsAfterAnInvalidTrimRange() {
        recordVideo()
        text("성공").click()
        text("blue 영상 자르기").click()

        val fields = device.wait(Until.findObjects(By.clazz("android.widget.EditText")), 15_000)
        require(fields.size >= 2) { "트리밍 시간 입력칸을 찾을 수 없습니다" }
        fields[0].text = "500"
        fields[1].text = "0"
        text("트리밍 완료").click()

        text("트리밍 구간을 확인하세요")
        text("트리밍 완료")
        text("원본 유지")
    }

    private fun completeOnboardingAndStartSession() {
        text("권한 요청").click()
        text("다음").click()
        text("오늘 어디서 클라이밍할까요?")
        text("암장 선택").click()
        text("클라이밍파크 성수점").click()
        text("세션 진행 중")
        Thread.sleep(2_000)
    }

    private fun recordVideo() {
        text("촬영 시작").click()
        text("녹화 중지")
        Thread.sleep(1_000)
        text("녹화 중지").click()
        text("성공 또는 실패를 선택하세요")
    }

    private fun grantPermission(permission: String) {
        instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} $permission").close()
    }

    private fun text(value: String): UiObject2 {
        device.wait(Until.findObject(By.text(value)), 2_000)?.let { return it }
        val scrollable = device.findObject(By.scrollable(true))
        repeat(4) {
            scrollable?.scroll(Direction.DOWN, 0.8f)
            device.wait(Until.findObject(By.text(value)), 2_000)?.let { return it }
        }
        throw AssertionError("text not found: $value")
    }
}
