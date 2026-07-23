package com.weclimb.android

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
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
        text("blue 영상 공유").click()
        device.wait(Until.hasObject(By.pkg("com.android.intentresolver")), 15_000)
        device.pressBack()
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

    private fun text(value: String): UiObject2 = device.wait(Until.findObject(By.text(value)), 15_000)
        ?: throw AssertionError("text not found: $value")
}
