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


val httpClient=HttpClient()
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
