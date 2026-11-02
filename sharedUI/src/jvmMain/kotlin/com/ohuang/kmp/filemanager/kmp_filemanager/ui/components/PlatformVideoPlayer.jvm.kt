package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * JVM/Desktop actual 实现：使用自定义 DesktopWebView 组件。
 * 通过系统浏览器打开视频，避免 JCEF WebView 卡顿/未响应问题。
 */
@Composable
actual fun PlatformVideoPlayer(
    url: String,
    modifier: Modifier,
    showControls: Boolean,
    autoPlay: Boolean
) {
    DesktopWebView(
        url = url,
        modifier = modifier,
        isVideo = true
    )
}