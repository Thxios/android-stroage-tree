package com.thxios.storagetree.ui.explorer

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.thxios.storagetree.data.scanner.FileSizeFormatter
import com.thxios.storagetree.domain.model.FileCategory
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.domain.model.ViewMode
import com.thxios.storagetree.ui.components.ErrorBanner
import com.thxios.storagetree.ui.components.ScanProgressBanner
import com.thxios.storagetree.ui.explorer.listview.StorageListView
import com.thxios.storagetree.ui.explorer.treemap.TreemapView
import com.thxios.storagetree.ui.theme.StorageTreeTheme

private val formatter = FileSizeFormatter()

@Composable
fun ExplorerScreen(
    viewModel: ExplorerViewModel = hiltViewModel(),
    navController: NavController? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.startScan(Environment.getExternalStorageDirectory().absolutePath)
    }

    BackHandler {
        if (uiState.currentPath.isNotEmpty() &&
            uiState.currentPath != Environment.getExternalStorageDirectory().absolutePath
        ) {
            viewModel.navigateUp()
        } else {
            navController?.popBackStack()
        }
    }

    uiState.pendingDeleteNode?.let { node ->
        AlertDialog(
            onDismissRequest = { viewModel.setPendingDelete(null) },
            title = { Text("Delete") },
            text = { Text("Delete \"${node.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteNode() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setPendingDelete(null) }) { Text("Cancel") }
            }
        )
    }

    ExplorerContent(
        uiState = uiState,
        onNodeClick = { node ->
            if (node.isDirectory) viewModel.navigateTo(node)
        },
        onNodeLongClick = { node -> viewModel.setPendingDelete(node) },
        onNavigateUp = {
            if (uiState.currentPath.isNotEmpty() &&
                uiState.currentPath != Environment.getExternalStorageDirectory().absolutePath
            ) {
                viewModel.navigateUp()
            } else {
                navController?.popBackStack()
            }
        },
        onToggleViewMode = { viewModel.toggleViewMode() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExplorerContent(
    uiState: ExplorerUiState,
    onNodeClick: (FileNode) -> Unit,
    onNodeLongClick: (FileNode) -> Unit,
    onNavigateUp: () -> Unit,
    onToggleViewMode: () -> Unit
) {
    val isAtRoot = uiState.currentPath.isEmpty() ||
        uiState.currentPath == Environment.getExternalStorageDirectory().absolutePath

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.currentPath.isEmpty()) "Storage"
                               else uiState.currentPath.substringAfterLast("/")
                    )
                },
                navigationIcon = {
                    if (!isAtRoot) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleViewMode) {
                        Icon(
                            imageVector = if (uiState.viewMode == ViewMode.LIST)
                                Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "Toggle view"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isScanning) {
                ScanProgressBanner(currentPath = uiState.scanningCurrentPath)
            }
            if (uiState.error != null) {
                ErrorBanner(message = uiState.error)
            }
            if (uiState.categorySummary.isNotEmpty()) {
                CategoryChipRow(
                    summary = uiState.categorySummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            when (uiState.viewMode) {
                ViewMode.LIST -> StorageListView(
                    nodes = uiState.displayedChildren,
                    onNodeClick = onNodeClick,
                    onNodeLongClick = onNodeLongClick,
                    modifier = Modifier.fillMaxSize()
                )
                ViewMode.TREEMAP -> TreemapView(
                    nodes = uiState.displayedChildren,
                    onNodeClick = onNodeClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CategoryChipRow(
    summary: Map<FileCategory, Long>,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier) {
        items(summary.entries.sortedByDescending { it.value }.toList()) { (category, size) ->
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text("${category.name} ${formatter.format(size)}") },
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExplorerScreenPreview() {
    StorageTreeTheme {
        ExplorerContent(
            uiState = ExplorerUiState(
                currentPath = "/storage/emulated/0",
                displayedChildren = listOf(
                    FileNode(name = "Downloads", path = "/storage/emulated/0/Downloads", sizeBytes = 500_000_000L, isDirectory = true),
                    FileNode(name = "DCIM", path = "/storage/emulated/0/DCIM", sizeBytes = 300_000_000L, isDirectory = true)
                ),
                categorySummary = mapOf(
                    FileCategory.IMAGE to 200_000_000L,
                    FileCategory.VIDEO to 100_000_000L
                )
            ),
            onNodeClick = {},
            onNodeLongClick = {},
            onNavigateUp = {},
            onToggleViewMode = {}
        )
    }
}
