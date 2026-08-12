package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.aryapreetam.cmpwebview.WebView

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.background(Color.Black)
    ) {
        WebView(
            url = url,
            modifier = Modifier.fillMaxSize(),
            onLoadStarted = {
                isLoading = true
                hasError = false
            },
            onLoadFinished = {
                isLoading = false
            },
            onLoadError = {
                isLoading = false
                hasError = true
            }
        )
    }
}