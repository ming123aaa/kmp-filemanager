package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ohuang.kmp.filemanager.kmp_filemanager.data.FilterMode
import com.ohuang.kmp.filemanager.kmp_filemanager.data.SortBy
import com.ohuang.kmp.filemanager.kmp_filemanager.data.SortDirection
import com.ohuang.kmp.filemanager.kmp_filemanager.data.ViewMode

@Composable
fun Toolbar(
    filterMode: FilterMode,
    onFilterModeChanged: (FilterMode) -> Unit,
    sortBy: SortBy,
    sortDirection: SortDirection,
    onSortChanged: (SortBy) -> Unit,
    onSortDirectionChanged: () -> Unit,
    onUploadClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onCreateFileClick: () -> Unit,
    onGoUpClick: () -> Unit,
    canGoUp: Boolean,
    viewMode: ViewMode = ViewMode.GRID,
    onViewModeChanged: (ViewMode) -> Unit = {},
    isMultiSelectMode: Boolean = false,
    onToggleMultiSelectMode: () -> Unit = {},
    isLocalFile: Boolean = false,
    downloadEnable: Boolean,
    readOnly: Boolean,
    isTablet: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val spacerWith = if (isTablet) 8.dp else 4.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onGoUpClick,
                contentPadding = PaddingValues(horizontal = spacerWith, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Go up",
                    tint = if (canGoUp) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Text("返回上级", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.width(spacerWith))

            FilterDropdown(filterMode = filterMode, onFilterModeChanged = onFilterModeChanged)
            SortSelector(
                sortBy = sortBy, sortDirection = sortDirection,
                onSortChanged = onSortChanged, onSortDirectionChanged = onSortDirectionChanged,
                isTablet = isTablet
            )

            Spacer(modifier = Modifier.width(spacerWith))

            ViewModeToggle(viewMode = viewMode, onViewModeChanged = onViewModeChanged)

            Spacer(modifier = Modifier.weight(1f))

            if (!readOnly || downloadEnable) {
                IconButton(onClick = onToggleMultiSelectMode) {
                    Icon(
                        imageVector = if (isMultiSelectMode) Icons.Default.Close else Icons.Default.CheckCircle,
                        contentDescription = if (isMultiSelectMode) "退出多选" else "多选",
                        tint = if (isMultiSelectMode) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!readOnly) {
                IconButton(onClick = onCreateFolderClick) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Create folder", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onCreateFileClick) {
                    Icon(Icons.Default.NoteAdd, contentDescription = "Create file", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onUploadClick) {
                    Icon(
                        imageVector = if (isLocalFile) Icons.Default.Add else Icons.Default.Upload,
                        contentDescription = if (isLocalFile) "Import" else "Upload",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun FilterDropdown(
    filterMode: FilterMode,
    onFilterModeChanged: (FilterMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val filterText = when (filterMode) {
        FilterMode.ALL -> "全部"
        FilterMode.FILES -> "文件"
        FilterMode.FOLDERS -> "文件夹"
    }

    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.padding(horizontal = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(filterText, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Filter dropdown", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("全部") }, onClick = { onFilterModeChanged(FilterMode.ALL); expanded = false })
            DropdownMenuItem(text = { Text("文件") }, onClick = { onFilterModeChanged(FilterMode.FILES); expanded = false })
            DropdownMenuItem(text = { Text("文件夹") }, onClick = { onFilterModeChanged(FilterMode.FOLDERS); expanded = false })
        }
    }
}

@Composable
fun SortSelector(
    sortBy: SortBy,
    sortDirection: SortDirection,
    onSortChanged: (SortBy) -> Unit,
    onSortDirectionChanged: () -> Unit,
    isTablet: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val spacerWith = if (isTablet) 8.dp else 4.dp

    Box {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(horizontal = spacerWith, vertical = 4.dp)
            ) {
                val sortText = when (sortBy) {
                    SortBy.NAME -> "名称"
                    SortBy.SIZE -> "大小"
                    SortBy.DATE -> "时间"
                }
                Text(sortText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Sort field", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }

            TextButton(
                onClick = onSortDirectionChanged,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    if (sortDirection == SortDirection.ASC) "顺序" else "倒序",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (sortDirection == SortDirection.ASC) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = "Sort direction",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("名称") },
                onClick = { onSortChanged(SortBy.NAME); expanded = false },
                leadingIcon = { if (sortBy == SortBy.NAME) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("大小") },
                onClick = { onSortChanged(SortBy.SIZE); expanded = false },
                leadingIcon = { if (sortBy == SortBy.SIZE) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            DropdownMenuItem(
                text = { Text("时间") },
                onClick = { onSortChanged(SortBy.DATE); expanded = false },
                leadingIcon = { if (sortBy == SortBy.DATE) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }
    }
}

@Composable
fun ViewModeToggle(
    viewMode: ViewMode,
    onViewModeChanged: (ViewMode) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onViewModeChanged(ViewMode.GRID) }) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "网格模式",
                    tint = if (viewMode == ViewMode.GRID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onViewModeChanged(ViewMode.PREVIEW) }) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = "预览模式",
                    tint = if (viewMode == ViewMode.PREVIEW) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}