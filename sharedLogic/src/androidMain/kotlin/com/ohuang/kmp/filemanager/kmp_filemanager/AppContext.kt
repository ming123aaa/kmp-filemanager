package com.ohuang.kmp.filemanager.kmp_filemanager

import android.app.Application
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import com.ohuang.kmp.filemanager.kmp_filemanager.server.WebStaticResourcesInfo
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress

object AppContext {
    lateinit var instance: Application
        private set

    fun init(context: Application) {
        instance = context
        copyAssetsWeb()
    }


    fun getWebStaticResources(): WebStaticResourcesInfo {
        return WebStaticResourcesInfo(remotePath = "/", basePackage = instance.filesDir.absolutePath + "/web")
    }

    fun getDeviceName(): String {
        try {

            val name = Settings.Global.getString(instance.contentResolver, Settings.Global.DEVICE_NAME)
            if (!TextUtils.isEmpty(name)) {
                return name
            }
        } catch (e: Exception) {

        }

        // 2. 降级方案：尝试获取系统主机名（通常与设备名一致）
        val hostName = getHostName()
        if (!hostName.isNullOrBlank()) {
            return hostName
        }

        return Build.MODEL
    }

    /**
     * 获取设备的主机名
     */
    private fun getHostName(): String? {
        return try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            // 在某些设备或网络环境下可能抛出异常
            null
        }
    }

    private fun copyAssetsWeb() {
        val targetDir = File(instance.filesDir, "web")
        targetDir.deleteRecursively()
        copyAssetPath("web", targetDir)
    }

    private fun copyAssetPath(assetPath: String, target: File) {
        val assets = instance.assets
        try {
            // 先尝试以文件方式打开，成功则复制
            assets.open(assetPath).use { input ->
                target.parentFile?.mkdirs()
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (_: java.io.FileNotFoundException) {
            // 打开失败说明是目录，递归处理
            if (!target.exists()) {
                target.mkdirs()
            }
            assets.list(assetPath)?.forEach { child ->
                copyAssetPath("$assetPath/$child", File(target, child))
            }
        }
    }
}