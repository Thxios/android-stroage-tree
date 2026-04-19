package com.thxios.storagetree.ui.explorer

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.ui.components.ErrorBanner
import com.thxios.storagetree.ui.components.ScanProgressBanner
import com.thxios.storagetree.ui.explorer.listview.StorageListView
import com.thxios.storagetree.ui.theme.StorageTreeTheme

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
        if (uiState.currentPath.isNotEmpty() && uiState.currentPath != Environment.getExternalStorageDirectory().absolutePath) {
            viewModel.navigateUp()
        } else if (uiState.displayedChildren.isNotEmpty() || uiState.currentPath == Environment.getExternalStorageDirectory().absolutePath) {
            navController?.popBackStack()
        } else {
            viewModel.navigateUp()
        }
    }

    ExplorerContent(
        uiState = uiState,
        onNodeClick = { node ->
            if (node.isDirectory) viewModel.navigateTo(node)
        },
        onNavigateUp = {
            if (uiState.currentPath.isNotEmpty() && uiState.currentPath != Environment.getExternalStorageDirectory().absolutePath) {
                viewModel.navigateUp()
            } else {
                navController?.popBackStack()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExplorerContent(
    uiState: ExplorerUiState,
    onNodeClick: (FileNode) -> Unit,
    onNavigateUp: () -> Unit
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
            StorageListView(
                nodes = uiState.displayedChildren,
                onNodeClick = onNodeClick,
                modifier = Modifier.fillMaxSize()
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
                    FileNode(
                        name = "Downloads",
                        path = "/storage/emulated/0/Downloads",
                        sizeBytes = 500_000_000L,
                        isDirectory = true
                    ),
                    FileNode(
                        name = "DCIM",
                        path = "/storage/emulated/0/DCIM",
                        sizeBytes = 300_000_000L,
                        isDirectory = true
                    )
                )
            ),
            onNodeClick = {},
            onNavigateUp = {}
        )
    }
}
