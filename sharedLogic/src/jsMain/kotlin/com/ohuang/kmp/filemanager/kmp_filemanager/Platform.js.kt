package com.ohuang.kmp.filemanager.kmp_filemanager

import web.navigator.navigator

class JsPlatform : Platform {
    private val userAgent = navigator.userAgent
    private val browserList = listOf("Chrome", "Firefox", "Safari", "Edge")

    override val name: String = userAgent.findAnyOf(browserList, ignoreCase = true)
        ?.let { (startIndex) -> userAgent.substring(startIndex).substringBefore(" ") }
        ?: "Unknown"

    override val type: PlatformType
        get() = PlatformType.Web
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun getDefaultServerRootPath(): String = "."