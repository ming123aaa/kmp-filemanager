package com.ohuang.kmp.filemanager.kmp_filemanager

interface Platform {
    val name: String
    val type: PlatformType
}

enum class PlatformType{
    Desktop,IOS,Android,Web
}

expect fun getPlatform(): Platform

expect fun getDefaultServerRootPath(): String