package com.ohuang.kmp.filemanager.kmp_filemanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun Breadcrumb(
    currentPath: String,
    onNavigate: (String) -> Unit,
    rootLabel: String = "根目录"
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clickable { onNavigate("") }
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = rootLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (currentPath.isEmpty()) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (currentPath.isNotEmpty()) {
            val parts = currentPath.split("/").filter { it.isNotEmpty() }
            var accPath = ""

            parts.forEachIndexed { index, part ->
                accPath += (if (accPath.isEmpty()) "" else "/") + part
                val isLast = index == parts.size - 1
                val navPath = accPath

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Separator",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )

                Box(
                    modifier = Modifier
                        .clickable { onNavigate(navPath) }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = part,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isLast) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 150.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
    }
}