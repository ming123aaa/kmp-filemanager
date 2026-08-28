package com.ohuang.kmp.filemanager.kmp_filemanager

import com.ohuang.kmp.filemanager.kmp_filemanager.server.WebStaticResourcesInfo
import com.ohuang.kmp.filemanager.kmp_filemanager.server.getLocalIpAddress
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.network.tls.certificates.buildKeyStore
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val type: PlatformType
        get() = PlatformType.Desktop
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun getDefaultServerRootPath(): String =
    getRealDownloadsDir()

actual fun getHttpsKeystorePath(): String {
    try {
        // 优先从 classpath 加载 a123456.p12
        val resource = object {}.javaClass.classLoader.getResource("a123456.p12")
        if (resource != null) {
            return if (resource.protocol == "file") {
                resource.toURI().path
            } else {
                val tempFile = File.createTempFile("keystore_", ".p12")
                tempFile.deleteOnExit()
                resource.openStream().use { input ->
                    java.io.FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile.absolutePath
            }
        }
    } catch (_: Exception) {}
    return  ""

}


 actual fun createHttpClient(): HttpClient {
     // 在创建HttpClient前设置
     HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }


     return HttpClient(CIO) {

         engine {
             https {


                 https {
                     trustManager = object : X509TrustManager {
                         override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                             // 不校验客户端证书

                         }

                         override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                             // 信任所有服务端证书（跳过校验）
                         }

                         override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                     }
                 }
             }
         }
     }
 }


 fun isWindows(): Boolean {
    return System.getProperty("os.name").lowercase().contains("win")
}

fun getRealDownloadsDir(): String {
    // Windows: 尝试从注册表获取用户真实的 Downloads 目录
    if (isWindows()) {
        try {
            val process = ProcessBuilder(
                "reg", "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\User Shell Folders",
                "/v", "{374DE290-123F-4565-9164-39C4925E467B}"
            ).start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            // 匹配 REG_EXPAND_SZ 或 REG_SZ 后的路径
            val match = Regex("""\{374DE290-123F-4565-9164-39C4925E467B}\s+REG\w+\s+(.+)""")
                .find(output)
            if (match != null) {
                var path = match.groupValues[1].trim()
                // 展开环境变量如 %USERPROFILE%
                val envRegex = Regex("""%([^%]+)%""")
                path = envRegex.replace(path) { mr ->
                    System.getenv(mr.groupValues[1]) ?: mr.value
                }
                val dir = File(path)
                if (dir.exists() && dir.isDirectory) {
                    return dir.absolutePath
                }
            }
        } catch (_: Exception) {
            // 注册表查询失败，使用默认路径
        }
    }

    // 默认路径：优先使用 USERPROFILE（Windows）/ HOME（Unix）
    val home = System.getenv("USERPROFILE")
        ?: System.getProperty("user.home")
    return File(home, "Downloads").absolutePath
}

actual fun getWebStaticResources(): WebStaticResourcesInfo {
    return WebStaticResourcesInfo(remotePath="/", basePackage = "web")
}