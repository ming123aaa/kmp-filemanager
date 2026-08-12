package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ohuang.kmp.filemanager.kmp_filemanager.data.FileItem
import com.ohuang.kmp.filemanager.kmp_filemanager.data.FolderTreeNode
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.utils.ScreenType
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.utils.rememberScreenType



@Composable
fun CreateFolderDialog(show: Boolean, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    if (!show) return
    var folderName by remember { mutableStateOf("") }


    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WeightRow() {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = "Create folder",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("创建文件夹", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = folderName, onValueChange = { folderName = it },
                        label = { Text("文件夹名称") }, placeholder = { Text("请输入文件夹名称") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { if (folderName.trim().isNotEmpty()) onCreate(folderName.trim()) },
                            enabled = folderName.trim().isNotEmpty()
                        ) { Text("创建") }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateFileDialog(show: Boolean, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    if (!show) return
    var fileName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WeightRow {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NoteAdd,
                            contentDescription = "Create file",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("创建文件", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = fileName, onValueChange = { fileName = it },
                        label = { Text("文件名称") }, placeholder = { Text("请输入文件名称，如 test.txt") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { if (fileName.trim().isNotEmpty()) onCreate(fileName.trim()) },
                            enabled = fileName.trim().isNotEmpty()
                        ) { Text("创建") }
                    }
                }
            }
        }
    }
}

