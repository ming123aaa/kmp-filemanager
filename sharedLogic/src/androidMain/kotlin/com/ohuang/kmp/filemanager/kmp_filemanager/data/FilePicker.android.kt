package com.ohuang.kmp.filemanager.kmp_filemanager.data

import android.net.Uri
import android.provider.DocumentsContract
import com.ohuang.kmp.filemanager.kmp_filemanager.ActivityContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File


data class FilePickerRequest(
    val allowMultiple: Boolean,
    val isFolder: Boolean,
    val callback: (List<String>) -> Unit
)

private val _pendingFilePick = MutableStateFlow<FilePickerRequest?>(null)
val pendingFilePick: StateFlow<FilePickerRequest?> = _pendingFilePick


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

/**
 * 将 content URI 转换为真实文件路径。
 * 优先路径解析（零复制），兜底复制到缓存目录。
 */
fun uriToFilePath(uri: Uri): String? {
    return try {
        val ctx = ActivityContext.get() ?: return null
        UriToFile.uriToFile(uri, ctx)?.absolutePath
    } catch (e: Exception) {
        null
    }
}

actual fun fileNameFromPath(path: String): String =
    File(path).name.ifBlank { "unknown_file" }

actual fun fileSizeBytes(path: String): Long = File(path).length()

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