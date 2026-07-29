package com.weclimb.android

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

fun isReadableVideoUri(context: Context, value: String): Boolean = runCatching {
    require(value.isNotBlank())
    context.contentResolver.openInputStream(Uri.parse(value)).use { input ->
        requireNotNull(input) { "영상을 읽을 수 없습니다" }
    }
}.isSuccess

@Composable
fun AndroidVideoPlayer(
    videoUri: String,
    modifier: Modifier = Modifier,
    useController: Boolean = true,
    controllerColor: Color = Color.Black.copy(alpha = .5f),
    controllerSize: Dp = 46.dp,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
        }
    }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PlayerView(context).apply {
                    this.player = player
                    this.useController = useController
                }
            },
            update = { view ->
                view.player = player
                view.useController = useController
            },
        )
        if (!useController) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(controllerSize)
                    .clip(CircleShape)
                    .background(controllerColor)
                    .clickable {
                        if (player.isPlaying) player.pause() else player.play()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (isPlaying) "Ⅱ" else "▶", color = Color.White)
            }
        }
    }
}
