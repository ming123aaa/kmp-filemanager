package com.ohuang.kmp.filemanager.kmp_filemanager.data

actual class LocalFileOperations actual constructor(rootDir: String) {
    actual val rootName: String get() = rootDir.substringAfterLast("/").ifEmpty { rootDir }
    actual fun listFiles(relativePath: String): List<FileItem> = emptyList()
    actual fun createFolder(relativePath: String, name: String): Boolean = false
    actual fun createFile(relativePath: String, name: String): Boolean = false
    actual fun renameFile(relativePath: String, oldName: String, newName: String): Boolean = false
    actual fun deleteFile(relativePath: String, name: String): Boolean = false
    actual fun moveFile(srcRelativePath: String, name: String, targetRelativePath: String): Boolean = false
    actual fun readText(relativePath: String, name: String): String? = null
    actual fun writeText(relativePath: String, name: String, content: String): Boolean = false
    actual fun getFullPath(relativePath: String, name: String): String = "$rootDir/$relativePath/$name"
    actual fun getAbsolutePath(relativePath: String): String = "$rootDir/$relativePath"
    actual fun exists(relativePath: String, name: String): Boolean = false
    actual fun isDirectory(relativePath: String): Boolean = false
    actual fun getFileSize(relativePath: String, name: String): Long = 0L
}