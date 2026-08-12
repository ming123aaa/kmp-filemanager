package com.ohuang.kmp.filemanager.kmp_filemanager.data

expect class FileManager() {
    fun getDownloadDir(): String
    fun fileExists(path: String): Boolean
    fun fileSize(path: String): Long
    fun fileLastModified(path: String): Long
    fun createParentDirs(path: String)
    fun writeFile(path: String, data: ByteArray, append: Boolean = false): Boolean
    fun readFileBytes(path: String, offset: Long, length: Int): ByteArray?
    fun deleteFile(path: String): Boolean
}