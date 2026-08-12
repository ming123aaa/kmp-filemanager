package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.ohuang.kmp.filemanager.kmp_filemanager.data.FileItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewCard(
    file: FileItem,
    fileUrl: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isSelected: Boolean = false,
    isLocalFile: Boolean,
    downloadEnable: Boolean,
    readOnly: Boolean,
    isMultiSelectMode: Boolean = false,
    onToggleSelection: () -> Unit = {},
    showContextMenu: Boolean = false,
    onContextMenuDismiss: () -> Unit = {},
    onOpen: (FileItem) -> Unit = {},
    onPreview: (FileItem) -> Unit = {},
    onEditString: (FileItem) -> Unit = {},
    onDownload: (FileItem) -> Unit = {},
    onRename: (FileItem) -> Unit = {},
    onMove: (FileItem) -> Unit = {},
    onDelete: (FileItem) -> Unit = {},
    onCopyLink: (FileItem) -> Unit = {},
    onOpenInNew: (FileItem) -> Unit = {}
) {
    val fileType = if (file.isFolder) FileType.FOLDER else FileType.getFileType(file.name)

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(file) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press) {
                                val change = event.changes.firstOrNull()
                                if (change != null && change.pressed && event.buttons.isSecondaryPressed) {
                                    change.consume()
                                    if (!isMultiSelectMode) onLongClick()
                                }
                            }
                        }
                    }
                }
                .combinedClickable(
                    onClick = { if (isMultiSelectMode) onToggleSelection() else onClick() },
                    onLongClick = { if (!isMultiSelectMode) onLongClick() }
                )
                .padding(4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 12f)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val isImage = fileType == FileType.IMAGE
                        val isVideo = fileType == FileType.VIDEO

                        when {
                            isImage && !file.isFolder -> {
                                SubcomposeAsyncImage(
                                    model = fileUrl,
                                    contentDescription = file.name,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                    contentScale = ContentScale.Crop,
                                    error = {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            FileIcon(file = file, size = 64.dp)
                                        }
                                    },
                                    loading = {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                        }
                                    }
                                )
                            }
                            isVideo -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                ) {
                                    SubcomposeAsyncImage(
                                        model = fileUrl,
                                        contentDescription = file.name,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                        contentScale = ContentScale.Crop,
                                        error = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFF1A1A2E)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                FileIcon(file = file, size = 48.dp)
                                            }
                                        },
                                        loading = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFF1A1A2E)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    )
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = "视频",
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FileIcon(file = file, size = 84.dp)
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        Text(
                            text = file.getFileName(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (file.isFolder) FontWeight.Medium else FontWeight.Normal
                            ),
                            color = if (file.isFolder) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(file.formatSize(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(file.formatDate(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (isMultiSelectMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = onContextMenuDismiss,
            offset = DpOffset(8.dp, 0.dp)
        ) {
            if (file.isFolder) {
                DropdownMenuItem(
                    text = { Text("打开") },
                    onClick = { onOpen(file) },
                    leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFFF59E0B)) }
                )
                if (downloadEnable) {
                    DropdownMenuItem(
                        text = { Text(if (isLocalFile) "导出" else "下载") },
                        onClick = { onDownload(file) },
                        leadingIcon = { Icon(if (isLocalFile) Icons.Default.Output else Icons.Default.Download, contentDescription = null) }
                    )
                }
                DropdownMenuItem(
                    text = { Text(if (isLocalFile) "复制路径" else "复制链接") },
                    onClick = { onCopyLink(file) },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                )
                if (!isLocalFile) {
                    DropdownMenuItem(
                        text = { Text(if (isLocalFile) "在其他应用打开" else "在浏览器打开") },
                        onClick = { onOpenInNew(file) },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) }
                    )
                }
            } else {
                if (!readOnly && FileType.isEditStringType(file.name) && !file.isWithinTextEditorLimit()) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = { onEditString(file) },
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) }
                    )
                }
                if (!isLocalFile) {
                    DropdownMenuItem(
                        text = { Text("预览") },
                        onClick = { onPreview(file) },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                    )
                }
                if (downloadEnable) {
                    DropdownMenuItem(
                        text = { Text(if (isLocalFile) "导出" else "下载") },
                        onClick = { onDownload(file) },
                        leadingIcon = { Icon(if (isLocalFile) Icons.Default.Output else Icons.Default.Download, contentDescription = null) }
                    )
                }
                DropdownMenuItem(
                    text = { Text(if (isLocalFile) "复制路径" else "复制下载链接") },
                    onClick = { onCopyLink(file) },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(if (isLocalFile) "在其他应用打开" else "在浏览器打开") },
                    onClick = { onOpenInNew(file) },
                    leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) }
                )
            }

            if (!readOnly) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = { Text("重命名") },
                    onClick = { onRename(file) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("移动") },
                    onClick = { onMove(file) },
                    leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = { onDelete(file) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                )
            }
        }
    }
}