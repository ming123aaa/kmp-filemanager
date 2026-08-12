package com.ohuang.kmp.filemanager.kmp_filemanager.data

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView

actual fun launchFilePicker(allowMultiple: Boolean, onResult: (List<String>) -> Unit) {
    val chooser = JFileChooser(FileSystemView.getFileSystemView().homeDirectory)
    chooser.fileSelectionMode = JFileChooser.FILES_ONLY
    chooser.isMultiSelectionEnabled = allowMultiple
    chooser.dialogTitle = "选择文件"

    val result = chooser.showOpenDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        val files = if (allowMultiple) {
            chooser.selectedFiles.map { it.absolutePath }
        } else {
            chooser.selectedFile?.let { listOf(it.absolutePath) } ?: emptyList()
        }
        onResult(files)
    } else {
        onResult(emptyList())
    }
}

actual fun launchFolderPicker(onResult: (String?) -> Unit) {
    val chooser = JFileChooser(FileSystemView.getFileSystemView().homeDirectory)
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    chooser.dialogTitle = "选择文件夹"

    val result = chooser.showOpenDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        onResult(chooser.selectedFile?.absolutePath)
    } else {
        onResult(null)
    }
}

actual fun fileSizeBytes(path: String): Long = File(path).length()
