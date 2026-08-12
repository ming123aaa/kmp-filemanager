package com.ohuang.kmp.filemanager.kmp_filemanager.data

import java.awt.Desktop
import java.net.URI

actual fun openUri(uri: String): Boolean {
    return try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(uri))
            true
        } else {
            val os = System.getProperty("os.name").lowercase()
            val cmd = when {
                os.contains("win") -> "rundll32 url.dll,FileProtocolHandler $uri"
                os.contains("mac") -> "open $uri"
                else -> "xdg-open $uri"
            }
            ProcessBuilder(cmd).start().waitFor()
            true
        }
    } catch (_: Exception) {
        false
    }
}
