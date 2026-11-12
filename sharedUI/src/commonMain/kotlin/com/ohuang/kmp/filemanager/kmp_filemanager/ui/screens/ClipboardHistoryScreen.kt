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
import com.ohuang.kmp.filemanager.kmp_filemanager.ApiService
import com.ohuang.kmp.filemanager.kmp_filemanager.clipboard.ClipboardHistoryItem
import com.ohuang.kmp.filemanager.kmp_filemanager.clipboard.ClipboardHistoryManager
import com.ohuang.kmp.filemanager.kmp_filemanager.clipboard.ClipboardSyncManager
import com.ohuang.kmp.filemanager.kmp_filemanager.clipboard.ClipSource
import com.ohuang.kmp.filemanager.kmp_filemanager.data.copyToClipboard
import com.ohuang.kmp.filemanager.kmp_filemanager.data.currentTimeMillis
import kotlinx.coroutines.launch

/**
 * 剪贴板历史记录来源模式
 */
enum class ClipHistorySource {
    LOCAL,   // 本地存储
    SERVER   // 服务端存储
}

/**
 * 剪贴板历史记录页面
 *
 * - 支持本地 / 服务端切换查看
 * - 本地模式：展示最多 [ClipboardHistoryManager.MAX_HISTORY] 条历史记录
 * - 服务端模式：从远端服务器拉取历史记录
 * - 每条可复制到剪贴板或删除
 * - 支持一键清空
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardHistoryScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val localHistory by ClipboardHistoryManager.history.collectAsState()
    
    var sourceMode by remember { mutableStateOf(ClipHistorySource.LOCAL) }
    var serverHistory by remember { mutableStateOf<List<ClipboardHistoryItem>>(emptyList()) }
    var isLoadingServer by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<String?>(null) }
    
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<ClipboardHistoryItem?>(null) }

    // 当前显示的列表
    val currentHistory = when (sourceMode) {
        ClipHistorySource.LOCAL -> localHistory
        ClipHistorySource.SERVER -> serverHistory
    }
    val currentMaxCount = when (sourceMode) {
        ClipHistorySource.LOCAL -> ClipboardHistoryManager.MAX_HISTORY
        ClipHistorySource.SERVER -> 100
    }

    // 切换到服务端模式时自动加载
    fun loadServerHistory() {
        scope.launch {
            isLoadingServer = true
            serverError = null
            try {
                serverHistory = ApiService.fetchServerClipboardHistory()
                if (serverHistory.isEmpty()) {
                    serverError = null // 空列表不算错误
                }
            } catch (e: Exception) {
                serverError = e.message ?: "加载失败"
                serverHistory = emptyList()
            } finally {
                isLoadingServer = false
            }
        }
    }

    // 清空确认对话框
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("清空历史记录") },
            text = {
                val label = if (sourceMode == ClipHistorySource.LOCAL) "本地" else "服务端"
                Text("确定要删除${label}全部 ${currentHistory.size} 条剪贴板历史记录吗？此操作不可撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (sourceMode) {
                                ClipHistorySource.LOCAL -> ClipboardHistoryManager.clear()
                                ClipHistorySource.SERVER -> {
                                    ApiService.clearServerClipboardHistory()
                                    serverHistory = emptyList()
                                }
                            }
                        }
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
                        scope.launch {
                            when (sourceMode) {
                                ClipHistorySource.LOCAL -> ClipboardHistoryManager.removeItem(item.id)
                                ClipHistorySource.SERVER -> {
                                    ApiService.deleteServerClipboardHistoryItem(item.id)
                                    serverHistory = serverHistory.filter { it.id != item.id }
                                }
                            }
                        }
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
                    if (currentHistory.isNotEmpty()) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ========== 本地 / 服务端切换栏 ==========
            SourceModeToggle(
                currentMode = sourceMode,
                onModeChange = { mode ->
                    sourceMode = mode
                    if (mode == ClipHistorySource.SERVER) {
                        loadServerHistory()
                    }
                },
                isLoadingServer = isLoadingServer
            )

            // ========== 内容区域 ==========
            when {
                // 服务端加载中
                sourceMode == ClipHistorySource.SERVER && isLoadingServer -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "正在加载服务端历史...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 服务端错误
                sourceMode == ClipHistorySource.SERVER && serverError != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "无法连接服务端",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                serverError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = { loadServerHistory() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("重试")
                            }
                        }
                    }
                }

                // 空状态
                currentHistory.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (sourceMode == ClipHistorySource.SERVER) Icons.Default.Cloud
                                else Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (sourceMode == ClipHistorySource.SERVER) "服务端暂无剪贴板历史"
                                else "暂无剪贴板历史",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (sourceMode == ClipHistorySource.SERVER) "从其他设备同步剪贴板后将自动出现在这里"
                                else "复制内容后将自动出现在这里",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            // 服务端模式空时提供刷新按钮
                            if (sourceMode == ClipHistorySource.SERVER) {
                                Spacer(Modifier.height(16.dp))
                                OutlinedButton(onClick = { loadServerHistory() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("刷新")
                                }
                            }
                        }
                    }
                }

                // 有历史记录
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 顶部统计
                        item(key = "header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "共 ${currentHistory.size} / $currentMaxCount 条",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                // 服务端模式显示刷新按钮
                                if (sourceMode == ClipHistorySource.SERVER) {
                                    TextButton(
                                        onClick = { loadServerHistory() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "刷新",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("刷新", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        items(currentHistory, key = { it.id }) { item ->
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
    }
}

// ==================== 来源模式切换栏 ====================

@Composable
private fun SourceModeToggle(
    currentMode: ClipHistorySource,
    onModeChange: (ClipHistorySource) -> Unit,
    isLoadingServer: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 本地按钮
            FilterChip(
                selected = currentMode == ClipHistorySource.LOCAL,
                onClick = { onModeChange(ClipHistorySource.LOCAL) },
                label = { Text("本地") },
                leadingIcon = {
                    Icon(
                        Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // 服务端按钮
            FilterChip(
                selected = currentMode == ClipHistorySource.SERVER,
                onClick = { onModeChange(ClipHistorySource.SERVER) },
                label = { Text("服务端") },
                leadingIcon = {
                    if (isLoadingServer && currentMode == ClipHistorySource.SERVER) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
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