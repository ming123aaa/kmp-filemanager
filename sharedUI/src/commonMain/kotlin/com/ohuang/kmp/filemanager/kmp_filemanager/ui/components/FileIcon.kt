package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.ohuang.kmp.filemanager.kmp_filemanager.data.FileItem

@Composable
fun FileIcon(file: FileItem, size: Dp) {
    val fileType = if (file.isFolder) FileType.FOLDER else FileType.getFileType(file.name)

    Icon(
        imageVector = fileType.icon,
        contentDescription = file.name,
        tint = fileType.color,
        modifier = Modifier.size(size)
    )
}

@Composable
fun FolderIcon(size: Dp, color: Color = FileType.FOLDER.color) {
    Icon(
        imageVector = Icons.Default.Folder,
        contentDescription = "Folder",
        tint = color,
        modifier = Modifier.size(size)
    )
}