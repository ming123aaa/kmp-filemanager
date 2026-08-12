package com.ohuang.kmp.filemanager.kmp_filemanager

import com.ohuang.kmp.filemanager.kmp_filemanager.data.FileItem
import com.ohuang.kmp.filemanager.kmp_filemanager.data.uploadFileImpl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes

import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object ApiService {
    private const val BASE_PATH = "/main"
    private const val DEFAULT_BUFFER_SIZE = 8192

    private val client = HttpClient()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getAllFiles(path: String, isConnect: Boolean = false): List<FileItem> {
        if (isConnect) {
            HttpConfig.checkConnect()
        }
        val response = client.get("${HttpConfig.getBaseUrl()}$BASE_PATH/getAllFile") {
            if (path.isNotEmpty()) {
                parameter("path", path)
            }
        }
        val text = response.bodyAsText()
        return json.decodeFromString(text)
    }

    suspend fun getFileInfo(path: String): FileItem {
        val response = client.get("${HttpConfig.getBaseUrl()}$BASE_PATH/fileInfo") {
            parameter("path", path)
        }
        val text = response.bodyAsText()
        return json.decodeFromString(text)
    }

    suspend fun createFolder(name: String, path: String = ""): String {
        val response = client.submitFormWithBinaryData(
            url = "${HttpConfig.getBaseUrl()}$BASE_PATH/mkdir",
            formData = formData {
                append("name", name)
                append("path", path)
            }
        )
        return response.bodyAsText()
    }

    suspend fun createFile(name: String, path: String = ""): String {
        val response = client.submitFormWithBinaryData(
            url = "${HttpConfig.getBaseUrl()}$BASE_PATH/createFile",
            formData = formData {
                append("name", name)
                append("path", path)
            }
        )
        return response.bodyAsText()
    }

    suspend fun deleteFile(path: String): String {
        val response = client.submitFormWithBinaryData(
            url = "${HttpConfig.getBaseUrl()}$BASE_PATH/delete",
            formData = formData {
                append("path", path)
            }
        )
        return response.bodyAsText()
    }

    suspend fun renameFile(path: String, newName: String): String {
        val response = client.submitFormWithBinaryData(
            url = "${HttpConfig.getBaseUrl()}$BASE_PATH/rename",
            formData = formData {
                append("path", path)
                append("newName", newName)
            }
        )
        return response.bodyAsText()
    }

    suspend fun moveFile(path: String, targetDir: String): String {
        val response = client.submitFormWithBinaryData(
            url = "${HttpConfig.getBaseUrl()}$BASE_PATH/move",
            formData = formData {
                append("path", path)
                append("targetDir", targetDir)
            }
        )
        return response.bodyAsText()
    }

    suspend fun readText(path: String, encoding: String = ""): String {
        val response = client.get("${HttpConfig.getBaseUrl()}$BASE_PATH/readText") {
            parameter("path", path)
            if (encoding.isNotEmpty()) {
                parameter("encoding", encoding)
            }
        }
        return response.bodyAsText()
    }

    suspend fun writeText(path: String, text: String): String {
        val response = client.submitFormWithBinaryData(
            url = "${HttpConfig.getBaseUrl()}$BASE_PATH/writeText",
            formData = formData {
                append("path", path)
                append("txt", text)
            }
        )
        return response.bodyAsText()
    }

    fun getDownloadPath(fullPath: String, isFolder: Boolean = false): String {
        if (fullPath.startsWith("http://") || fullPath.startsWith("https://")) {
            return fullPath
        }
        val baseUrl = HttpConfig.getBaseUrl()
        val path = encodeUrlPath(fullPath)
        val encodedPath = encodeUrlFull(fullPath)
        return if (isFolder) {
            if (HttpConfig.readOnly.value) {
                "${baseUrl}/index.html?path=${encodedPath}"
            } else {
                "${baseUrl}/file.html?path=${encodedPath}"
            }
        } else {
            "${baseUrl}/main/files/$path"
        }
    }

    suspend fun testConnect(baseUrl: String = HttpConfig.getBaseUrl()): String {
        val response = client.get("$baseUrl/test/connect")
        return response.bodyAsText()
    }

    suspend fun checkDownloadPath(downloadPath: String): FileInfo {
        val response = client.head(downloadPath)
        return FileInfo(
            contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L,
            fileName = response.headers["Content-Disposition"]?.let { header ->
                val match = Regex("filename[^;=\\n]*=((['\"]).*?\\2|[^;\\n]*)").find(header)
                match?.groupValues?.get(1)?.trim('\'', '"')
            }
        )
    }

    suspend fun uploadFile(
        filePath: String,
        fileName: String,
        path: String,
        onProgress: (current: Long, total: Long) -> Unit = { _, _ -> }
    ): String {
        return uploadFileImpl(filePath, fileName, path, onProgress)
    }

    suspend fun download(
        url: String,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): ByteArray? {
        return try {
            val response = client.prepareGet(url).execute { response ->
                val channel: ByteReadChannel = response.bodyAsChannel()
                val total = response.contentLength() ?: 0L
                var downloaded = 0L
                val bytes = mutableListOf<Byte>()

                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    while (!packet.exhausted()) {
                        @Suppress("DEPRECATION")
                        val chunk = packet.readBytes()
                        bytes.addAll(chunk.toList())
                        downloaded += chunk.size
                        onProgress(downloaded, total)
                    }
                }
                bytes.toByteArray()
            }
            response
        } catch (_: Exception) {
            null
        }
    }

    suspend fun downloadToFile(
        url: String,
        savePath: String,
        resumeFrom: Long = 0L,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): Boolean {
        return try {
            client.prepareGet(url) {
                if (resumeFrom > 0) {
                    headers.append(HttpHeaders.Range, "bytes=$resumeFrom-")
                }
            }.execute { response ->
                val channel: ByteReadChannel = response.bodyAsChannel()
                val total = response.contentLength()?.let { it + resumeFrom } ?: 0L
                var downloaded = resumeFrom
                val fileManager = com.ohuang.kmp.filemanager.kmp_filemanager.data.FileManager()

                if (!fileManager.fileExists(savePath) && resumeFrom > 0) {
                    return@execute false
                }

                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    while (!packet.exhausted()) {
                        @Suppress("DEPRECATION")
                        val chunk = packet.readBytes()
                        fileManager.writeFile(savePath, chunk, append = true)
                        downloaded += chunk.size
                        onProgress(downloaded, total)
                    }
                }
                true
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun encodeUrlPath(fullPath: String): String {
        return fullPath.split("/").joinToString("/") { part ->
            part.encodeURLPathComponent()
        }
    }

    private fun encodeUrlFull(fullPath: String): String {
        return fullPath.split("/").joinToString("/") { part ->
            part.encodeURLComponent()
        }
    }
}

data class FileInfo(
    val contentLength: Long,
    val fileName: String?
)

private fun String.encodeURLComponent(): String {
    val sb = StringBuilder()
    for (ch in this) {
        when (ch) {
            in 'A'..'Z', in 'a'..'z', in '0'..'9', '-', '_', '.', '~' -> sb.append(ch)
            ' ' -> sb.append("%20")
            else -> {
                val bytes = ch.toString().encodeToByteArray()
                for (byte in bytes) {
                    sb.append('%')
                    sb.append(byte.toHexString().uppercase().takeLast(2).padStart(2, '0'))
                }
            }
        }
    }
    return sb.toString()
}

private fun String.encodeURLPathComponent(): String {
    val sb = StringBuilder()
    for (ch in this) {
        when (ch) {
            in 'A'..'Z', in 'a'..'z', in '0'..'9', '-', '_', '.', '~', '/' -> sb.append(ch)
            ' ' -> sb.append("%20")
            else -> {
                val bytes = ch.toString().encodeToByteArray()
                for (byte in bytes) {
                    sb.append('%')
                    sb.append(byte.toHexString().uppercase().takeLast(2).padStart(2, '0'))
                }
            }
        }
    }
    return sb.toString()
}

private fun Byte.toHexString(): String {
    val i = this.toInt() and 0xFF
    return i.toString(16)
}