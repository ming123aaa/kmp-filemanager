package com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ohuang.kmp.filemanager.kmp_filemanager.Settings
import com.ohuang.kmp.filemanager.kmp_filemanager.data.TextEditorNavData
import com.ohuang.kmp.filemanager.kmp_filemanager.data.ViewMode
import com.ohuang.kmp.filemanager.kmp_filemanager.data.copyToClipboard
import com.ohuang.kmp.filemanager.kmp_filemanager.data.openLocalFile
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.components.*
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.viewmodel.LocalFileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalFileManagerScreen(
    rootDir: String,
    initialSubDir: String? = null,
    settings: Settings? = null,
    readOnly: Boolean = false,
    downloadEnable: Boolean = true,
    onBack: () -> Unit,
    goTextEditor: (TextEditorNavData) -> Unit = {},
    goMediaPreview: (List<MediaFileInfo>, Int) -> Unit = { _, _ -> },
    goVideoPlayer: (String, String) -> Unit = { _, _ -> },
    onImportFiles: ((currentRelativePath: String, onComplete: () -> Unit) -> Unit) = { _, _ -> },
    onExportFile: ((filePath: String, onComplete: () -> Unit) -> Unit) = { _, _ -> },
    onExportFiles: ((filePaths: List<String>, onComplete: () -> Unit) -> Unit) = { _, _ -> }
) {
    val viewModel: LocalFileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: kotlin.reflect.KClass<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return LocalFileViewModel(rootDir, initialSubDir, settings) as T
            }
        }
    )

    val files by viewModel.files.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showToast by viewModel.showToast.collectAsState()
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
    val exportFile by viewModel.exportFile.collectAsState()
    val moveTargetPath by viewModel.moveTargetPath.collectAsState()
    val folderTree by viewModel.folderTree.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val navigateToTextEditor by viewModel.navigateToTextEditor.collectAsState()

    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val showBatchDeleteDialog by viewModel.showBatchDeleteDialog.collectAsState()
    val showBatchMoveDialog by viewModel.showBatchMoveDialog.collectAsState()
    val showBatchExportDialog by viewModel.showBatchExportDialog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("文件查找...") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
                    onSortChanged = { viewModel.setSortBy(it) },
                    onSortDirectionChanged = { viewModel.toggleSortDirection() },
                    onUploadClick = {
                        onImportFiles(viewModel.currentPath.value, { viewModel.loadFiles() })
                    },
                    onCreateFolderClick = { viewModel.showMkdirDialog() },
                    onCreateFileClick = { viewModel.showCreateFileDialog() },
                    onGoUpClick = { viewModel.goUp() },
                    canGoUp = viewModel.canGoUp(),
                    viewMode = viewMode,
                    onViewModeChanged = { viewModel.setViewMode(it) },
                    isMultiSelectMode = isMultiSelectMode,
                    onToggleMultiSelectMode = { viewModel.toggleMultiSelectMode() },
                    isLocalFile = true,
                    downloadEnable = downloadEnable,
                    readOnly = readOnly
                )

                HorizontalDivider()

                Breadcrumb(
                    currentPath = currentPath,
                    onNavigate = { viewModel.goToRelativePath(it) },
                    rootLabel = viewModel.rootName
                )

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
                        isLocalFile = true,
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
                            } else {
                                handleLocalFileClick(
                                    file = file,
                                    files = files,
                                    viewModel = viewModel,
                                    goMediaPreview = goMediaPreview,
                                    goVideoPlayer = goVideoPlayer
                                )
                            }
                        },
                        onPreview = { file ->
                            if (file.isFolder) {
                                viewModel.navigateToFolder(file)
                                viewModel.setSelectedFile(null)
                            } else {
                                handleLocalFilePreview(file, files, viewModel, goMediaPreview, goVideoPlayer)
                            }
                        },
                        onEditString = { file -> viewModel.readFileContent(file, defaultEditMode = true) },
                        onDownload = { file ->
                            if (onExportFile != null) {
                                viewModel.showExportDialog(file)
                            }
                        },
                        onRename = { file -> viewModel.showRenameDialog(file) },
                        onDelete = { file -> viewModel.showDeleteDialog(file) },
                        onMove = { file -> viewModel.showMoveDialog(file) },
                        onCopyLink = { file ->
                            copyToClipboard(viewModel.getFullPath(file))
                        },
                        onOpenInNew = { file ->
                            openLocalFile(viewModel.getFullPath(file))
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
                        ErrorState(errorMessage = errorMessage!!) { viewModel.loadFiles() }
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
                    onDownload = { viewModel.showBatchExportDialog() },
                    onCancel = { viewModel.exitMultiSelectMode() },
                    downloadEnable = downloadEnable,
                    readOnly = readOnly,
                    isLocalFile = true,
                )
            }
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
        CreateFolderDialog(
            show = showMkdirDialog,
            onDismiss = { viewModel.hideMkdirDialog() },
            onCreate = { viewModel.createFolder(it) })
        CreateFileDialog(
            show = showCreateFileDialog,
            onDismiss = { viewModel.hideCreateFileDialog() },
            onCreate = { viewModel.createFile(it) })
        RenameDialog(
            show = showRenameDialog,
            file = renameFile,
            onDismiss = { viewModel.hideRenameDialog() },
            onRename = { newName -> renameFile?.let { viewModel.renameFile(it, newName) } })
        DeleteDialog(
            show = showDeleteDialog,
            file = deleteFile,
            onDismiss = { viewModel.hideDeleteDialog() },
            onDelete = { deleteFile?.let { viewModel.deleteFile(it) } })
        MoveDialog(
            show = showMoveDialog,
            file = moveFile,
            folderTree = folderTree,
            selectedPath = moveTargetPath,
            onDismiss = { viewModel.hideMoveDialog() },
            onMove = { targetPath -> moveFile?.let { viewModel.moveFile(it, targetPath) } },
            onToggleFolder = { viewModel.toggleFolder(it) },
            onSelectPath = { viewModel.setMoveTargetPath(it) }
        )
        BatchDeleteDialog(
            show = showBatchDeleteDialog,
            selectedFiles = selectedFiles,
            onDismiss = { viewModel.hideBatchDeleteDialog() },
            onDelete = { viewModel.deleteSelectedFiles() })
        BatchMoveDialog(
            show = showBatchMoveDialog,
            selectedFiles = selectedFiles,
            folderTree = folderTree,
            selectedPath = moveTargetPath,
            onDismiss = { viewModel.hideBatchMoveDialog() },
            onMove = { viewModel.moveSelectedFiles(it) },
            onToggleFolder = { viewModel.toggleFolder(it) },
            onSelectPath = { viewModel.setMoveTargetPath(it) }
        )
    }

    LaunchedEffect(navigateToTextEditor) {
        navigateToTextEditor?.let {
            goTextEditor(it)
            viewModel.clearNavigateToTextEditor()
        }
    }

    if (downloadEnable && onExportFile != null) {
        DownloadDialog(
            show = showDownloadDialog,
            file = exportFile,
            isLocal = true,
            onDismiss = { viewModel.hideExportDialog() },
            onDownload = {
                exportFile?.let { file ->
                    onExportFile(viewModel.getFullPath(file)) { viewModel.hideExportDialog() }
                }
            }
        )
        if (onExportFiles != null) {
            BatchDownloadDialog(
                show = showBatchExportDialog,
                selectedFiles = selectedFiles,
                onDismiss = { viewModel.hideBatchExportDialog() },
                onDownload = {
                    onExportFiles(viewModel.getExportFileList()) {
                        viewModel.hideBatchExportDialog()
                        viewModel.exitMultiSelectMode()
                    }
                }
            )
        }
    }

    LoadingDialog(show = showLoadingDialog)
}

