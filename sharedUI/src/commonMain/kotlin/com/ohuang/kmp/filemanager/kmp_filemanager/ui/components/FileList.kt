package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ohuang.kmp.filemanager.kmp_filemanager.PlatformType
import com.ohuang.kmp.filemanager.kmp_filemanager.data.FileItem
import com.ohuang.kmp.filemanager.kmp_filemanager.data.ViewMode
import com.ohuang.kmp.filemanager.kmp_filemanager.getPlatform
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileList(
    files: List<FileItem>,
    selectedFile: FileItem?,
    downloadEnable: Boolean,
    readOnly: Boolean,
    onFileClick: (FileItem) -> Unit,
    lazyGridState: LazyGridState,
    isLocalFile: Boolean = false,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    viewMode: ViewMode = ViewMode.GRID,
    getFileUrl: (FileItem) -> String = { "" },
    onPreview: (FileItem) -> Unit = {},
    onEditString: (FileItem) -> Unit = {},
    onDownload: (FileItem) -> Unit = {},
    onRename: (FileItem) -> Unit = {},
    onDelete: (FileItem) -> Unit = {},
    onMove: (FileItem) -> Unit = {},
    onCopyLink: (FileItem) -> Unit = {},
    onOpenInNew: (FileItem) -> Unit = {},
    isMultiSelectMode: Boolean = false,
    selectedFiles: Set<FileItem> = emptySet(),
    onToggleFileSelection: (FileItem) -> Unit = {}
) {
    var contextMenuFile by remember { mutableStateOf<FileItem?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var showScrollToTopButton by remember { mutableStateOf(false) }

    LaunchedEffect(lazyGridState.firstVisibleItemIndex) {
        showScrollToTopButton = lazyGridState.firstVisibleItemIndex > 1
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }

        if (files.isEmpty()) {
            EmptyState()
        } else {
            val minSize = if (viewMode == ViewMode.PREVIEW) {
                if (getPlatform().type == PlatformType.Desktop) {
                    220.dp
                } else {
                    170.dp
                }
            } else {
                if (getPlatform().type == PlatformType.Desktop) {
                    180.dp
                } else {
                    150.dp
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = minSize),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                state = lazyGridState
            ) {
                items(files) { file ->
                    val isFileSelected = selectedFiles.contains(file)

                    if (viewMode == ViewMode.PREVIEW) {
                        PreviewCard(
                            file = file,
                            fileUrl = getFileUrl(file),
                            onClick = { onFileClick(file) },
                            onLongClick = { contextMenuFile = file },
                            isLocalFile = isLocalFile,
                            isSelected = isFileSelected,
                            isMultiSelectMode = isMultiSelectMode,
                            onToggleSelection = { onToggleFileSelection(file) },
                            showContextMenu = contextMenuFile == file,
                            onContextMenuDismiss = { contextMenuFile = null },
                            onOpen = { f -> contextMenuFile = null; if (f.isFolder) onFileClick(f) else onPreview(f) },
                            onPreview = { f -> contextMenuFile = null; onPreview(f) },
                            onEditString = { f -> contextMenuFile = null; onEditString(f) },
                            onDownload = { f -> contextMenuFile = null; onDownload(f) },
                            onRename = { f -> contextMenuFile = null; onRename(f) },
                            onMove = { f -> contextMenuFile = null; onMove(f) },
                            onDelete = { f -> contextMenuFile = null; onDelete(f) },
                            onCopyLink = { f -> contextMenuFile = null; onCopyLink(f) },
                            onOpenInNew = { f -> contextMenuFile = null; onOpenInNew(f) },
                            downloadEnable = downloadEnable,
                            readOnly = readOnly,
                        )
                    } else {
                        FileCard(
                            file = file,
                            onClick = { onFileClick(file) },
                            onLongClick = { contextMenuFile = file },
                            isSelected = isFileSelected,
                            isMultiSelectMode = isMultiSelectMode,
                            onToggleSelection = { onToggleFileSelection(file) },
                            showContextMenu = contextMenuFile == file,
                            onContextMenuDismiss = { contextMenuFile = null },
                            isLocalFile = isLocalFile,
                            onOpen = { f -> contextMenuFile = null; if (f.isFolder) onFileClick(f) else onPreview(f) },
                            onPreview = { f -> contextMenuFile = null; onPreview(f) },
                            onEditString = { f -> contextMenuFile = null; onEditString(f) },
                            onDownload = { f -> contextMenuFile = null; onDownload(f) },
                            onRename = { f -> contextMenuFile = null; onRename(f) },
                            onMove = { f -> contextMenuFile = null; onMove(f) },
                            onDelete = { f -> contextMenuFile = null; onDelete(f) },
                            onCopyLink = { f -> contextMenuFile = null; onCopyLink(f) },
                            onOpenInNew = { f -> contextMenuFile = null; onOpenInNew(f) },
                            downloadEnable = downloadEnable,
                            readOnly = readOnly,
                        )
                    }
                }
                item { Column(modifier = Modifier.height(80.dp)) {} }
            }
        }

        AnimatedVisibility(
            visible = showScrollToTopButton,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch { lazyGridState.animateScrollToItem(0) }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = "滚动到顶部")
            }
        }

        AnimatedVisibility(
            visible = !showScrollToTopButton,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch { onRefresh() }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "刷新")
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(64.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = "Empty folder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "此目录为空",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击上方\"上传\"按钮添加文件",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}