package com.ohuang.kmp.filemanager.kmp_filemanager.server

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.EngineSSLConnectorBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.options
import io.ktor.server.routing.put
import io.ktor.server.routing.delete
import io.ktor.server.routing.route
import io.ktor.server.routing.method
import io.ktor.server.routing.routing
import io.ktor.server.request.receiveStream
import io.ktor.server.request.receiveText
import io.ktor.server.request.httpMethod
import io.ktor.server.request.header as requestHeader
import io.ktor.server.request.path
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import java.net.URLDecoder
import java.security.KeyStore
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Locale
import java.util.UUID
import java.util.TimeZone

class WebDavServer(private val config: ServerConfig) {
    private var onThrowable: (Throwable) -> Unit = {}
    private val server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> by lazy {
        createServer()
    }
    private var coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun createServer(): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
        File(config.rootPath).mkdirs()
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            onThrowable(throwable)
        }

        val connectors = if (config.webDavUseHttps) {
            val keyStoreType = getKeyStoreType(config.keystorePath)
            val keyStore = KeyStore.getInstance(keyStoreType)
            FileInputStream(File(config.keystorePath)).use { fis ->
                keyStore.load(fis, config.keystorePassword.toCharArray())
            }
            arrayOf(
                EngineSSLConnectorBuilder(
                    keyStore = keyStore,
                    keyAlias = config.keyAlias,
                    keyStorePassword = { config.keystorePassword.toCharArray() },
                    privateKeyPassword = { config.keyPassword.toCharArray() }
                ).apply {
                    host = config.bindAddress
                    port = config.webDavPort
                }
            )
        } else {
            arrayOf(
                EngineConnectorBuilder().apply {
                    host = config.bindAddress
                    port = config.webDavPort
                }
            )
        }

        return coroutineScope.embeddedServer(
            factory = Netty,
            connectors = connectors,
            parentCoroutineContext = exceptionHandler,
            module = {
                routing {
                    webDavRoutes()
                }
            }
        )
    }

    fun start(onThrowable: (Throwable) -> Unit) {
        this.onThrowable = onThrowable
        try {
            server.start()
        } catch (e: Throwable) {
            onThrowable(e)
        }
    }

    private fun Routing.webDavRoutes() {
        val Propfind = HttpMethod("PROPFIND")
        val Proppatch = HttpMethod("PROPPATCH")
        val Mkcol = HttpMethod("MKCOL")
        val Move = HttpMethod("MOVE")
        val Copy = HttpMethod("COPY")
        val Lock = HttpMethod("LOCK")
        val Unlock = HttpMethod("UNLOCK")

        // OPTIONS - required for WebDAV discovery
        options("{...}") {
            call.response.header("DAV", "1,2")
            call.response.header("Allow", "OPTIONS,PROPFIND,PROPPATCH,GET,HEAD,PUT,DELETE,MKCOL,MOVE,COPY,LOCK,UNLOCK")
            call.response.header("MS-Author-Via", "DAV")
            call.respond(HttpStatusCode.OK)
        }

        route("{...}") {
            // PROPFIND - directory listing
            method(Propfind) {
                handle {
                    if (!checkAuth(call)) return@handle

                    val depth = call.request.requestHeader("Depth") ?: "1"
                    val decodedPath = decodePath(call)
                    val file = resolveFile(decodedPath)

                    if (!file.exists()) {
                        call.respond(HttpStatusCode.NotFound)
                        return@handle
                    }

                    val requestedProps = parsePropfindBody(call)
                    val xml = buildPropfindXml(file, decodedPath, depth, requestedProps)
                    call.response.header("Content-Type", "application/xml; charset=utf-8")
                    call.respondText(xml, ContentType.Application.Xml, HttpStatusCode.MultiStatus)
                }
            }

            // PROPPATCH - set/remove properties
            method(Proppatch) {
                handle {
                    if (!checkAuth(call)) return@handle

                    val decodedPath = decodePath(call)
                    val file = resolveFile(decodedPath)

                    if (!file.exists()) {
                        call.respond(HttpStatusCode.NotFound)
                        return@handle
                    }

                    val body = call.receiveText()
                    val xml = buildProppatchResponse(file, decodedPath, body)
                    call.response.header("Content-Type", "application/xml; charset=utf-8")
                    call.respondText(xml, ContentType.Application.Xml, HttpStatusCode.MultiStatus)
                }
            }

            // GET - download file
            get {
                if (!checkAuth(call)) return@get

                val decodedPath = decodePath(call)
                val file = resolveFile(decodedPath)

                if (!file.exists()) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                if (file.isDirectory) {
                    val html = buildDirectoryHtml(file, decodedPath)
                    call.response.header("Content-Type", "text/html; charset=utf-8")
                    call.respondText(html, ContentType.Text.Html, HttpStatusCode.OK)
                    return@get
                }

                call.response.header("Content-Type", contentTypeForFile(file.name).toString())
                call.response.header("Content-Length", file.length().toString())
                call.response.header("ETag", computeEtag(file))
                call.respondOutputStream {
                    file.inputStream().use { input ->
                        input.copyTo(this)
                    }
                }
            }

            // HEAD
            head {
                if (!checkAuth(call)) return@head

                val decodedPath = decodePath(call)
                val file = resolveFile(decodedPath)

                if (!file.exists()) {
                    call.respond(HttpStatusCode.NotFound)
                    return@head
                }

                if (file.isDirectory) {
                    call.response.header("Content-Type", "text/html; charset=utf-8")
                    call.response.header("Content-Length", "0")
                } else {
                    call.response.header("Content-Type", contentTypeForFile(file.name).toString())
                    call.response.header("Content-Length", file.length().toString())
                    call.response.header("ETag", computeEtag(file))
                }
                call.respond(HttpStatusCode.OK)
            }

            // PUT - upload file
            put {
                if (config.readOnly) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@put
                }
                if (!checkAuth(call)) return@put

                checkIfHeader(call)

                val decodedPath = decodePath(call)
                val file = resolveFile(decodedPath)
                file.parentFile?.mkdirs()

                val stream = call.receiveStream()
                FileOutputStream(file).use { output ->
                    stream.copyTo(output)
                }
                call.respond(HttpStatusCode.Created)
            }

            // DELETE
            delete {
                if (config.readOnly) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@delete
                }
                if (!checkAuth(call)) return@delete

                checkIfHeader(call)

                val decodedPath = decodePath(call)
                val file = resolveFile(decodedPath)

                if (!file.exists()) {
                    call.respond(HttpStatusCode.NotFound)
                    return@delete
                }

                val success = file.deleteRecursively()
                call.respond(if (success) HttpStatusCode.NoContent else HttpStatusCode.InternalServerError)
            }

            // MKCOL - create directory
            method(Mkcol) {
                handle {
                    if (config.readOnly) {
                        call.respond(HttpStatusCode.Forbidden)
                        return@handle
                    }
                    if (!checkAuth(call)) return@handle

                    checkIfHeader(call)

                    val decodedPath = decodePath(call)
                    val file = resolveFile(decodedPath)

                    if (file.exists()) {
                        call.respond(HttpStatusCode.Conflict)
                        return@handle
                    }

                    val success = file.mkdirs()
                    call.respond(if (success) HttpStatusCode.Created else HttpStatusCode.InternalServerError)
                }
            }

            // MOVE - rename/move
            method(Move) {
                handle {
                    if (config.readOnly) {
                        call.respond(HttpStatusCode.Forbidden)
                        return@handle
                    }
                    if (!checkAuth(call)) return@handle

                    checkIfHeader(call)

                    val destination = call.request.requestHeader("Destination") ?: ""
                    if (destination.isEmpty()) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@handle
                    }

                    val overwrite = call.request.requestHeader("Overwrite") != "F"

                    val decodedPath = decodePath(call)
                    val srcFile = resolveFile(decodedPath)
                    if (!srcFile.exists()) {
                        call.respond(HttpStatusCode.NotFound)
                        return@handle
                    }

                    val destPath = extractPathFromUrl(destination)
                    val destFile = resolveFile(destPath)

                    if (destFile.exists()) {
                        if (!overwrite) {
                            call.respond(HttpStatusCode.PreconditionFailed)
                            return@handle
                        }
                        destFile.deleteRecursively()
                    }
                    destFile.parentFile?.mkdirs()

                    val success = srcFile.renameTo(destFile)
                    call.respond(if (success) HttpStatusCode.Created else HttpStatusCode.InternalServerError)
                }
            }

            // COPY
            method(Copy) {
                handle {
                    if (config.readOnly) {
                        call.respond(HttpStatusCode.Forbidden)
                        return@handle
                    }
                    if (!checkAuth(call)) return@handle

                    checkIfHeader(call)

                    val destination = call.request.requestHeader("Destination") ?: ""
                    if (destination.isEmpty()) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@handle
                    }

                    val overwrite = call.request.requestHeader("Overwrite") != "F"

                    val decodedPath = decodePath(call)
                    val srcFile = resolveFile(decodedPath)
                    if (!srcFile.exists()) {
                        call.respond(HttpStatusCode.NotFound)
                        return@handle
                    }

                    val destPath = extractPathFromUrl(destination)
                    val destFile = resolveFile(destPath)

                    if (destFile.exists()) {
                        if (!overwrite) {
                            call.respond(HttpStatusCode.PreconditionFailed)
                            return@handle
                        }
                    }
                    destFile.parentFile?.mkdirs()

                    srcFile.copyRecursively(destFile, overwrite = true)
                    call.respond(HttpStatusCode.Created)
                }
            }

            // LOCK
            method(Lock) {
                handle {
                    if (!checkAuth(call)) return@handle

                    val decodedPath = decodePath(call)
                    val file = resolveFile(decodedPath)

                    if (!file.exists()) {
                        call.respond(HttpStatusCode.NotFound)
                        return@handle
                    }

                    val lockToken = "urn:uuid:${UUID.randomUUID()}"
                    val xml = """<?xml version="1.0" encoding="utf-8"?>
<D:prop xmlns:D="DAV:">
<D:lockdiscovery>
<D:activelock>
<D:locktype><D:write/></D:locktype>
<D:lockscope><D:exclusive/></D:lockscope>
<D:depth>infinity</D:depth>
<D:locktoken><D:href>$lockToken</D:href></D:locktoken>
<D:timeout>Second-3600</D:timeout>
</D:activelock>
</D:lockdiscovery>
</D:prop>"""
                    call.response.header("Lock-Token", "<$lockToken>")
                    call.response.header("Content-Type", "application/xml; charset=utf-8")
                    call.respondText(xml, ContentType.Application.Xml, HttpStatusCode.OK)
                }
            }

            // UNLOCK
            method(Unlock) {
                handle {
                    if (!checkAuth(call)) return@handle

                    val lockToken = call.request.requestHeader("Lock-Token") ?: ""
                    if (lockToken.isEmpty()) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@handle
                    }
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }

    private suspend fun checkAuth(call: ApplicationCall): Boolean {
        val authHeader = call.request.requestHeader("Authorization") ?: run {
            call.response.header("WWW-Authenticate", "Basic realm=\"WebDAV\"")
            call.respond(HttpStatusCode.Unauthorized)
            return false
        }

        if (!authHeader.startsWith("Basic ")) {
            call.response.header("WWW-Authenticate", "Basic realm=\"WebDAV\"")
            call.respond(HttpStatusCode.Unauthorized)
            return false
        }

        try {
            val decoded = String(Base64.getDecoder().decode(authHeader.substring(6)))
            val parts = decoded.split(":", limit = 2)
            if (parts.size != 2 || parts[0] != config.webDavUser || parts[1] != config.webDavPassword) {
                call.response.header("WWW-Authenticate", "Basic realm=\"WebDAV\"")
                call.respond(HttpStatusCode.Unauthorized)
                return false
            }
        } catch (_: Exception) {
            call.response.header("WWW-Authenticate", "Basic realm=\"WebDAV\"")
            call.respond(HttpStatusCode.Unauthorized)
            return false
        }

        return true
    }

    private fun decodePath(call: ApplicationCall): String {
        val rawPath = call.request.path().removePrefix("/")
        return URLDecoder.decode(rawPath, "UTF-8")
    }

    private fun resolveFile(relativePath: String): File {
        if (relativePath.isEmpty()) return File(config.rootPath)
        val file = File(config.rootPath, relativePath)
        val canonicalRoot = File(config.rootPath).canonicalPath
        if (!file.canonicalPath.startsWith(canonicalRoot)) {
            throw SecurityException("路径遍历攻击检测")
        }
        return file
    }

    private fun buildPropfindXml(file: File, requestPath: String, depth: String, requestedProps: Set<String> = emptySet()): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        sb.append("<D:multistatus xmlns:D=\"DAV:\">")

        appendResponse(sb, file, requestPath, requestedProps)

        if (file.isDirectory) {
            when (depth) {
                "1" -> appendChildren(sb, file, requestPath, requestedProps, recursive = false)
                "infinity" -> appendChildren(sb, file, requestPath, requestedProps, recursive = true)
            }
        }

        sb.append("</D:multistatus>")
        return sb.toString()
    }

    private fun appendChildren(sb: StringBuilder, dir: File, parentPath: String, requestedProps: Set<String>, recursive: Boolean) {
        dir.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            ?.forEach { child ->
                val childPath = if (parentPath.endsWith("/")) "$parentPath${child.name}"
                else "$parentPath/${child.name}"
                appendResponse(sb, child, childPath, requestedProps)
                if (recursive && child.isDirectory) {
                    appendChildren(sb, child, childPath, requestedProps, recursive = true)
                }
            }
    }

    private fun appendResponse(sb: StringBuilder, file: File, path: String, requestedProps: Set<String> = emptySet()) {
        val encodedPath = encodeXmlPath(path)
        val allProps = requestedProps.isEmpty()|| "allprop" in requestedProps
        sb.append("<D:response>")
        sb.append("<D:href>/$encodedPath</D:href>")
        sb.append("<D:propstat>")
        sb.append("<D:prop>")

        if (allProps || "displayname" in requestedProps) {
            sb.append("<D:displayname>${file.name.encodeXml()}</D:displayname>")
        }
        if (allProps || "resourcetype" in requestedProps) {
            if (file.isDirectory) {
                sb.append("<D:resourcetype><D:collection/></D:resourcetype>")
            } else {
                sb.append("<D:resourcetype/>")
            }
        }
        if (allProps || "getcontentlength" in requestedProps) {
            sb.append("<D:getcontentlength>${if (file.isDirectory) 0 else file.length()}</D:getcontentlength>")
        }
        if (allProps || "getlastmodified" in requestedProps) {
            sb.append("<D:getlastmodified>${formatHttpDate(file.lastModified())}</D:getlastmodified>")
        }
        if (allProps || "creationdate" in requestedProps) {
            sb.append("<D:creationdate>${formatIsoDate(file.lastModified())}</D:creationdate>")
        }
        if (allProps || "getcontenttype" in requestedProps) {
            if (file.isDirectory) {
                sb.append("<D:getcontenttype>httpd/unix-directory</D:getcontenttype>")
            } else {
                sb.append("<D:getcontenttype>${contentTypeForFile(file.name)}</D:getcontenttype>")
            }
        }
        if (allProps || "getetag" in requestedProps) {
            sb.append("<D:getetag>\"${computeEtag(file)}\"</D:getetag>")
        }
        if (allProps || "supportedlock" in requestedProps) {
            sb.append("<D:supportedlock>")
            sb.append("<D:lockentry>")
            sb.append("<D:lockscope><D:exclusive/></D:lockscope>")
            sb.append("<D:locktype><D:write/></D:locktype>")
            sb.append("</D:lockentry>")
            sb.append("<D:lockentry>")
            sb.append("<D:lockscope><D:shared/></D:lockscope>")
            sb.append("<D:locktype><D:write/></D:locktype>")
            sb.append("</D:lockentry>")
            sb.append("</D:supportedlock>")
        }
        if (allProps || "lockdiscovery" in requestedProps) {
            sb.append("<D:lockdiscovery/>")
        }
        sb.append("</D:prop>")
        sb.append("<D:status>HTTP/1.1 200 OK</D:status>")
        sb.append("</D:propstat>")
        sb.append("</D:response>")
    }

    private suspend fun parsePropfindBody(call: ApplicationCall): Set<String> {
        return try {
            val body = call.receiveText()
            val props = mutableSetOf<String>()
            // Extract property names from <D:prop>...</D:prop>
            val propRegex = Regex("<(?:D:)?(\\w+)/?>")
            propRegex.findAll(body).forEach { match ->
                val name = match.groupValues[1].lowercase()
                if (name != "prop" && name != "propfind") {
                    props.add(name)
                }
            }
            // Also check for custom namespace properties
            val nsPropRegex = Regex("<\\w+:(\\w+)/?>")
            nsPropRegex.findAll(body).forEach { match ->
                val name = match.groupValues[1].lowercase()
                if (name != "prop" && name != "propfind") {
                    props.add(name)
                }
            }
            props
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun buildProppatchResponse(file: File, requestPath: String, body: String): String {
        val encodedPath = encodeXmlPath(requestPath)
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        sb.append("<D:multistatus xmlns:D=\"DAV:\">")
        sb.append("<D:response>")
        sb.append("<D:href>/$encodedPath</D:href>")
        sb.append("<D:propstat>")
        sb.append("<D:prop/>")
        sb.append("<D:status>HTTP/1.1 200 OK</D:status>")
        sb.append("</D:propstat>")
        sb.append("</D:response>")
        sb.append("</D:multistatus>")
        return sb.toString()
    }

    private fun extractPathFromUrl(destinationUrl: String): String {
        return try {
            val uri = URI(destinationUrl)
            val path = uri.rawPath
            URLDecoder.decode(path.removePrefix("/"), "UTF-8")
        } catch (_: Exception) {
            URLDecoder.decode(destinationUrl.substringAfterLast("/"), "UTF-8")
        }
    }

    private fun computeEtag(file: File): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val input = "${file.absolutePath}-${file.lastModified()}-${file.length()}"
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            "${file.lastModified()}-${file.length()}"
        }
    }

    private fun buildDirectoryHtml(file: File, requestPath: String): String {
        // Normalize: strip trailing slash for consistent path handling
        val normalizedPath = requestPath.trimEnd('/')
        val pathDisplay = if (normalizedPath.isEmpty()) "/" else "/$normalizedPath/"
        val parentPath = if (normalizedPath.isEmpty()) "" else {
            val parent = normalizedPath.substringBeforeLast("/", "")
            if (parent.isEmpty()) "/" else "/$parent/"
        }

        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
        sb.append("<title>Index of ${pathDisplay.encodeXml()}</title>")
        sb.append("<style>body{font-family:sans-serif;margin:20px;}a{text-decoration:none;}")
        sb.append("table{border-collapse:collapse;}td{padding:4px 16px;}")
        sb.append("tr:hover{background:#f0f0f0;}</style></head><body>")
        sb.append("<h1>Index of ${pathDisplay.encodeXml()}</h1><table>")
        sb.append("<tr><th>Name</th><th>Size</th><th>Modified</th></tr>")

        if (normalizedPath.isNotEmpty()) {
            sb.append("<tr><td><a href=\"$parentPath\">../</a></td><td></td><td></td></tr>")
        }

        file.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            ?.forEach { child ->
                val childPath = if (normalizedPath.isEmpty()) child.name
                else "$normalizedPath/${child.name}"
                val encodedChildPath = "/${encodeXmlPath(childPath)}"
                val displayName = child.name.encodeXml() + if (child.isDirectory) "/" else ""
                val size = if (child.isDirectory) "-" else formatFileSize(child.length())
                val modified = formatHttpDate(child.lastModified())
                sb.append("<tr><td><a href=\"$encodedChildPath\">$displayName</a></td>")
                sb.append("<td>$size</td><td>$modified</td></tr>")
            }

        sb.append("</table></body></html>")
        return sb.toString()
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
            else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
        }
    }

    private fun formatIsoDate(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.format(millis)
    }

    private suspend fun checkIfHeader(call: ApplicationCall) {
        // If header is present, validate lock token format (accept any valid token)
        val ifHeader = call.request.requestHeader("If") ?: return
        if (ifHeader.isBlank()) return
        // Accept any well-formed If header that contains a lock token
        if (!ifHeader.contains("urn:uuid:") && !ifHeader.contains("<") && ifHeader != "*") {
            call.respond(HttpStatusCode.PreconditionFailed)
        }
    }

    private fun encodeXmlPath(path: String): String {
        return path.split("/").filter { it.isNotEmpty() }.joinToString("/") { part ->
            part.encodeURL()
        }
    }

    private fun String.encodeURL(): String {
        val sb = StringBuilder()
        for (ch in this) {
            when (ch) {
                in 'A'..'Z', in 'a'..'z', in '0'..'9', '-', '_', '.', '~', '/' -> sb.append(ch)
                ' ' -> sb.append("%20")
                else -> {
                    val bytes = ch.toString().encodeToByteArray()
                    for (byte in bytes) {
                        sb.append('%')
                        sb.append(((byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')))
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun String.encodeXml(): String {
        return this
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    private fun formatHttpDate(millis: Long): String {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        return sdf.format(millis)
    }

    private fun contentTypeForFile(fileName: String): ContentType {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val mime = when (ext) {
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "txt" -> "text/plain"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            else -> "application/octet-stream"
        }
        return ContentType.parse(mime)
    }


    fun stop() {
        coroutineScope.launch {
            try {
                server.stop(1000, 1000)
            } catch (e: Throwable) {

            }

            coroutineScope.cancel()
        }

    }

    private fun getKeyStoreType(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "p12", "pfx" -> "PKCS12"
            "bks" -> "BKS"
            "jks" -> "JKS"
            else -> "PKCS12"
        }
    }
}
