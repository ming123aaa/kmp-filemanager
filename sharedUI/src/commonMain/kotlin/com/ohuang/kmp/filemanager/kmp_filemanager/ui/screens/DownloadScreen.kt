package com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohuang.kmp.filemanager.kmp_filemanager.data.AppDownloadManager
import com.ohuang.kmp.filemanager.kmp_filemanager.data.DownloadTask
import com.ohuang.kmp.filemanager.kmp_filemanager.data.openLocalFile
import kotlinx.coroutines.launch

private enum class DownloadFilter(val label: String) {
    ALL("全部"),
    DOWNLOADING("下载中"),
    PREPARING("准备"),
    COMPLETED("完成"),
    FAILED("失败")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(onBack: () -> Unit) {
    val tasks by AppDownloadManager.tasks.collectAsState()
    val progressMessage by AppDownloadManager.progressMessage.collectAsState()
    val hasActive by AppDownloadManager.hasActiveDownloads.collectAsState()
    val isContinueDownload by AppDownloadManager.isContinueDownload.collectAsState()
    val downloadInterval by AppDownloadManager.downloadInterval.collectAsState()
    val overwriteFile by AppDownloadManager.overwriteFile.collectAsState()
    val folderFairContinue by AppDownloadManager.folderFairContinue.collectAsState()

    var selectedFilter by remember { mutableStateOf(DownloadFilter.ALL) }
    var showNewDownloadDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedTaskIds by remember { mutableStateOf(setOf<Long>()) }

    val taskList = remember(tasks) { tasks.values.toList() }
    val filteredTasks = remember(taskList, selectedFilter) {
        taskList.filter {
            when (selectedFilter) {
                DownloadFilter.ALL -> true
                DownloadFilter.DOWNLOADING -> it.status == DownloadTask.Status.DOWNLOADING
                DownloadFilter.PREPARING -> it.status == DownloadTask.Status.WAITING || it.status == DownloadTask.Status.PAUSED
                DownloadFilter.COMPLETED -> it.status == DownloadTask.Status.COMPLETED
                DownloadFilter.FAILED -> it.status == DownloadTask.Status.FAILED
            }
        }
    }.sortedWith(
        compareBy<DownloadTask> {
            when (it.status) {
                DownloadTask.Status.DOWNLOADING -> 0
                DownloadTask.Status.WAITING -> 1
                DownloadTask.Status.PAUSED -> 2
                DownloadTask.Status.FAILED -> 3
                DownloadTask.Status.COMPLETED -> 4
            }
        }.thenByDescending { it.id }
    )

    val filterCounts = remember(taskList) {
        mapOf(
            DownloadFilter.ALL to taskList.size,
            DownloadFilter.DOWNLOADING to taskList.count { it.status == DownloadTask.Status.DOWNLOADING },
            DownloadFilter.PREPARING to taskList.count { it.status == DownloadTask.Status.WAITING || it.status == DownloadTask.Status.PAUSED },
            DownloadFilter.COMPLETED to taskList.count { it.status == DownloadTask.Status.COMPLETED },
            DownloadFilter.FAILED to taskList.count { it.status == DownloadTask.Status.FAILED }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isMultiSelectMode) {
                        Text("已选 ${selectedTaskIds.size} 项")
                    } else {
                        Text("下载")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = {
                            selectedTaskIds = emptySet()
                            isMultiSelectMode = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "退出多选")
                        }
                        val allSelected = filteredTasks.isNotEmpty() && filteredTasks.all { selectedTaskIds.contains(it.id) }
                        TextButton(onClick = {
                            selectedTaskIds = if (allSelected) emptySet() else filteredTasks.map { it.id }.toSet()
                        }) {
                            Text(if (allSelected) "取消全选" else "全选")
                        }
                        IconButton(onClick = {
                            selectedTaskIds.forEach { AppDownloadManager.removeTask(it) }
                            selectedTaskIds = emptySet()
                            isMultiSelectMode = false
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除所选")
                        }
                    } else {
                        IconButton(onClick = { showNewDownloadDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "新建下载")
                        }
                        if (tasks.any { it.value.status == DownloadTask.Status.DOWNLOADING || it.value.status == DownloadTask.Status.WAITING }) {
                            IconButton(onClick = { AppDownloadManager.pauseAll() }) {
                                Icon(Icons.Default.PauseCircle, contentDescription = "全部暂停")
                            }
                        } else if (tasks.any { it.value.status == DownloadTask.Status.PAUSED }) {
                            IconButton(onClick = { AppDownloadManager.resumeAll() }) {
                                Icon(Icons.Default.PlayCircle, contentDescription = "全部继续")
                            }
                        }
                        if (tasks.isNotEmpty()) {
                            IconButton(onClick = {
                                selectedTaskIds = emptySet()
                                isMultiSelectMode = true
                            }) {
                                Icon(Icons.Default.CleaningServices, contentDescription = "选择清除")
                            }
                        }

                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "下载目录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "当前为应用默认下载目录",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            if (progressMessage.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = progressMessage,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (tasks.isNotEmpty()) {
                FilterChips(
                    selectedFilter = selectedFilter,
                    filterCounts = filterCounts,
                    onFilterChanged = { selectedFilter = it }
                )
            }

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (tasks.isEmpty()) Icons.Default.DownloadDone else Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (tasks.isEmpty()) "暂无下载任务" else "无匹配的下载任务",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (tasks.isEmpty()) "点击右下角 + 新建下载任务" else "尝试切换筛选条件",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        DownloadTaskItem(
                            task = task,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = selectedTaskIds.contains(task.id),
                            onToggleSelect = {
                                selectedTaskIds = if (selectedTaskIds.contains(task.id))
                                    selectedTaskIds - task.id
                                else
                                    selectedTaskIds + task.id
                            },
                            onPause = { AppDownloadManager.pauseDownload(task.id) },
                            onResume = { AppDownloadManager.resumeDownload(task.id) },
                            onRetry = { AppDownloadManager.retryDownload(task.id) },
                            onRemove = { AppDownloadManager.removeTask(task.id) },
                            onOpenFolder = {
                                openLocalFile(task.localFilePath)
                            }
                        )
                    }
                }
            }

            if (isMultiSelectMode && selectedTaskIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("已选 ${selectedTaskIds.size} 项")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                selectedTaskIds = filteredTasks.map { it.id }.toSet()
                            }) { Text("全选") }
                            TextButton(onClick = { selectedTaskIds = emptySet() }) { Text("取消") }
                            Button(
                                onClick = {
                                    selectedTaskIds.forEach { AppDownloadManager.removeTask(it) }
                                    selectedTaskIds = emptySet()
                                    isMultiSelectMode = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) { Text("删除") }
                        }
                    }
                }
            }
        }
    }

    if (showNewDownloadDialog) {
        NewDownloadDialog(onDismiss = { showNewDownloadDialog = false })
    }

    if (showSettingsDialog) {
        DownloadSettingsDialog(
            isContinueDownload = isContinueDownload,
            downloadInterval = downloadInterval,
            overwriteFile = overwriteFile,
            folderFairContinue = folderFairContinue,
            onContinueDownloadChanged = { AppDownloadManager.setContinueDownload(it) },
            onDownloadIntervalChanged = { AppDownloadManager.setDownloadInterval(it) },
            onOverwriteFileChanged = { AppDownloadManager.setOverwriteFile(it) },
            onFolderFairContinueChanged = { AppDownloadManager.setFolderFairContinue(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun FilterChips(
    selectedFilter: DownloadFilter,
    filterCounts: Map<DownloadFilter, Int>,
    onFilterChanged: (DownloadFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DownloadFilter.entries.forEach { filter ->
            val count = filterCounts[filter] ?: 0
            val icon = when (filter) {
                DownloadFilter.ALL -> Icons.Default.Apps
                DownloadFilter.DOWNLOADING -> Icons.Default.Download
                DownloadFilter.PREPARING -> Icons.Default.HourglassTop
                DownloadFilter.COMPLETED -> Icons.Default.CheckCircle
                DownloadFilter.FAILED -> Icons.Default.ErrorOutline
            }
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = when (filter) {
                    DownloadFilter.ALL, DownloadFilter.DOWNLOADING -> MaterialTheme.colorScheme.primaryContainer
                    DownloadFilter.PREPARING -> MaterialTheme.colorScheme.tertiaryContainer
                    DownloadFilter.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
                    DownloadFilter.FAILED -> MaterialTheme.colorScheme.errorContainer
                },
                selectedLabelColor = when (filter) {
                    DownloadFilter.ALL, DownloadFilter.DOWNLOADING -> MaterialTheme.colorScheme.onPrimaryContainer
                    DownloadFilter.PREPARING -> MaterialTheme.colorScheme.onTertiaryContainer
                    DownloadFilter.COMPLETED -> MaterialTheme.colorScheme.onSecondaryContainer
                    DownloadFilter.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                },
                selectedLeadingIconColor = when (filter) {
                    DownloadFilter.ALL, DownloadFilter.DOWNLOADING -> MaterialTheme.colorScheme.onPrimaryContainer
                    DownloadFilter.PREPARING -> MaterialTheme.colorScheme.onTertiaryContainer
                    DownloadFilter.COMPLETED -> MaterialTheme.colorScheme.onSecondaryContainer
                    DownloadFilter.FAILED -> MaterialTheme.colorScheme.onErrorContainer
                }
            )
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChanged(filter) },
                label = { Text("${filter.label} ($count)") },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                colors = chipColors
            )
        }
    }
}

@Composable
private fun DownloadTaskItem(
    task: DownloadTask,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onOpenFolder: () -> Unit = {}
) {
    val isCompleted = task.status == DownloadTask.Status.COMPLETED
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(
                if (isMultiSelectMode) Modifier.clickable { onToggleSelect() }
                else if (isCompleted) Modifier.clickable { onOpenFolder() }
                else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isMultiSelectMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = when {
                            task.isFolder -> Icons.Default.Folder
                            task.status == DownloadTask.Status.COMPLETED -> Icons.Default.CheckCircle
                            task.status == DownloadTask.Status.FAILED -> Icons.Default.Error
                            task.status == DownloadTask.Status.PAUSED -> Icons.Default.PauseCircle
                            task.status == DownloadTask.Status.DOWNLOADING -> Icons.Default.Download
                            else -> Icons.Default.Schedule
                        },
                        contentDescription = null,
                        tint = when (task.status) {
                            DownloadTask.Status.COMPLETED -> Color(0xFF4CAF50)
                            DownloadTask.Status.FAILED -> MaterialTheme.colorScheme.error
                            DownloadTask.Status.PAUSED -> Color(0xFFFF9800)
                            DownloadTask.Status.DOWNLOADING -> MaterialTheme.colorScheme.primary
                            DownloadTask.Status.WAITING -> Color(0xFFFFC107)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.fileName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (task.status) {
                            DownloadTask.Status.WAITING -> "等待中"
                            DownloadTask.Status.DOWNLOADING -> {
                                if (task.isFolder) {
                                    if (task.downloadedSize == 0L) {
                                        "扫描文件中，已扫描 ${task.totalFiles} 个文件"
                                    } else {
                                        "${task.completedFiles}/${task.totalFiles} 文件  ${task.formatDownloadedSize()} / ${task.formatTotalSize()}"
                                    }
                                } else {
                                    "${task.formatDownloadedSize()} / ${task.formatTotalSize()}"
                                }
                            }
                            DownloadTask.Status.PAUSED -> {
                                if (task.isFolder) "已暂停  ${task.completedFiles}/${task.totalFiles} 文件  ${task.formatDownloadedSize()} / ${task.formatTotalSize()}"
                                else "已暂停  ${task.formatDownloadedSize()} / ${task.formatTotalSize()}"
                            }
                            DownloadTask.Status.COMPLETED -> {
                                if (task.isFolder) "下载完成  ${task.totalFiles} 个文件  ${task.formatTotalSize()}"
                                else "下载完成  ${task.formatTotalSize()}"
                            }
                            DownloadTask.Status.FAILED -> task.errorMessage ?: "下载失败"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (task.status == DownloadTask.Status.FAILED) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                if (!isMultiSelectMode) {
                    Row {
                        when (task.status) {
                            DownloadTask.Status.WAITING, DownloadTask.Status.DOWNLOADING -> {
                                IconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Pause, contentDescription = "暂停", modifier = Modifier.size(22.dp))
                                }
                                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "取消", modifier = Modifier.size(22.dp))
                                }
                            }
                            DownloadTask.Status.PAUSED -> {
                                IconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "继续", modifier = Modifier.size(22.dp))
                                }
                                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "取消", modifier = Modifier.size(22.dp))
                                }
                            }
                            DownloadTask.Status.FAILED -> {
                                IconButton(onClick = onRetry, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Refresh, contentDescription = "重试", modifier = Modifier.size(22.dp))
                                }
                                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "移除", modifier = Modifier.size(22.dp))
                                }
                            }
                            DownloadTask.Status.COMPLETED -> {
                                IconButton(onClick = onOpenFolder, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = "打开文件", modifier = Modifier.size(22.dp))
                                }
                                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "移除", modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (task.status == DownloadTask.Status.DOWNLOADING || task.status == DownloadTask.Status.PAUSED) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
        }
    }
}

