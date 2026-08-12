package com.ohuang.kmp.filemanager.kmp_filemanager

import android.content.Context
import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val type: PlatformType
        get() = PlatformType.Android
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getDefaultServerRootPath(): String =
    getDefaultServiceFilePath()



fun getDefaultServiceFilePath(context: Context = AppContext.instance): String {
    return (context.getExternalFilesDir(null)?.absolutePath
        ?: context.filesDir.absolutePath) + "/fileManager"
}