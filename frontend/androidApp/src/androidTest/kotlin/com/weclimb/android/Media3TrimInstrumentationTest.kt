package com.weclimb.android

import android.media.MediaMetadataRetriever
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.weclimb.media.TrimRequest
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Media3TrimInstrumentationTest {
    @Test
    fun createsCompletedMp4ForValidEditListRange() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "trim-instrumentation").apply { mkdirs() }
        val source = Mp4Fixture.create(File(directory, "source.mp4"))
        val output = File(directory, "trimmed.mp4")
        output.delete()
        val completed = CountDownLatch(1)
        var error: String? = null

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Media3EditListExporter(context).export(
                TrimRequest(
                    sourcePath = source.absolutePath,
                    outputPath = output.absolutePath,
                    startMillis = 500,
                    endMillis = 1_500,
                    durationMillis = 2_000,
                ),
                onCompleted = { completed.countDown() },
                onError = { error = it; completed.countDown() },
            )
        }

        assertTrue("trim did not finish", completed.await(30, TimeUnit.SECONDS))
        assertTrue(error ?: "trim output is missing", output.isFile && output.length() > 0L)
        val duration = MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(output.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
        }
        assertTrue("unexpected duration: $duration", duration in 800..1_200)
        assertTrue(kotlin.math.abs(sourceLuma(source, 500) - sourceLuma(output, 0)) <= 20)
        assertTrue(kotlin.math.abs(sourceLuma(source, 1_400) - sourceLuma(output, 900)) <= 20)
    }

    private fun sourceLuma(file: File, timeMillis: Long): Int = MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(file.absolutePath)
        val frame = requireNotNull(
            retriever.getFrameAtTime(timeMillis * 1_000, MediaMetadataRetriever.OPTION_CLOSEST),
        )
        Color.red(frame.getPixel(frame.width / 2, frame.height / 2))
    }
}
