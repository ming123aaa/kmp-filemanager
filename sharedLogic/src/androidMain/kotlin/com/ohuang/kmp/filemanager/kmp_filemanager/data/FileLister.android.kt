package com.ohuang.kmp.filemanager.kmp_filemanager.data

import kotlinx.coroutines.isActive
import kotlinx.coroutines.supervisorScope
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

actual fun listFilesInDirectory(folderPath: String): List<UploadFileInfo> {
    val rootDir = File(folderPath)
    val result = mutableListOf<UploadFileInfo>()
    if (rootDir.exists() && rootDir.isDirectory) {
        val rootAbsPath = rootDir.absolutePath
        val rootName = rootDir.name
        rootDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val subPath = file.absolutePath.removePrefix(rootAbsPath)
                    .removePrefix(File.separator)
                    .replace(File.separator, "/")
                val relativePath = if (subPath.isEmpty()) rootName else "$rootName/$subPath"
                result.add(UploadFileInfo(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    relativePath = relativePath,
                    totalSize = file.length()
                ))
            }
        }
    }
    return result
}


actual suspend fun awaitFilesInDirectory(folderPath: String): List<com.ohuang.kmp.filemanager.kmp_filemanager.data.UploadFileInfo> {
    val rootDir = File(folderPath)
    val result = mutableListOf<UploadFileInfo>()

    if (rootDir.exists() && rootDir.isDirectory) {
        val rootAbsPath = rootDir.absolutePath
        val rootName = rootDir.name
        supervisorScope {
            rootDir.walkTopDown().forEach { file ->
                if (!isActive){
                    throw CancellationException()
                }
                if (file.isFile) {
                    val subPath = file.absolutePath.removePrefix(rootAbsPath)
                        .removePrefix(File.separator)
                        .replace(File.separator, "/")
                    val relativePath = if (subPath.isEmpty()) rootName else "$rootName/$subPath"
                    result.add(UploadFileInfo(
                        filePath = file.absolutePath,
                        fileName = file.name,
                        relativePath = relativePath,
                        totalSize = file.length()
                    ))
                }
            }
        }

    }
    return result
}
