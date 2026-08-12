package com.ohuang.kmp.filemanager.kmp_filemanager.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ohuang.kmp.filemanager.kmp_filemanager.HttpConfig
import com.ohuang.kmp.filemanager.kmp_filemanager.data.TextEditorNavData
import com.ohuang.kmp.filemanager.kmp_filemanager.data.openUri
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.utils.ScreenType
import com.ohuang.kmp.filemanager.kmp_filemanager.ui.utils.rememberScreenType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    navData: TextEditorNavData,
    isRemote: Boolean = true,
    onBack: () -> Unit,
    onSaved: suspend (String) -> Result<Unit>
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isEditMode by remember { mutableStateOf(navData.defaultEditMode) }
    var editContent by remember { mutableStateOf(navData.content) }
    var hasChanges by remember { mutableStateOf(false) }
    var selectionKey by remember { mutableLongStateOf(0L) }
    var isSaving by remember { mutableStateOf(false) }
    var fontSize by remember { mutableStateOf(HttpConfig.loadFontSize().sp) }
    val rememberScreenType = rememberScreenType()

    val contentPadding = 16.dp
    val verticalPadding = 8.dp
    val innerPadding = 12.dp

    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = navData.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (isEditMode) "编辑模式" else "查看模式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (hasChanges) {
                        IconButton(
                            enabled = hasChanges && !isSaving,
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    try {
                                        val result = onSaved(editContent)
                                        if (result.isSuccess) {
                                            snackbarHostState.showSnackbar("保存成功")
                                            hasChanges = false
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                "保存失败: ${result.exceptionOrNull()?.message ?: "未知错误"}"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("保存失败: ${e.message ?: "未知错误"}")
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "保存")
                        }
                    }

                    if (!hasChanges||rememberScreenType!= ScreenType.PHONE) {
                        IconButton(
                            enabled = fontSize > 8.sp,
                            onClick = {
                                fontSize = (fontSize.value - 2).sp
                                HttpConfig.saveFontSize(fontSize.value)
                            }
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "缩小字号")
                        }
                        IconButton(
                            enabled = fontSize < 32.sp,
                            onClick = {
                                fontSize = (fontSize.value + 2).sp
                                HttpConfig.saveFontSize(fontSize.value)
                            }
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "放大字号")
                        }

                    }

                    IconButton(onClick = {
                        hasChanges=editContent!=navData.content
                        isEditMode = !isEditMode
                    }) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.EditOff else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "查看模式" else "编辑模式"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isEditMode) {
                OutlinedTextField(
                    value = editContent,
                    onValueChange = {
                        editContent = it
                        hasChanges = true
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = contentPadding, vertical = verticalPadding)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(6.dp)
                        ),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = fontSize,
                        lineHeight = fontSize * 1.5
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Text
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(6.dp),
                    placeholder = {
                        Text(
                            text = "当前无内容",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                )
            } else {
                val scrollState = rememberScrollState()
                val annotatedString = buildLinkAnnotatedString(editContent)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = contentPadding, vertical = verticalPadding)
                ) {
                    key(selectionKey) {
                        SelectionContainer {
                            ClickableText(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 300.dp)
                                    .padding(vertical = verticalPadding),
                                text = annotatedString,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = fontSize,
                                    lineHeight = fontSize * 1.5
                                ),
                                onClick = { offset ->
                                    val annotation = annotatedString
                                        .getStringAnnotations("URL", offset, offset)
                                        .firstOrNull()
                                    if (annotation != null) {
                                        val success = openUri(annotation.item)
                                        if (!success) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("无法打开链接")
                                            }
                                        }
                                    } else {
                                        selectionKey++
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private val URL_PATTERN = Regex(
    """https?://[^\s"'<>，。；;！!？）\)】\]】〗]+|www\.[^\s"'<>，。；;！!？）\)】\]】〗]+""",
    RegexOption.IGNORE_CASE
)

@Composable
private fun buildLinkAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        var lastIndex = 0
        val matches = URL_PATTERN.findAll(text)
        for (match in matches) {
            if (match.range.first > lastIndex) {
                append(text.substring(lastIndex, match.range.first))
            }
            val url = match.value
            val displayUrl = if (url.startsWith("www.")) "https://$url" else url
            pushStringAnnotation("URL", displayUrl)
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            ) {
                append(url)
            }
            pop()
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}
