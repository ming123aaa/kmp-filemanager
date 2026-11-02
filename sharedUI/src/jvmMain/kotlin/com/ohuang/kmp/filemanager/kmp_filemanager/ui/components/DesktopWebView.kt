package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ohuang.kmp.filemanager.kmp_filemanager.util.openUrlInBrowser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 桌面端自定义 WebView 组件。
 *
 * 用于在 Compose Desktop 中展示网页内容/视频。
 * 采用在系统浏览器中打开的策略，避免 JCEF 初始化导致的卡顿和未响应问题。
 *
 * @param url 要加载的 URL
 * @param modifier 修饰符
 * @param isVideo 是否为视频内容（影响 UI 展示风格）
 * @param onLoadStarted 开始加载回调
 * @param onLoadFinished 加载完成回调
 * @param onLoadError 加载出错回调
 */
@Composable
fun DesktopWebView(
    url: String,
    modifier: Modifier = Modifier,
    isVideo: Boolean = false,
    onLoadStarted: () -> Unit = {},
    onLoadFinished: () -> Unit = {},
    onLoadError: (message: String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            // 清理回调避免泄漏
        }
    }

    Box(
        modifier = modifier.background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            // 加载中
            LoadingContent()
        } else if (hasError) {
            // 错误状态
            ErrorContent(
                message = errorMessage,
                onRetry = {
                    hasError = false
                    openUrl(url, scope, onLoadStarted, onLoadFinished) { msg ->
                        hasError = true
                        errorMessage = msg
                        onLoadError(msg)
                    }
                },
                onOpenBrowser = { scope.launch { openUrlInBrowser(url) } }
            )
        } else {
            // 初始状态：展示预览区域和操作按钮
            PreviewContent(
                isVideo = isVideo,
                onOpenClick = {
                    isLoading = true
                    onLoadStarted()
                    openUrl(url, scope, onLoadStarted, {
                        isLoading = false
                        onLoadFinished()
                    }) { msg ->
                        isLoading = false
                        hasError = true
                        errorMessage = msg
                        onLoadError(msg)
                    }
                },
                onOpenBrowser = { scope.launch { openUrlInBrowser(url) } }
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = Color(0xFF4FC3F7),
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "正在打开...",
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun PreviewContent(
    isVideo: Boolean,
    onOpenClick: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 大图标
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF1E88E5).copy(alpha = 0.2f))
                .clickable { onOpenClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isVideo) Icons.Default.PlayArrow else Icons.Default.Language,
                contentDescription = if (isVideo) "播放视频" else "打开网页",
                tint = Color(0xFF4FC3F7),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isVideo) "点击播放视频" else "点击打开网页",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isVideo) "将在系统默认浏览器中播放" else "将在系统默认浏览器中打开",
            color = Color.White.copy(alpha = 0.4f),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 操作按钮
        TextButton(onClick = onOpenBrowser) {
            Icon(
                imageVector = Icons.Default.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color(0xFF4FC3F7)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = "在浏览器中打开",
                color = Color(0xFF4FC3F7)
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "打开失败",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.titleMedium
        )

        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onRetry) {
            Text("重试")
        }
        TextButton(onClick = onOpenBrowser) {
            Text("在浏览器中打开")
        }
    }
}

/**
 * 在系统浏览器中打开 URL，带异常处理。
 */
private fun openUrl(
    url: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onLoadStarted: () -> Unit,
    onFinished: () -> Unit,
    onError: (String) -> Unit
) {
    scope.launch {
        try {
            onLoadStarted()
            // 短暂延迟让加载动画显示出来
            delay(300)
            withContext(Dispatchers.IO) {
                openUrlInBrowser(url)
            }
            // 给浏览器一些时间启动
            delay(800)
            onFinished()
        } catch (e: Exception) {
            onError(e.message ?: "未知错误")
        }
    }
}