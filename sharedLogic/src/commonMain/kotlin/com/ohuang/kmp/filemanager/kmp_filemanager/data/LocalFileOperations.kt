package com.ohuang.kmp.filemanager.kmp_filemanager.data

/**
 * 本地文件操作抽象层，提供跨平台的文件系统操作接口。
 * 各平台通过 actual 实现，Android/JVM 使用 java.io.File。
 */
expect class LocalFileOperations(rootDir: String) {
    /** 列出指定相对路径下的所有文件 */
    fun listFiles(relativePath: String): List<FileItem>

    /** 创建文件夹，返回 true 表示成功 */
    fun createFolder(relativePath: String, name: String): Boolean

    /** 创建文件，返回 true 表示成功 */
    fun createFile(relativePath: String, name: String): Boolean

    /** 重命名文件/文件夹，返回 true 表示成功 */
    fun renameFile(relativePath: String, oldName: String, newName: String): Boolean

    /** 删除文件/文件夹（递归），返回 true 表示成功 */
    fun deleteFile(relativePath: String, name: String): Boolean

    /** 移动文件/文件夹到目标目录 */
    fun moveFile(srcRelativePath: String, name: String, targetRelativePath: String): Boolean

    /** 读取文本文件内容，大文件返回 null */
    fun readText(relativePath: String, name: String): String?

    /** 写入文本文件内容 */
    fun writeText(relativePath: String, name: String, content: String): Boolean

    /** 获取文件的完整绝对路径 */
    fun getFullPath(relativePath: String, name: String): String

    /** 获取目录的绝对路径 */
    fun getAbsolutePath(relativePath: String): String

    /** 检查文件/文件夹是否存在 */
    fun exists(relativePath: String, name: String): Boolean

    /** 判断路径是否为目录 */
    fun isDirectory(relativePath: String): Boolean

    /** 获取文件/文件夹大小 */
    fun getFileSize(relativePath: String, name: String): Long

    /** 根目录名称 */
    val rootName: String
}