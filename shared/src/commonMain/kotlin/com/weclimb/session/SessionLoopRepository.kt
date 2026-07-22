package com.weclimb.session

interface SessionLoopRepository {
    fun saveGuestProfile(): Result<Unit>

    fun hasGuestProfile(): Boolean

    fun gyms(): List<Gym>

    fun saveGym(gym: Gym): Result<Unit>

    fun activeSession(): Session?

    fun saveSession(session: Session): Result<Unit>

    fun attempts(sessionId: String): List<Attempt>

    fun saveAttempt(attempt: Attempt): Result<Unit>
}
