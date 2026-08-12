package com.ohuang.kmp.filemanager.kmp_filemanager.server

data class ServerConfig(
    val port: Int = 8080,
    val rootPath: String = "",
    val bindAddress: String = "0.0.0.0",
    val readOnly: Boolean = false
)