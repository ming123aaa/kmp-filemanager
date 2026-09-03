package com.ohuang.kmp.filemanager.kmp_filemanager.ui.viewmodel

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohuang.kmp.filemanager.kmp_filemanager.Settings
import com.ohuang.kmp.filemanager.kmp_filemanager.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalFileViewModel(
    private val rootDir: String,
    initialSubDir: String? = null,
    private val settings: Settings? = null
) : ViewModel() {

    private val fileOps = LocalFileOperations(rootDir)

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath

    private val _selectedFile = MutableStateFlow<FileItem?>(null)
    val selectedFile: StateFlow<FileItem?> = _selectedFile

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _filterMode = MutableStateFlow(FilterMode.ALL)
    val filterMode: StateFlow<FilterMode> = _filterMode

    private val _sortBy = MutableStateFlow(SortBy.NAME)
    val sortBy: StateFlow<SortBy> = _sortBy

    private val _sortDirection = MutableStateFlow(SortDirection.ASC)
    val sortDirection: StateFlow<SortDirection> = _sortDirection

    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    val viewMode: StateFlow<ViewMode> = _viewMode

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _showToast = MutableStateFlow<String?>(null)
    val showToast: StateFlow<String?> = _showToast

    private val _showMkdirDialog = MutableStateFlow(false)
    val showMkdirDialog: StateFlow<Boolean> = _showMkdirDialog

    private val _showCreateFileDialog = MutableStateFlow(false)
    val showCreateFileDialog: StateFlow<Boolean> = _showCreateFileDialog

    private val _showRenameDialog = MutableStateFlow(false)
    val showRenameDialog: StateFlow<Boolean> = _showRenameDialog

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog

    private val _showMoveDialog = MutableStateFlow(false)
    val showMoveDialog: StateFlow<Boolean> = _showMoveDialog

    private val _showLoadingDialog = MutableStateFlow(false)
    val showLoadingDialog: StateFlow<Boolean> = _showLoadingDialog

    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog

    private val _navigateToTextEditor = MutableStateFlow<TextEditorNavData?>(null)
    val navigateToTextEditor: StateFlow<TextEditorNavData?> = _navigateToTextEditor

    private val _showDownloadDialog = MutableStateFlow(false)
    val showDownloadDialog: StateFlow<Boolean> = _showDownloadDialog

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode

    private val _selectedFiles = MutableStateFlow<Set<FileItem>>(emptySet())
    val selectedFiles: StateFlow<Set<FileItem>> = _selectedFiles

    private val _showBatchDeleteDialog = MutableStateFlow(false)
    val showBatchDeleteDialog: StateFlow<Boolean> = _showBatchDeleteDialog

    private val _showBatchMoveDialog = MutableStateFlow(false)
    val showBatchMoveDialog: StateFlow<Boolean> = _showBatchMoveDialog

    private val _showBatchExportDialog = MutableStateFlow(false)
    val showBatchExportDialog: StateFlow<Boolean> = _showBatchExportDialog

    private val _previewFile = MutableStateFlow<FileItem?>(null)
    val previewFile: StateFlow<FileItem?> = _previewFile

    private val _renameFile = MutableStateFlow<FileItem?>(null)
    val renameFile: StateFlow<FileItem?> = _renameFile

    private val _deleteFile = MutableStateFlow<FileItem?>(null)
    val deleteFile: StateFlow<FileItem?> = _deleteFile

    private val _moveFile = MutableStateFlow<FileItem?>(null)
    val moveFile: StateFlow<FileItem?> = _moveFile

    private val _exportFile = MutableStateFlow<FileItem?>(null)
    val exportFile: StateFlow<FileItem?> = _exportFile

    private val _editFileContent = MutableStateFlow("")
    val editFileContent: StateFlow<String> = _editFileContent

    private val _defaultEditMode = MutableStateFlow(false)
    val defaultEditMode: StateFlow<Boolean> = _defaultEditMode

    private val _moveTargetPath = MutableStateFlow("")
    val moveTargetPath: StateFlow<String> = _moveTargetPath

    private val _folderTree = MutableStateFlow<List<FolderTreeNode>>(emptyList())
    val folderTree: StateFlow<List<FolderTreeNode>> = _folderTree

    private var allFiles: List<FileItem> = emptyList()
    private var mLazyGridStateMap: SnapshotStateMap<String, LazyGridState> = mutableStateMapOf("" to LazyGridState())
    private var currentRelativePath: String = initialSubDir?.trim('/').orEmpty()

    val rootName: String get() = fileOps.rootName

    // ==================== 初始化 ====================

    init {
        loadSortState()
        loadFiles()
    }

    private fun loadSortState() {
        val s = settings ?: return
        try { _sortBy.value = SortBy.valueOf(s.getString("lfm_sortBy", SortBy.NAME.name)) } catch (_: Exception) {}
        try { _sortDirection.value = SortDirection.valueOf(s.getString("lfm_sortDir", SortDirection.ASC.name)) } catch (_: Exception) {}
        try { _viewMode.value = ViewMode.valueOf(s.getString("lfm_viewMode", ViewMode.GRID.name)) } catch (_: Exception) {}
    }

    private fun saveSortState() {
        val s = settings ?: return
        s.putString("lfm_sortBy", _sortBy.value.name)
        s.putString("lfm_sortDir", _sortDirection.value.name)
        s.putString("lfm_viewMode", _viewMode.value.name)
    }

    // ==================== 文件加载 ====================

    fun loadFiles() {
        viewModelScope.launch { loadFilesInternal() }
    }

    private suspend fun loadFilesInternal(isRefresh: Boolean = false) {
        if (isRefresh) delay(500)
        _isLoading.value = true
        _errorMessage.value = null
        try {
            withContext(Dispatchers.IO) {
                val exists = fileOps.isDirectory(currentRelativePath) || fileOps.exists(currentRelativePath, "")
                if (!exists) {
                    _errorMessage.value = "目录不存在: ${fileOps.getAbsolutePath(currentRelativePath)}"
                } else {
                    allFiles = fileOps.listFiles(currentRelativePath)
                    _currentPath.value = currentRelativePath
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "加载失败"
        } finally {
            _isLoading.value = false
            if (_errorMessage.value == null) applyFilters()
        }
    }

    suspend fun refreshFiles() {
        loadFilesInternal(isRefresh = true)
        if (_errorMessage.value == null) showToastMessage("刷新完成")
    }

    // ==================== 过滤与排序 ====================

    fun applyFilters() {
        var filtered = allFiles

        when (_filterMode.value) {
            FilterMode.FILES -> filtered = filtered.filter { !it.isFolder }
            FilterMode.FOLDERS -> filtered = filtered.filter { it.isFolder }
            FilterMode.ALL -> {}
        }

        if (_searchQuery.value.isNotEmpty()) {
            val query = _searchQuery.value.lowercase()
            filtered = filtered.filter { it.name.lowercase().contains(query) }
        }

        val comparator: Comparator<FileItem> = when (_sortBy.value) {
            SortBy.NAME -> compareBy { it.name.lowercase() }
            SortBy.SIZE -> compareBy { it.length }
            SortBy.DATE -> compareBy { it.lastModified }
        }
        val directionComparator = if (_sortDirection.value == SortDirection.DESC) comparator.reversed() else comparator
        _files.value = filtered.sortedWith(compareByDescending<FileItem> { it.isFolder }.then(directionComparator))
    }

    // ==================== 导航 ====================

    fun navigateToFolder(file: FileItem) {
        if (!file.isFolder) return
        currentRelativePath = if (currentRelativePath.isEmpty()) file.name else "$currentRelativePath/${file.name}"
        _selectedFile.value = null
        viewModelScope.launch { loadFilesInternal() }
    }

    fun goUp() {
        if (currentRelativePath.isEmpty()) return
        currentRelativePath = currentRelativePath.split("/").filter { it.isNotEmpty() }.dropLast(1).joinToString("/")
        _selectedFile.value = null
        viewModelScope.launch { loadFilesInternal() }
    }

    fun goToRelativePath(relPath: String) {
        currentRelativePath = relPath
        viewModelScope.launch { loadFilesInternal() }
    }

    fun canGoUp(): Boolean = currentRelativePath.isNotEmpty()

    fun getFullPath(file: FileItem): String = fileOps.getFullPath(currentRelativePath, file.name)
    fun getFileUrl(file: FileItem): String = getFullPath(file)

    // ==================== 状态设置 ====================

    fun setSelectedFile(file: FileItem?) { _selectedFile.value = file }
    fun setSearchQuery(query: String) { _searchQuery.value = query; applyFilters() }
    fun setFilterMode(mode: FilterMode) { _filterMode.value = mode; applyFilters() }
    fun setSortBy(sortBy: SortBy) { _sortBy.value = sortBy; applyFilters(); saveSortState() }
    fun setSortDirection(direction: SortDirection) { _sortDirection.value = direction; applyFilters(); saveSortState() }
    fun toggleSortDirection() {
        _sortDirection.value = if (_sortDirection.value == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
        applyFilters(); saveSortState()
    }
    fun setViewMode(mode: ViewMode) { _viewMode.value = mode; saveSortState() }

    fun getLazyGridState(): LazyGridState {
        val path = _currentPath.value
        return mLazyGridStateMap.getOrPut(path) { LazyGridState() }
    }

    // ==================== 文件操作 ====================

    fun createFolder(name: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) { fileOps.createFolder(currentRelativePath, name) }
                _showMkdirDialog.value = false
                if (success) { showToastMessage("文件夹创建成功"); loadFilesInternal() }
                else showToastMessage("文件夹已存在")
            } catch (e: Exception) {
                _showMkdirDialog.value = false
                showToastMessage(e.message ?: "创建失败")
            }
        }
    }

    fun createFile(name: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) { fileOps.createFile(currentRelativePath, name) }
                _showCreateFileDialog.value = false
                if (success) { showToastMessage("文件创建成功"); loadFilesInternal() }
                else showToastMessage("文件已存在")
            } catch (e: Exception) {
                _showCreateFileDialog.value = false
                showToastMessage(e.message ?: "创建失败")
            }
        }
    }

    fun renameFile(file: FileItem, newName: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    fileOps.renameFile(currentRelativePath, file.name, newName)
                }
                _showRenameDialog.value = false
                if (success) { showToastMessage("重命名成功"); loadFilesInternal() }
                else showToastMessage("重命名失败（目标名称已存在）")
            } catch (e: Exception) {
                _showRenameDialog.value = false
                showToastMessage(e.message ?: "重命名失败")
            }
        }
    }

    fun deleteFile(file: FileItem) {
        if (_isLoading.value) { showToastMessage("正在删除中,请稍后!"); return }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { fileOps.deleteFile(currentRelativePath, file.name) }
                _showDeleteDialog.value = false
                showToastMessage(if (file.isFolder) "文件夹删除成功" else "文件删除成功")
                loadFilesInternal()
            } catch (e: Exception) {
                _showDeleteDialog.value = false
                showToastMessage(e.message ?: "删除失败")
            }
        }
    }

    fun moveFile(file: FileItem, targetRelativePath: String) {
        if (_isLoading.value) { showToastMessage("正在移动中,请稍后!"); return }
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    fileOps.moveFile(currentRelativePath, file.name, targetRelativePath)
                }
                _showMoveDialog.value = false
                _showBatchMoveDialog.value = false
                if (success) { showToastMessage("移动成功"); loadFilesInternal() }
                else showToastMessage("移动失败（目标位置已存在同名文件）")
            } catch (e: Exception) {
                _showMoveDialog.value = false
                _showBatchMoveDialog.value = false
                showToastMessage(e.message ?: "移动失败")
            }
        }
    }

    // ==================== 文本编辑 ====================

    fun readFileContent(file: FileItem, defaultEditMode: Boolean = false) {
        viewModelScope.launch {
            val job = launch { delay(100); showLoadingDialog() }
            try {
                val content = withContext(Dispatchers.IO) {
                    fileOps.readText(currentRelativePath, file.name)
                }
                job.cancel()
                hideLoadingDialog()
                if (content != null) {
                    _previewFile.value = file
                    _editFileContent.value = content
                    _defaultEditMode.value = defaultEditMode
                    _navigateToTextEditor.value = TextEditorNavData(
                        getFullPath(file), file.getFileName(), content, file, defaultEditMode
                    )
                } else {
                    showToastMessage("文件过大或无法读取")
                }
            } catch (e: Exception) {
                job.cancel()
                hideLoadingDialog()
                showToastMessage(e.message ?: "读取失败")
            }
        }
    }

    fun saveFileContent(file: FileItem, content: String) {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { fileOps.writeText(currentRelativePath, file.name, content) }
                _isLoading.value = false
                _showEditDialog.value = false
                showToastMessage("保存成功")
            } catch (e: Exception) {
                _isLoading.value = false
                showToastMessage(e.message ?: "保存失败")
            }
        }
    }

    // ==================== 文件夹树 ====================

    private suspend fun listSubFolders(relativePath: String): List<FileItem> =
        withContext(Dispatchers.IO) {
            fileOps.listFiles(relativePath).filter { it.isFolder }
        }

    fun loadFolderTree(dirRelativePath: String) {
        viewModelScope.launch {
            val folders = listSubFolders(dirRelativePath)
            if (dirRelativePath.isEmpty()) {
                val rootNode = FolderTreeNode(
                    path = "", name = rootName, isExpanded = true,
                    children = folders.map { FolderTreeNode(path = it.name, name = it.name) }
                )
                _folderTree.value = listOf(rootNode)
            } else {
                updateFolderTreeWithChildren(dirRelativePath, folders)
            }
        }
    }

    fun toggleFolder(node: FolderTreeNode) {
        viewModelScope.launch {
            val currentTree = _folderTree.value.toMutableList()
            updateNodeInTree(currentTree, node.path) {
                if (it.isExpanded) it.copy(isExpanded = false)
                else it.copy(isExpanded = true, isLoading = true)
            }
            _folderTree.value = currentTree

            if (!node.isExpanded) {
                val folders = listSubFolders(node.path)
                val updatedTree = _folderTree.value.toMutableList()
                updateNodeInTree(updatedTree, node.path) {
                    it.copy(
                        isExpanded = true, isLoading = false,
                        hasSubfolders = folders.isNotEmpty(),
                        children = folders.map { f ->
                            val path = if (node.path.isEmpty()) f.name else "${node.path}/${f.name}"
                            FolderTreeNode(path = path, name = f.name)
                        }
                    )
                }
                _folderTree.value = updatedTree
            }
        }
    }

    private fun updateNodeInTree(
        tree: MutableList<FolderTreeNode>, targetPath: String,
        updater: (FolderTreeNode) -> FolderTreeNode
    ): Boolean {
        for (i in tree.indices) {
            if (tree[i].path == targetPath) { tree[i] = updater(tree[i]); return true }
            if (tree[i].children.isNotEmpty()) {
                val children = tree[i].children.toMutableList()
                if (updateNodeInTree(children, targetPath, updater)) {
                    tree[i] = tree[i].copy(children = children)
                    return true
                }
            }
        }
        return false
    }

    private fun updateFolderTreeWithChildren(dirRelativePath: String, folders: List<FileItem>) {
        val tree = _folderTree.value.toMutableList()
        updateNodeInTree(tree, dirRelativePath) {
            it.copy(
                hasSubfolders = folders.isNotEmpty(),
                children = folders.map { f ->
                    val path = if (dirRelativePath.isEmpty()) f.name else "$dirRelativePath/${f.name}"
                    FolderTreeNode(path = path, name = f.name)
                }
            )
        }
        _folderTree.value = tree
    }

    // ==================== 多选功能 ====================

    fun toggleMultiSelectMode() {
        _isMultiSelectMode.value = !_isMultiSelectMode.value
        if (!_isMultiSelectMode.value) _selectedFiles.value = emptySet()
    }
    fun exitMultiSelectMode() { _isMultiSelectMode.value = false; _selectedFiles.value = emptySet() }
    fun toggleFileSelection(file: FileItem) {
        _selectedFiles.value = _selectedFiles.value.toMutableSet().apply {
            if (contains(file)) remove(file) else add(file)
        }
    }
    fun selectAllFiles() { _selectedFiles.value = _files.value.toSet() }
    fun deselectAllFiles() { _selectedFiles.value = emptySet() }
    fun showBatchDeleteDialog() { if (_selectedFiles.value.isNotEmpty()) _showBatchDeleteDialog.value = true }
    fun hideBatchDeleteDialog() { _showBatchDeleteDialog.value = false }
    fun showBatchMoveDialog() {
        if (_selectedFiles.value.isNotEmpty()) { _moveTargetPath.value = ""; _showBatchMoveDialog.value = true; loadFolderTree("") }
    }
    fun hideBatchMoveDialog() { _showBatchMoveDialog.value = false; _moveTargetPath.value = ""; _folderTree.value = emptyList() }
    fun showBatchExportDialog() { if (_selectedFiles.value.isNotEmpty()) _showBatchExportDialog.value = true }
    fun hideBatchExportDialog() { _showBatchExportDialog.value = false }

    fun deleteSelectedFiles() {
        if (_isLoading.value) { showToastMessage("正在删除中,请稍后!"); return }
        if (_selectedFiles.value.isEmpty()) { showToastMessage("请先选择要删除的文件"); return }
        viewModelScope.launch {
            var successCount = 0; var failCount = 0
            withContext(Dispatchers.IO) {
                for (file in _selectedFiles.value) {
                    try {
                        if (fileOps.deleteFile(currentRelativePath, file.name)) successCount++ else failCount++
                    } catch (_: Exception) { failCount++ }
                }
            }
            _showBatchDeleteDialog.value = false
            showToastMessage(formatBatchResult("删除", successCount, failCount))
            _selectedFiles.value = emptySet(); _isMultiSelectMode.value = false
            loadFilesInternal()
        }
    }

    fun moveSelectedFiles(targetRelativePath: String) {
        if (_isLoading.value) { showToastMessage("正在移动中,请稍后!"); return }
        if (_selectedFiles.value.isEmpty()) { showToastMessage("请先选择要移动的文件"); return }
        viewModelScope.launch {
            var successCount = 0; var failCount = 0
            withContext(Dispatchers.IO) {
                for (file in _selectedFiles.value) {
                    try {
                        if (fileOps.moveFile(currentRelativePath, file.name, targetRelativePath)) successCount++ else failCount++
                    } catch (_: Exception) { failCount++ }
                }
            }
            _showBatchMoveDialog.value = false
            showToastMessage(formatBatchResult("移动", successCount, failCount))
            _selectedFiles.value = emptySet(); _isMultiSelectMode.value = false; _folderTree.value = emptyList()
            loadFilesInternal()
        }
    }

    private fun formatBatchResult(action: String, success: Int, fail: Int): String = when {
        fail == 0 -> "成功${action} $success 个项目"
        success == 0 -> "${action}失败"
        else -> "成功${action} $success 个，失败 $fail 个"
    }

    // ==================== 导出功能 ====================

    fun showExportDialog(file: FileItem) { _exportFile.value = file; _showDownloadDialog.value = true }
    fun hideExportDialog() { _showDownloadDialog.value = false; _exportFile.value = null }

    fun getExportFileList(): List<String> = _selectedFiles.value.map { getFullPath(it) }
    fun getCurrentExportFile(): String? = _exportFile.value?.let { getFullPath(it) }

    // ==================== 对话框控制 ====================

    fun showMkdirDialog() { _showMkdirDialog.value = true }
    fun hideMkdirDialog() { _showMkdirDialog.value = false }
    fun showCreateFileDialog() { _showCreateFileDialog.value = true }
    fun hideCreateFileDialog() { _showCreateFileDialog.value = false }
    fun showRenameDialog(file: FileItem) { _renameFile.value = file; _showRenameDialog.value = true }
    fun hideRenameDialog() { _showRenameDialog.value = false; _renameFile.value = null }
    fun showDeleteDialog(file: FileItem) { _deleteFile.value = file; _showDeleteDialog.value = true }
    fun hideDeleteDialog() { _showDeleteDialog.value = false; _deleteFile.value = null }
    fun showMoveDialog(file: FileItem) { _moveFile.value = file; _moveTargetPath.value = ""; _showMoveDialog.value = true; loadFolderTree("") }
    fun hideMoveDialog() { _showMoveDialog.value = false; _moveFile.value = null; _moveTargetPath.value = ""; _folderTree.value = emptyList() }
    fun setMoveTargetPath(path: String) { _moveTargetPath.value = path }
    fun showLoadingDialog() { _showLoadingDialog.value = true }
    fun hideLoadingDialog() { _showLoadingDialog.value = false }
    fun hideEditDialog() { _showEditDialog.value = false; _previewFile.value = null; _editFileContent.value = ""; _defaultEditMode.value = false }
    fun clearNavigateToTextEditor() { _navigateToTextEditor.value = null }
    fun showToastMessage(message: String) { _showToast.value = message; viewModelScope.launch { delay(2000); _showToast.value = null } }
    fun hideToastMessage() { _showToast.value = null }
}