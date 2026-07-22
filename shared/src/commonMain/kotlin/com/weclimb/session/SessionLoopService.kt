package com.weclimb.session

enum class Permission {
    CAMERA,
    MICROPHONE,
}

data class PermissionState(
    val cameraGranted: Boolean,
    val microphoneGranted: Boolean,
)

sealed interface AppDestination {
    data object Home : AppDestination

    data class SessionBoard(val sessionId: String) : AppDestination
}

sealed interface OnboardingResult {
    data class PermissionRequired(val permission: Permission) : OnboardingResult

    data class Completed(val destination: AppDestination) : OnboardingResult
}

class OnboardingService {
    fun complete(permissions: PermissionState): OnboardingResult = when {
        !permissions.cameraGranted -> OnboardingResult.PermissionRequired(Permission.CAMERA)
        !permissions.microphoneGranted -> OnboardingResult.PermissionRequired(Permission.MICROPHONE)
        else -> OnboardingResult.Completed(AppDestination.Home)
    }
}

enum class GymSource {
    SEEDED,
    USER_ADDED,
}

data class Gym(
    val id: String,
    val name: String,
    val source: GymSource,
    val hidden: Boolean = false,
)

sealed interface GymAddResult {
    data class Added(val gym: Gym) : GymAddResult

    data object InvalidName : GymAddResult
}

class GymCatalog {
    fun search(gyms: List<Gym>, query: String): List<Gym> {
        val normalizedQuery = normalize(query)
        return gyms.filter { gym ->
            !gym.hidden && (normalizedQuery.isEmpty() || normalize(gym.name).contains(normalizedQuery))
        }
    }

    fun addUserGym(name: String, idGenerator: () -> String): GymAddResult {
        val trimmedName = name.trim()
        return if (trimmedName.isEmpty()) {
            GymAddResult.InvalidName
        } else {
            GymAddResult.Added(Gym(idGenerator(), trimmedName, GymSource.USER_ADDED))
        }
    }

    fun rename(gym: Gym, name: String): Gym = gym.copy(name = name.trim())

    fun hide(gym: Gym): Gym = gym.copy(hidden = true)

    private fun normalize(value: String): String = value.filterNot(Char::isWhitespace).lowercase()
}

enum class SessionStatus {
    ACTIVE,
    ENDED,
}

data class Session(
    val id: String,
    val gymId: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long? = null,
    val status: SessionStatus,
)

sealed interface SessionStartResult {
    data class Started(val session: Session) : SessionStartResult

    data class AlreadyActive(val session: Session) : SessionStartResult
}

class SessionNavigator {
    fun initialDestination(activeSession: Session?): AppDestination = activeSession
        ?.takeIf { it.status == SessionStatus.ACTIVE }
        ?.let { AppDestination.SessionBoard(it.id) }
        ?: AppDestination.Home

    fun homeDestination(activeSession: Session?): AppDestination = initialDestination(activeSession)

    fun startSession(
        gym: Gym,
        activeSession: Session?,
        idGenerator: () -> String,
        now: Long,
    ): SessionStartResult = activeSession
        ?.takeIf { it.status == SessionStatus.ACTIVE }
        ?.let(SessionStartResult::AlreadyActive)
        ?: SessionStartResult.Started(
            Session(
                id = idGenerator(),
                gymId = gym.id,
                startedAtEpochMillis = now,
                status = SessionStatus.ACTIVE,
            ),
        )
}
