package com.ohuang.kmp.filemanager.kmp_filemanager.data

import com.ohuang.kmp.filemanager.kmp_filemanager.HttpConfig
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDataDelegateProtocol
import platform.Foundation.NSURLSessionTask
import platform.Foundation.NSURLSessionTaskDelegateProtocol
import platform.Foundation.NSURLResponse
import platform.Foundation.NSError
import platform.Foundation.NSDataReadingOptions
import platform.Foundation.create
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual suspend fun uploadFileImpl(
    filePath: String,
    fileName: String,
    path: String,
    onProgress: (current: Long, total: Long) -> Unit
): String = suspendCancellableCoroutine { continuation ->
    val fileManager = platform.Foundation.NSFileManager.defaultManager
    if (!fileManager.fileExistsAtPath(filePath)) {
        continuation.resume("上传失败: 文件不存在")
        return@suspendCancellableCoroutine
    }

    val fileData = NSData.dataWithContentsOfFile(filePath)
        ?: run {
            continuation.resume("上传失败: 无法读取文件")
            return@suspendCancellableCoroutine
        }

    val boundary = "Boundary-${currentTimeMillis()}"
    val baseUrl = HttpConfig.getBaseUrl()
    val url = NSURL.URLWithString("$baseUrl/main/fileUpload")
        ?: run {
            continuation.resume("上传失败: 无效的URL")
            return@suspendCancellableCoroutine
        }

    val request = NSMutableURLRequest.requestWithURL(url).apply {
        setHTTPMethod("POST")
        setValue("multipart/form-data; boundary=$boundary", forHTTPHeaderField = "Content-Type")
        setHTTPBody(buildMultipartBody(fileData, fileName, path, boundary))
    }

    val totalSize = fileData.length.toLong()

    val delegate = UploadDelegate(
        onProgress = { current ->
            onProgress(current, totalSize)
        },
        onComplete = { result ->
            continuation.resume(result)
        },
        onError = { error ->
            continuation.resume("上传失败: $error")
        }
    )

    val session = NSURLSession.sessionWithConfiguration(
        NSURLSessionConfiguration.defaultSessionConfiguration,
        delegate = delegate,
        delegateQueue = null
    )

    val task = session.uploadTaskWithRequest(request, fromData = fileData)
    delegate.task = task

    continuation.invokeOnCancellation {
        task.cancel()
        session.finishTasksAndInvalidate()
    }

    task.resume()
}

@OptIn(ExperimentalForeignApi::class)
private fun buildMultipartBody(
    fileData: NSData,
    fileName: String,
    path: String,
    boundary: String
): NSData {
    val body = platform.Foundation.NSMutableData()
    val crlf = "\r\n"

    fun appendString(string: String) {
        body.appendData(string.encodeToByteArray().toNSData())
    }

    appendString("--$boundary$crlf")
    appendString("Content-Disposition: form-data; name=\"path\"$crlf$crlf")
    appendString("$path$crlf")

    appendString("--$boundary$crlf")
    appendString("Content-Disposition: form-data; name=\"fileName\"; filename=\"$fileName\"$crlf")
    appendString("Content-Type: application/octet-stream$crlf$crlf")
    body.appendData(fileData)
    appendString(crlf)

    appendString("--$boundary--$crlf")

    return body
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }
}

private class UploadDelegate(
    private val onProgress: (Long) -> Unit,
    private val onComplete: (String) -> Unit,
    private val onError: (String) -> Unit
) : NSObject(), NSURLSessionTaskDelegateProtocol, NSURLSessionDataDelegateProtocol {

    var task: platform.Foundation.NSURLSessionTask? = null
    private val responseData = platform.Foundation.NSMutableData()

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didSendBodyData: Long,
        totalBytesSent: Long,
        totalBytesExpectedToSend: Long
    ) {
        onProgress(totalBytesSent)
    }

    override fun URLSession(
        session: NSURLSession,
        dataTask: platform.Foundation.NSURLSessionDataTask,
        didReceiveData: NSData
    ) {
        responseData.appendData(didReceiveData)
    }

    override fun URLSession(
        session: NSURLSession,
        task: NSURLSessionTask,
        didCompleteWithError: NSError?
    ) {
        if (didCompleteWithError != null) {
            onError(didCompleteWithError.localizedDescription ?: "未知错误")
        } else {
            val responseString = NSString.create(
                data = responseData,
                encoding = platform.Foundation.NSUTF8StringEncoding
            ) ?: "上传失败: 无法解析响应"
            onComplete(responseString as String)
        }
        session.finishTasksAndInvalidate()
    }
}

private typealias NSString = platform.Foundation.NSString