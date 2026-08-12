package com.ohuang.kmp.filemanager.kmp_filemanager.ui.viewmodel

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohuang.kmp.filemanager.kmp_filemanager.ApiService
import com.ohuang.kmp.filemanager.kmp_filemanager.Settings
import com.ohuang.kmp.filemanager.kmp_filemanager.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FileViewModel(private val settings: Settings) : ViewModel() {
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

    private val _showErrorToast = MutableStateFlow<String?>(null)
    val showErrorToast: StateFlow<String?> = _showErrorToast

    private val _showMkdirDialog = MutableStateFlow(false)
    val showMkdirDialog: StateFlow<Boolean> = _showMkdirDialog

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

    private val _showCreateFileDialog = MutableStateFlow(false)
    val showCreateFileDialog: StateFlow<Boolean> = _showCreateFileDialog

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode

    private val _selectedFiles = MutableStateFlow<Set<FileItem>>(emptySet())
    val selectedFiles: StateFlow<Set<FileItem>> = _selectedFiles

    private val _showBatchDeleteDialog = MutableStateFlow(false)
    val showBatchDeleteDialog: StateFlow<Boolean> = _showBatchDeleteDialog

    private val _showBatchMoveDialog = MutableStateFlow(false)
    val showBatchMoveDialog: StateFlow<Boolean> = _showBatchMoveDialog

    private val _showBatchDownloadDialog = MutableStateFlow(false)
    val showBatchDownloadDialog: StateFlow<Boolean> = _showBatchDownloadDialog

    private val _previewFile = MutableStateFlow<FileItem?>(null)
    val previewFile: StateFlow<FileItem?> = _previewFile

    private val _renameFile = MutableStateFlow<FileItem?>(null)
    val renameFile: StateFlow<FileItem?> = _renameFile

    private val _deleteFile = MutableStateFlow<FileItem?>(null)
    val deleteFile: StateFlow<FileItem?> = _deleteFile

    private val _moveFile = MutableStateFlow<FileItem?>(null)
    val moveFile: StateFlow<FileItem?> = _moveFile

    private val _editFileContent = MutableStateFlow("")
    val editFileContent: StateFlow<String> = _editFileContent

    private val _defaultEditMode = MutableStateFlow(false)
    val defaultEditMode: StateFlow<Boolean> = _defaultEditMode

    private val _downloadFile = MutableStateFlow<FileItem?>(null)
    val downloadFile: StateFlow<FileItem?> = _downloadFile

    private val _moveTargetPath = MutableStateFlow("")
    val moveTargetPath: StateFlow<String> = _moveTargetPath

    private val _folderTree = MutableStateFlow<List<FolderTreeNode>>(emptyList())
    val folderTree: StateFlow<List<FolderTreeNode>> = _folderTree

    private var allFiles: List<FileItem> = emptyList()

    private var mLazyGridStateMap: SnapshotStateMap<String, LazyGridState> = mutableStateMapOf("" to LazyGridState())

    init {
        loadFiles()
    }

    fun initPrefs(key: String, value: String) {
        settings.putString(key, value)
    }

    fun getPrefs(key: String, default: String): String = settings.getString(key, default)

    fun getLazyGridState(): LazyGridState {
        val path = _currentPath.value
        val lazyGridState = if (mLazyGridStateMap.contains(path)) {
            mLazyGridStateMap[path]!!
        } else {
            LazyGridState().apply { mLazyGridStateMap[path] = this@apply }
        }
        val removeKeys = mLazyGridStateMap.filterKeys { it.isNotBlank() && !path.startsWith(it) }.map { it.key }
        removeKeys.forEach { mLazyGridStateMap.remove(it) }
        return lazyGridState
    }

    fun loadFiles(path: String = "") {
        viewModelScope.launch { requestFiles(path) }
    }

    private suspend fun requestFiles(path: String, isRefresh: Boolean = false): Boolean {
        if (isRefresh) delay(500)
        _isLoading.value = true
        _errorMessage.value = null
        try {
            val data = ApiService.getAllFiles(path, true)
            _isLoading.value = false
            allFiles = data
            _currentPath.value = path
            applyFilters()
            return true
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "未知错误"
            _isLoading.value = false
            return false
        }
    }

    suspend fun refreshFiles() {
        if (requestFiles(_currentPath.value, true)) showToastMessage("刷新完成")
    }

    fun applyFilters() {
        var filtered = allFiles
        when (_filterMode.value) {
            FilterMode.ALL -> {}
            FilterMode.FILES -> filtered = filtered.filter { !it.isFolder }
            FilterMode.FOLDERS -> filtered = filtered.filter { it.isFolder }
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
        filtered = filtered.sortedWith(compareByDescending<FileItem> { it.isFolder }.then(directionComparator))
        _files.value = filtered
    }

    fun setSelectedFile(file: FileItem?) { _selectedFile.value = file }
    fun setSearchQuery(query: String) { _searchQuery.value = query; applyFilters() }
    fun setFilterMode(mode: FilterMode) { _filterMode.value = mode; applyFilters() }
    fun setSortBy(sortBy: SortBy) { _sortBy.value = sortBy; applyFilters() }
    fun setSortDirection(direction: SortDirection) { _sortDirection.value = direction; applyFilters() }
    fun toggleSortDirection() {
        _sortDirection.value = if (_sortDirection.value == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
        applyFilters()
    }

    fun loadSortState() {
        val savedSortBy = getPrefs("fm_sortBy", SortBy.NAME.name)
        val savedSortDir = getPrefs("fm_sortDir", SortDirection.ASC.name)
        val viewMode = getPrefs("fm_viewMode", ViewMode.GRID.name)
        try { _sortBy.value = SortBy.valueOf(savedSortBy) } catch (_: Exception) {}
        try { _sortDirection.value = SortDirection.valueOf(savedSortDir) } catch (_: Exception) {}
        try { _viewMode.value = ViewMode.valueOf(viewMode) } catch (_: Exception) {}
        applyFilters()
    }

    fun toggleViewMode() { _viewMode.value = if (_viewMode.value == ViewMode.GRID) ViewMode.PREVIEW else ViewMode.GRID }
    fun setViewMode(mode: ViewMode) { _viewMode.value = mode }

    fun createFolder(name: String) {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = ApiService.createFolder(name, _currentPath.value)
                _isLoading.value = false
                _showMkdirDialog.value = false
                if (result.contains("成功")) { showToastMessage("文件夹创建成功"); loadFiles(_currentPath.value) }
                else showToastMessage(result)
            } catch (e: Exception) {
                _isLoading.value = false
                _showMkdirDialog.value = false
                showErrorToastMessage(e.message ?: "未知错误")
            }
        }
    }

    fun renameFile(file: FileItem, newName: String) {
        if (_isLoading.value) return
        _isLoading.value = true
        val fullPath = getFullPath(file)
        viewModelScope.launch {
            try {
                val result = ApiService.renameFile(fullPath, newName)
                _isLoading.value = false
                _showRenameDialog.value = false
                if (result.contains("成功")) { showToastMessage("重命名成功"); loadFiles(_currentPath.value) }
                else showToastMessage(result)
            } catch (e: Exception) {
                _isLoading.value = false
                _showRenameDialog.value = false
                showErrorToastMessage(e.message ?: "未知错误")
            }
        }
    }

    fun deleteFile(file: FileItem) {
        if (_isLoading.value) { showToastMessage("正在删除中,请稍后!"); return }
        _isLoading.value = true
        val fullPath = getFullPath(file)
        viewModelScope.launch {
            try {
                val result = ApiService.deleteFile(fullPath)
                _isLoading.value = false
                _showDeleteDialog.value = false
                if (result.contains("成功")) { showToastMessage(if (file.isFolder) "文件夹删除成功" else "文件删除成功"); loadFiles(_currentPath.value) }
                else showToastMessage(result)
            } catch (e: Exception) {
                _isLoading.value = false
                _showDeleteDialog.value = false
                showErrorToastMessage(e.message ?: "未知错误")
            }
        }
    }

    fun moveFile(file: FileItem, targetPath: String) {
        if (_isLoading.value) { showToastMessage("正在移动中,请稍后!"); return }
        _isLoading.value = true
        val fullPath = getFullPath(file)
        viewModelScope.launch {
            try {
                val result = ApiService.moveFile(fullPath, targetPath)
                _isLoading.value = false
                _showMoveDialog.value = false
                if (result.contains("成功")) { showToastMessage("移动成功"); loadFiles(_currentPath.value) }
                else showToastMessage(result)
            } catch (e: Exception) {
                _isLoading.value = false
                _showMoveDialog.value = false
                showErrorToastMessage(e.message ?: "未知错误")
            }
        }
    }

    fun readFileContent(file: FileItem, defaultEditMode: Boolean = false) {
        val fullPath = getFullPath(file)
        viewModelScope.launch {
            val job = launch { delay(100); showLoadingDialog() }
            try {
                val content = ApiService.readText(fullPath)
                job.cancel()
                hideLoadingDialog()
                _previewFile.value = file
                _editFileContent.value = content
                _defaultEditMode.value = defaultEditMode
                _navigateToTextEditor.value = TextEditorNavData(fullPath, file.getFileName(), content, file, defaultEditMode)
            } catch (e: Exception) {
                job.cancel()
                hideLoadingDialog()
                showErrorToastMessage(e.message ?: "未知错误")
            }
        }
    }

    fun saveFileContent(file: FileItem, content: String) {
        if (_isLoading.value) { showToastMessage("正在保存中,请稍后!"); return }
        _isLoading.value = true
        val fullPath = getFullPath(file)
        viewModelScope.launch {
            try {
                val result = ApiService.writeText(fullPath, content)
                _isLoading.value = false
                if (result.contains("成功")) { showToastMessage("保存成功"); _showEditDialog.value = false }
                else showToastMessage(result)
            } catch (e: Exception) {
                _isLoading.value = false
                showToastMessage(e.message ?: "未知错误")
            }
        }
    }

    fun navigateToFolder(file: FileItem) {
        if (file.isFolder) {
            val newPath = if (_currentPath.value.isEmpty()) file.name else "${_currentPath.value}/${file.name}"
            loadFiles(newPath)
            _selectedFile.value = null
        }
    }

    fun goUp() {
        if (_currentPath.value.isEmpty()) return
        val parts = _currentPath.value.split("/").filter { it.isNotEmpty() }
        val newPath = parts.dropLast(1).joinToString("/")
        loadFiles(newPath)
        _selectedFile.value = null
    }

    fun showMkdirDialog() { _showMkdirDialog.value = true }
    fun hideMkdirDialog() { _showMkdirDialog.value = false }
    fun showCreateFileDialog() { _showCreateFileDialog.value = true }
    fun hideCreateFileDialog() { _showCreateFileDialog.value = false }

    fun createFile(name: String) {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = ApiService.createFile(name, _currentPath.value)
                _isLoading.value = false
                _showCreateFileDialog.value = false
                if (result.contains("成功")) { showToastMessage("文件创建成功"); loadFiles(_currentPath.value) }
                else showToastMessage(result)
            } catch (e: Exception) {
                _isLoading.value = false
                _showCreateFileDialog.value = false
                showToastMessage(e.message ?: "未知错误")
            }
        }
    }

    fun showRenameDialog(file: FileItem) { _renameFile.value = file; _showRenameDialog.value = true }
    fun hideRenameDialog() { _showRenameDialog.value = false; _renameFile.value = null }
    fun showDeleteDialog(file: FileItem) { _deleteFile.value = file; _showDeleteDialog.value = true }
    fun hideDeleteDialog() { _showDeleteDialog.value = false; _deleteFile.value = null }
    fun showMoveDialog(file: FileItem) { _moveFile.value = file; _moveTargetPath.value = ""; _showMoveDialog.value = true; loadFolderTree("") }
    fun hideMoveDialog() { _showMoveDialog.value = false; _moveFile.value = null; _moveTargetPath.value = ""; _folderTree.value = emptyList() }
    fun setMoveTargetPath(path: String) { _moveTargetPath.value = path }

    fun loadFolderTree(dirPath: String) {
        viewModelScope.launch {
            try {
                val data = ApiService.getAllFiles(dirPath)
                val folders = data.filter { it.isFolder }
                if (dirPath.isEmpty()) {
                    val rootNode = FolderTreeNode(path = "", name = "根目录", isExpanded = true, children = folders.map { f ->
                        FolderTreeNode(path = f.name, name = f.name)
                    })
                    _folderTree.value = listOf(rootNode)
                } else {
                    updateFolderTreeWithChildren(dirPath, folders)
                }
            } catch (_: Exception) {}
        }
    }

    fun toggleFolder(node: FolderTreeNode) {
        viewModelScope.launch {
            val currentTree = _folderTree.value.toMutableList()
            updateNodeInTree(currentTree, node.path) { existingNode ->
                if (existingNode.isExpanded) existingNode.copy(isExpanded = false)
                else existingNode.copy(isExpanded = true, isLoading = true)
            }
            _folderTree.value = currentTree.toList()
            if (!node.isExpanded) {
                try {
                    val data = ApiService.getAllFiles(node.path)
                    val folders = data.filter { it.isFolder }
                    val updatedTree = _folderTree.value.toMutableList()
                    updateNodeInTree(updatedTree, node.path) { existingNode ->
                        existingNode.copy(isExpanded = true, isLoading = false, hasSubfolders = folders.isNotEmpty(),
                            children = folders.map { f ->
                                val fullPath = if (node.path.isEmpty()) f.name else "${node.path}/${f.name}"
                                FolderTreeNode(path = fullPath, name = f.name)
                            })
                    }
                    _folderTree.value = updatedTree.toList()
                } catch (_: Exception) {}
            }
        }
    }

    private fun updateNodeInTree(tree: MutableList<FolderTreeNode>, targetPath: String, updater: (FolderTreeNode) -> FolderTreeNode): Boolean {
        for (i in tree.indices) {
            if (tree[i].path == targetPath) { tree[i] = updater(tree[i]); return true }
            if (tree[i].children.isNotEmpty()) {
                val childrenList = tree[i].children.toMutableList()
                if (updateNodeInTree(childrenList, targetPath, updater)) { tree[i] = tree[i].copy(children = childrenList.toList()); return true }
            }
        }
        return false
    }

    private fun updateFolderTreeWithChildren(dirPath: String, folders: List<FileItem>) {
        val currentTree = _folderTree.value.toMutableList()
        updateNodeInTree(currentTree, dirPath) { existingNode ->
            existingNode.copy(hasSubfolders = folders.isNotEmpty(), children = folders.map { f ->
                val fullPath = if (dirPath.isEmpty()) f.name else "$dirPath/${f.name}"
                FolderTreeNode(path = fullPath, name = f.name)
            })
        }
        _folderTree.value = currentTree.toList()
    }

    fun showDownloadDialog(file: FileItem) { _downloadFile.value = file; _showDownloadDialog.value = true }
    fun hideDownloadDialog() { _showDownloadDialog.value = false; _downloadFile.value = null }
    fun showLoadingDialog() { _showLoadingDialog.value = true }
    fun hideLoadingDialog() { _showLoadingDialog.value = false }
    fun hideEditDialog() { _showEditDialog.value = false; _previewFile.value = null; _editFileContent.value = ""; _defaultEditMode.value = false }
    fun clearNavigateToTextEditor() { _navigateToTextEditor.value = null }
    fun showToastMessage(message: String) { _showToast.value = message; viewModelScope.launch { delay(2000); _showToast.value = null } }
    fun showErrorToastMessage(message: String) { _showErrorToast.value = message; viewModelScope.launch { delay(3000); _showErrorToast.value = null } }
    fun hideToastMessage() { _showToast.value = null }
    fun hideErrorToastMessage() { _showErrorToast.value = null }
    fun getFullPath(file: FileItem): String = if (_currentPath.value.isEmpty()) file.name else "${_currentPath.value}/${file.name}"
    fun getFileUrl(file: FileItem): String = ApiService.getDownloadPath(getFullPath(file), file.isFolder)

    // Multi-select
    fun toggleMultiSelectMode() {
        _isMultiSelectMode.value = !_isMultiSelectMode.value
        if (!_isMultiSelectMode.value) _selectedFiles.value = emptySet()
    }
    fun enterMultiSelectMode() { _isMultiSelectMode.value = true }
    fun exitMultiSelectMode() { _isMultiSelectMode.value = false; _selectedFiles.value = emptySet() }
    fun toggleFileSelection(file: FileItem) {
        val current = _selectedFiles.value.toMutableSet()
        if (current.contains(file)) current.remove(file) else current.add(file)
        _selectedFiles.value = current
    }
    fun selectAllFiles() { _selectedFiles.value = _files.value.toSet() }
    fun deselectAllFiles() { _selectedFiles.value = emptySet() }
    fun showBatchDeleteDialog() { if (_selectedFiles.value.isNotEmpty()) _showBatchDeleteDialog.value = true }
    fun hideBatchDeleteDialog() { _showBatchDeleteDialog.value = false }
    fun showBatchMoveDialog() { if (_selectedFiles.value.isNotEmpty()) { _moveTargetPath.value = ""; _showBatchMoveDialog.value = true; loadFolderTree("") } }
    fun hideBatchMoveDialog() { _showBatchMoveDialog.value = false; _moveTargetPath.value = ""; _folderTree.value = emptyList() }
    fun showBatchDownloadDialog() { if (_selectedFiles.value.isNotEmpty()) _showBatchDownloadDialog.value = true }
    fun hideBatchDownloadDialog() { _showBatchDownloadDialog.value = false }

    fun deleteSelectedFiles() {
        if (_isLoading.value) { showToastMessage("正在删除中,请稍后!"); return }
        if (_selectedFiles.value.isEmpty()) { showToastMessage("请先选择要删除的文件"); return }
        _isLoading.value = true
        viewModelScope.launch {
            var successCount = 0; var failCount = 0
            for (file in _selectedFiles.value) {
                val fullPath = getFullPath(file)
                try {
                    val result = ApiService.deleteFile(fullPath)
                    if (result.contains("成功")) successCount++ else failCount++
                } catch (_: Exception) { failCount++ }
            }
            _isLoading.value = false; _showBatchDeleteDialog.value = false
            val message = when {
                failCount == 0 -> "成功删除 $successCount 个项目"
                successCount == 0 -> "删除失败"
                else -> "成功删除 $successCount 个，失败 $failCount 个"
            }
            showToastMessage(message)
            _selectedFiles.value = emptySet(); _isMultiSelectMode.value = false
            loadFiles(_currentPath.value)
        }
    }

    fun moveSelectedFiles(targetPath: String) {
        if (_isLoading.value) { showToastMessage("正在移动中,请稍后!"); return }
        if (_selectedFiles.value.isEmpty()) { showToastMessage("请先选择要移动的文件"); return }
        _isLoading.value = true
        viewModelScope.launch {
            var successCount = 0; var failCount = 0
            for (file in _selectedFiles.value) {
                val fullPath = getFullPath(file)
                try {
                    val result = ApiService.moveFile(fullPath, targetPath)
                    if (result.contains("成功")) successCount++ else failCount++
                } catch (_: Exception) { failCount++ }
            }
            _isLoading.value = false; _showBatchMoveDialog.value = false
            val message = when {
                failCount == 0 -> "成功移动 $successCount 个项目"
                successCount == 0 -> "移动失败"
                else -> "成功移动 $successCount 个，失败 $failCount 个"
            }
            showToastMessage(message)
            _selectedFiles.value = emptySet(); _isMultiSelectMode.value = false; _folderTree.value = emptyList()
            loadFiles(_currentPath.value)
        }
    }

    fun downloadFileOrFolder(file: FileItem) {
        val fullPath = getFullPath(file)
        val fileName = file.getFileName()
        if (file.isFolder) {
            val added = AppDownloadManager.downloadFolder(fullPath, fileName)
            if (added) {
                showToastMessage("已开始下载文件夹: $fileName")
            } else {
                showErrorToastMessage("文件夹已在下载列表中: $fileName")
            }
        } else {
            val added = AppDownloadManager.downloadFile(fullPath, fileName, file.length)
            if (added) {
                showToastMessage("已开始下载: $fileName")
            } else {
                showErrorToastMessage("文件已在下载列表中: $fileName")
            }
        }
        _showDownloadDialog.value = false
        _downloadFile.value = null
    }

    fun downloadSelectedFiles() {
        if (_selectedFiles.value.isEmpty()) {
            showToastMessage("请先选择要下载的文件")
            return
        }
        var skipCount = 0
        for (file in _selectedFiles.value) {
            val fullPath = getFullPath(file)
            val fileName = file.getFileName()
            if (file.isFolder) {
                val added = AppDownloadManager.downloadFolder(fullPath, fileName)
                if (!added) skipCount++
            } else {
                val added = AppDownloadManager.downloadFile(fullPath, fileName, file.length)
                if (!added) skipCount++
            }
        }
        val total = _selectedFiles.value.size
        val message = if (skipCount == 0) "已添加 $total 个下载任务"
        else "已添加 ${total - skipCount} 个任务，$skipCount 个已在列表中"
        showToastMessage(message)
        _showBatchDownloadDialog.value = false
        _selectedFiles.value = emptySet()
        _isMultiSelectMode.value = false
    }
}