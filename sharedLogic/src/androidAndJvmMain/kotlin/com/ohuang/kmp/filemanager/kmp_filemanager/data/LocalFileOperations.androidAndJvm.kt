package com.ohuang.kmp.filemanager.kmp_filemanager.data

import java.io.File

actual class LocalFileOperations actual constructor(private val rootDir: String) {
    actual val rootName: String
        get() = rootDir.substringAfterLast("/").ifEmpty { rootDir }

    private fun resolveDir(relativePath: String): File =
        if (relativePath.isEmpty()) File(rootDir) else File(rootDir, relativePath)

    actual fun listFiles(relativePath: String): List<FileItem> {
        val dir = resolveDir(relativePath)
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return (dir.listFiles() ?: emptyArray()).map { f ->
            FileItem(
                name = f.name,
                length = if (f.isDirectory) 0L else f.length(),
                isFolder = f.isDirectory,
                lastModified = f.lastModified()
            )
        }
    }

    actual fun createFolder(relativePath: String, name: String): Boolean {
        val dir = File(resolveDir(relativePath), name)
        if (dir.exists()) return false
        return dir.mkdirs()
    }

    actual fun createFile(relativePath: String, name: String): Boolean {
        val file = File(resolveDir(relativePath), name)
        if (file.exists()) return false
        return file.createNewFile()
    }

    actual fun renameFile(relativePath: String, oldName: String, newName: String): Boolean {
        val src = File(resolveDir(relativePath), oldName)
        val dest = File(resolveDir(relativePath), newName)
        if (!src.exists() || dest.exists()) return false
        return src.renameTo(dest)
    }

    actual fun deleteFile(relativePath: String, name: String): Boolean {
        val target = File(resolveDir(relativePath), name)
        if (!target.exists()) return false
        return target.deleteRecursively()
    }

    actual fun moveFile(srcRelativePath: String, name: String, targetRelativePath: String): Boolean {
        val src = File(resolveDir(srcRelativePath), name)
        val targetDir = resolveDir(targetRelativePath)
        val dest = File(targetDir, name)
        if (!src.exists() || dest.exists()) return false
        return src.renameTo(dest)
    }

    actual fun readText(relativePath: String, name: String): String? {
        val file = File(resolveDir(relativePath), name)
        if (!file.exists() || !file.isFile) return null
        if (file.length() >= 100 * 1024) return null // 文件过大
        return file.readText(Charsets.UTF_8)
    }

    actual fun writeText(relativePath: String, name: String, content: String): Boolean {
        val file = File(resolveDir(relativePath), name)
        file.parentFile?.mkdirs()
        file.writeText(content, Charsets.UTF_8)
        return true
    }

    actual fun getFullPath(relativePath: String, name: String): String {
        return File(resolveDir(relativePath), name).absolutePath
    }

    actual fun getAbsolutePath(relativePath: String): String {
        return resolveDir(relativePath).absolutePath
    }

    actual fun exists(relativePath: String, name: String): Boolean {
        return File(resolveDir(relativePath), name).exists()
    }

    actual fun isDirectory(relativePath: String): Boolean {
        return resolveDir(relativePath).isDirectory
    }

    actual fun getFileSize(relativePath: String, name: String): Long {
        val file = File(resolveDir(relativePath), name)
        return if (file.exists()) file.length() else 0L
    }
}