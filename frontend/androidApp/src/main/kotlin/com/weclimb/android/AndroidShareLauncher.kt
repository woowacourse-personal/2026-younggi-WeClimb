package com.weclimb.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.weclimb.media.ShareRequest

class AndroidShareLauncher(
    private val context: Context,
) {
    fun launch(request: ShareRequest) {
        context.startActivity(Intent.createChooser(shareIntent(request), "영상 공유"))
    }

    fun shareIntent(request: ShareRequest): Intent {
        val intent = Intent(request.action)
            .setType(request.mimeType)
            .putExtra(Intent.EXTRA_STREAM, Uri.parse(request.streamUri))
        if (request.grantsReadPermission) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return intent
    }
}
