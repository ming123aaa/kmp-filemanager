package com.ohuang.kmp.filemanager.kmp_filemanager


import com.ohuang.kmp.filemanager.kmp_filemanager.server.WebStaticResourcesInfo

interface Platform {
    val name: String
    val type: PlatformType
}

enum class PlatformType {
    Desktop, IOS, Android, Web
}

inline fun tryCatch(onError: (Throwable) -> Unit = {}, onFinally: () -> Unit = {}, block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        onError(e)
    } finally {
        onFinally()
    }
}

fun PlatformType.isTypes(vararg types: PlatformType): Boolean {
    return types.any { this == it }
}

expect fun getPlatform(): Platform

expect fun getDefaultServerRootPath(): String

expect fun getDefaultDeviceName(): String

expect fun getHttpsKeystorePath(): String

expect fun createHttpClient(): io.ktor.client.HttpClient


expect fun getWebStaticResources(): WebStaticResourcesInfo