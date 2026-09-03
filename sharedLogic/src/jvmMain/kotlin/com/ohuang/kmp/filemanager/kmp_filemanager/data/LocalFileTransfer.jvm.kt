package com.ohuang.kmp.filemanager.kmp_filemanager.data

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView

actual fun importFilesToLocalDir(targetDir: String, onResult: (Int, Int) -> Unit) {
    Thread {
        val chooser = JFileChooser(FileSystemView.getFileSystemView().homeDirectory)
        chooser.fileSelectionMode = JFileChooser.FILES_ONLY
        chooser.isMultiSelectionEnabled = true
        chooser.dialogTitle = "选择要导入的文件"
        val result = chooser.showOpenDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) {
            onResult(0, 0)
            return@Thread
        }
        val selectedFiles = chooser.selectedFiles
        var successCount = 0
        var failCount = 0
        val destDir = File(targetDir)
        if (!destDir.exists()) destDir.mkdirs()
        for (src in selectedFiles) {
            try {
                val dest = File(destDir, src.name)
                src.copyTo(dest, overwrite = false)
                successCount++
            } catch (_: Exception) {
                failCount++
            }
        }
        onResult(successCount, failCount)
    }.start()
}

actual fun pickExportFolder(onResult: (String?) -> Unit) {
    Thread {
        val chooser = JFileChooser(FileSystemView.getFileSystemView().homeDirectory)
        chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        chooser.dialogTitle = "选择导出位置"
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            onResult(chooser.selectedFile?.absolutePath)
        } else {
            onResult(null)
        }
    }.start()
}

actual fun exportFileToLocalDir(sourcePath: String, destDir: String, onResult: (Boolean, String) -> Unit) {
    Thread {
        try {
            val src = File(sourcePath)
            if (!src.exists()) {
                onResult(false, "源文件不存在")
                return@Thread
            }
            val destDirFile = File(destDir)
            if (!destDirFile.exists()) destDirFile.mkdirs()
            val dest = File(destDirFile, src.name)
            if (src.isDirectory) {
                src.copyRecursively(dest, overwrite = false)
            } else {
                src.copyTo(dest, overwrite = false)
            }
            onResult(true, dest.absolutePath)
        } catch (e: Exception) {
            onResult(false, e.message ?: "导出失败")
        }
    }.start()
}

actual fun exportFilesToLocalDir(sourcePaths: List<String>, destDir: String, onResult: (Int, Int) -> Unit) {
    Thread {
        var successCount = 0
        var failCount = 0
        val destDirFile = File(destDir)
        if (!destDirFile.exists()) destDirFile.mkdirs()
        for (path in sourcePaths) {
            try {
                val src = File(path)
                val dest = File(destDirFile, src.name)
                if (src.isDirectory) {
                    src.copyRecursively(dest, overwrite = false)
                } else {
                    src.copyTo(dest, overwrite = false)
                }
                successCount++
            } catch (_: Exception) {
                failCount++
            }
        }
        onResult(successCount, failCount)
    }.start()
}