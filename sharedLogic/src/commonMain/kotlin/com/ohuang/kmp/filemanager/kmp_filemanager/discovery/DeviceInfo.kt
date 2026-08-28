package com.ohuang.kmp.filemanager.kmp_filemanager.discovery

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val host: String,
    val port: Int,
    val useHttps: Boolean = false,
    val version: String = "1.0"
) {
    val baseUrl: String
        get() = "${if (useHttps) "https" else "http"}://$host:$port"

    val displayLabel: String
        get() = "$deviceName ($host:$port)"
}