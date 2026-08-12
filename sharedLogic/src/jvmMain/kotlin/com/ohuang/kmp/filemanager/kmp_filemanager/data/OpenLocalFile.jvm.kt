package com.ohuang.kmp.filemanager.kmp_filemanager.data

import com.ohuang.kmp.filemanager.kmp_filemanager.PlatformType
import com.ohuang.kmp.filemanager.kmp_filemanager.isWindows
import java.awt.Desktop
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException



actual fun openLocalFile(path: String): Boolean {
    return try {
        val file = File(path)
        if (file.exists()) {
            if (file.isFile&&isWindows()){
                showFileInExplorer(file)
            }else {
                Desktop.getDesktop().open(file)
            }
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
}


@Throws(IOException::class)
fun showFileInExplorer(file: File) {
    if (!file.exists()) {
        throw FileNotFoundException("文件不存在: " + file.getAbsolutePath())
    }

    val command = "explorer.exe /select,\"" + file.getAbsolutePath() + "\""
    Runtime.getRuntime().exec(command)
}