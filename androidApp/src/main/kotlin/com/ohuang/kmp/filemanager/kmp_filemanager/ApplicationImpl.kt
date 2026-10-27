package com.ohuang.kmp.filemanager.kmp_filemanager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.ohuang.kmp.filemanager.kmp_filemanager.server.ServerConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.server.getServerManager


class ApplicationImpl : Application() {

    override fun onCreate() {
        super.onCreate()
        registerActivityListner()
        AppContext.init(this)
        HttpConfig.init(Settings(this))
        startServer()

    }

    private fun registerActivityListner() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

            }

            override fun onActivityDestroyed(activity: Activity) {

            }

            override fun onActivityPaused(activity: Activity) {

            }

            override fun onActivityResumed(activity: Activity) {
                // 回到前台时主动检测剪贴板变化
                com.ohuang.kmp.filemanager.kmp_filemanager.data.notifyForeground()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

            }

            override fun onActivityStarted(activity: Activity) {

            }

            override fun onActivityStopped(activity: Activity) {

            }
        })
    }

    private fun startServer() {
        val savedPort = HttpConfig.loadServerPort()
        val savedRootPath = HttpConfig.loadServerRootPath()
        val savedFtpPort = HttpConfig.loadFtpPort()
        val savedFtpUser = HttpConfig.loadFtpUser()
        val savedFtpPassword = HttpConfig.loadFtpPassword()
        val savedFtpEnabled = HttpConfig.loadFtpEnabled()
        val savedFtpAnonymous = HttpConfig.loadFtpAnonymous()
        val savedWebDavPort = HttpConfig.loadWebDavPort()
        val savedWebDavUser = HttpConfig.loadWebDavUser()
        val savedWebDavPassword = HttpConfig.loadWebDavPassword()
        val savedWebDavEnabled = HttpConfig.loadWebDavEnabled()
        val savedWebDavAnonymous = HttpConfig.loadWebDavAnonymous()
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
                ftpAnonymous = savedFtpAnonymous,
                webDavPort = savedWebDavPort,
                webDavUser = savedWebDavUser,
                webDavPassword = savedWebDavPassword,
                webDavEnabled = savedWebDavEnabled,
                webDavAnonymous = savedWebDavAnonymous,
                webDavUseHttps = savedWebDavUseHttps,
                keystorePath = HttpConfig.loadKeystorePath(),
                keystorePassword = HttpConfig.loadKeystorePassword(),
                keyAlias = HttpConfig.loadKeyAlias(),
                keyPassword = HttpConfig.loadKeyPassword()
            )
        )
    }


}