@Composable
private fun NewDownloadDialog(onDismiss: () -> Unit) {
    var url by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var isFolder by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun isValidFileName(name: String): Boolean {
        if (name.isBlank()) return false
        val invalidChars = setOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
        return name.none { it in invalidChars }
    }

    fun doStartDownload() {
        when {
            url.isBlank() -> errorMessage = "请输入下载地址"
            isFolder -> {
                val name = if (fileName.isNotBlank() && isValidFileName(fileName)) fileName
                else url.split("/").lastOrNull()?.takeIf { it.isNotBlank() } ?: "download"
                val added = AppDownloadManager.downloadFolder(url, name)
                if (!added) errorMessage = "任务已在列表中" else onDismiss()
            }
            else -> {
                val name = if (fileName.isNotBlank() && isValidFileName(fileName)) fileName
                else url.split("/").lastOrNull()?.split("?")?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "download"
                if (!isValidFileName(name)) {
                    errorMessage = "文件名包含非法字符: $name"
                    return
                }
                val added = AppDownloadManager.downloadFile(url, name)
                if (!added) errorMessage = "任务已在列表中" else onDismiss()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建下载") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        errorMessage = null
                    },
                    label = { Text("下载地址") },
                    placeholder = { Text("输入文件URL或服务器路径") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (!isFolder) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = {
                            fileName = it
                            errorMessage = null
                        },
                        label = { Text("保存文件名") },
                        placeholder = { Text("留空则自动从URL获取") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading,
                        isError = fileName.isNotBlank() && !isValidFileName(fileName),
                        supportingText = if (fileName.isNotBlank() && !isValidFileName(fileName)) {
                            { Text("文件名包含非法字符: \\ / : * ? \" < > |") }
                        } else null
                    )
                }
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { doStartDownload() },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("开始下载")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("取消") }
        }
    )
}

