package com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun PlatformSettingsSection(snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()

    // 缓存
    Text(
        "缓存",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val cacheDir = File(
                            System.getProperty("java.io.tmpdir"),
                            "filemanager_video_thumbnails"
                        )
                        cacheDir.listFiles()?.forEach { it.delete() }
                    }
                    snackbarHostState.showSnackbar("缓存已清理")
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1A1A2E)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("清理视频缩略图缓存")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "清除本地缓存的视频预览图，下次浏览时重新生成",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(24.dp))
}