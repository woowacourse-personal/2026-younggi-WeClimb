package com.weclimb.android

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.weclimb.media.CacheGateway
import com.weclimb.media.MediaStoreGateway
import java.io.File

class AndroidMediaStoreGateway(
    private val context: Context,
) : MediaStoreGateway {
    override fun save(path: String): Result<String> = runCatching {
        val source = File(path)
        check(source.isFile) { "저장할 영상을 찾을 수 없습니다" }
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, source.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/WeClimb")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = requireNotNull(
            resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values),
        ) { "MediaStore 항목을 만들 수 없습니다" }
        try {
            resolver.openOutputStream(uri).use { output ->
                requireNotNull(output) { "MediaStore 출력 스트림을 열 수 없습니다" }
                source.inputStream().use { input -> input.copyTo(output) }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri.toString()
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }
}

class AndroidCacheGateway : CacheGateway {
    override fun delete(path: String) {
        File(path).delete()
    }
}
