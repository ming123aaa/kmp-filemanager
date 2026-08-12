package com.ohuang.kmp.filemanager.kmp_filemanager.data

import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.NSDataReadingOptions
import platform.Foundation.NSDataWritingOptions
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.memcpy

actual class FileManager {
    private val fileManager = NSFileManager.defaultManager

    actual fun getDownloadDir(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )
        val documentsDir = paths.first() as String
        val dir = "$documentsDir/fileManager"
        if (!fileManager.fileExistsAtPath(dir)) {
            fileManager.createDirectoryAtPath(dir, true, null, null)
        }
        return dir
    }

    actual fun fileExists(path: String): Boolean {
        return fileManager.fileExistsAtPath(path)
    }

    actual fun fileSize(path: String): Long {
        return try {
            val attributes = fileManager.attributesOfItemAtPath(path, null)
            (attributes?.get("NSFileSize") as? NSNumber)?.longValue ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    actual fun fileLastModified(path: String): Long {
        return try {
            val attributes = fileManager.attributesOfItemAtPath(path, null)
            val date = attributes?.get("NSFileModificationDate") as? platform.Foundation.NSDate
            (date?.timeIntervalSince1970?.times(1000))?.toLong() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    actual fun createParentDirs(path: String) {
        val parentPath = path.substringBeforeLast("/", "")
        if (parentPath.isNotEmpty() && !fileManager.fileExistsAtPath(parentPath)) {
            fileManager.createDirectoryAtPath(parentPath, true, null, null)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun writeFile(path: String, data: ByteArray, append: Boolean): Boolean {
        return try {
            createParentDirs(path)
            if (append && fileManager.fileExistsAtPath(path)) {
                val existingData = NSData.dataWithContentsOfFile(path)
                val mutableData = existingData?.mutableCopy() as? platform.Foundation.NSMutableData
                    ?: platform.Foundation.NSMutableData()
                mutableData.appendBytes(data.toNSData().bytes, data.size.toULong())
                mutableData.writeToFile(path, true)
            } else {
                val nsData = data.toNSData()
                nsData.writeToFile(path, true)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    actual fun readFileBytes(path: String, offset: Long, length: Int): ByteArray? {
        return try {
            if (!fileManager.fileExistsAtPath(path)) return null
            val fileHandle = platform.Foundation.NSFileHandle.fileHandleForReadingAtPath(path) ?: return null
            fileHandle.seekToFileOffset(offset.toULong())
            val chunkData = fileHandle.readDataOfLength(length.toULong())
            fileHandle.closeFile()
            chunkData.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    actual fun deleteFile(path: String): Boolean {
        return try {
            fileManager.removeItemAtPath(path, null)
            true
        } catch (_: Exception) {
            false
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return bytes
}