package com.ohuang.kmp.filemanager.kmp_filemanager.data

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openLocalFile(path: String): Boolean {
    return try {
        val url = NSURL.fileURLWithPath(path)
        if (UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
}