package com.weclimb.media

data class ShareRequest(
    val action: String,
    val mimeType: String,
    val streamUri: String,
    val grantsReadPermission: Boolean,
)

class ShareRequestFactory {
    fun create(streamUri: String): ShareRequest = ShareRequest(
        action = "android.intent.action.SEND",
        mimeType = "video/*",
        streamUri = streamUri,
        grantsReadPermission = true,
    )
}
