package com.weclimb.android

import android.content.Intent
import com.weclimb.media.ShareRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AndroidShareLauncherTest {
    @Test
    fun createsSingleVideoSendIntentWithReadPermission() {
        val request = ShareRequest(
            action = Intent.ACTION_SEND,
            mimeType = "video/*",
            streamUri = "content://media/external/video/media/42",
            grantsReadPermission = true,
        )

        val intent = AndroidShareLauncher(RuntimeEnvironment.getApplication()).shareIntent(request)

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("video/*", intent.type)
        assertEquals(request.streamUri, intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.toString())
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
