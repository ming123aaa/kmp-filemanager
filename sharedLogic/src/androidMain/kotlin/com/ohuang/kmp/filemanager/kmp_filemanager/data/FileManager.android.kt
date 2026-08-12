package com.ohuang.kmp.filemanager.kmp_filemanager.data

import android.os.Environment
import java.io.File
import java.io.RandomAccessFile

actual class FileManager {
    actual fun getDownloadDir(): String {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "fileManager"
        )
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    actual fun fileExists(path: String): Boolean = File(path).exists()
    actual fun fileSize(path: String): Long = File(path).length()
    actual fun fileLastModified(path: String): Long = File(path).lastModified()

    actual fun createParentDirs(path: String) {
        File(path).parentFile?.let { if (!it.exists()) it.mkdirs() }
    }

    actual fun writeFile(path: String, data: ByteArray, append: Boolean): Boolean {
        return try {
            val file = File(path)
            createParentDirs(path)
            if (append) {
                RandomAccessFile(file, "rw").use { raf ->
                    raf.seek(file.length())
                    raf.write(data)
                }
            } else {
                file.writeBytes(data)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    actual fun readFileBytes(path: String, offset: Long, length: Int): ByteArray? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            RandomAccessFile(file, "r").use { raf ->
                val actualLength = minOf(length.toLong(), file.length() - offset).toInt()
                if (actualLength <= 0) return ByteArray(0)
                raf.seek(offset)
                val bytes = ByteArray(actualLength)
                raf.read(bytes)
                bytes
            }
        } catch (_: Exception) {
            null
        }
    }

    actual fun deleteFile(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (_: Exception) {
            false
        }
    }
}