package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import VideoPlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
actual fun PlatformVideoPlayer(
    url: String,
    modifier: Modifier,
    showControls: Boolean,
    autoPlay: Boolean
) {
    VideoPlayer(
        url = url,
        modifier = modifier,
        showControls = showControls,
        autoPlay = autoPlay
    )
}