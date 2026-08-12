package com.ohuang.kmp.filemanager.kmp_filemanager.data

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUri(uri: String): Boolean {
    return try {
        val url = NSURL.URLWithString(uri) ?: return false
        UIApplication.sharedApplication().openURL(url)
    } catch (_: Exception) {
        false
    }
}
