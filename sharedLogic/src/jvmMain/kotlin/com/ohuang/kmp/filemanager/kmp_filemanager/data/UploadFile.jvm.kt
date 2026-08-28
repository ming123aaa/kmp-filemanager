package com.ohuang.kmp.filemanager.kmp_filemanager.data

import com.ohuang.kmp.filemanager.kmp_filemanager.HttpConfig

import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import com.ohuang.kthttp.HttpClient
import com.ohuang.kthttp.call.HttpCall
import com.ohuang.kthttp.call.await
import com.ohuang.kthttp.call.map
import com.ohuang.kthttp.download
import com.ohuang.kthttp.download.FileInfo
import com.ohuang.kthttp.downloadFileInfo
import com.ohuang.kthttp.jsonCall
import com.ohuang.kthttp.post
import com.ohuang.kthttp.stringHttpResponseCall
import com.ohuang.kthttp.upload.addFile
import com.ohuang.kthttp.upload.addFileInputSteam
import com.ohuang.kthttp.upload.postUploadFile
import com.ohuang.kthttp.url
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


/**
 * 获取一个忽略所有 HTTPS 证书校验的 OkHttpClient
 * 仅用于开发和测试环境！
 */
fun getUnsafeOkHttpClient(): OkHttpClient {
    try {
        // 1. 创建信任所有证书的 TrustManager（使用 object 表达式）
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    // 不作校验，直接通过
                }

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                    // 不作校验，直接通过
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
        )

        // 2. 用自定义 TrustManager 初始化 SSLContext
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        // 3. 构建 OkHttpClient
        return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }  // 忽略主机名验证
            .build()
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}



val httpClient = HttpClient(okHttpClient =getUnsafeOkHttpClient())
actual suspend fun uploadFileImpl(
    filePath: String,
    fileName: String,
    path: String,
    onProgress: (current: Long, total: Long) -> Unit
): String {

    val file = File(filePath)

    return httpClient.stringCall {
        url("${HttpConfig.getBaseUrl()}/main/fileUpload")
        postUploadFile {
            addFormDataPart("path", path)
            addFileInputSteam(key = "fileName", file = file.inputStream(), fileName = fileName, callBack = onProgress)
        }

    }.await()
}

private fun writeMultipartBody(
    output: OutputStream,
    boundary: String,
    path: String,
    fileName: String,
    file: File,
    onProgress: (current: Long, total: Long) -> Unit
) {
    val totalSize = file.length()
    var uploaded = 0L
    val crlf = "\r\n"

    fun writePart(name: String, value: String) {
        output.write("--$boundary$crlf".toByteArray())
        output.write("Content-Disposition: form-data; name=\"$name\"$crlf$crlf".toByteArray())
        output.write(value.toByteArray())
        output.write(crlf.toByteArray())
    }

    writePart("path", path)

    output.write("--$boundary$crlf".toByteArray())
    output.write("Content-Disposition: form-data; name=\"fileName\"; filename=\"$fileName\"$crlf".toByteArray())
    output.write("Content-Type: application/octet-stream$crlf$crlf".toByteArray())

    val buffer = ByteArray(8192)
    FileInputStream(file).use { fis ->
        while (true) {
            val bytesRead = fis.read(buffer)
            if (bytesRead == -1) break
            output.write(buffer, 0, bytesRead)
            uploaded += bytesRead
            onProgress(uploaded, totalSize)
        }
    }

    output.write(crlf.toByteArray())
    output.write("--$boundary--$crlf".toByteArray())
}