@Composable
private fun DownloadSettingsDialog(
    isContinueDownload: Boolean,
    downloadInterval: Long,
    overwriteFile: Boolean,
    folderFairContinue: Boolean,
    onContinueDownloadChanged: (Boolean) -> Unit,
    onDownloadIntervalChanged: (Long) -> Unit,
    onOverwriteFileChanged: (Boolean) -> Unit,
    onFolderFairContinueChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("下载设置") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("断点续传", style = MaterialTheme.typography.bodyMedium)
                        Text("支持从上次中断位置继续下载", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isContinueDownload, onCheckedChange = onContinueDownloadChanged)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("覆盖已存在文件", style = MaterialTheme.typography.bodyMedium)
                        Text("关闭后，已存在的文件将被跳过", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = overwriteFile, onCheckedChange = onOverwriteFileChanged)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("文件夹下载容错", style = MaterialTheme.typography.bodyMedium)
                        Text("单个文件失败时继续下载其余文件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = folderFairContinue, onCheckedChange = onFolderFairContinueChanged)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("下载间隔: ${downloadInterval}ms", style = MaterialTheme.typography.bodyMedium)
                Text("控制每次请求之间的延迟，避免服务器压力过大", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = downloadInterval.toFloat(),
                    onValueChange = { onDownloadIntervalChanged(it.toLong()) },
                    valueRange = 0f..1000f,
                    steps = 9
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("确定") }
        }
    )
}