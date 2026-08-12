package com.ohuang.kmp.filemanager.kmp_filemanager.data

import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerMode
import platform.Foundation.NSArray
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject
import kotlinx.cinterop.ExperimentalForeignApi

actual fun launchFilePicker(allowMultiple: Boolean, onResult: (List<String>) -> Unit) {
    val types = listOf(UTTypeData)
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true).apply {
        allowsMultipleSelection = allowMultiple
        delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                val paths = didPickDocumentsAtURLs.mapNotNull { url ->
                    (url as? NSURL)?.path
                }
                onResult(paths)
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                onResult(emptyList())
            }
        }
    }
    presentPicker(picker)
}

actual fun launchFolderPicker(onResult: (String?) -> Unit) {
    val types = listOf(UTTypeFolder)
    val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = false).apply {
        allowsMultipleSelection = false
        delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                val path = didPickDocumentsAtURLs.firstOrNull().let { url ->
                    (url as? NSURL)?.path
                }
                onResult(path)
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                onResult(null)
            }
        }
    }
    presentPicker(picker)
}

actual fun fileSizeBytes(path: String): Long {
    return try {
        val fileManager = NSFileManager.defaultManager
        val attributes = fileManager.attributesOfItemAtPath(path, null)
        (attributes?.get("NSFileSize") as? platform.Foundation.NSNumber)?.longValue ?: 0L
    } catch (_: Exception) {
        0L
    }
}

private fun presentPicker(picker: UIDocumentPickerViewController) {
    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        ?: UIApplication.sharedApplication.windows.firstOrNull()?.rootViewController
    rootViewController?.presentViewController(picker, animated = true, completion = null)
}