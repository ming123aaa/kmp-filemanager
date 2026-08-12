package com.ohuang.kmp.filemanager.kmp_filemanager.server

import kotlinx.coroutines.flow.StateFlow

interface ServerManager {
    val isRunning: StateFlow<Boolean>
    val lastError: StateFlow<String?>
    val accessUrl: StateFlow<String?>
    val currentConfig: ServerConfig

    fun start(config: ServerConfig)
    fun stop()
    fun clearError()
}

expect fun getServerManager(): ServerManager