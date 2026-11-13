package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Collections
import java.util.LinkedHashMap
import javax.imageio.ImageIO

// ============================================================
// 缩略图缓存
// ============================================================

 internal object VideoThumbnailCacheJvm {

    private const val DISK_CACHE_DIR = "filemanager_video_thumbnails"
    private const val TIMEOUT_MS = 7 * 24 * 3600 * 1000L

    private val diskCacheDir: File by lazy {
        File(System.getProperty("java.io.tmpdir"), DISK_CACHE_DIR).also { it.mkdirs() }
    }

    private val memCache =
        Collections.synchronizedMap(object : LinkedHashMap<String, ImageBitmap>(50, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, ImageBitmap>): Boolean {
                return size > 20
            }
        })

    fun get(key: String): ImageBitmap? {
        memCache[key]?.let { return it }
        val fromDisk = getFromDisk(key)
        if (fromDisk != null) memCache[key] = fromDisk
        return fromDisk
    }

    fun save(key: String, pngBytes: ByteArray): ImageBitmap? {
        return try {
            val bitmap = org.jetbrains.skia.Image.makeFromEncoded(pngBytes).toComposeImageBitmap()
            memCache[key] = bitmap
            saveToDisk(key, pngBytes)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun getFromDisk(key: String): ImageBitmap? {
        val file = cacheFile(key)
        return if (file.exists()) {
            if (System.currentTimeMillis() - file.lastModified() > TIMEOUT_MS) {
                file.delete()
                null
            } else {
                try {
                    org.jetbrains.skia.Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
                } catch (_: Exception) { null }
            }
        } else null
    }

    private fun saveToDisk(key: String, pngBytes: ByteArray) {
        val file = cacheFile(key)
        file.parentFile?.mkdirs()
        try { FileOutputStream(file).use { it.write(pngBytes) } } catch (_: Exception) {}
    }

    private fun cacheFile(key: String) = File(diskCacheDir, "${md5(key)}.png")

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

// ============================================================
// JavaCV FFmpegFrameGrabber 帧提取
// ============================================================

/**
 * 使用 JavaCV FFmpegFrameGrabber 提取视频首帧。
 * 支持本地文件和 HTTP URL，格式覆盖 FFmpeg 支持的全部编码。
 *
 * @return PNG 字节数组，失败返回 null
 */
private fun extractVideoFrame(videoPath: String): ByteArray? {
    val grabber = FFmpegFrameGrabber(videoPath)
    try {

        // 容忍轻微损坏，不因个别 atom 损坏而放弃
        grabber.setOption("err_detect", "ignore_err")

        try {
            grabber.startUnsafe(true)
        }catch (e:Exception){
            // 示例：给足探测时间与数据量（按需调整）
            grabber.setOption("analyzeduration", "10000000"); // 10秒（单位：微秒）
            grabber.setOption("probesize", "10000000");       // 约10MB（单位：字节）
            grabber.start(false)
        }
        val frame = grabber.grabImage()
        if (frame == null) return null

        val converter = Java2DFrameConverter()
        val bufferedImage = converter.convert(frame)
        if (bufferedImage == null) return null

        val scaled = scaleBufferedImage(bufferedImage, 320)

        val baos = ByteArrayOutputStream()
        ImageIO.write(scaled, "PNG", baos)
        return baos.toByteArray()
    } catch (e: Exception) {
        return null
    } finally {
        try { grabber.stop() } catch (_: Exception) {}
        try { grabber.release() } catch (_: Exception) {}
    }
}

private fun scaleBufferedImage(image: BufferedImage, targetSize: Int): BufferedImage {
    val w = image.width
    val h = image.height
    if (w <= targetSize && h <= targetSize) return image

    val scale = targetSize.toDouble() / maxOf(w, h)
    val newW = (w * scale).toInt()
    val newH = (h * scale).toInt()

    val scaled = BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB)
    val g = scaled.createGraphics()
    g.drawImage(image.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH), 0, 0, null)
    g.dispose()
    return scaled
}

// ============================================================
// JVM/Desktop actual
// ============================================================

@Composable
actual fun VideoThumbnail(
    videoUrl: String,
    modifier: Modifier,
    contentScale: ContentScale,
    thumbnailSize: Dp,
    placeholder: @Composable () -> Unit
) {
    var thumbnail by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(videoUrl) {
        isLoading = true
        loadFailed = false
        thumbnail = null
        job?.cancel()

        val cached = VideoThumbnailCacheJvm.get(videoUrl)
        if (cached != null) {
            thumbnail = cached
            isLoading = false
            return@LaunchedEffect
        }
        delay(500)
        job = scope.launch(Dispatchers.IO) {

            try {
                val pngBytes = withTimeoutOrNull(15_000) {
                    extractVideoFrame(videoUrl)
                }
                if (pngBytes != null) {
                    val bitmap = VideoThumbnailCacheJvm.save(videoUrl, pngBytes)
                    withContext(Dispatchers.Main) {
                        if (bitmap != null) thumbnail = bitmap else loadFailed = true
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loadFailed = true
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    loadFailed = true
                    isLoading = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { job?.cancel() }
    }

    Box(modifier = modifier) {
        when {
            isLoading -> placeholder()
            loadFailed -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = "视频",
                        tint = Color(0xFF9B59B6),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            thumbnail != null -> {
                Image(
                    bitmap = thumbnail!!,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        }
    }
}