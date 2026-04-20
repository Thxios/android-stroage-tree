package com.thxios.storagetree.ui.explorer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thxios.storagetree.ui.theme.StorageTreeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerSheet(
    currentPath: String,
    entries: List<String>,
    onDismiss: () -> Unit,
    onNavigateInto: (String) -> Unit,
    onStartScan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Breadcrumb row
            PickerBreadcrumb(
                currentPath = currentPath,
                onSegmentClick = onNavigateInto,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subdirectory list
            if (entries.isEmpty()) {
                Text(
                    text = "하위 폴더 없음",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(entries) { path ->
                        val name = path.substringAfterLast("/")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateInto(path) }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 0.dp)
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Start scan button
            Button(
                onClick = { onStartScan(currentPath) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "여기서 스캔 시작")
            }
        }
    }
}

@Composable
private fun PickerBreadcrumb(
    currentPath: String,
    onSegmentClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Strip /storage/emulated prefix for display
    val displayPath = currentPath.removePrefix("/storage/emulated")
    val segments = displayPath.split("/").filter { it.isNotEmpty() }
    val rootPrefix = if (currentPath.startsWith("/storage/emulated")) "/storage/emulated" else ""

    val scrollState = rememberScrollState()
    Row(
        modifier = modifier.horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        segments.forEachIndexed { i, segment ->
            val segmentPath = rootPrefix + "/" + segments.subList(0, i + 1).joinToString("/")
            val isLast = i == segments.lastIndex
            Text(
                text = segment,
                style = MaterialTheme.typography.titleSmall,
                color = if (isLast) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.primary,
                modifier = if (!isLast) Modifier.clickable { onSegmentClick(segmentPath) }
                           else Modifier,
                maxLines = 1
            )
            if (!isLast) {
                Text(
                    text = " / ",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun FolderPickerSheetPreview() {
    StorageTreeTheme {
        FolderPickerSheet(
            currentPath = "/storage/emulated/0/Download",
            entries = listOf(
                "/storage/emulated/0/Download/Apps",
                "/storage/emulated/0/Download/Music",
                "/storage/emulated/0/Download/Videos"
            ),
            onDismiss = {},
            onNavigateInto = {},
            onStartScan = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun FolderPickerSheetEmptyPreview() {
    StorageTreeTheme {
        FolderPickerSheet(
            currentPath = "/storage/emulated/0/Download/Apps",
            entries = emptyList(),
            onDismiss = {},
            onNavigateInto = {},
            onStartScan = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PickerBreadcrumbPreview() {
    StorageTreeTheme {
        PickerBreadcrumb(
            currentPath = "/storage/emulated/0/Download/Apps",
            onSegmentClick = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}
