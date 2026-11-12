package com.ohuang.kmp.filemanager.kmp_filemanager

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.ohuang.kmp.filemanager.kmp_filemanager.data.ClipboardA11yCallback
import com.ohuang.kmp.filemanager.kmp_filemanager.data.ClipboardOverlayReader
import com.ohuang.kmp.filemanager.kmp_filemanager.data.notifyClipboardReadFromOverlay
import com.ohuang.kmp.filemanager.kmp_filemanager.data.tryAutoEnableAccessibilityService
import com.ohuang.kmp.filemanager.kmp_filemanager.server.ServerConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.server.getServerManager
import com.ohuang.kmp.filemanager.kmp_filemanager.service.ClipboardAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope


class ApplicationImpl : Application() {


    companion object {

        var appResume:Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        registerActivityListner()
        AppContext.init(this)
        HttpConfig.init(Settings(this))
        if (HttpConfig.loadServerAutoStart()) {
            startServer()
        }

        // 尝试自动开启无障碍服务（需 WRITE_SECURE_SETTINGS 已通过 adb 授权）
        tryAutoEnableAccessibilityService()

        // 无障碍服务 → 悬浮窗读取器 → 剪贴板监听器的联动
        setupClipboardAccessibilityBridge()

    }

    /**
     * 建立无障碍服务与悬浮窗读取器的桥梁：
     * 无障碍服务检测到疑似复制 → 悬浮窗瞬获焦点读取剪贴板 → 通知剪贴板变化监听器
     */
    private fun setupClipboardAccessibilityBridge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            ClipboardAccessibilityService.onPossibleCopyDetected = ClipboardA11yCallback {
                ClipboardOverlayReader.readClipboardOnce { text ->
                    if (text != null) {
                        notifyClipboardReadFromOverlay(text)
                    }
                }

            }
        }

    }

    private fun registerActivityListner() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            var resumeActivity = mutableSetOf<Activity>()

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

            }

            override fun onActivityDestroyed(activity: Activity) {

            }

            override fun onActivityPaused(activity: Activity) {

            }

            override fun onActivityResumed(activity: Activity) {
                // 回到前台时主动检测剪贴板变化
                resumeActivity.add(activity)
                appResume= resumeActivity.isNotEmpty()
                com.ohuang.kmp.filemanager.kmp_filemanager.data.notifyForeground()


            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

            }

            override fun onActivityStarted(activity: Activity) {

            }

            override fun onActivityStopped(activity: Activity) {
                resumeActivity.remove(activity)
                appResume= resumeActivity.isNotEmpty()
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