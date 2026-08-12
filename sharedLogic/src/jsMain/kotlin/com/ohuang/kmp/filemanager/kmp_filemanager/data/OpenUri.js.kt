package com.ohuang.kmp.filemanager.kmp_filemanager.data

import js("window")

actual fun openUri(uri: String): Boolean {
    return try {
        window.open(uri, "_blank")
        true
    } catch (_: Exception) {
        false
    }
}
