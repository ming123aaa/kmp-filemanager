package com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.ohuang.kmp.filemanager.kmp_filemanager.*
import com.ohuang.kmp.filemanager.kmp_filemanager.ConnectionMode
import com.ohuang.kmp.filemanager.kmp_filemanager.data.FileManager
import com.ohuang.kmp.filemanager.kmp_filemanager.data.launchFilePicker
import com.ohuang.kmp.filemanager.kmp_filemanager.data.launchFolderPicker
import com.ohuang.kmp.filemanager.kmp_filemanager.data.openUri
import com.ohuang.kmp.filemanager.kmp_filemanager.discovery.DeviceBindingManager
import com.ohuang.kmp.filemanager.kmp_filemanager.discovery.DeviceInfo
import com.ohuang.kmp.filemanager.kmp_filemanager.server.ServerConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.server.getServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, settings: Settings? = null) {
    var serverUrl by remember { mutableStateOf(HttpConfig.getBaseUrl()) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var downloadDir by remember { mutableStateOf(HttpConfig.getDownloadDir()) }
    var connectionMode by remember { mutableStateOf(HttpConfig.getConnectionMode()) }
    var boundDeviceId by remember { mutableStateOf(HttpConfig.getBoundDeviceId()) }

    val scope = rememberCoroutineScope()
    val fileManager = remember { FileManager() }
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
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Column(
                    modifier = Modifier.widthIn(max = 800.dp)
                        .fillMaxWidth()
                ) {



                    // 远端服务器
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            "服务器绑定",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(modifier = Modifier.animateContentSize()) {


                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {



                            // 扫描绑定模式
                            DeviceScanSection(
                                currentMode = connectionMode,
                                currentBoundDeviceId = boundDeviceId,
                                onSwitchToManual = {
                                    HttpConfig.saveConnectionMode(ConnectionMode.MANUAL_URL)
                                    HttpConfig.saveBoundDeviceId("")
                                    connectionMode = ConnectionMode.MANUAL_URL
                                    boundDeviceId = ""
                                },
                                onSwitchToBoundDevice = { deviceId ->
                                    HttpConfig.saveConnectionMode(ConnectionMode.BOUND_DEVICE)
                                    HttpConfig.saveBoundDeviceId(deviceId)
                                    connectionMode = ConnectionMode.BOUND_DEVICE
                                    boundDeviceId = deviceId
                                }
                            )

                            if (connectionMode == ConnectionMode.MANUAL_URL) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "通过地址绑定",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                // 手动输入模式
                                OutlinedTextField(
                                    value = serverUrl,
                                    onValueChange = { serverUrl = it },
                                    label = { Text("远端服务器地址") },
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
                                            HttpConfig.saveConnectionMode(ConnectionMode.MANUAL_URL)
                                            HttpConfig.saveBoundDeviceId("")
                                            connectionMode = ConnectionMode.MANUAL_URL
                                            boundDeviceId = ""
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
                                                        if (result.lowercase()
                                                                .contains("read")
                                                        ) "连接成功 (只读模式)" else "连接成功"
                                                } catch (e: Exception) {
                                                    testResult = "连接失败: ${e.message}"
                                                }
                                                isTesting = false
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isTesting
                                    ) {
                                        if (isTesting) CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
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
                            }

                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 服务器管理
                    ServerManagementSection(
                        snackbarHostState = snackbarHostState
                    )

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
                    Text(
                        "关于",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerManagementSection(
    snackbarHostState: SnackbarHostState,
) {
    val serverManager = remember { getServerManager() }
    val isServerRunning by serverManager.isRunning.collectAsState()
    val lastError by serverManager.lastError.collectAsState()
    val accessUrl by serverManager.accessUrl.collectAsState()
    val ftpAccessUrl by serverManager.ftpAccessUrl.collectAsState()
    val webDavAccessUrl by serverManager.webDavAccessUrl.collectAsState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var serverPort by remember { mutableStateOf(HttpConfig.loadServerPort().toString()) }
    var serverName by remember { mutableStateOf(HttpConfig.getDeviceName()) }
    var serverRootPath by remember {
        mutableStateOf(
            HttpConfig.loadServerRootPath().ifEmpty { serverManager.currentConfig.rootPath })
    }
    var serverReadOnly by remember { mutableStateOf(HttpConfig.loadServerReadOnly()) }
    var serverUseHttps by remember { mutableStateOf(HttpConfig.loadServerUseHttps()) }
    var ftpPort by remember { mutableStateOf(HttpConfig.loadFtpPort().toString()) }
    var ftpUser by remember { mutableStateOf(HttpConfig.loadFtpUser()) }
    var ftpPassword by remember { mutableStateOf(HttpConfig.loadFtpPassword()) }
    var ftpEnabled by remember { mutableStateOf(HttpConfig.loadFtpEnabled()) }
    var webDavPort by remember { mutableStateOf(HttpConfig.loadWebDavPort().toString()) }
    var webDavUser by remember { mutableStateOf(HttpConfig.loadWebDavUser()) }
    var webDavPassword by remember { mutableStateOf(HttpConfig.loadWebDavPassword()) }
    var webDavEnabled by remember { mutableStateOf(HttpConfig.loadWebDavEnabled()) }
    var webDavUseHttps by remember { mutableStateOf(HttpConfig.loadWebDavUseHttps()) }
    var keystorePath by remember { mutableStateOf(HttpConfig.loadKeystorePath()) }
    var keystorePassword by remember { mutableStateOf(HttpConfig.loadKeystorePassword()) }
    var keyAlias by remember { mutableStateOf(HttpConfig.loadKeyAlias()) }
    var keyPassword by remember { mutableStateOf(HttpConfig.loadKeyPassword()) }
    var advancedExpanded by remember { mutableStateOf(false) }
    var usageDialogType by remember { mutableStateOf<String?>(null) }

    Text(
        "服务器管理",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 服务器名称
            OutlinedTextField(
                value = serverName,
                onValueChange = { serverName = it },
                label = { Text("服务器名称") },
                placeholder = { Text("用于在其他设备上识别本机") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isServerRunning,
                trailingIcon = {
                    if (serverName.isNotEmpty() && !isServerRunning) {
                        IconButton(onClick = { serverName = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 启停按钮
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isServerRunning && accessUrl != null) {
                    Button(
                        onClick = {
                            val page = if (serverReadOnly) "index.html" else "file.html"
                            if (accessUrl?.endsWith("/") == true) {
                                openUri("$accessUrl$page")
                            } else {
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
                            val ftpPortVal = ftpPort.toIntOrNull() ?: 2121
                            val webDavPortVal = webDavPort.toIntOrNull() ?: 8081
                            val config = ServerConfig(
                                port = port,
                                rootPath = serverRootPath,
                                readOnly = serverReadOnly,
                                useHttps = serverUseHttps,
                                ftpPort = ftpPortVal,
                                ftpUser = ftpUser,
                                ftpPassword = ftpPassword,
                                ftpEnabled = ftpEnabled,
                                webDavPort = webDavPortVal,
                                webDavUser = webDavUser,
                                webDavPassword = webDavPassword,
                                webDavEnabled = webDavEnabled,
                                webDavUseHttps = webDavUseHttps,
                                keystorePath = keystorePath,
                                keystorePassword = keystorePassword,
                                keyAlias = keyAlias,
                                keyPassword = keyPassword
                            )
                            HttpConfig.saveServerPort(port)
                            HttpConfig.saveServerRootPath(serverRootPath)
                            HttpConfig.saveServerReadOnly(serverReadOnly)
                            HttpConfig.saveServerUseHttps(serverUseHttps)
                            HttpConfig.saveDeviceName(serverName)
                            HttpConfig.saveFtpPort(ftpPortVal)
                            HttpConfig.saveFtpUser(ftpUser)
                            HttpConfig.saveFtpPassword(ftpPassword)
                            HttpConfig.saveFtpEnabled(ftpEnabled)
                            HttpConfig.saveWebDavPort(webDavPortVal)
                            HttpConfig.saveWebDavUser(webDavUser)
                            HttpConfig.saveWebDavPassword(webDavPassword)
                            HttpConfig.saveWebDavEnabled(webDavEnabled)
                            HttpConfig.saveWebDavUseHttps(webDavUseHttps)
                            HttpConfig.saveKeystorePath(keystorePath)
                            HttpConfig.saveKeystorePassword(keystorePassword)
                            HttpConfig.saveKeyAlias(keyAlias)
                            HttpConfig.saveKeyPassword(keyPassword)
                            serverManager.start(config)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isServerRunning
                    ) { Text("启动服务器") }
                }
                OutlinedButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            serverManager.stop()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isServerRunning
                ) { Text("停止服务器") }
            }
            Spacer(modifier = Modifier.height(12.dp))

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
                        IconButton(onClick = { usageDialogType = "http" }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "使用说明",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
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

            // WebDAV 访问地址
            webDavAccessUrl?.let { url ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(url))
                                scope.launch {
                                    snackbarHostState.showSnackbar("已复制 WebDAV 地址")
                                }
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "WebDAV 地址",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                url,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "账户: ${HttpConfig.loadWebDavUser()}  密码: ${HttpConfig.loadWebDavPassword()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(onClick = { usageDialogType = "webdav" }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "使用说明",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }


            // FTP 访问地址
            ftpAccessUrl?.let { url ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(url))
                                scope.launch {
                                    snackbarHostState.showSnackbar("已复制 FTP 地址")
                                }
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "FTP 地址",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                url,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                "密码: ${HttpConfig.loadFtpPassword()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(onClick = { usageDialogType = "ftp" }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "使用说明",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }


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
                    onClick = { serverRootPath = getDefaultServerRootPath() },
                    enabled = !isServerRunning
                ) { Text("恢复默认") }
            }


            Spacer(modifier = Modifier.height(12.dp))

            // 高级设置 展开/收缩
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { advancedExpanded = !advancedExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    "高级设置",
                    style = MaterialTheme.typography.titleSmall
                )
                Icon(
                    if (advancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (advancedExpanded) "收缩" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = advancedExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "本地服务器(http)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))

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
                    // 可读写开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column {
                            Text("服务器可读写", style = MaterialTheme.typography.bodySmall)
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
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column {
                            Text("启用 HTTPS", style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (serverUseHttps) "使用加密连接" else "使用 HTTP 明文连接",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = serverUseHttps,
                            onCheckedChange = { serverUseHttps = it },
                            enabled = !isServerRunning
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))


                    // FTP 配置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            "FTP 服务",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Switch(
                            checked = ftpEnabled,
                            onCheckedChange = { ftpEnabled = it },
                            enabled = !isServerRunning
                        )
                    }
                    if (ftpEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = ftpPort,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                                    ftpPort = newValue
                                }
                            },
                            label = { Text("FTP 端口") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isServerRunning
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = ftpUser,
                            onValueChange = { ftpUser = it },
                            label = { Text("用户名") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isServerRunning
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = ftpPassword,
                            onValueChange = { ftpPassword = it },
                            label = { Text("密码") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isServerRunning
                        )

                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    // WebDAV 配置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            "WebDAV 服务",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Switch(
                            checked = webDavEnabled,
                            onCheckedChange = { webDavEnabled = it },
                            enabled = !isServerRunning
                        )
                    }
                    if (webDavEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = webDavPort,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                                    webDavPort = newValue
                                }
                            },
                            label = { Text("WebDAV 端口") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isServerRunning
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column {
                                Text("启用 HTTPS", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    if (webDavUseHttps) "使用加密连接" else "使用 HTTP 明文连接",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = webDavUseHttps,
                                onCheckedChange = { webDavUseHttps = it },
                                enabled = !isServerRunning
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = webDavUser,
                            onValueChange = { webDavUser = it },
                            label = { Text("用户名") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isServerRunning
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = webDavPassword,
                            onValueChange = { webDavPassword = it },
                            label = { Text("密码") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isServerRunning
                        )

                    }

                    // HTTPS 证书配置（LocalFileServer 和 WebDAV 共用）
                    if (serverUseHttps || webDavUseHttps) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "HTTPS 证书配置",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = keystorePath,
                            onValueChange = { keystorePath = it },
                            label = { Text("Keystore 路径") },
                            placeholder = { Text("留空则自动生成") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isServerRunning,
                            trailingIcon = {
                                if (!isServerRunning) {
                                    IconButton(onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            launchFilePicker(allowMultiple = false) { paths ->
                                                if (paths.isNotEmpty()) {
                                                    keystorePath = paths.first()
                                                }
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Default.Folder, contentDescription = "选择 Keystore 文件")
                                    }
                                }
                            }
                        )
                        if (keystorePath.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = keystorePassword,
                                onValueChange = { keystorePassword = it },
                                label = { Text("Keystore 密码") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isServerRunning
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = keyAlias,
                                onValueChange = { keyAlias = it },
                                label = { Text("密钥别名") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isServerRunning
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = keyPassword,
                                onValueChange = { keyPassword = it },
                                label = { Text("密钥密码") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isServerRunning
                            )
                        }

                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // 使用说明弹窗
        usageDialogType?.let { type ->
            AlertDialog(
                onDismissRequest = { usageDialogType = null },
                title = {
                    Text(
                        when (type) {
                            "http" -> "HTTP 访问地址"
                            "ftp" -> "FTP 访问地址"
                            "webdav" -> "WebDAV 访问地址"
                            else -> ""
                        }
                    )
                },
                text = {
                    Text(
                        when (type) {
                            "http" -> "在浏览器中打开此地址，可浏览和管理文件。\n\n支持操作：上传、下载、删除、重命名、移动、在线预览。"
                            "ftp" -> "在 Windows 资源管理器地址栏输入此地址，即可像本地文件夹一样浏览文件。\n\n也可使用 FileZilla、WinSCP 等 FTP 客户端连接。\n\n注意：FTP 为明文传输，仅建议在局域网内使用。"
                            "webdav" -> "在 Windows 资源管理器中右键「此电脑」→「映射网络驱动器」，输入此地址并填写账户密码，即可将远程文件夹映射为本地盘符。\n\n映射后双击文件直接用本地应用打开，体验与本地磁盘一致。\n\n也可使用 RaiDrive、Cyberduck 等 WebDAV 客户端。"
                            else -> ""
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { usageDialogType = null }) {
                        Text("知道了")
                    }
                }
            )
        }
    }
}

@Composable
private fun DeviceScanSection(
    currentMode: ConnectionMode,
    currentBoundDeviceId: String,
    onSwitchToManual: () -> Unit,
    onSwitchToBoundDevice: (deviceId: String) -> Unit
) {
    val scope = rememberCoroutineScope()

    val boundDevices by DeviceBindingManager.boundDevices.collectAsState()
    val discoveredDevices by DeviceBindingManager.scanDevices.collectAsState()
    var isScanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var connectMsg by remember { mutableStateOf<String>("") }

    // 已绑定设备
    if (boundDevices.isNotEmpty()) {
        Text(
            "已绑定设备",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )


        boundDevices.sortedBy { device ->
            val isActive = currentMode == ConnectionMode.BOUND_DEVICE && currentBoundDeviceId == device.deviceId
            return@sortedBy !isActive
        }.forEach { device ->
            val isActive = currentMode == ConnectionMode.BOUND_DEVICE && currentBoundDeviceId == device.deviceId

            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                shape = MaterialTheme.shapes.medium,
                color = if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.background,
                tonalElevation = if (isActive) 2.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        if (isActive) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Column {
                            Text(
                                device.deviceName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                device.baseUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isActive) {
                            FilledTonalButton(
                                onClick = onSwitchToManual,
                                colors = ButtonDefaults.filledTonalButtonColors().copy(contentColor =
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("断开", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {

                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        onSwitchToBoundDevice(device.deviceId)
                                        connectMsg = "连接中..."
                                        DeviceBindingManager.scanOrNull()
                                        connectMsg = HttpConfig.checkConnect()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("连接", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        IconButton(
                            onClick = {
                                DeviceBindingManager.unbindDevice(device.deviceId)
                                if (isActive) onSwitchToManual()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "解绑",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            if (isActive && connectMsg.isNotEmpty()) {
                Text(
                    "状态:$connectMsg",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (connectMsg.contains("成功")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

    }
    Spacer(modifier = Modifier.height(12.dp))
    // 扫描按钮
    Button(
        onClick = {
            scope.launch {
                isScanning = true
                scanError = null
                try {
                    DeviceBindingManager.scan()
                } catch (e: Exception) {
                    scanError = "扫描失败: ${e.message}"
                }
                isScanning = false
            }
        },
        enabled = !isScanning,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        )
    ) {
        if (isScanning) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onTertiary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text("正在扫描局域网设备...")
        } else {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text("扫描局域网设备")
        }
    }

    // 扫描结果
    if (discoveredDevices.isNotEmpty()) {



        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(
                Icons.Default.Devices,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "发现 ${discoveredDevices.size} 台设备",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))


        discoveredDevices.forEach { device ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {


                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Computer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                device.deviceName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            device.baseUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 22.dp)
                        )
                    }
                    val isActive = currentMode == ConnectionMode.BOUND_DEVICE && currentBoundDeviceId == device.deviceId
                    val isBound = boundDevices.any { it.deviceId == device.deviceId }
                    if (isBound) {
                        if (isActive) {
                            FilledTonalButton(
                                onClick = onSwitchToManual,
                                colors = ButtonDefaults.filledTonalButtonColors().copy(contentColor =
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.7f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("断开", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        connectMsg = "连接中..."
                                        DeviceBindingManager.updateDevice(device)
                                        onSwitchToBoundDevice(device.deviceId)
                                        connectMsg = HttpConfig.checkConnect()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("连接", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                DeviceBindingManager.bindDevice(device)
                                onSwitchToBoundDevice(device.deviceId)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("绑定", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

    }

    // 扫描错误
    scanError?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Text(
                it,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}