@Composable
fun RenameDialog(show: Boolean, file: FileItem?, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    if (!show || file == null) return
    var newName by remember { mutableStateOf(file.getFileName()) }

    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WeightRow {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Rename",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("重命名", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "将 \"${file.getFileName()}\" 重命名为:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("新名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { if (newName.trim().isNotEmpty()) onRename(newName.trim()) },
                            enabled = newName.trim().isNotEmpty() && newName != file.getFileName()
                        ) { Text("确定") }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteDialog(show: Boolean, file: FileItem?, onDismiss: () -> Unit, onDelete: () -> Unit) {
    if (!show || file == null) return

    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WeightRow() {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "删除确认",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (file.isFolder) "确定要删除文件夹 \"${file.getFileName()}\" 及其所有内容吗？" else "确定要删除文件 \"${file.getFileName()}\" 吗？",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                            Text(
                                "删除"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoveDialog(
    show: Boolean, file: FileItem?, folderTree: List<FolderTreeNode>, selectedPath: String,
    onDismiss: () -> Unit, onMove: (targetPath: String) -> Unit,
    onToggleFolder: (FolderTreeNode) -> Unit, onSelectPath: (String) -> Unit
) {
    if (!show || file == null) return

    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WeightRow() {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DriveFileMove,
                            contentDescription = "Move",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("移动文件", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "源文件: \"${file.getFileName()}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "选择目标位置:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small).padding(8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            folderTree.forEach { node ->
                                FolderTreeItem(
                                    node = node,
                                    depth = 0,
                                    selectedPath = selectedPath,
                                    onToggleFolder = onToggleFolder,
                                    onSelectPath = onSelectPath
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = { onMove(selectedPath) }) { Text("移动") }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderTreeItem(
    node: FolderTreeNode, depth: Int, selectedPath: String,
    onToggleFolder: (FolderTreeNode) -> Unit, onSelectPath: (String) -> Unit
) {
    val isSelected = node.path == selectedPath
    val mayHaveSubfolders = node.hasSubfolders ?: true

    Column {
        Surface(
            modifier = Modifier.fillMaxWidth().then(
                if (isSelected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.shapes.small
                ) else Modifier
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) else Color.Transparent,
            shape = MaterialTheme.shapes.small,
            onClick = {
                onSelectPath(node.path)
                if (!node.isExpanded && mayHaveSubfolders) onToggleFolder(node)
                else if (mayHaveSubfolders) onToggleFolder(node)
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = (depth * 16).dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mayHaveSubfolders) {
                    Box(
                        modifier = Modifier.size(32.dp).clickable { onToggleFolder(node) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (node.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = if (node.isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                                contentDescription = if (node.isExpanded) "折叠" else "展开",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(32.dp))
                }
                Icon(
                    imageVector = if (node.path.isEmpty()) Icons.Default.Home else Icons.Default.Folder,
                    contentDescription = "Folder",
                    tint = if (node.path.isEmpty()) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    node.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (node.isExpanded && node.children.isNotEmpty()) {
            node.children.forEach { childNode ->
                FolderTreeItem(
                    node = childNode,
                    depth = depth + 1,
                    selectedPath = selectedPath,
                    onToggleFolder = onToggleFolder,
                    onSelectPath = onSelectPath
                )
            }
        }
    }
}

@Composable
fun DownloadDialog(
    show: Boolean,
    file: FileItem?,
    isLocal: Boolean = false,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    if (!show || file == null) return
    val action = if (isLocal) "导出" else "下载"

    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WeightRow() {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isLocal) Icons.Default.Output else Icons.Default.Download,
                            contentDescription = if (isLocal) "export" else "Download",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "${action}文件",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "文件将保存到系统下载目录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "确定要${action} \"${file.getFileName()}\" 文件吗？",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!file.isFolder) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "文件大小: ${file.formatSize()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = onDownload) { Text(action) }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingDialog(show: Boolean, message: String = "加载中...") {
    if (!show) return
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        WeightRow(){
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BatchDeleteDialog(show: Boolean, selectedFiles: Set<FileItem>, onDismiss: () -> Unit, onDelete: () -> Unit) {
    if (!show || selectedFiles.isEmpty()) return
    val folderCount = selectedFiles.count { it.isFolder }
    val fileCount = selectedFiles.count { !it.isFolder }

    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WeightRow {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "批量删除确认",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        buildString {
                            append("确定要删除选中的 ${selectedFiles.size} 个项目吗？\n")
                            if (folderCount > 0) append("文件夹: $folderCount 个\n")
                            if (fileCount > 0) append("文件: $fileCount 个")
                        }.trim(),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                            Text(
                                "删除 (${selectedFiles.size})"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatchMoveDialog(
    show: Boolean, selectedFiles: Set<FileItem>, folderTree: List<FolderTreeNode>, selectedPath: String,
    onDismiss: () -> Unit, onMove: (targetPath: String) -> Unit,
    onToggleFolder: (FolderTreeNode) -> Unit, onSelectPath: (String) -> Unit
) {
    if (!show || selectedFiles.isEmpty()) return

    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WeightRow {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DriveFileMove,
                            contentDescription = "Move",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("批量移动", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val folderCount = selectedFiles.count { it.isFolder }
                    val fileCount = selectedFiles.count { !it.isFolder }
                    Text(
                        buildString {
                            append("已选中 ${selectedFiles.size} 个项目\n")
                            if (folderCount > 0) append("文件夹: $folderCount 个\n")
                            if (fileCount > 0) append("文件: $fileCount 个")
                        }.trim(),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "选择目标位置:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small).padding(8.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            folderTree.forEach { node ->
                                FolderTreeItem(
                                    node = node,
                                    depth = 0,
                                    selectedPath = selectedPath,
                                    onToggleFolder = onToggleFolder,
                                    onSelectPath = onSelectPath
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = { onMove(selectedPath) }) { Text("移动 (${selectedFiles.size})") }
                    }
                }
            }
        }
    }
}

@Composable
private fun  getDefaultWeight(): Float{
    val type = rememberScreenType()
    return when(type){
        ScreenType.DESKTOP->0.6f
        ScreenType.TABLET->0.8f
        else ->0.9f

    }
}

@Composable
fun WeightRow(modifier: Modifier = Modifier, weight: Float = getDefaultWeight(), content: @Composable () -> Unit) {

    val space = (1 - weight).coerceIn(0f, 1f)
    Row(modifier) {
        Spacer(modifier.weight(space / 2))
        Box(modifier.weight(weight.coerceIn(0f, 1f))) {
            content()
        }
        Spacer(modifier.weight(space / 2))
    }
}

@Composable
fun ExitConfirmDialog(show: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    if (!show) return

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WeightRow() {
            Card(
                modifier = Modifier.padding(horizontal = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Exit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("退出确认", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "确定要退出文件管理器吗？",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = onConfirm) { Text("退出") }
                    }
                }
            }
        }

    }
}

@Composable
fun BatchDownloadDialog(show: Boolean, selectedFiles: Set<FileItem>, onDismiss: () -> Unit, onDownload: () -> Unit) {
    if (!show || selectedFiles.isEmpty()) return
    val folderCount = selectedFiles.count { it.isFolder }
    val fileCount = selectedFiles.count { !it.isFolder }

    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        WeightRow {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("批量下载确认", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        buildString {
                            append("确定要下载选中的 ${selectedFiles.size} 个项目吗？\n")
                            if (folderCount > 0) append("文件夹: $folderCount 个\n")
                            if (fileCount > 0) append("文件: $fileCount 个")
                        }.trim(),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "文件将保存到系统下载目录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("取消") }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = { onDownload() }) { Text("下载 (${selectedFiles.size})") }
                    }
                }
            }
        }
    }
}