package com.ohuang.kmp.filemanager.kmp_filemanager

import android.app.Application
import com.ohuang.kmp.filemanager.kmp_filemanager.server.ServerConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.server.getServerManager

class ApplicationImpl : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContext.init(this)
        HttpConfig.init(Settings(this))
        startServer()
    }

    private fun startServer() {
        val savedPort = HttpConfig.loadServerPort()
        val savedRootPath = HttpConfig.loadServerRootPath()
        getServerManager().start(
            ServerConfig(
                port = savedPort,
                rootPath = savedRootPath.ifEmpty { getDefaultServerRootPath() }
            )
        )
    }


}