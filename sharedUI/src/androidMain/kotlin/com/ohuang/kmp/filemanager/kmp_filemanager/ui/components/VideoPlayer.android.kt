package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import android.net.Uri
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    isLoading = state == Player.STATE_BUFFERING
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    hasError = true
                    isLoading = false
                }
            })
        }
    }

    LaunchedEffect(url) {
        hasError = false
        isLoading = true
        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (hasError) {
            androidx.compose.material3.Text(
                text = "视频播放失败",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    object : PlayerView(ctx) {
                        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                            // 阻止 PlayerView 内部处理返回键，避免与 onBackPressedDispatcher 冲突导致返回两次
                            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                                return false
                            }
                            return super.dispatchKeyEvent(event)
                        }
                    }.apply {
                        player = exoPlayer
                        useController = true
                    }
                }
            )
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}