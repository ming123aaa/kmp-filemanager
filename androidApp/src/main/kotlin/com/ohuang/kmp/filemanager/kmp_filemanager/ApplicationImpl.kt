package com.ohuang.kmp.filemanager.kmp_filemanager

import android.app.Application
import android.util.Log
import com.ohuang.kmp.filemanager.kmp_filemanager.server.ServerConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.server.getServerManager
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor


class ApplicationImpl : Application() {

    override fun onCreate() {
        super.onCreate()
        AppContext.init(this)
        HttpConfig.init(Settings(this))
        startServer()

        GlobalScope.launch(Dispatchers.IO) {
            try {
                delay(5000)
                val sardine: Sardine = OkHttpSardine(
                    OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply {
                        // 2. 设置日志级别，根据需要选择
                        setLevel(HttpLoggingInterceptor.Level.BODY)
                    }).build())
                sardine.setCredentials("admin", "admin",true)
                val list = sardine.list("http://192.168.2.103:8083/")
                list.forEach {
                    Log.d("webDAV","name=${it.name} ,path=${it.path} ,disName=${it.displayName} ,type=${it.contentType} ,isDirectory=${it.isDirectory}, herf=${it.href}")
                }
            }catch (e: Exception){
               e.printStackTrace()
            }

        }

    }

    private fun startServer() {
        val savedPort = HttpConfig.loadServerPort()
        val savedRootPath = HttpConfig.loadServerRootPath()
        val savedFtpPort = HttpConfig.loadFtpPort()
        val savedFtpUser = HttpConfig.loadFtpUser()
        val savedFtpPassword = HttpConfig.loadFtpPassword()
        val savedFtpEnabled = HttpConfig.loadFtpEnabled()
        val savedWebDavPort = HttpConfig.loadWebDavPort()
        val savedWebDavUser = HttpConfig.loadWebDavUser()
        val savedWebDavPassword = HttpConfig.loadWebDavPassword()
        val savedWebDavEnabled = HttpConfig.loadWebDavEnabled()
        val savedWebDavUseHttps = HttpConfig.loadWebDavUseHttps()
        val savedServerUseHttps = HttpConfig.loadServerUseHttps()
        getServerManager().start(
            ServerConfig(
                port = savedPort,
                rootPath = savedRootPath.ifEmpty { getDefaultServerRootPath() },
                useHttps = savedServerUseHttps,
                ftpPort = savedFtpPort,
                ftpUser = savedFtpUser,
                ftpPassword = savedFtpPassword,
                ftpEnabled = savedFtpEnabled,
                webDavPort = savedWebDavPort,
                webDavUser = savedWebDavUser,
                webDavPassword = savedWebDavPassword,
                webDavEnabled = savedWebDavEnabled,
                webDavUseHttps = savedWebDavUseHttps,
                keystorePath = HttpConfig.loadKeystorePath(),
                keystorePassword = HttpConfig.loadKeystorePassword(),
                keyAlias = HttpConfig.loadKeyAlias(),
                keyPassword = HttpConfig.loadKeyPassword()
            )
        )
    }


}