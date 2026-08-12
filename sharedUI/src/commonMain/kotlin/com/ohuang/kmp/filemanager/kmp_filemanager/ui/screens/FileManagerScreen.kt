package com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ohuang.kmp.filemanager.kmp_filemanager.HttpConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.Settings
import com.ohuang.kmp.filemanager.kmp_filemanager.data.AppDownloadManager
import com.ohuang.kmp.filemanager.kmp_filemanager.data.TextEditorNavData
import com.ohuang.kmp.filemanager.kmp_filemanager.data.ViewMode
import com.ohuang.kmp.filemanager.kmp_filemanager.data.copyToClipboard
import com.ohuang.kmp.filemanager.kmp_filemanager.data.openUri
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.components.*
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.viewmodel.FileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    settings: Settings,
    goSetting: () -> Unit,
    goDownload: () -> Unit = {},
    goTextEditor: (TextEditorNavData) -> Unit = {},
    goUpload: () -> Unit = {},
    goMediaPreview: (List<MediaFileInfo>, Int) -> Unit = { _, _ -> },
    goVideoPlayer: (String, String) -> Unit = { _, _ -> },
    isTablet: Boolean = false,
    downloadEnable: Boolean = true,
    goUpSignal: Int = 0
) {
    val viewModel: FileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: kotlin.reflect.KClass<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return FileViewModel(settings) as T
            }
        }
    )
    val readOnly by HttpConfig.readOnly.collectAsState()
    val hasActiveDownloads by AppDownloadManager.hasActiveDownloads.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSortState()
    }

    val files by viewModel.files.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()

    LaunchedEffect(currentPath) {
        com.ohuang.kmp.filemanager.kmp_filemanager.FileManagerState.currentPath = currentPath
    }

    LaunchedEffect(goUpSignal) {
        if (goUpSignal > 0) viewModel.goUp()
    }

    val selectedFile by viewModel.selectedFile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showToast by viewModel.showToast.collectAsState()
    val showErrorToast by viewModel.showErrorToast.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(!isLoading) { if (!isLoading) isRefreshing = false }

    val showMkdirDialog by viewModel.showMkdirDialog.collectAsState()
    val showCreateFileDialog by viewModel.showCreateFileDialog.collectAsState()
    val showRenameDialog by viewModel.showRenameDialog.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val showMoveDialog by viewModel.showMoveDialog.collectAsState()
    val showDownloadDialog by viewModel.showDownloadDialog.collectAsState()
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsState()

    val renameFile by viewModel.renameFile.collectAsState()
    val deleteFile by viewModel.deleteFile.collectAsState()
    val moveFile by viewModel.moveFile.collectAsState()
    val downloadFile by viewModel.downloadFile.collectAsState()
    val moveTargetPath by viewModel.moveTargetPath.collectAsState()
    val folderTree by viewModel.folderTree.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val navigateToTextEditor by viewModel.navigateToTextEditor.collectAsState()

    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val showBatchDeleteDialog by viewModel.showBatchDeleteDialog.collectAsState()
    val showBatchMoveDialog by viewModel.showBatchMoveDialog.collectAsState()
    val showBatchDownloadDialog by viewModel.showBatchDownloadDialog.collectAsState()

    LaunchedEffect(isMultiSelectMode) {
        com.ohuang.kmp.filemanager.kmp_filemanager.FileManagerState.isMultiSelectMode = isMultiSelectMode
    }
    LaunchedEffect(Unit) {
        com.ohuang.kmp.filemanager.kmp_filemanager.FileManagerState.onExitMultiSelectMode = { viewModel.exitMultiSelectMode() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("文件查找...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                },
                actions = {
                    if (downloadEnable) {
                        Box {
                            IconButton(onClick = goDownload) {
                                Icon(Icons.Default.Download, contentDescription = "Downloads")
                            }
                            if (hasActiveDownloads) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(10.dp)
                                        .background(Color.Red, CircleShape)
                                )
                            }
                        }
                    }
                    IconButton(onClick = goSetting) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {

        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                val sortBy = viewModel.sortBy.collectAsState()
                val filterMode = viewModel.filterMode.collectAsState()
                val sortDirection = viewModel.sortDirection.collectAsState()

                Toolbar(
                    filterMode = filterMode.value,
                    onFilterModeChanged = { viewModel.setFilterMode(it) },
                    sortBy = sortBy.value,
                    sortDirection = sortDirection.value,
                    onSortChanged = {
                        viewModel.setSortBy(it)
                        viewModel.initPrefs("fm_sortBy", it.name)
                    },
                    onSortDirectionChanged = {
                        viewModel.toggleSortDirection()
                        viewModel.initPrefs("fm_sortDir", viewModel.sortDirection.value.name)
                    },
                    onUploadClick = { goUpload() },
                    onCreateFolderClick = { viewModel.showMkdirDialog() },
                    onCreateFileClick = { viewModel.showCreateFileDialog() },
                    onGoUpClick = { viewModel.goUp() },
                    canGoUp = currentPath.isNotEmpty(),
                    viewMode = viewMode,
                    onViewModeChanged = { newMode ->
                        viewModel.setViewMode(newMode)
                        viewModel.initPrefs("fm_viewMode", newMode.name)
                    },
                    isMultiSelectMode = isMultiSelectMode,
                    onToggleMultiSelectMode = { viewModel.toggleMultiSelectMode() },
                    downloadEnable = downloadEnable,
                    readOnly = readOnly,
                    isTablet = isTablet
                )

                HorizontalDivider()

                Breadcrumb(currentPath = currentPath, onNavigate = { viewModel.loadFiles(it) })

                HorizontalDivider()

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val lazyGridState = remember(currentPath) { viewModel.getLazyGridState() }
                    val rememberCoroutineScope = rememberCoroutineScope()

                    FileList(
                        files = files,
                        selectedFile = selectedFile,
                        isRefreshing = isRefreshing,
                        lazyGridState = lazyGridState,
                        viewMode = viewMode,
                        getFileUrl = { file -> viewModel.getFileUrl(file) },
                        onRefresh = {
                            rememberCoroutineScope.launch {
                                if (!isRefreshing) {
                                    isRefreshing = true
                                    viewModel.refreshFiles()
                                    isRefreshing = false
                                }
                            }
                        },
                        onFileClick = { file ->
                            if (file.isFolder) {
                                viewModel.navigateToFolder(file)
                                viewModel.setSelectedFile(null)
                            } else if (FileType.getFileType(file.name) == FileType.IMAGE) {
                                // 收集当前目录所有图片，构建预览列表
                                val mediaFiles = files.filter { !it.isFolder && FileType.getFileType(it.name) == FileType.IMAGE }
                                    .map { MediaFileInfo(url = viewModel.getFileUrl(it), name = it.name) }
                                val index = mediaFiles.indexOfFirst { it.name == file.name }.coerceAtLeast(0)
                                goMediaPreview(mediaFiles, index)
                            } else if (FileType.getFileType(file.name) == FileType.VIDEO) {
                                goVideoPlayer(viewModel.getFileUrl(file), file.name)
                            } else {
                                viewModel.setSelectedFile(file)
                                val canEdit = FileType.isEditStringType(file.name)
                                if (canEdit && !file.isWithinTextEditorLimit()) {
                                    viewModel.readFileContent(file)
                                } else {
                                    viewModel.showDownloadDialog(file)
                                }
                            }
                        },
                        onPreview = { file ->
                            if (file.isFolder) {
                                viewModel.navigateToFolder(file)
                                viewModel.setSelectedFile(null)
                            } else if (FileType.getFileType(file.name) == FileType.IMAGE) {
                                val mediaFiles = files.filter { !it.isFolder && FileType.getFileType(it.name) == FileType.IMAGE }
                                    .map { MediaFileInfo(url = viewModel.getFileUrl(it), name = it.name) }
                                val index = mediaFiles.indexOfFirst { it.name == file.name }.coerceAtLeast(0)
                                goMediaPreview(mediaFiles, index)
                            } else if (FileType.getFileType(file.name) == FileType.VIDEO) {
                                goVideoPlayer(viewModel.getFileUrl(file), file.name)
                            }
                        },
                        onEditString = { file -> viewModel.readFileContent(file, defaultEditMode = true) },
                        onDownload = { file -> viewModel.showDownloadDialog(file) },
                        onRename = { file -> viewModel.showRenameDialog(file) },
                        onDelete = { file -> viewModel.showDeleteDialog(file) },
                        onMove = { file -> viewModel.showMoveDialog(file) },
                        onCopyLink = { file ->
                                copyToClipboard(viewModel.getFileUrl(file))
                            },
                            onOpenInNew = { file ->
                                openUri(viewModel.getFileUrl(file))
                            },
                        isMultiSelectMode = isMultiSelectMode,
                        selectedFiles = selectedFiles,
                        onToggleFileSelection = { file -> viewModel.toggleFileSelection(file) },
                        downloadEnable = downloadEnable,
                        readOnly = readOnly
                    )

                    var isShowLoading by remember { mutableStateOf(false) }
                    LaunchedEffect(isLoading) {
                        if (isLoading) {
                            delay(100)
                            isShowLoading = true
                        } else {
                            isShowLoading = false
                        }
                    }

                    if (isShowLoading) {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }

                    if (!isLoading && errorMessage != null) {
                        ErrorState(errorMessage = errorMessage!!) { viewModel.loadFiles(currentPath) }
                    }
                }

                if (isMultiSelectMode) {
                    MultiSelectBottomBar(
                        selectedCount = selectedFiles.size,
                        totalCount = files.size,
                        onSelectAll = { viewModel.selectAllFiles() },
                        onDeselectAll = { viewModel.deselectAllFiles() },
                        onDelete = { viewModel.showBatchDeleteDialog() },
                        onMove = { viewModel.showBatchMoveDialog() },
                        onDownload = { viewModel.showBatchDownloadDialog() },
                        onCancel = { viewModel.exitMultiSelectMode() },
                        readOnly = readOnly,
                        downloadEnable = downloadEnable
                    )
                }
            }

            showErrorToast?.let {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = { TextButton(onClick = { viewModel.hideErrorToastMessage() }) { Text("关闭") } }
                ) { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            showToast?.let {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = { TextButton(onClick = { viewModel.hideToastMessage() }) { Text("关闭") } }
                ) { Text(it, color = MaterialTheme.colorScheme.onPrimary) }
            }
        }
    }

    if (!readOnly) {
        CreateFolderDialog(show = showMkdirDialog, onDismiss = { viewModel.hideMkdirDialog() }, onCreate = { viewModel.createFolder(it) })
        CreateFileDialog(show = showCreateFileDialog, onDismiss = { viewModel.hideCreateFileDialog() }, onCreate = { viewModel.createFile(it) })
        RenameDialog(show = showRenameDialog, file = renameFile, onDismiss = { viewModel.hideRenameDialog() }, onRename = { newName -> renameFile?.let { viewModel.renameFile(it, newName) } })
        DeleteDialog(show = showDeleteDialog, file = deleteFile, onDismiss = { viewModel.hideDeleteDialog() }, onDelete = { deleteFile?.let { viewModel.deleteFile(it) } })
        MoveDialog(
            show = showMoveDialog, file = moveFile, folderTree = folderTree, selectedPath = moveTargetPath,
            onDismiss = { viewModel.hideMoveDialog() }, onMove = { targetPath -> moveFile?.let { viewModel.moveFile(it, targetPath) } },
            onToggleFolder = { viewModel.toggleFolder(it) }, onSelectPath = { viewModel.setMoveTargetPath(it) }
        )
        BatchDeleteDialog(show = showBatchDeleteDialog, selectedFiles = selectedFiles, onDismiss = { viewModel.hideBatchDeleteDialog() }, onDelete = { viewModel.deleteSelectedFiles() })
        BatchMoveDialog(
            show = showBatchMoveDialog, selectedFiles = selectedFiles, folderTree = folderTree, selectedPath = moveTargetPath,
            onDismiss = { viewModel.hideBatchMoveDialog() }, onMove = { viewModel.moveSelectedFiles(it) },
            onToggleFolder = { viewModel.toggleFolder(it) }, onSelectPath = { viewModel.setMoveTargetPath(it) }
        )
    }

    LaunchedEffect(navigateToTextEditor) {
        navigateToTextEditor?.let {
            goTextEditor(it)
            viewModel.clearNavigateToTextEditor()
        }
    }

    if (downloadEnable) {
        DownloadDialog(show = showDownloadDialog, file = downloadFile, onDismiss = { viewModel.hideDownloadDialog() }, onDownload = { downloadFile?.let { viewModel.downloadFileOrFolder(it) } })
        BatchDownloadDialog(show = showBatchDownloadDialog, selectedFiles = selectedFiles, onDismiss = { viewModel.hideBatchDownloadDialog() }, onDownload = { viewModel.downloadSelectedFiles() })
    }

    LoadingDialog(show = showLoadingDialog)
}

