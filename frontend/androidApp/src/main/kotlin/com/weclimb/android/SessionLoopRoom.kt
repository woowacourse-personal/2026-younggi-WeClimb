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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.weclimb.media.AttemptMedia
import com.weclimb.media.AttemptMediaState
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
    val mediaState: String = AttemptMediaState.NONE.name,
    val originalVideoUri: String? = null,
    val trimmedVideoUri: String? = null,
    val mediaErrorMessage: String? = null,
)

data class ArchiveAttemptRow(
    val id: String,
    val sessionId: String,
    val color: String,
    val recordedAtEpochMillis: Long,
    val videoUri: String?,
    val mediaState: String,
    val originalVideoUri: String?,
    val trimmedVideoUri: String?,
    val mediaErrorMessage: String?,
    val gymName: String,
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

    @Query("""
        SELECT attempts.id, attempts.sessionId, attempts.color, attempts.recordedAtEpochMillis,
            attempts.videoUri, attempts.mediaState, attempts.originalVideoUri,
            attempts.trimmedVideoUri, attempts.mediaErrorMessage, gyms.name AS gymName
        FROM attempts
        JOIN sessions ON sessions.id = attempts.sessionId
        JOIN gyms ON gyms.id = sessions.gymId
        WHERE attempts.outcome = 'SUCCESS'
        ORDER BY attempts.recordedAtEpochMillis DESC
    """)
    fun archiveAttempts(): List<ArchiveAttemptRow>

    @Query("UPDATE attempts SET mediaState = 'TRIM_FAILED', trimmedVideoUri = NULL, mediaErrorMessage = :message WHERE mediaState = 'TRIM_PROCESSING'")
    fun recoverInterruptedTrims(message: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAttempt(attempt: AttemptEntity)

    @Query("DELETE FROM attempts WHERE id IN (:ids)")
    fun deleteAttempts(ids: List<String>)
}

@Database(
    entities = [GuestProfileEntity::class, GymEntity::class, SessionEntity::class, AttemptEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class SessionLoopDatabase : RoomDatabase() {
    abstract fun sessionLoopDao(): SessionLoopDao

    companion object {
        fun create(context: Context): SessionLoopDatabase = Room.databaseBuilder(
            context.applicationContext,
            SessionLoopDatabase::class.java,
            "we-climb.db",
        ).addMigrations(MIGRATION_1_2).build()
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE attempts ADD COLUMN mediaState TEXT NOT NULL DEFAULT 'NONE'")
        database.execSQL("ALTER TABLE attempts ADD COLUMN originalVideoUri TEXT")
        database.execSQL("ALTER TABLE attempts ADD COLUMN trimmedVideoUri TEXT")
        database.execSQL("ALTER TABLE attempts ADD COLUMN mediaErrorMessage TEXT")
        database.execSQL("UPDATE attempts SET originalVideoUri = videoUri, mediaState = 'TRIM_PENDING' WHERE outcome = 'SUCCESS' AND videoUri IS NOT NULL")
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

    fun archiveAttempts(): List<ArchiveAttempt> = dao.archiveAttempts().map(ArchiveAttemptRow::toArchiveAttempt)

    fun recoverInterruptedTrims(): Result<Unit> = runCatching {
        dao.recoverInterruptedTrims("트리밍이 중단되었습니다")
    }

    override fun saveAttempt(attempt: Attempt): Result<Unit> = runCatching { dao.saveAttempt(attempt.toEntity()) }

    override fun deleteAttempts(ids: List<String>): Result<Unit> = runCatching {
        if (ids.isNotEmpty()) dao.deleteAttempts(ids)
    }

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

internal fun GymEntity.toDomain(): Gym = Gym(id, name, GymSource.valueOf(source), hidden)

internal fun Gym.toEntity(): GymEntity = GymEntity(id, name, source.name, hidden)

internal fun SessionEntity.toDomain(): Session = Session(id, gymId, startedAtEpochMillis, endedAtEpochMillis, SessionStatus.valueOf(status))

internal fun Session.toEntity(): SessionEntity = SessionEntity(id, gymId, startedAtEpochMillis, endedAtEpochMillis, status.name)

internal fun AttemptEntity.toDomain(): Attempt = Attempt(
    id = id,
    sessionId = sessionId,
    color = color,
    recordedAtEpochMillis = recordedAtEpochMillis,
    outcome = AttemptOutcome.valueOf(outcome),
    videoUri = videoUri,
    cachePath = cachePath,
    media = mediaFromColumns(mediaState, originalVideoUri, trimmedVideoUri, mediaErrorMessage, videoUri),
)

internal fun Attempt.toEntity(): AttemptEntity = AttemptEntity(
    id = id,
    sessionId = sessionId,
    color = color,
    recordedAtEpochMillis = recordedAtEpochMillis,
    outcome = outcome.name,
    videoUri = videoUri,
    cachePath = cachePath,
    mediaState = media.state.name,
    originalVideoUri = media.originalVideoUri.ifBlank { null },
    trimmedVideoUri = media.trimmedVideoUri,
    mediaErrorMessage = media.errorMessage,
)

data class ArchiveAttempt(
    val attempt: Attempt,
    val gymName: String,
)

internal fun ArchiveAttemptRow.toArchiveAttempt(): ArchiveAttempt = ArchiveAttempt(
    attempt = Attempt(
        id = id,
        sessionId = sessionId,
        color = color,
        recordedAtEpochMillis = recordedAtEpochMillis,
        outcome = AttemptOutcome.SUCCESS,
        videoUri = videoUri,
        cachePath = null,
        media = mediaFromColumns(mediaState, originalVideoUri, trimmedVideoUri, mediaErrorMessage, videoUri),
    ),
    gymName = gymName,
)

private fun mediaFromColumns(
    state: String,
    originalVideoUri: String?,
    trimmedVideoUri: String?,
    errorMessage: String?,
    videoUri: String?,
): AttemptMedia {
    val original = originalVideoUri ?: videoUri ?: return AttemptMedia.none()
    return AttemptMedia(
        state = runCatching { AttemptMediaState.valueOf(state) }.getOrDefault(AttemptMediaState.TRIM_PENDING),
        originalVideoUri = original,
        trimmedVideoUri = trimmedVideoUri,
        errorMessage = errorMessage,
    )
}

private const val LOCAL_GUEST_ID = "local-guest"
