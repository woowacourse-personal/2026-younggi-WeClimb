package com.weclimb.android

import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome
import com.weclimb.session.Gym
import com.weclimb.session.GymSource
import com.weclimb.session.Session
import com.weclimb.session.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionLoopRoomMappingTest {
    @Test
    fun preservesGymSessionAndAttemptFieldsAcrossRoomMappings() {
        val gym = Gym("gym-1", "개인 암장", GymSource.USER_ADDED)
        val session = Session("session-1", gym.id, 10L, status = SessionStatus.ACTIVE)
        val attempt = Attempt("attempt-1", session.id, "blue", 20L, AttemptOutcome.SAVE_PENDING, null, "/cache/attempt.mp4")

        assertEquals(gym, gym.toEntity().toDomain())
        assertEquals(session, session.toEntity().toDomain())
        assertEquals(attempt, attempt.toEntity().toDomain())
    }
}