@Composable
fun ErrorState(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Delete, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(errorMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("重试") }
    }
}

@Composable
fun MultiSelectBottomBar(
    selectedCount: Int,
    totalCount: Int,
    downloadEnable: Boolean,
    readOnly: Boolean,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("已选中 $selectedCount / $totalCount 项", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    TextButton(onClick = onSelectAll) { Text("全选") }
                    TextButton(onClick = onDeselectAll) { Text("取消全选") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                // 根据窗口宽度自适应：足够宽时显示一行，否则显示两行
                val buttonCount = listOf(!readOnly, !readOnly, downloadEnable, true).count { it }
                val useSingleRow = maxWidth >= 120.dp * buttonCount

                if (useSingleRow) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!readOnly) {
                            Button(onClick = onDelete, enabled = selectedCount > 0, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("删除")
                            }
                            Button(onClick = onMove, enabled = selectedCount > 0, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("移动")
                            }
                        }
                        if (downloadEnable) {
                            Button(onClick = onDownload, enabled = selectedCount > 0, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("下载")
                            }
                        }
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (!readOnly) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onDelete, enabled = selectedCount > 0, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("删除")
                                }
                                Button(onClick = onMove, enabled = selectedCount > 0, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("移动")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (downloadEnable) {
                                Button(onClick = onDownload, enabled = selectedCount > 0, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("下载")
                                }
                            }
                            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") }
                        }
                    }

                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}