package com.weclimb.android

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.weclimb.session.Attempt
import com.weclimb.session.AttemptOutcome
import com.weclimb.session.Gym
import com.weclimb.session.GymSource
import com.weclimb.session.Session
import com.weclimb.session.SessionLoopRepository
import com.weclimb.session.SessionStatus

@Entity(tableName = "guest_profiles")
data class GuestProfileEntity(@PrimaryKey val id: String = LOCAL_GUEST_ID)

@Entity(tableName = "gyms")
data class GymEntity(@PrimaryKey val id: String, val name: String, val source: String, val hidden: Boolean)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val gymId: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val status: String,
)

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val color: String,
    val recordedAtEpochMillis: Long,
    val outcome: String,
    val videoUri: String?,
    val cachePath: String?,
)

@Dao
interface SessionLoopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveGuestProfile(guestProfile: GuestProfileEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM guest_profiles WHERE id = :id)")
    fun hasGuestProfile(id: String = LOCAL_GUEST_ID): Boolean

    @Query("SELECT * FROM gyms ORDER BY name")
    fun gyms(): List<GymEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveGym(gym: GymEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertGyms(gyms: List<GymEntity>)

    @Query("SELECT * FROM sessions WHERE status = 'ACTIVE' LIMIT 1")
    fun activeSession(): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSession(session: SessionEntity)

    @Query("SELECT * FROM attempts WHERE sessionId = :sessionId ORDER BY recordedAtEpochMillis")
    fun attempts(sessionId: String): List<AttemptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAttempt(attempt: AttemptEntity)
}

@Database(
    entities = [GuestProfileEntity::class, GymEntity::class, SessionEntity::class, AttemptEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SessionLoopDatabase : RoomDatabase() {
    abstract fun sessionLoopDao(): SessionLoopDao

    companion object {
        fun create(context: Context): SessionLoopDatabase = Room.databaseBuilder(
            context.applicationContext,
            SessionLoopDatabase::class.java,
            "we-climb.db",
        ).build()
    }
}

class RoomSessionLoopRepository(private val dao: SessionLoopDao) : SessionLoopRepository {
    override fun saveGuestProfile(): Result<Unit> = runCatching { dao.saveGuestProfile(GuestProfileEntity()) }

    override fun hasGuestProfile(): Boolean = dao.hasGuestProfile()

    override fun gyms(): List<Gym> = dao.gyms().map(GymEntity::toDomain)

    override fun saveGym(gym: Gym): Result<Unit> = runCatching { dao.saveGym(gym.toEntity()) }

    override fun activeSession(): Session? = dao.activeSession()?.toDomain()

    override fun saveSession(session: Session): Result<Unit> = runCatching { dao.saveSession(session.toEntity()) }

    override fun attempts(sessionId: String): List<Attempt> = dao.attempts(sessionId).map(AttemptEntity::toDomain)

    override fun saveAttempt(attempt: Attempt): Result<Unit> = runCatching { dao.saveAttempt(attempt.toEntity()) }

    fun importSeedGyms(seedGyms: List<Gym>) {
        dao.insertGyms(seedGyms.map(Gym::toEntity))
    }
}

fun loadSeedGyms(context: Context): List<Gym> = context.assets.open("seoul-gym-seed.csv")
    .bufferedReader()
    .useLines { lines ->
        lines.drop(1).mapNotNull { line ->
            line.split(',', limit = 2).takeIf { it.size == 2 }?.let { (id, name) ->
                Gym(id = id, name = name, source = GymSource.SEEDED)
            }
        }.toList()
    }

private fun GymEntity.toDomain(): Gym = Gym(id, name, GymSource.valueOf(source), hidden)

private fun Gym.toEntity(): GymEntity = GymEntity(id, name, source.name, hidden)

private fun SessionEntity.toDomain(): Session = Session(id, gymId, startedAtEpochMillis, endedAtEpochMillis, SessionStatus.valueOf(status))

private fun Session.toEntity(): SessionEntity = SessionEntity(id, gymId, startedAtEpochMillis, endedAtEpochMillis, status.name)

private fun AttemptEntity.toDomain(): Attempt = Attempt(id, sessionId, color, recordedAtEpochMillis, AttemptOutcome.valueOf(outcome), videoUri, cachePath)

private fun Attempt.toEntity(): AttemptEntity = AttemptEntity(id, sessionId, color, recordedAtEpochMillis, outcome.name, videoUri, cachePath)

private const val LOCAL_GUEST_ID = "local-guest"
