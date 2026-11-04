package com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsPower
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ohuang.kmp.filemanager.kmp_filemanager.data.calculateCacheSize
import com.ohuang.kmp.filemanager.kmp_filemanager.data.canDrawOverlays
import com.ohuang.kmp.filemanager.kmp_filemanager.data.clearCache
import com.ohuang.kmp.filemanager.kmp_filemanager.data.copyToClipboard
import com.ohuang.kmp.filemanager.kmp_filemanager.data.getEnableAccessibilityAdbCommand
import com.ohuang.kmp.filemanager.kmp_filemanager.data.isClipboardAccessibilityEnabled
import com.ohuang.kmp.filemanager.kmp_filemanager.data.launchPowerSavingMode
import com.ohuang.kmp.filemanager.kmp_filemanager.data.openAccessibilitySettingsPage
import com.ohuang.kmp.filemanager.kmp_filemanager.data.openOverlayPermissionSettings
import com.ohuang.kmp.filemanager.kmp_filemanager.data.requestIgnoreBatteryOptimization
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun PlatformSettingsSection(snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()

    // 缓存清理
    var cacheSize by remember { mutableStateOf("计算中...") }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            cacheSize = calculateCacheSize()
        }
    }

    SectionTitle("系统工具")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 忽略电池优化
            SettingsItem(
                icon = Icons.Default.SettingsPower,
                title = "忽略电池优化",
                subtitle = "防止系统关闭后台服务",
                onClick = { requestIgnoreBatteryOptimization() }
            )

            // 黑屏省电
            SettingsItem(
                icon = Icons.Default.DarkMode,
                title = "黑屏省电",
                subtitle = "降低功耗，适合长时间运行",
                onClick = { launchPowerSavingMode() }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // ── 剪贴板权限 ──
    SectionTitle("剪贴板权限")
    var overlayGranted by remember { mutableStateOf(canDrawOverlays()) }
    var a11yEnabled by remember { mutableStateOf(isClipboardAccessibilityEnabled()) }
    val adbCommand = remember { getEnableAccessibilityAdbCommand() }

    // 刷新状态
    LaunchedEffect(Unit) {
        overlayGranted = canDrawOverlays()
        a11yEnabled = isClipboardAccessibilityEnabled()
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 状态摘要
            val allGranted = overlayGranted && a11yEnabled
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (allGranted)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (allGranted) Icons.Default.Check else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (allGranted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (allGranted) "剪贴板权限已就绪" else "剪贴板权限未完整",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (allGranted) "悬浮窗 ✓ | 无障碍 ✓"
                            else buildString {
                                if (!overlayGranted) append("悬浮窗 ✗ ")
                                if (!a11yEnabled) append("无障碍 ✗")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // 悬浮窗权限
            SettingsItem(
                icon = Icons.Default.SettingsPower,
                title = "悬浮窗权限",
                subtitle = if (overlayGranted) "已授权" else "未授权 — 点击去设置",
                onClick = {
                    if (!overlayGranted) openOverlayPermissionSettings()
                }
            )

            // 无障碍服务
            SettingsItem(
                icon = Icons.Default.Settings,
                title = "无障碍服务",
                subtitle = if (a11yEnabled) "已开启" else "未开启 — 点击去开启",
                onClick = {
                    if (!a11yEnabled) openAccessibilitySettingsPage()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // ADB 命令
            Text(
                text = "一键授权并开启无障碍（ADB）",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = adbCommand,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 5
                    )
                    IconButton(
                        onClick = {
                            copyToClipboard(adbCommand)
                            scope.launch {
                                snackbarHostState.showSnackbar("ADB 命令已复制到剪贴板")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "复制命令",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 缓存清理
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "清理缓存",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "当前缓存: $cacheSize",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = cacheSize != "计算中..." && cacheSize != "0 B",
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("清理缓存")
            }
        }
    }

    // 确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null
                )
            },
            title = { Text("清理缓存") },
            text = { Text("确定要清理应用缓存吗？这将删除缩略图和临时文件。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar("正在清理缓存...")
                            withContext(Dispatchers.IO) {
                                clearCache()
                            }
                            cacheSize = calculateCacheSize()
                            snackbarHostState.showSnackbar("缓存清理完成")
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}