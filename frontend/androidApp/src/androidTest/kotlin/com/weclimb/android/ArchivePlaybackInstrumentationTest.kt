package com.weclimb.android

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.weclimb.media.AttemptMedia
import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome
import com.weclimb.session.Gym
import com.weclimb.session.GymSource
import com.weclimb.session.Session
import com.weclimb.session.SessionStatus
import java.text.DateFormat
import java.util.Date
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArchivePlaybackInstrumentationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        context.deleteDatabase("we-climb.db")
        val repository = RoomSessionLoopRepository(SessionLoopDatabase.create(context).sessionLoopDao())
        val gym = Gym("gym-1", "테스트 암장", GymSource.USER_ADDED)
        val recordedAt = 1_700_000_000_000L
        repository.saveGuestProfile().getOrThrow()
        repository.saveGym(gym).getOrThrow()
        repository.saveSession(Session("session-1", gym.id, recordedAt, status = SessionStatus.ENDED)).getOrThrow()
        repository.saveAttempt(
            Attempt(
                id = "missing-video",
                sessionId = "session-1",
                color = "blue",
                recordedAtEpochMillis = recordedAt,
                outcome = AttemptOutcome.SUCCESS,
                videoUri = "content://media/external/video/media/does-not-exist",
                cachePath = null,
                media = AttemptMedia.pending("content://media/external/video/media/does-not-exist"),
            ),
        ).getOrThrow()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun keepsUnreadableArchiveAttemptAndShowsItsPlaybackPlaceholder() {
        text("영상 아카이브").click()

        text("테스트 암장")
        text("blue 완등")
        text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(1_700_000_000_000L)))
        text("재생").click()
        text("재생 불가 · 기기에서 영상을 읽을 수 없습니다")
        text("테스트 암장")
    }

    private fun text(value: String) = device.wait(Until.findObject(By.text(value)), 15_000)
        ?: throw AssertionError("text not found: $value")
}
