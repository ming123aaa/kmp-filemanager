package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohuang.kmp.filemanager.kmp_filemanager.util.openUrlInBrowser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * JVM/Desktop actual 实现。
 *
 * 使用 VideoThumbnail（JCodec）作为背景预览，点击后调用系统默认浏览器播放视频。
 * 本地文件显示真实缩略图，远程 URL 降级为图标 + 播放按钮。
 */
@Composable
actual fun PlatformVideoPlayer(
    url: String,
    modifier: Modifier,
    showControls: Boolean,
    autoPlay: Boolean
) {
    val scope = rememberCoroutineScope()
    var isOpening by remember { mutableStateOf(false) }
    var openError by remember { mutableStateOf<String?>(null) }

    // 自动播放
    LaunchedEffect(autoPlay) {
        if (autoPlay) {
            isOpening = true
            openInBrowser(scope, url,
                onError = { msg ->
                    isOpening = false
                    openError = msg
                },
                onDone = { isOpening = false }
            )
        }
    }

    Box(modifier = modifier.background(Color.Black)) {

        // ── 背景：视频缩略图 ──
        VideoThumbnail(
            videoUrl = url,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            // 加载中占位
            placeholder = {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF4FC3F7)
                    )
                }
            }
        )

        // ── 半透明遮罩（让播放按钮更可读）──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // ── 中心区域 ──
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                // 错误状态
                openError != null -> {
                    ErrorOverlay(
                        message = openError!!,
                        onRetry = {
                            openError = null
                            isOpening = true
                            openInBrowser(scope, url,
                                onError = { msg ->
                                    isOpening = false
                                    openError = msg
                                },
                                onDone = { isOpening = false }
                            )
                        }
                    )
                }
                // 正在打开浏览器
                isOpening -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp,
                        color = Color(0xFF4FC3F7)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在用浏览器打开…",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                // 空闲：显示播放按钮
                else -> {
                    PlayButton(
                        onClick = {
                            isOpening = true
                            openInBrowser(scope, url,
                                onError = { msg ->
                                    isOpening = false
                                    openError = msg
                                },
                                onDone = { isOpening = false }
                            )
                        }
                    )
                }
            }
        }

        // ── 底部提示（仅空闲时显示）──
        AnimatedVisibility(
            visible = !isOpening && openError == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text(
                text = "点击播放 · 在系统浏览器中打开",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }
}

// ── 播放按钮 ──

@Composable
private fun PlayButton(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "播放视频",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF4FC3F7)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "在浏览器中打开",
                color = Color(0xFF4FC3F7),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── 错误覆盖层 ──

@Composable
private fun ErrorOverlay(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 40.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "无法打开浏览器",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onRetry) {
            Text("重试", color = Color(0xFF4FC3F7))
        }
    }
}

// ── 浏览器打开逻辑 ──

private fun openInBrowser(
    scope: kotlinx.coroutines.CoroutineScope,
    url: String,
    onError: (String) -> Unit,
    onDone: () -> Unit
) {
    scope.launch {
        try {
            // 短暂延迟让 UI 反馈可见
            delay(200)
            withContext(Dispatchers.IO) {
                openUrlInBrowser(url)
            }
            // 给浏览器启动留时间
            delay(600)
            onDone()
        } catch (e: Exception) {
            onError(e.message ?: "未知错误")
        }
    }
}