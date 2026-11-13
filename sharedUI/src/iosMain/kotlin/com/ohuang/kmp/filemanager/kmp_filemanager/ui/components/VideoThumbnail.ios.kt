package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp

/**
 * iOS actual：iOS 端退回显示视频图标占位。
 */
@Composable
actual fun VideoThumbnail(
    videoUrl: String,
    modifier: Modifier,
    contentScale: ContentScale,
    thumbnailSize: Dp,
    placeholder: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize().background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = "视频",
            tint = Color(0xFF9B59B6),
            modifier = Modifier.size(48.dp)
        )
    }
}