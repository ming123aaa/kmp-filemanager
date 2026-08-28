package com.ohuang.kmp.filemanager.kmp_filemanager.discovery

/**
 * iOS 平台暂不支持 UDP 广播扫描，返回空列表
 */
actual class DeviceScanner actual constructor() {
    actual suspend fun scan(timeoutMs: Long): List<DeviceInfo> = emptyList()
    actual fun cancel() {}
}