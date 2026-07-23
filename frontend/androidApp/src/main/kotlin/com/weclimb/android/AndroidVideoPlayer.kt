package com.weclimb.android

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

fun isReadableVideoUri(context: Context, value: String): Boolean = runCatching {
    require(value.isNotBlank())
    context.contentResolver.openInputStream(Uri.parse(value)).use { input ->
        requireNotNull(input) { "영상을 읽을 수 없습니다" }
    }
}.isSuccess

@Composable
fun AndroidVideoPlayer(videoUri: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose(player::release)
    }
    AndroidView(
        modifier = modifier,
        factory = { PlayerView(context).apply { this.player = player } },
        update = { view -> view.player = player },
    )
}
