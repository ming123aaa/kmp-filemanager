package com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ohuang.kmp.filemanager.kmp_filemanager.clipboard.ClipboardHistoryItem
import com.ohuang.kmp.filemanager.kmp_filemanager.clipboard.ClipboardHistoryManager
import com.ohuang.kmp.filemanager.kmp_filemanager.clipboard.ClipboardSyncManager
import com.ohuang.kmp.filemanager.kmp_filemanager.clipboard.ClipSource
import com.ohuang.kmp.filemanager.kmp_filemanager.data.copyToClipboard
import com.ohuang.kmp.filemanager.kmp_filemanager.data.currentTimeMillis

/**
 * 剪贴板历史记录页面
 *
 * - 展示最多 [ClipboardHistoryManager.MAX_HISTORY] 条历史记录
 * - 每条可复制到剪贴板或删除
 * - 支持一键清空
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardHistoryScreen(onBack: () -> Unit) {
    val history by ClipboardHistoryManager.history.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<ClipboardHistoryItem?>(null) }

    // 清空确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("清空历史记录") },
            text = { Text("确定要删除全部 ${history.size} 条剪贴板历史记录吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        ClipboardHistoryManager.clear()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }

    // 单条删除确认对话框
    showDeleteDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("删除记录") },
            text = {
                Text(
                    "确定要删除这条记录吗？",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ClipboardHistoryManager.removeItem(item.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("剪贴板历史") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "清空历史",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (history.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "暂无剪贴板历史",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "复制内容后将自动出现在这里",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 顶部统计
                item(key = "header") {
                    Text(
                        "共 ${history.size} / ${ClipboardHistoryManager.MAX_HISTORY} 条",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(history, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        onCopy = {
                            ClipboardSyncManager.skipNextChange()
                            copyToClipboard(item.text)
                        },
                        onDelete = { showDeleteDialog = item }
                    )
                }
            }
        }
    }
}

// ==================== 单条历史记录卡片 ====================

@Composable
private fun HistoryItemCard(
    item: ClipboardHistoryItem,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var isOverflowed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCopy)
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 来源图标
            Icon(
                imageVector = if (item.source == ClipSource.LOCAL) Icons.Default.PhoneAndroid else Icons.Default.Cloud,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(10.dp))

            // 文本内容 + 元信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) 50 else 3,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { result ->
                        if (!expanded) {
                            isOverflowed = result.hasVisualOverflow
                        }
                    }
                )

                // 展开 / 收起按钮
                if (isOverflowed) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            if (expanded) "收起" else "展开全部",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.deviceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatRelativeTime(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // 操作按钮
            IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "复制",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ==================== 相对时间格式化 ====================

/**
 * 将毫秒时间戳转换为人类可读的相对时间。
 * 例：刚刚、5分钟前、3小时前、2天前、2025-01-15
 */
private fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0) return "未知"

    val now = currentTimeMillis()
    val diffMs = now - timestamp

    if (diffMs < 0) return "刚刚"

    val seconds = diffMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "刚刚"
        minutes < 60 -> "${minutes}分钟前"
        hours < 24 -> "${hours}小时前"
        days < 7 -> "${days}天前"
        else -> {
            // 超过一周显示具体日期
            val totalDays = days.toInt()
            // 简单估算：用 30 天近似一个月
            if (totalDays < 30) "${days}天前"
            else "${days / 30}个月前"
        }
    }
}