package com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.ohuang.kmp.filemanager.kmp_filemanager.ApiService
import com.ohuang.kmp.filemanager.kmp_filemanager.HttpConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.Settings
import com.ohuang.kmp.filemanager.kmp_filemanager.data.FileManager
import com.ohuang.kmp.filemanager.kmp_filemanager.data.launchFolderPicker
import com.ohuang.kmp.filemanager.kmp_filemanager.data.openUri
import com.ohuang.kmp.filemanager.kmp_filemanager.getDefaultServerRootPath
import com.ohuang.kmp.filemanager.kmp_filemanager.getPlatform
import com.ohuang.kmp.filemanager.kmp_filemanager.server.ServerConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.server.getServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, settings: Settings? = null) {
    var serverUrl by remember { mutableStateOf(HttpConfig.getBaseUrl()) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var downloadDir by remember { mutableStateOf(HttpConfig.getDownloadDir()) }
    val scope = rememberCoroutineScope()
    val fileManager = remember { FileManager() }
    val serverManager = remember { getServerManager() }
    val isServerRunning by serverManager.isRunning.collectAsState()
    val lastError by serverManager.lastError.collectAsState()
    val accessUrl by serverManager.accessUrl.collectAsState()
    var serverPort by remember { mutableStateOf(HttpConfig.loadServerPort().toString()) }
    var serverRootPath by remember {
        mutableStateOf(
            HttpConfig.loadServerRootPath().ifEmpty { serverManager.currentConfig.rootPath })
    }
    var serverReadOnly by remember { mutableStateOf(HttpConfig.loadServerReadOnly()) }
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        downloadDir = HttpConfig.getDownloadDir()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier

                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 服务器地址
                Text(
                    "服务器地址",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("http://localhost:8080") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (serverUrl.isNotEmpty()) {
                            IconButton(onClick = { serverUrl = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            HttpConfig.saveBaseUrl(serverUrl)
                            testResult = "已保存"
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("保存") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isTesting = true
                                testResult = null
                                try {
                                    val result = ApiService.testConnect(serverUrl)
                                    testResult =
                                        if (result.lowercase().contains("read")) "连接成功 (只读模式)" else "连接成功"
                                } catch (e: Exception) {
                                    testResult = "连接失败: ${e.message}"
                                }
                                isTesting = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isTesting
                    ) {
                        if (isTesting) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("测试连接")
                    }
                }
                testResult?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.contains("成功")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                // 服务器管理
                Text(
                    "服务器管理",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 服务器状态
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("服务器状态", style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                androidx.compose.foundation.Canvas(
                                    modifier = Modifier.size(10.dp)
                                ) {
                                    drawCircle(
                                        color = if (isServerRunning) androidx.compose.ui.graphics.Color(0xFF4CAF50)
                                        else androidx.compose.ui.graphics.Color(0xFFF44336)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isServerRunning) "运行中" else "已停止",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isServerRunning) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // 错误信息
                        lastError?.let { error ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text(
                                        error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { serverManager.clearError() }) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "关闭",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 访问地址
                        accessUrl?.let { url ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(url))
                                            scope.launch {
                                                snackbarHostState.showSnackbar("已复制访问地址")
                                            }
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "访问地址",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            url,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "复制",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 端口
                        OutlinedTextField(
                            value = serverPort,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                                    serverPort = newValue
                                }
                            },
                            label = { Text("端口号") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isServerRunning
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 根路径
                        OutlinedTextField(
                            value = serverRootPath,
                            onValueChange = { serverRootPath = it },
                            label = { Text("根路径") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isServerRunning,
                            trailingIcon = {
                                if (!isServerRunning) {
                                    IconButton(onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            launchFolderPicker { path ->
                                                if (path != null) {
                                                    serverRootPath = path
                                                }
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Default.Folder, contentDescription = "选择文件夹")
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    serverRootPath = getDefaultServerRootPath()
                                },
                                enabled = !isServerRunning
                            ) { Text("恢复默认") }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // 可读写开关
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column {
                                Text("服务器可读写", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (serverReadOnly) "只读模式" else "可读写模式",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = !serverReadOnly,
                                onCheckedChange = { serverReadOnly = !it },
                                enabled = !isServerRunning
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // 启停按钮
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                            // 在浏览器中打开
                            if (isServerRunning && accessUrl != null) {
                                Button(
                                    onClick = {

                                        val page = if (serverReadOnly) "index.html" else "file.html"
                                        if (accessUrl?.endsWith("/")==true) {
                                            openUri("$accessUrl$page")
                                        }else {
                                            openUri("$accessUrl/$page")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("访问网页")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val port = serverPort.toIntOrNull() ?: 8080
                                        val config = ServerConfig(
                                            port = port,
                                            rootPath = serverRootPath,
                                            readOnly = serverReadOnly
                                        )
                                        HttpConfig.saveServerPort(port)
                                        HttpConfig.saveServerRootPath(serverRootPath)
                                        HttpConfig.saveServerReadOnly(serverReadOnly)
                                        serverManager.start(config)
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isServerRunning
                                ) { Text("启动服务器") }
                            }
                            OutlinedButton(
                                onClick = { serverManager.stop() },
                                modifier = Modifier.weight(1f),
                                enabled = isServerRunning
                            ) { Text("停止服务器") }
                        }


                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                // 下载目录
                Text(
                    "下载目录",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = downloadDir,
                    onValueChange = { },
                    label = { Text("保存位置") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                launchFolderPicker { path ->
                                    if (path != null) {
                                        downloadDir = path
                                        HttpConfig.saveDownloadDir(path)
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Folder, contentDescription = "选择文件夹")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val defaultDir = FileManager().getDownloadDir()
                            downloadDir = defaultDir
                            HttpConfig.saveDownloadDir(defaultDir)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("恢复默认") }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                // 平台特有设置
                PlatformSettingsSection(snackbarHostState)


                // 关于
                Text("关于", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("应用名称", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "File Manager",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("平台", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                getPlatform().name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
