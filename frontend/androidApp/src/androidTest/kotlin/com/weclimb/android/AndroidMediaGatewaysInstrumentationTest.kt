package com.weclimb.android

import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.weclimb.media.VideoPersistence
import com.weclimb.session.AttemptService
import com.weclimb.session.Session
import com.weclimb.session.SessionStatus
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidMediaGatewaysInstrumentationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun savesPlayableVideoUnderWeClimbMoviesAndReturnsReadableUri() {
        val source = Mp4Fixture.create(File(context.cacheDir, "media-store-success.mp4"))
        val result = AndroidMediaStoreGateway(context).save(source.absolutePath).getOrThrow()

        val uri = android.net.Uri.parse(result)
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                assertNotNull(input)
                assertTrue(requireNotNull(input).read() >= 0)
            }
            context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.Video.Media.RELATIVE_PATH), null, null, null).use { cursor ->
                assertTrue(requireNotNull(cursor).moveToFirst())
                assertEquals(
                    "${Environment.DIRECTORY_MOVIES}/WeClimb/",
                    cursor.getString(0),
                )
            }
        } finally {
            context.contentResolver.delete(uri, null, null)
            source.delete()
        }
    }

    @Test
    fun deletesOnlyFailedCacheFiles() {
        val directory = File(context.cacheDir, "cache-delete").apply { mkdirs() }
        val failedOne = File(directory, "failed-one.mp4").apply { writeText("failed") }
        val failedTwo = File(directory, "failed-two.mp4").apply { writeText("failed") }
        val successful = File(directory, "successful.mp4").apply { writeText("success") }

        VideoPersistence(mediaStore = UnusedMediaStoreGateway, cache = AndroidCacheGateway()).deleteFailed(
            listOf(failedOne.absolutePath, failedTwo.absolutePath),
        )

        assertTrue(!failedOne.exists())
        assertTrue(!failedTwo.exists())
        assertTrue(successful.isFile)
        successful.delete()
        directory.delete()
    }

    @Test
    fun preservesCachePathAndFailureReasonWhenMediaStoreSaveFails() {
        val missingSource = File(context.cacheDir, "missing-media-store-source.mp4")
        missingSource.delete()
        val session = Session("session-1", "gym-1", 1L, status = SessionStatus.ACTIVE)

        val result = AttemptService(AndroidMediaStoreGateway(context)).recordSuccess(
            session = session,
            color = "blue",
            cachePath = missingSource.absolutePath,
            recordedAtEpochMillis = 2L,
        )

        assertEquals(missingSource.absolutePath, result.attempt.cachePath)
        assertNotNull(result.saveErrorMessage)
        assertTrue(!missingSource.exists())
    }
}

private object UnusedMediaStoreGateway : com.weclimb.media.MediaStoreGateway {
    override fun save(path: String): Result<String> = error("MediaStore save is not expected")
}
