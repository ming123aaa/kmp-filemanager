package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class FileType(val icon: ImageVector, val color: Color) {
    FOLDER(Icons.Default.Folder, Color(0xFFF59E0B)),
    IMAGE(Icons.Default.Image, Color(0xFFE74C3C)),
    VIDEO(Icons.Default.Movie, Color(0xFF9B59B6)),
    AUDIO(Icons.Default.Audiotrack, Color(0xFFF39C12)),
    TEXT(Icons.Default.Description, Color(0xFF3498DB)),
    HTML(Icons.Default.Html, Color(0xFF3498DB)),
    CODE(Icons.Default.Code, Color(0xFF27AE60)),
    DOC(Icons.Default.Article, Color(0xFF2980B9)),
    PDF(Icons.Default.PictureAsPdf, Color(0xFFE74C3C)),
    ZIP(Icons.Default.FolderZip, Color(0xFFF39C12)),
    EXE(Icons.Default.Settings, Color(0xFF7F8C8D)),
    APK(Icons.Default.Android, Color(0xFF27AE60)),
    OTHER(Icons.Default.InsertDriveFile, Color(0xFF7F8C8D));

    companion object {
        private val EXTENSION_MAP = mapOf(
            "jpg" to IMAGE, "jpeg" to IMAGE, "png" to IMAGE, "gif" to IMAGE,
            "bmp" to IMAGE, "webp" to IMAGE, "svg" to IMAGE, "ico" to IMAGE,
            "tiff" to IMAGE,
            "mp4" to VIDEO, "avi" to VIDEO, "mkv" to VIDEO, "mov" to VIDEO,
            "wmv" to VIDEO, "flv" to VIDEO, "webm" to VIDEO, "m4v" to VIDEO,
            "mp3" to AUDIO, "wav" to AUDIO, "flac" to AUDIO, "aac" to AUDIO,
            "ogg" to AUDIO, "wma" to AUDIO, "m4a" to AUDIO,
            "md" to TEXT, "txt" to TEXT, "log" to TEXT, "csv" to TEXT,
            "json" to TEXT, "xml" to TEXT, "yaml" to TEXT, "yml" to TEXT,
            "ini" to TEXT, "cfg" to TEXT, "conf" to TEXT, "html" to HTML, "properties" to TEXT,
            "gitignore" to CODE, "kt" to CODE, "java" to CODE, "py" to CODE,
            "js" to CODE, "ts" to CODE, "css" to CODE,
            "php" to CODE, "c" to CODE, "cpp" to CODE, "h" to CODE,
            "go" to CODE, "rs" to CODE, "rb" to CODE, "sh" to CODE,
            "bat" to CODE, "sql" to CODE, "vue" to CODE, "jsx" to CODE,
            "tsx" to CODE,
            "doc" to DOC, "docx" to DOC, "xls" to DOC, "xlsx" to DOC,
            "ppt" to DOC, "pptx" to DOC,
            "pdf" to PDF,
            "zip" to ZIP, "rar" to ZIP, "7z" to ZIP, "tar" to ZIP,
            "gz" to ZIP, "bz2" to ZIP, "xz" to ZIP,
            "exe" to EXE, "msi" to EXE, "dmg" to EXE, "app" to EXE,
            "deb" to EXE, "rpm" to EXE, "apk" to APK
        )

        fun getFileType(name: String): FileType {
            if (name.isEmpty()) return OTHER
            val ext = name.split(".").lastOrNull()?.lowercase() ?: return OTHER
            return EXTENSION_MAP[ext] ?: OTHER
        }

        fun isPreViewType(name: String): Boolean {
            return listOf(IMAGE, VIDEO, AUDIO, HTML).contains(getFileType(name))
        }

        fun isMediaType(name: String): Boolean {
            return listOf(IMAGE, VIDEO).contains(getFileType(name))
        }

        fun isEditStringType(name: String): Boolean {
            return listOf(TEXT, HTML, CODE).contains(getFileType(name))
        }
    }
}