package com.ohuang.kmp.filemanager.kmp_filemanager.server

import kotlinx.io.files.Path

data class ServerConfig(
    val port: Int = 8080,
    val rootPath: String = "",
    val bindAddress: String = "0.0.0.0",
    val readOnly: Boolean = false,
    val useHttps: Boolean = false,
    val ftpPort: Int = 2121,
    val ftpUser: String = "admin",
    val ftpPassword: String = "admin",
    val ftpEnabled: Boolean = true,
    val webDavPort: Int = 8081,
    val webDavUser: String = "admin",
    val webDavPassword: String = "admin",
    val webDavEnabled: Boolean = true,
    // HTTPS 配置
    val webDavUseHttps: Boolean = false,
    val keystorePath: String = "",
    val keystorePassword: String = "",
    val keyAlias: String = "",
    val keyPassword: String = ""
)

fun getDeviceScannerPort(): Int {
    return 24680
}

data class WebStaticResourcesInfo(
    val remotePath: String,
    val basePackage: String?,
    val index: String = "index.html",
)

data class KeystoreInfo(val keystorePath: String, val keystorePassword: String, val keyAlias: String,val keyPassword: String)