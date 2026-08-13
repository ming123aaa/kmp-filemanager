package com.ohuang.kmp.filemanager.kmp_filemanager.server

import com.ohuang.kmp.filemanager.kmp_filemanager.data.FileItem
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.http.content.staticResources
import io.ktor.http.content.PartData
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.response.header
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.nio.charset.Charset
import kotlin.collections.emptyList

class LocalFileServer(private val config: ServerConfig) {

    private var onThrowable: (Throwable) -> Unit = {}
    private val server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> by lazy {
        createServer()
    }
    private var coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    fun createServer(): EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> {
        File(config.rootPath).mkdirs()
        return coroutineScope.embeddedServer(
            factory = CIO,
            port = config.port,
            host = config.bindAddress,
            parentCoroutineContext = CoroutineExceptionHandler { context, throwable ->
                onThrowable(throwable)
            }
        ) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = false
                })
            }
            install(CORS) {
                anyHost()
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
                allowHeader("Content-Type")
            }
            routing {
                get("/test/connect") {
                    val mode = if (config.readOnly) "success (read)" else "success"
                    call.respondText(mode, ContentType.Text.Plain)
                }

                route("/main") {
                    get("/getAllFile") {
                        val path = call.request.queryParameters["path"] ?: ""
                        val dir = resolvePath(path)
                        if (!dir.exists() || !dir.isDirectory) {
                            call.respond(emptyList<FileItem>())
                            return@get
                        }
                        val files = dir.listFiles()?.map { file ->
                            FileItem(
                                name = file.name,
                                length = if (file.isFile) file.length() else 0L,
                                isFolder = file.isDirectory,
                                lastModified = file.lastModified()
                            )
                        }?.sortedWith(
                            compareBy<FileItem> { !it.isFolder }.thenBy { it.name.lowercase() }
                        ) ?: emptyList()
                        call.respond(files)
                    }

                    get("/fileInfo") {
                        val path = call.request.queryParameters["path"] ?: ""
                        val file = resolvePath(path)
                        if (!file.exists()) {
                            call.respond("文件不存在")
                            return@get
                        }
                        call.respond(
                            FileItem(
                                name = file.name,
                                length = if (file.isFile) file.length() else 0L,
                                isFolder = file.isDirectory,
                                lastModified = file.lastModified()
                            )
                        )
                    }

                    post("/mkdir") {
                        if (config.readOnly) {
                            call.respond("服务器为只读模式")
                            return@post
                        }
                        val params = call.receiveParameters()
                        val name = params["name"] ?: ""
                        val path = params["path"] ?: ""
                        if (name.isEmpty()) {
                            call.respond("名称不能为空")
                            return@post
                        }
                        val parentDir = if (path.isEmpty()) File(config.rootPath) else resolvePath(path)
                        val dir = File(parentDir, name)
                        if (dir.exists()) {
                            call.respond("已存在")
                            return@post
                        }
                        val success = dir.mkdirs()
                        call.respond(if (success) "创建成功" else "创建失败")
                    }

                    post("/createFile") {
                        if (config.readOnly) {
                            call.respond("服务器为只读模式")
                            return@post
                        }
                        val params = call.receiveParameters()
                        val name = params["name"] ?: ""
                        val path = params["path"] ?: ""
                        if (name.isEmpty()) {
                            call.respond("名称不能为空")
                            return@post
                        }
                        val parentDir = if (path.isEmpty()) File(config.rootPath) else resolvePath(path)
                        parentDir.mkdirs()
                        val file = File(parentDir, name)
                        if (file.exists()) {
                            call.respond("已存在")
                            return@post
                        }
                        val success = file.createNewFile()
                        call.respond(if (success) "创建成功" else "创建失败")
                    }

                    post("/delete") {
                        if (config.readOnly) {
                            call.respond("服务器为只读模式")
                            return@post
                        }
                        val params = call.receiveParameters()
                        val path = params["path"] ?: ""
                        if (path.isEmpty()) {
                            call.respond("路径不能为空")
                            return@post
                        }
                        val file = resolvePath(path)
                        if (!file.exists()) {
                            call.respond("文件不存在")
                            return@post
                        }
                        val success = file.deleteRecursively()
                        call.respond(if (success) "删除成功" else "删除失败")
                    }

                    post("/rename") {
                        if (config.readOnly) {
                            call.respond("服务器为只读模式")
                            return@post
                        }
                        val params = call.receiveParameters()
                        val path = params["path"] ?: ""
                        val newName = params["newName"] ?: ""
                        if (path.isEmpty() || newName.isEmpty()) {
                            call.respond("参数不完整")
                            return@post
                        }
                        val file = resolvePath(path)
                        if (!file.exists()) {
                            call.respond("文件不存在")
                            return@post
                        }
                        val newFile = File(file.parentFile, newName)
                        if (newFile.exists()) {
                            call.respond("目标名称已存在")
                            return@post
                        }
                        val success = file.renameTo(newFile)
                        call.respond(if (success) "重命名成功" else "重命名失败")
                    }

                    post("/move") {
                        if (config.readOnly) {
                            call.respond("服务器为只读模式")
                            return@post
                        }
                        val params = call.receiveParameters()
                        val path = params["path"] ?: ""
                        val targetDir = params["targetDir"] ?: ""
                        if (path.isEmpty()) {
                            call.respond("参数不完整")
                            return@post
                        }
                        val file = resolvePath(path)
                        if (!file.exists()) {
                            call.respond("文件不存在")
                            return@post
                        }
                        val targetDirFile = resolvePath(targetDir)
                        if (!targetDirFile.exists() || !targetDirFile.isDirectory) {
                            call.respond("目标目录不存在")
                            return@post
                        }
                        val target = File(targetDirFile, file.name)
                        if (target.exists()) {
                            call.respond("目标位置已存在同名文件")
                            return@post
                        }
                        val success = file.renameTo(target)
                        call.respond(if (success) "移动成功" else "移动失败")
                    }

                    get("/readText") {
                        val path = call.request.queryParameters["path"] ?: ""
                        val encoding = call.request.queryParameters["encoding"] ?: "UTF-8"
                        if (path.isEmpty()) {
                            call.respond("路径不能为空")
                            return@get
                        }
                        val file = resolvePath(path)
                        if (!file.exists() || !file.isFile) {
                            call.respond("文件不存在")
                            return@get
                        }
                        try {
                            val charset = try {
                                Charset.forName(encoding)
                            } catch (_: Exception) {
                                Charsets.UTF_8
                            }
                            val text = file.readText(charset)
                            call.respondText(text, ContentType.Text.Plain)
                        } catch (e: Exception) {
                            call.respond("读取失败: ${e.message}")
                        }
                    }

                    post("/writeText") {
                        if (config.readOnly) {
                            call.respond("服务器为只读模式")
                            return@post
                        }
                        val params = call.receiveParameters()
                        val path = params["path"] ?: ""
                        val txt = params["txt"] ?: ""
                        if (path.isEmpty()) {
                            call.respond("路径不能为空")
                            return@post
                        }
                        val file = resolvePath(path)
                        try {
                            file.parentFile?.mkdirs()
                            file.writeText(txt, Charsets.UTF_8)
                            call.respond("保存成功")
                        } catch (e: Exception) {
                            call.respond("保存失败: ${e.message}")
                        }
                    }

                    post("/fileUpload") {
                        if (config.readOnly) {
                            call.respond("服务器为只读模式")
                            return@post
                        }
                        val multipartData = call.receiveMultipart(formFieldLimit = Long.MAX_VALUE)
                        val tempDir = File(config.rootPath, ".temp").also { it.mkdirs() }
                        var path = ""
                        var tempFile: File? = null
                        while (true) {
                            val part = multipartData.readPart() ?: break
                            when (part) {
                                is PartData.FormItem -> {
                                    if (part.name == "path") {
                                        path = part.value
                                    }
                                }

                                is PartData.FileItem -> {
                                    if (part.name == "fileName") {
                                        val safeFileName = File(part.originalFileName ?: "").name
                                        if (safeFileName.isEmpty()) break
                                        tempFile = File(tempDir, safeFileName)
                                        try {
                                            part.provider().toInputStream().use { input ->
                                                FileOutputStream(tempFile).use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            tempFile.delete()
                                            call.respond("上传失败: ${e.message}")
                                            return@post
                                        }
                                    }
                                }

                                else -> {}
                            }
                        }
                        val src = tempFile
                        if (src == null || !src.exists()) {
                            call.respond("文件为空")
                            return@post
                        }

                        val targetDir = resolvePath(path)
                        targetDir.mkdirs()
                        val dest = File(targetDir, src.name)
                        if (dest.exists()) dest.delete()
                        if (!src.renameTo(dest)) {
                            src.delete()
                            call.respond("移动文件失败")
                            return@post
                        }

                        call.respond("上传成功")


                    }

                    post("/multifileUpload") {
                        if (config.readOnly) {
                            call.respond("服务器为只读模式")
                            return@post
                        }
                        val multipartData = call.receiveMultipart(formFieldLimit = Long.MAX_VALUE)
                        val tempDir = File(config.rootPath, ".temp").also { it.mkdirs() }
                        var path = ""
                        val tempFiles = mutableListOf<File>()
                        while (true) {
                            val part = multipartData.readPart() ?: break
                            when (part) {
                                is PartData.FormItem -> {
                                    if (part.name == "path") {
                                        path = part.value
                                    }
                                }

                                is PartData.FileItem -> {
                                    if (part.name == "fileName") {
                                        val safeName = File(part.originalFileName ?: "").name
                                        if (safeName.isEmpty()) continue
                                        val tempFile = File(tempDir, safeName)
                                        try {
                                            part.provider().toInputStream().use { input ->
                                                FileOutputStream(tempFile).use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                            tempFiles.add(tempFile)
                                        } catch (_: Exception) {
                                        }
                                    }
                                }

                                else -> {}
                            }
                        }
                        if (tempFiles.isEmpty()) {
                            call.respond("没有选择文件")
                            return@post
                        }

                        val targetDir = resolvePath(path)
                        targetDir.mkdirs()
                        var successCount = 0
                        for (src in tempFiles) {
                            val dest = File(targetDir, src.name)
                            if (dest.exists()) dest.delete()
                            if (src.renameTo(dest)) {
                                successCount++
                            } else {
                                src.delete()
                            }
                        }
                        call.respond("成功上传 $successCount 个文件")

                    }

                    get("/files/{path...}") {
                        val path = call.parameters.getAll("path")?.joinToString("/") ?: ""
                        if (path.isEmpty()) {
                            call.respond("路径不能为空")
                            return@get
                        }
                        val decodedPath = URLDecoder.decode(path, "UTF-8")
                        val file = File(config.rootPath, decodedPath)
                        if (!file.exists() || !file.isFile) {
                            call.respond("文件不存在")
                            return@get
                        }
                        if (!file.canonicalPath.startsWith(File(config.rootPath).canonicalPath)) {
                            call.respond("禁止访问")
                            return@get
                        }
                        val contentType = contentTypeForFile(file.name)
                        val isInline = isInlineContentType(file.name)
                        call.response.header("Content-Type", contentType.toString())
                        call.response.headers.append(
                            "Content-Disposition",
                            if (isInline) "inline; filename=\"${file.name}\""
                            else "attachment; filename=\"${file.name}\""
                        )
                        call.respondFile(file)
                    }
                }

                staticResources("/", "web")
            }
        }

    }

    fun start(onThrowable: (Throwable) -> Unit) {
        this.onThrowable = onThrowable
        server.start()
    }

    fun stop() {
        server.stop(1000, 1000)
        coroutineScope.cancel()
    }

    private fun resolvePath(relativePath: String): File {
        if (relativePath.isEmpty()) return File(config.rootPath)
        val decodedPath = URLDecoder.decode(relativePath, "UTF-8")
        val file = File(config.rootPath, decodedPath)
        val canonicalRoot = File(config.rootPath).canonicalPath
        if (!file.canonicalPath.startsWith(canonicalRoot)) {
            throw SecurityException("路径遍历攻击检测: $relativePath")
        }
        return file
    }

    companion object {
        private val MIME_MAP = mapOf(
            // 图片
            "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png",
            "gif" to "image/gif", "bmp" to "image/bmp", "webp" to "image/webp",
            "svg" to "image/svg+xml", "ico" to "image/x-icon", "tiff" to "image/tiff",
            // 视频
            "mp4" to "video/mp4", "avi" to "video/x-msvideo",
            "mkv" to "video/x-matroska", "mov" to "video/quicktime",
            "wmv" to "video/x-ms-wmv", "flv" to "video/x-flv",
            "webm" to "video/webm", "m4v" to "video/x-m4v",
            // 音频
            "mp3" to "audio/mpeg", "wav" to "audio/wav",
            "flac" to "audio/flac", "aac" to "audio/aac",
            "ogg" to "audio/ogg", "wma" to "audio/x-ms-wma",
            "m4a" to "audio/x-m4a",
            // 文本
            "txt" to "text/plain", "html" to "text/html", "htm" to "text/html",
            "css" to "text/css", "csv" to "text/csv",
            "xml" to "application/xml", "json" to "application/json",
            "js" to "application/javascript", "ts" to "application/typescript",
            "md" to "text/markdown",
            // 文档
            "pdf" to "application/pdf",
            "doc" to "application/msword",
            "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "xls" to "application/vnd.ms-excel",
            "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "ppt" to "application/vnd.ms-powerpoint",
            "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            // 压缩包
            "zip" to "application/zip", "rar" to "application/vnd.rar",
            "7z" to "application/x-7z-compressed", "tar" to "application/x-tar",
            "gz" to "application/gzip", "bz2" to "application/x-bzip2",
            "xz" to "application/x-xz",
            // 安装包
            "apk" to "application/vnd.android.package-archive",
            "exe" to "application/vnd.microsoft.portable-executable",
            "msi" to "application/x-msi",
            "deb" to "application/vnd.debian.binary-package",
            "rpm" to "application/x-rpm",
            "dmg" to "application/x-apple-diskimage",
            // 代码
            "kt" to "text/plain", "java" to "text/plain",
            "py" to "text/plain", "c" to "text/plain",
            "cpp" to "text/plain", "h" to "text/plain",
            "go" to "text/plain", "rs" to "text/plain",
            "rb" to "text/plain", "sh" to "text/plain", "bat" to "text/plain",
            "sql" to "text/plain", "yaml" to "text/plain", "yml" to "text/plain",
            "ini" to "text/plain", "cfg" to "text/plain", "conf" to "text/plain",
            "log" to "text/plain", "properties" to "text/plain",
            "vue" to "text/plain", "jsx" to "text/plain", "tsx" to "text/plain",
            "php" to "text/plain"
        )

        private val INLINE_EXTENSIONS = setOf(
            // 图片和视频在浏览器中内联显示
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff",
            "mp4", "webm", "ogg", "mp3", "wav", "flac", "aac", "m4a",
            "txt", "html", "htm", "css", "csv", "xml", "json", "js", "ts", "md",
            "pdf"
        )

        private fun contentTypeForFile(fileName: String): ContentType {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            val mime = MIME_MAP[ext] ?: "application/octet-stream"
            return ContentType.parse(mime)
        }

        private fun isInlineContentType(fileName: String): Boolean {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return ext in INLINE_EXTENSIONS
        }
    }
}

