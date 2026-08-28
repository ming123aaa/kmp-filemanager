package com.ohuang.kmp.filemanager.kmp_filemanager

import android.app.Application
import com.ohuang.kmp.filemanager.kmp_filemanager.server.WebStaticResourcesInfo
import java.io.File
import java.io.FileOutputStream

object AppContext {
    lateinit var instance: Application
        private set

    fun init(context: Application) {
        instance = context
        copyAssetsWeb()
    }




    fun getWebStaticResources(): WebStaticResourcesInfo {
        return WebStaticResourcesInfo(remotePath="/", basePackage = instance.filesDir.absolutePath+"/web")
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