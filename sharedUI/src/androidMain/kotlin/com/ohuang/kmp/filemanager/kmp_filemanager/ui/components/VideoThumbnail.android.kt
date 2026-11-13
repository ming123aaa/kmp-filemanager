package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.ohuang.kmp.filemanager.kmp_filemanager.AppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Collections
import java.util.LinkedHashMap

// ============================================================
// 视频缩略图磁盘/内存缓存（Android 专用）
// ============================================================

private object VideoThumbnailCache {

    private const val DISK_CACHE_DIR = "video_thumbnails"
    private const val MAX_DISK_CACHE_SIZE = 1000 * 1024 * 1024 // 1000MB
    private const val TIMEOUT_MS = 7 * 24 * 3600 * 1000 // 七天后过期

    private val diskCacheDir: File by lazy {
        File(AppContext.instance.cacheDir, DISK_CACHE_DIR)
    }

    private val cache =
        Collections.synchronizedMap(object : LinkedHashMap<String, Bitmap>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<String, Bitmap>): Boolean {
                return size > 10
            }
        })

    fun get(url: String): Bitmap? = cache[url]

    fun putCache(url: String, bitmap: Bitmap) {
        cache[url] = bitmap
    }

    fun put(url: String, bitmap: Bitmap) {
        cache[url] = bitmap
        saveToDiskCache(url, bitmap)
    }

    fun getFromDiskCache(url: String): Bitmap? {
        val file = getCacheFile(url)
        return if (file.exists()) {
            if (System.currentTimeMillis() - file.lastModified() > TIMEOUT_MS) {
                file.delete()
                null
            } else {
                BitmapFactory.decodeFile(file.absolutePath)
            }
        } else {
            null
        }
    }

    private fun saveToDiskCache(url: String, bitmap: Bitmap) {
        val file = getCacheFile(url)
        try {
            if (!file.parentFile!!.exists()) {
                file.parentFile!!.mkdirs()
            }
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCacheFile(url: String): File {
        val hashedName = md5(url)
        return File(diskCacheDir, "$hashedName.webp")
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        val hexString = StringBuilder()
        for (b in digest) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }

    fun clear() {
        cache.clear()
        diskCacheDir.listFiles()?.forEach { it.delete() }
    }
}

// ============================================================
// Android actual 实现
// ============================================================

@Composable
actual fun VideoThumbnail(
    videoUrl: String,
    modifier: Modifier,
    contentScale: ContentScale,
    thumbnailSize: Dp,
    placeholder: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val targetSize = remember(thumbnailSize) {
        with(density) { thumbnailSize.toPx().toInt() }
    }

    val bitmapState = remember { mutableStateOf<Bitmap?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(videoUrl) {
        isLoading.value = true
        bitmapState.value = null

        job?.cancel()

        // 1. 检查内存缓存
        val cachedBitmap = VideoThumbnailCache.get(videoUrl)
        if (cachedBitmap != null) {
            bitmapState.value = cachedBitmap
            isLoading.value = false
            return@LaunchedEffect
        }

        // 2. 检查磁盘缓存并提取缩略图
        job = scope.launch(Dispatchers.IO) {
            // 检查磁盘缓存
            delay(100)
            val diskCachedBitmap = VideoThumbnailCache.getFromDiskCache(videoUrl)
            if (diskCachedBitmap != null) {
                VideoThumbnailCache.putCache(url = videoUrl, diskCachedBitmap)
                withContext(Dispatchers.Main) {
                    bitmapState.value = diskCachedBitmap
                    isLoading.value = false
                }
                return@launch
            }

            // 使用 MediaMetadataRetriever 提取视频帧
            val retriever = MediaMetadataRetriever()
            try {
                if (videoUrl.contains("://")) {
                    retriever.setDataSource(videoUrl, emptyMap<String, String>())
                } else {
                    retriever.setDataSource(videoUrl)
                }
                val frame: Bitmap? = retriever.frameAtTime

                if (frame != null) {
                    val scaledBitmap = scaleBitmap(frame, targetSize)
                    VideoThumbnailCache.put(videoUrl, scaledBitmap)
                    withContext(Dispatchers.Main) {
                        bitmapState.value = scaledBitmap
                        isLoading.value = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isLoading.value = false
                }
            } finally {
                retriever.release()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            job?.cancel()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading.value) {
            placeholder()
        }

        bitmapState.value?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
    }
}

private fun scaleBitmap(bitmap: Bitmap, targetSize: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    if (width <= targetSize && height <= targetSize) {
        return bitmap
    }

    val scale = targetSize.toFloat() / maxOf(width, height)
    val newWidth = (width * scale).toInt()
    val newHeight = (height * scale).toInt()

    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}