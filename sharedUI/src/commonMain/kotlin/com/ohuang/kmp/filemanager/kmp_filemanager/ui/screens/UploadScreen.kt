package com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ohuang.kmp.filemanager.kmp_filemanager.data.AppUploadManager
import com.ohuang.kmp.filemanager.kmp_filemanager.data.UploadFileInfo
import com.ohuang.kmp.filemanager.kmp_filemanager.data.UploadTask
import com.ohuang.kmp.filemanager.kmp_filemanager.data.awaitFilesInDirectory
import com.ohuang.kmp.filemanager.kmp_filemanager.data.fileSizeBytes
import com.ohuang.kmp.filemanager.kmp_filemanager.data.launchFilePicker
import com.ohuang.kmp.filemanager.kmp_filemanager.data.launchFolderPicker
import com.ohuang.kmp.filemanager.kmp_filemanager.data.listFilesInDirectory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    currentPath: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val tasks by AppUploadManager.tasks.collectAsState(emptyMap())
    val progressMessage by AppUploadManager.progressMessage.collectAsState()
    val hasActive by AppUploadManager.hasActiveUploads.collectAsState()

    var selectedFiles by remember { mutableStateOf<List<UploadFileInfo>>(emptyList()) }
    var selectedFolders by remember { mutableStateOf<List<String>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var hasCompleted by remember { mutableStateOf(false) }
    var wasActive by remember { mutableStateOf(false) }
    var job: Job? by remember { mutableStateOf(null) }

    // 检测上传完成: hasActive 从 true 变为 false
    LaunchedEffect(hasActive) {
        if (!hasActive && wasActive) {
            hasCompleted = true
        }
        wasActive = hasActive
    }

    val hasSelection = selectedFiles.isNotEmpty() || selectedFolders.isNotEmpty()

    fun pickFiles(isAppend: Boolean) {
        scope.launch(Dispatchers.IO) {
            launchFilePicker(allowMultiple = true) { paths ->
                val newFiles = paths.map { path ->
                    UploadFileInfo(
                        filePath = path,
                        fileName = path.substringAfterLast("/").substringAfterLast("\\"),
                        totalSize = fileSizeBytes(path)
                    )
                }
                selectedFiles = if (isAppend) {
                    (selectedFiles + newFiles).distinctBy { it.filePath }
                } else {
                    newFiles
                }
            }
        }
    }

    fun pickFolder() {
        scope.launch(Dispatchers.IO) {
            launchFolderPicker { path ->
                if (path != null) {
                    selectedFolders = (selectedFolders + path).distinct()
                }
            }
        }
    }

    fun startUpload(): Job? {
        if (selectedFiles.isEmpty() && selectedFolders.isEmpty()) return null
        val filesToUpload = selectedFiles.toMutableList()
        val foldersToScan = selectedFolders.toList()
        selectedFiles = emptyList()
        selectedFolders = emptyList()
        isScanning = true
        return scope.launch(Dispatchers.IO) {
            try {
                val folderFiles = foldersToScan.flatMap { folder ->
                    awaitFilesInDirectory(folder)
                }
                val allFiles = (filesToUpload + folderFiles).distinctBy { it.filePath }
                isScanning = false
                if (allFiles.isEmpty()) return@launch
                if (isActive) {
                    AppUploadManager.addFiles(allFiles, currentPath)
                }
            } catch (e: Exception) {

            } finally {
                isScanning = false
            }



        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空文件") },
            text = { Text("确定要清空已选择的所有文件和文件夹吗?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedFiles = emptyList()
                    selectedFolders = emptyList()
                    showClearDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("上传文件") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 上传路径
                Text(
                    text = "文件上传位置:\n根目录 >" + currentPath.replace("/", ">"),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                // 大图标 - 点击选择文件
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(80.dp)
                        .clickable {
                            if (!hasActive && !isScanning) {
                                pickFiles(false)
                            }
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 文件计数
                val totalSelection = selectedFiles.size + selectedFolders.size
                Text(
                    text = if (totalSelection == 0) "未选择文件" else "已选择 $totalSelection 个文件",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 已选文件/文件夹列表
                if (hasSelection) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showClearDialog = true },
                                enabled = !hasActive && !isScanning,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("全部清空")
                            }
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            items(selectedFolders) { folder ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = folder.substringAfterLast("/").substringAfterLast("\\"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = {
                                            selectedFolders = selectedFolders.filter { it != folder }
                                        },
                                        enabled = !hasActive && !isScanning
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            items(selectedFiles) { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = file.fileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = {
                                            selectedFiles = selectedFiles.filter { it.filePath != file.filePath }
                                        },
                                        enabled = !hasActive && !isScanning
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 操作区域
                when {
                    hasActive || isScanning -> {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "正在扫描文件夹...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            // 总上传进度
                            val uploadingTasks = tasks.values.filter {
                                it.status == UploadTask.Status.UPLOADING || it.status == UploadTask.Status.PENDING
                            }
                            val info by AppUploadManager.uploadInfo.collectAsState()
                            val totalSize = info.totalSize
                            val totalUploaded = info.currentSize
                            val totalPercent = if (totalSize > 0) (totalUploaded * 100 / totalSize).toInt() else 0

                            Text(
                                text = "总进度: ${totalPercent}% (${formatBytes(totalUploaded)}/${formatBytes(totalSize)}) ${info.completeFiles}/${info.files}个文件",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (totalSize > 0) totalUploaded.toFloat() / totalSize else 0f },
                                modifier = Modifier.fillMaxWidth(),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // 各文件上传进度
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 250.dp)
                                ) {
                                    items(uploadingTasks.sortedBy { it.id }) { task ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = task.file.fileName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = task.formatProgress(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = {
                                                    if (task.file.totalSize > 0)
                                                        task.uploadedSize.toFloat() / task.file.totalSize
                                                    else 0f
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                trackColor = MaterialTheme.colorScheme.surface,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                job?.cancel()
                                AppUploadManager.cancelTask()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("停止")
                        }
                    }

                    hasCompleted -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = progressMessage.ifEmpty { "上传完成" },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    hasCompleted = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("继续上传")
                            }
                            Button(
                                onClick = { onBack() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("返回")
                            }
                        }
                    }

                    else -> {
                        if (!hasSelection) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { pickFiles(false) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("选择文件")
                                }
                                OutlinedButton(
                                    onClick = { pickFolder() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("选择文件夹")
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { pickFiles(true) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("添加文件")
                                }
                                OutlinedButton(
                                    onClick = { pickFolder() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("添加文件夹")
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = progressMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { job = startUpload() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("开始上传")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) "${size.toLong()} ${units[unitIndex]}"
    else "%.1f %s".format(size, units[unitIndex])
}
