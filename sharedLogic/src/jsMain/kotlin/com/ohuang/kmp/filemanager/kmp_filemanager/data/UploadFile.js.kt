package com.ohuang.kmp.filemanager.kmp_filemanager.data

actual suspend fun uploadFileImpl(
    filePath: String,
    fileName: String,
    path: String,
    onProgress: (current: Long, total: Long) -> Unit
): String {
    throw UnsupportedOperationException("JS upload not supported")
}
