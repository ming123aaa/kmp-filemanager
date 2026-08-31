package com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens

import com.ohuang.kmp.filemanager.kmp_filemanager.ui.components.PlatformVideoPlayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import com.ohuang.kmp.filemanager.kmp_filemanager.util.openUrlInBrowser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    url: String,
    fileName: String,
    onClose: () -> Unit
) {


    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = Color.Black, topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileName,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "关闭",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                ),
                actions = {
                    IconButton(onClick = { openUrlInBrowser(url) }) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "在浏览器中打开",
                            tint = Color.White
                        )
                    }
                }
            )
        }) {

        Column(modifier = Modifier.fillMaxSize().padding(it)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                PlatformVideoPlayer(
                    url = url,
                    modifier = Modifier.fillMaxSize(),
                    showControls = true,
                    autoPlay = true
                )
            }
        }

    }

}