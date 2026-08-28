package com.ohuang.kmp.filemanager.kmp_filemanager.discovery

/**
 * 局域网设备扫描器
 * 通过 UDP 广播发起扫描请求，等待局域网内运行 FileManager 服务器的设备响应
 */
expect class DeviceScanner() {
    /**
     * 扫描局域网内的设备
     * @param timeoutMs 扫描超时时间（毫秒），默认 3000ms
     * @return 发现的设备列表
     */
    suspend fun scan(timeoutMs: Long = 3000): List<DeviceInfo>

    /** 取消正在进行的扫描 */
    fun cancel()
}