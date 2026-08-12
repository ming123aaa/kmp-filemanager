package com.ohuang.kmp.filemanager.kmp_filemanager.data

enum class FilterMode {
    ALL, FILES, FOLDERS
}

enum class SortBy {
    NAME, SIZE, DATE
}

enum class SortDirection {
    ASC, DESC
}

enum class ViewMode {
    GRID,
    PREVIEW
}

data class FolderTreeNode(
    val path: String,
    val name: String,
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val children: List<FolderTreeNode> = emptyList(),
    val hasSubfolders: Boolean? = null
)

data class TextEditorNavData(
    val filePath: String,
    val fileName: String,
    val content: String,
    val file: FileItem,
    val defaultEditMode: Boolean = false
)