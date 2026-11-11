package com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ohuang.kmp.filemanager.kmp_filemanager.ApiService
import com.ohuang.kmp.filemanager.kmp_filemanager.HttpConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.PlatformType
import com.ohuang.kmp.filemanager.kmp_filemanager.clipboard.ClipboardSyncManager
import com.ohuang.kmp.filemanager.kmp_filemanager.getPlatform
import com.ohuang.kmp.filemanager.kmp_filemanager.isTypes
import com.ohuang.kmp.filemanager.kmp_filemanager.server.getServerManager
import kotlinx.coroutines.launch

/**
 * 剪贴板共享设置面板
 * 显示条件：本地服务运行中 或 已连接到支持剪贴板的远端
 * 面板可见时自动启用同步
 */
@Composable
fun ClipboardSettingsSection(
    snackbarHostState: SnackbarHostState,
    goClipboardHistory: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    ClipboardSyncPanel(snackbarHostState, scope, goClipboardHistory)
}

// ==================== 面板 ====================

@Composable
private fun ClipboardSyncPanel(
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    goClipboardHistory: () -> Unit = {}
) {
    var clipboardAutoSync by remember { mutableStateOf(HttpConfig.loadClipboardAutoSync()) }

    val currentText by ClipboardSyncManager.currentText.collectAsState()
    val syncEvents by ClipboardSyncManager.syncEvents.collectAsState()

    LaunchedEffect(syncEvents) {
        syncEvents?.let { event ->
            event.message?.let { msg -> snackbarHostState.showSnackbar(msg) }
        }
    }

    Text(
        "剪贴板共享",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Card(modifier = Modifier.animateContentSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 自动同步
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("自动同步", style = MaterialTheme.typography.bodyLarge)
                    Text("本地复制时自动推送到其他设备", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (getPlatform().type.isTypes(PlatformType.Android)){
                        Text("android 10以上的版本,应用需在前台才能自动同步", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                Switch(
                    checked = clipboardAutoSync,
                    onCheckedChange = { enabled ->
                        clipboardAutoSync = enabled
                        HttpConfig.saveClipboardAutoSync(enabled)

                    }
                )
            }

            HorizontalDivider()
            CurrentClipboardPreview(currentText)
            HorizontalDivider()

            // 查看历史记录
            OutlinedButton(
                onClick = goClipboardHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("查看剪贴板历史记录")
            }

            HorizontalDivider()

            ManualSyncButtons(
                onPush = {
                    scope.launch {
                        val result = ClipboardSyncManager.pushToRemote()
                        result.fold(
                            onSuccess = { snackbarHostState.showSnackbar("已推送到远端:${it}") },
                            onFailure = { e -> snackbarHostState.showSnackbar("推送失败: ${e.message}") }
                        )
                    }
                },
                onPull = {
                    scope.launch {
                        val result = ClipboardSyncManager.pullFromRemote()
                        result.fold(
                            onSuccess = { text ->
                                if (text != null) snackbarHostState.showSnackbar("已从远端同步剪贴板")
                                else snackbarHostState.showSnackbar("远端剪贴板为空")
                            },
                            onFailure = { e -> snackbarHostState.showSnackbar("拉取失败: ${e.message}") }
                        )
                    }
                }
            )
        }
    }
}

// ==================== 子组件 ====================

@Composable
private fun CurrentClipboardPreview(text: String?) {
    Column {
        Text("当前剪贴板", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = if (text.isNullOrBlank()) "（空）" else text,
                style = MaterialTheme.typography.bodySmall,
                color = if (text.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
                maxLines = 3
            )
        }
    }
}

@Composable
private fun ManualSyncButtons(
    onPush: () -> Unit,
    onPull: () -> Unit,
    enabled: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("主动同步", style = MaterialTheme.typography.bodyLarge)
        Text("手动推送当前剪贴板或从其他设备拉取", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onPush, modifier = Modifier.weight(1f), enabled = enabled) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("推送")
            }
            OutlinedButton(onClick = onPull, modifier = Modifier.weight(1f), enabled = enabled) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("拉取")
            }
        }
    }
}