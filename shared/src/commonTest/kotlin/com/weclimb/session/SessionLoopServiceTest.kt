package com.weclimb.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SessionLoopServiceTest {
    private val gym = Gym("seed-1", "클라이밍파크 성수점", GymSource.SEEDED)

    @Test
    fun blocksOnboardingWhenCameraPermissionIsMissing() {
        val result = OnboardingService().complete(PermissionState(cameraGranted = false, microphoneGranted = true))

        assertEquals(OnboardingResult.PermissionRequired(Permission.CAMERA), result)
    }

    @Test
    fun completesOnboardingWhenAllPermissionsAreGranted() {
        val result = OnboardingService().complete(PermissionState(cameraGranted = true, microphoneGranted = true))

        assertEquals(OnboardingResult.Completed(AppDestination.Home), result)
    }

    @Test
    fun restoresActiveSessionInsteadOfHome() {
        val session = Session("session-1", gym.id, 1L, status = SessionStatus.ACTIVE)

        assertEquals(AppDestination.SessionBoard(session.id), SessionNavigator().initialDestination(session))
    }

    @Test
    fun keepsSessionBoardWhenHomeIsRequestedDuringActiveSession() {
        val session = Session("session-1", gym.id, 1L, status = SessionStatus.ACTIVE)

        assertEquals(AppDestination.SessionBoard(session.id), SessionNavigator().homeDestination(session))
    }

    @Test
    fun searchesVisibleSeededAndUserAddedGymsIgnoringWhitespaceAndCase() {
        val gyms = listOf(gym, Gym("user-1", "  Peak  Gym ", GymSource.USER_ADDED))

        assertEquals(listOf("user-1"), GymCatalog().search(gyms, "peakgym").map(Gym::id))
        assertEquals(listOf("seed-1"), GymCatalog().search(gyms, "클라이밍 파크").map(Gym::id))
    }

    @Test
    fun addsUserGymWithGeneratedIdWhenNameIsPresent() {
        val result = GymCatalog().addUserGym("새 암장") { "user-1" }

        assertEquals(Gym("user-1", "새 암장", GymSource.USER_ADDED), assertIs<GymAddResult.Added>(result).gym)
    }

    @Test
    fun rejectsBlankUserGymName() {
        assertEquals(GymAddResult.InvalidName, GymCatalog().addUserGym("   ") { "user-1" })
    }

    @Test
    fun renamesUserGymWithoutChangingItsId() {
        val userGym = Gym("user-1", "이전 이름", GymSource.USER_ADDED)

        assertEquals(userGym.copy(name = "새 이름"), GymCatalog().rename(userGym, "새 이름"))
    }

    @Test
    fun hidesUserGymWithSessionHistoryInsteadOfRemovingIt() {
        val userGym = Gym("user-1", "개인 암장", GymSource.USER_ADDED)

        assertTrue(GymCatalog().hide(userGym).hidden)
    }

    @Test
    fun startsOnlyOneActiveSession() {
        val result = SessionNavigator().startSession(gym, activeSession = null, idGenerator = { "session-1" }, now = 1L)

        assertEquals("session-1", assertIs<SessionStartResult.Started>(result).session.id)
    }

    @Test
    fun returnsExistingSessionInsteadOfStartingAnother() {
        val active = Session("session-1", gym.id, 1L, status = SessionStatus.ACTIVE)

        assertEquals(SessionStartResult.AlreadyActive(active), SessionNavigator().startSession(gym, active, { "session-2" }, 2L))
    }
}
