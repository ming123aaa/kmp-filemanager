package com.ohuang.kmp.filemanager.kmp_filemanager.data

expect fun listFilesInDirectory(folderPath: String): List<UploadFileInfo>
expect suspend fun awaitFilesInDirectory(folderPath: String): List<UploadFileInfo>