private fun handleLocalFileClick(
    file: com.ohuang.kmp.filemanager.kmp_filemanager.data.FileItem,
    files: List<com.ohuang.kmp.filemanager.kmp_filemanager.data.FileItem>,
    viewModel: LocalFileViewModel,
    goMediaPreview: (List<MediaFileInfo>, Int) -> Unit,
    goVideoPlayer: (String, String) -> Unit
) {
    viewModel.setSelectedFile(file)
    when {
        FileType.isMediaType(file.name) -> {
            val mediaFiles = buildLocalMediaFileList(files, viewModel)
            val idx = mediaFiles.indexOfFirst { it.name == file.getFileName() }.coerceAtLeast(0)
            goMediaPreview(mediaFiles, idx)
        }

        FileType.isEditStringType(file.name) && !file.isWithinTextEditorLimit() ->
            viewModel.readFileContent(file)

        else -> openLocalFile(viewModel.getFullPath(file))
    }
}

private fun handleLocalFilePreview(
    file: com.ohuang.kmp.filemanager.kmp_filemanager.data.FileItem,
    files: List<com.ohuang.kmp.filemanager.kmp_filemanager.data.FileItem>,
    viewModel: LocalFileViewModel,
    goMediaPreview: (List<MediaFileInfo>, Int) -> Unit,
    goVideoPlayer: (String, String) -> Unit
) {
    when {
        FileType.isMediaType(file.name) -> {
            val mediaFiles = buildLocalMediaFileList(files, viewModel)
            val idx = mediaFiles.indexOfFirst { it.name == file.getFileName() }.coerceAtLeast(0)
            goMediaPreview(mediaFiles, idx)
        }

        else -> openLocalFile(viewModel.getFullPath(file))
    }
}

private fun buildLocalMediaFileList(
    files: List<com.ohuang.kmp.filemanager.kmp_filemanager.data.FileItem>,
    viewModel: LocalFileViewModel
): List<MediaFileInfo> = files
    .filter { !it.isFolder && FileType.isMediaType(it.name) }
    .map { MediaFileInfo(url = viewModel.getFullPath(it), name = it.getFileName()) }