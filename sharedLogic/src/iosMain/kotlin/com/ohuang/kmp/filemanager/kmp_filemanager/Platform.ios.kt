package com.ohuang.kmp.filemanager.kmp_filemanager

import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val type: PlatformType
        get() = PlatformType.IOS
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getDefaultServerRootPath(): String = "."