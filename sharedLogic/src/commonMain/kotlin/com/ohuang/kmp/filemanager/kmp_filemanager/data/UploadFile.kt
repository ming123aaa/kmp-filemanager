package com.ohuang.kmp.filemanager.kmp_filemanager.data

expect suspend fun uploadFileImpl(
    filePath: String,
    fileName: String,
    path: String,
    onProgress: (current: Long, total: Long) -> Unit
): String
