package com.ohuang.kmp.filemanager.kmp_filemanager

import android.content.Context
import android.os.Build
import com.ohuang.kmp.filemanager.kmp_filemanager.server.WebStaticResourcesInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val type: PlatformType
        get() = PlatformType.Android
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getDefaultServerRootPath(): String =
    getDefaultServiceFilePath()

actual fun getHttpsKeystorePath(): String {
    try {
        val keystoreName= "a123456.bks"

        val context = AppContext.instance
        val jksFile = java.io.File(context.filesDir,keystoreName )
        if (!jksFile.exists()) {
            // 优先从 assets 加载
            context.assets.open(keystoreName).use { input ->
                java.io.FileOutputStream(jksFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return jksFile.absolutePath
    } catch (_: Exception) {
        return ""
    }
}



actual fun createHttpClient(): HttpClient = HttpClient(Android) {
    engine {
        sslManager = { httpsURLConnection ->
            val trustAll = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(trustAll), java.security.SecureRandom())
            httpsURLConnection.sslSocketFactory = sslContext.socketFactory
            httpsURLConnection.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
        }
    }
}


fun getDefaultServiceFilePath(context: Context = AppContext.instance): String {
    return (context.getExternalFilesDir(null)?.absolutePath
        ?: context.filesDir.absolutePath) + "/fileManager"
}

actual fun getWebStaticResources(): WebStaticResourcesInfo {
    return AppContext.getWebStaticResources()
}

actual fun getDefaultDeviceName(): String{
    return AppContext.getDeviceName()
}

