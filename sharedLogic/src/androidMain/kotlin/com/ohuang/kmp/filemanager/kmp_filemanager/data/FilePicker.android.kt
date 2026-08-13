package com.ohuang.kmp.filemanager.kmp_filemanager.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

private var appContext: Context? = null

data class FilePickerRequest(
    val allowMultiple: Boolean,
    val isFolder: Boolean,
    val callback: (List<String>) -> Unit
)

private val _pendingFilePick = MutableStateFlow<FilePickerRequest?>(null)
val pendingFilePick: StateFlow<FilePickerRequest?> = _pendingFilePick

fun initFilePicker(context: Context) {
    appContext = context.applicationContext
}

fun onFilePickerResult(uris: List<Uri>, request: FilePickerRequest) {
    val paths = uris.mapNotNull { uriToFilePath(it) }
    request.callback(paths)
    _pendingFilePick.value = null
}

fun onFolderPickerResult(uri: Uri?, request: FilePickerRequest) {
    val path = resolveFolderPath(uri)
    val result = if (path != null) listOf(path) else emptyList()
    request.callback(result)
    _pendingFilePick.value = null
}

private fun resolveFolderPath(uri: Uri?): String? {
    if (uri == null) return null
    return try {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val split = docId.split(":")
        if (split[0].equals("primary", ignoreCase = true)) {
            @Suppress("DEPRECATION")
            "${android.os.Environment.getExternalStorageDirectory().absolutePath}/${split[1]}"
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

fun uriToFilePath(uri: Uri): String? {
    return try {
        val ctx = appContext ?: return null

        // 尝试从 ContentResolver 获取文件名
        var fileName: String? = null
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        ctx.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }

        // 回退: 从 URI 路径中提取文件名
        if (fileName.isNullOrBlank()) {
            fileName = uri.lastPathSegment
                ?.substringAfterLast("/")
                ?.substringAfterLast("\\")
            if (fileName.isNullOrBlank()) {
                fileName = "unknown_file"
            }
        }

        // 复制文件到缓存目录
        val cacheDir = File(ctx.cacheDir, "upload_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val file = File(cacheDir, fileName)
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null

        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

actual fun launchFilePicker(allowMultiple: Boolean, onResult: (List<String>) -> Unit) {
    _pendingFilePick.value = FilePickerRequest(
        allowMultiple = allowMultiple,
        isFolder = false,
        callback = onResult
    )
}

actual fun launchFolderPicker(onResult: (String?) -> Unit) {
    _pendingFilePick.value = FilePickerRequest(
        allowMultiple = false,
        isFolder = true,
        callback = { paths ->
            onResult(paths.firstOrNull())
        }
    )
}

actual fun fileSizeBytes(path: String): Long = File(path).length()
