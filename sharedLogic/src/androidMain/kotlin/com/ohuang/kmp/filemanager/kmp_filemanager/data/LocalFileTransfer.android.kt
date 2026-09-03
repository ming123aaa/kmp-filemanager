package com.ohuang.kmp.filemanager.kmp_filemanager.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.ohuang.kmp.filemanager.kmp_filemanager.ActivityContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class LocalImportRequest(
    val targetDir: String,
    val callback: (Int, Int) -> Unit
)

private val _pendingLocalImport = MutableStateFlow<LocalImportRequest?>(null)
val pendingLocalImport: StateFlow<LocalImportRequest?> = _pendingLocalImport

fun onLocalImportResult(uris: List<Uri>) {
    val request = _pendingLocalImport.value ?: return
    _pendingLocalImport.value = null
    if (uris.isEmpty()) {
        request.callback(0, 0)
        return
    }
    Thread {
        val ctx = ActivityContext.get()
        var successCount = 0
        var failCount = 0
        for (uri in uris) {
            try {
                val fileName = getFileNameFromUri(ctx, uri) ?: "unknown_${System.currentTimeMillis()}"
                val dest = File(request.targetDir, fileName)
                val inputStream = ctx?.contentResolver?.openInputStream(uri)
                if (inputStream == null) {
                    failCount++; continue
                }
                inputStream.use { input ->
                    dest.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                successCount++
            } catch (_: Exception) {
                failCount++
            }
        }
        request.callback(successCount, failCount)
    }.start()
}

private fun getFileNameFromUri(context: Context?, uri: Uri): String? {
    if (context == null) return uri.lastPathSegment
    var name: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
    }
    if (name == null) name = uri.lastPathSegment
    return name
}

actual fun importFilesToLocalDir(targetDir: String, onResult: (Int, Int) -> Unit) {
    _pendingLocalImport.value = LocalImportRequest(targetDir, onResult)
}

// 导出文件夹选择
private val _pendingExportFolderPick = MutableStateFlow<((String?) -> Unit)?>(null)
val pendingExportFolderPick: StateFlow<((String?) -> Unit)?> = _pendingExportFolderPick

fun onExportFolderPicked(uri: Uri?) {
    val callback = _pendingExportFolderPick.value ?: return
    _pendingExportFolderPick.value = null
    // 直接传递 URI 字符串，让 export 函数通过 DocumentFile API 写入
    callback(uri?.toString())
}

actual fun pickExportFolder(onResult: (String?) -> Unit) {
    _pendingExportFolderPick.value = onResult
}

actual fun exportFileToLocalDir(sourcePath: String, destDir: String, onResult: (Boolean, String) -> Unit) {
    Thread {
        try {
            val src = File(sourcePath)
            if (!src.exists()) {
                onResult(false, "源文件不存在")
                return@Thread
            }
            if (destDir.startsWith("content://")) {
                // 通过 DocumentFile API 写入（支持 scoped storage）
                val ctx = ActivityContext.get()
                if (ctx == null) {
                    onResult(false, "无法获取 Context")
                    return@Thread
                }
                val treeUri = Uri.parse(destDir)
                val destFolder = DocumentFile.fromTreeUri(ctx, treeUri)
                if (destFolder == null || !destFolder.canWrite()) {
                    onResult(false, "目标目录不可写")
                    return@Thread
                }
                copyToDocumentFile(ctx, src, destFolder)
                onResult(true, destFolder.name ?: destDir)
            } else {
                // 普通文件路径
                val destDirFile = File(destDir)
                if (!destDirFile.exists()) destDirFile.mkdirs()
                val dest = File(destDirFile, src.name)
                if (src.isDirectory) {
                    src.copyRecursively(dest, overwrite = false)
                } else {
                    src.copyTo(dest, overwrite = false)
                }
                onResult(true, dest.absolutePath)
            }
        } catch (e: Exception) {
            onResult(false, e.message ?: "导出失败")
        }
    }.start()
}

actual fun exportFilesToLocalDir(sourcePaths: List<String>, destDir: String, onResult: (Int, Int) -> Unit) {
    Thread {
        var successCount = 0
        var failCount = 0
        if (destDir.startsWith("content://")) {
            val ctx = ActivityContext.get()
            if (ctx == null) {
                onResult(0, sourcePaths.size)
                return@Thread
            }
            val treeUri = Uri.parse(destDir)
            val destFolder = DocumentFile.fromTreeUri(ctx, treeUri)
            if (destFolder == null || !destFolder.canWrite()) {
                onResult(0, sourcePaths.size)
                return@Thread
            }
            for (path in sourcePaths) {
                try {
                    copyToDocumentFile(ctx, File(path), destFolder)
                    successCount++
                } catch (_: Exception) {
                    failCount++
                }
            }
        } else {
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
        }
        onResult(successCount, failCount)
    }.start()
}

/**
 * 通过 DocumentFile API 将文件/文件夹复制到目标 DocumentFile 目录
 */
private fun copyToDocumentFile(context: Context, src: File, destFolder: DocumentFile) {
    if (src.isDirectory) {
        // 创建子文件夹
        val subFolder = destFolder.createDirectory(src.name)
            ?: throw IllegalStateException("无法创建目录: ${src.name}")
        src.listFiles()?.forEach { child ->
            copyToDocumentFile(context, child, subFolder)
        }
    } else {
        // 复制文件
        val mimeType = android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(src.extension.lowercase())
            ?: "application/octet-stream"
        val destFile = destFolder.createFile(mimeType, src.name)
            ?: throw IllegalStateException("无法创建文件: ${src.name}")
        val outputStream = context.contentResolver.openOutputStream(destFile.uri)
            ?: throw IllegalStateException("无法打开输出流: ${src.name}")
        outputStream.use { out ->
            src.inputStream().use { input ->
                input.copyTo(out)
            }
        }
    }
}