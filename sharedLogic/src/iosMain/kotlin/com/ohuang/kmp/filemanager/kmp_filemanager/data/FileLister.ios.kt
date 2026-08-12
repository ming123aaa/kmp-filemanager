package com.ohuang.kmp.filemanager.kmp_filemanager.data

import kotlinx.coroutines.isActive
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL

actual fun listFilesInDirectory(folderPath: String): List<UploadFileInfo> {
    val fileManager = NSFileManager.defaultManager
    val result = mutableListOf<UploadFileInfo>()
    if (!fileManager.fileExistsAtPath(folderPath)) return result

    var isDir: platform.objc.BooleanVar = platform.objc.BooleanVar(false)
    if (!fileManager.fileExistsAtPath(folderPath, isDirectory = isDir.ptr) || !isDir.value) {
        return result
    }

    val rootName = (folderPath as NSString).lastPathComponent
    val enumerator = fileManager.enumeratorAtPath(folderPath) ?: return result
    while (true) {
        val relativePath = enumerator.nextObject() as? String ?: break
        val fullPath = "$folderPath/$relativePath"
        var fileIsDir: platform.objc.BooleanVar = platform.objc.BooleanVar(false)
        if (fileManager.fileExistsAtPath(fullPath, isDirectory = fileIsDir.ptr) && !fileIsDir.value) {
            val fileName = (relativePath as NSString).lastPathComponent
            val attributes = fileManager.attributesOfItemAtPath(fullPath, null)
            val fileSize = (attributes?.get("NSFileSize") as? platform.Foundation.NSNumber)?.longValue ?: 0L
            result.add(
                UploadFileInfo(
                    filePath = fullPath,
                    fileName = fileName,
                    relativePath = "$rootName/$relativePath",
                    totalSize = fileSize
                )
            )
        }
    }
    return result
}

actual suspend fun awaitFilesInDirectory(folderPath: String): List<UploadFileInfo> {
    val fileManager = NSFileManager.defaultManager
    val result = mutableListOf<UploadFileInfo>()
    if (!fileManager.fileExistsAtPath(folderPath)) return result

    var isDir: platform.objc.BooleanVar = platform.objc.BooleanVar(false)
    if (!fileManager.fileExistsAtPath(folderPath, isDirectory = isDir.ptr) || !isDir.value) {
        return result
    }

    val rootName = (folderPath as NSString).lastPathComponent
    val enumerator = fileManager.enumeratorAtPath(folderPath) ?: return result
    while (true) {
        if (!isActive) break
        val relativePath = enumerator.nextObject() as? String ?: break
        val fullPath = "$folderPath/$relativePath"
        var fileIsDir: platform.objc.BooleanVar = platform.objc.BooleanVar(false)
        if (fileManager.fileExistsAtPath(fullPath, isDirectory = fileIsDir.ptr) && !fileIsDir.value) {
            val fileName = (relativePath as NSString).lastPathComponent
            val attributes = fileManager.attributesOfItemAtPath(fullPath, null)
            val fileSize = (attributes?.get("NSFileSize") as? platform.Foundation.NSNumber)?.longValue ?: 0L
            result.add(
                UploadFileInfo(
                    filePath = fullPath,
                    fileName = fileName,
                    relativePath = "$rootName/$relativePath",
                    totalSize = fileSize
                )
            )
        }
    }
    return result
}

private typealias NSString = platform.Foundation.NSString