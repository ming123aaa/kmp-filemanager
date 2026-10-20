package com.ohuang.kmp.filemanager.kmp_filemanager.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object UriToFile {

    private class FileDescriptorInputStream(
        private var parcelFileDescriptor: ParcelFileDescriptor?
    ) : FileInputStream(parcelFileDescriptor!!.fileDescriptor) {

        override fun close() {
            super.close()
            parcelFileDescriptor = null
        }
    }

    @JvmStatic
    fun getFileName(uri: Uri, context: Context): String? {
        return getFileName(context.contentResolver, uri)
    }

    @JvmStatic
    fun uriToFileInputStream(uri: Uri, context: Context): FileInputStream? {
        var fis: FileInputStream? = null
        try {
            val r = context.contentResolver.openFileDescriptor(uri, "r")
            if (r != null) {
                fis = FileDescriptorInputStream(r)
            }
        } catch (_: Throwable) {
        }
        if (fis == null) {
            try {
                val file = uriToFile(uri, context)
                if (file != null) {
                    fis = FileInputStream(file)
                }
            } catch (e: FileNotFoundException) {
                throw RuntimeException(e)
            }
        }
        return fis
    }

    /**
     * 统一的 URI 转 File 方法，兼容所有 Android 版本
     */
    @JvmStatic
    fun uriToFile(uri: Uri?, context: Context): File? {
        if (uri == null) return null
        return when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> {
                val path = uri.path ?: return null
                File(path).takeIf { it.exists() }
            }
            ContentResolver.SCHEME_CONTENT -> contentUriToFile(uri, context)
            else -> null
        }
    }

    /**
     * 将 content:// URI 转换为本地文件
     * 优先尝试直接获取文件路径，失败则复制到缓存目录
     */
    private fun contentUriToFile(uri: Uri, context: Context): File? {
        val path = getPath(context, uri)
        if (path != null) {
            File(path).takeIf { it.exists() && it.length() > 0 }?.let { return it }
        }
        return copyToCache(uri, context)
    }

    /**
     * 将 content:// URI 的内容复制到缓存目录
     */
    private fun copyToCache(uri: Uri, context: Context): File? {
        val resolver = context.contentResolver
        val fileName = getFileName(resolver, uri) ?: run {
            val ext = getExtensionFromUri(resolver, uri)
            val name = "${System.currentTimeMillis()}_${(Math.random() * 10000).toInt()}"
            if (ext != null) "$name.$ext" else name
        }

        return try {
            resolver.openInputStream(uri)?.use { input ->
                val cacheDir = File(context.cacheDir, "upload_cache")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val destFile = File(cacheDir, fileName)
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
                destFile
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun copyChecheDir(context: Context): File {
        return File(context.cacheDir, "upload_cache")
    }

    private fun getFileName(resolver: ContentResolver, uri: Uri): String? {
        return try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index)?.takeIf { it.isNotEmpty() } else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getExtensionFromUri(resolver: ContentResolver, uri: Uri): String? {
        return try {
            val mimeType = resolver.getType(uri)
            if (mimeType != null) {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    @JvmStatic
    fun getPath(context: Context, uri: Uri?): String? {
        if (uri == null) return null
        if (uri.scheme == null) return null

        return when {
            ContentResolver.SCHEME_FILE == uri.scheme -> uri.path
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                    && DocumentsContract.isDocumentUri(context, uri) -> getDocumentPath(context, uri)
            ContentResolver.SCHEME_CONTENT == uri.scheme -> getContentPath(context, uri)
            else -> null
        }
    }

    private fun getDocumentPath(context: Context, uri: Uri): String? {
        val authority = uri.authority ?: return null

        return when {
            isExternalStorageDocument(uri) -> {
                val docId = DocumentsContract.getDocumentId(uri)
                val parts = docId.split(":")
                if (parts.size < 2) return null
                if ("primary".equals(parts[0], ignoreCase = true)) {
                    Environment.getExternalStorageDirectory().toString() + "/" + parts[1]
                } else null
            }
            isDownloadsDocument(uri) -> {
                val id = DocumentsContract.getDocumentId(uri) ?: return null
                try {
                    val longId = id.toLong()
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), longId
                    )
                    getDataColumn(context, contentUri, null, null)
                } catch (_: NumberFormatException) {
                    when {
                        id.startsWith("raw:") -> id.substring(4)
                        id.startsWith("content://") -> getDataColumn(context, Uri.parse(id), null, null)
                        else -> null
                    }
                }
            }
            isMediaDocument(uri) -> {
                val docId = DocumentsContract.getDocumentId(uri) ?: return null
                val parts = docId.split(":")
                if (parts.size < 2) return null
                val contentUri = getMediaContentUri(parts[0])
                if (contentUri != null) {
                    getDataColumn(context, contentUri, "_id=?", arrayOf(parts[1]))
                } else null
            }
            else -> null
        }
    }

    private fun getContentPath(context: Context, uri: Uri): String? {
        return if (isGooglePhotosUri(uri)) {
            uri.lastPathSegment
        } else {
            getDataColumn(context, uri, null, null)
        }
    }

    private fun getMediaContentUri(type: String): Uri? {
        return when (type) {
            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> null
        }
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        return try {
            context.contentResolver.query(uri, arrayOf("_data"), selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex("_data")
                    if (index >= 0) cursor.getString(index) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean =
        "com.android.externalstorage.documents" == uri.authority

    private fun isDownloadsDocument(uri: Uri): Boolean =
        "com.android.providers.downloads.documents" == uri.authority

    private fun isMediaDocument(uri: Uri): Boolean =
        "com.android.providers.media.documents" == uri.authority

    private fun isGooglePhotosUri(uri: Uri): Boolean =
        "com.google.android.apps.photos.content" == uri.authority
}