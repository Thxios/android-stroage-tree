package com.thxios.storagetree.ui.explorer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thxios.storagetree.ui.components.ErrorBanner
import com.thxios.storagetree.ui.components.ScanProgressBanner
import com.thxios.storagetree.ui.explorer.listview.StorageListView
import com.thxios.storagetree.ui.theme.StorageTreeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(viewModel: ExplorerViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ExplorerContent(
        uiState = uiState,
        onNodeClick = viewModel::navigateTo
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExplorerContent(
    uiState: ExplorerUiState,
    onNodeClick: (com.thxios.storagetree.domain.model.FileNode) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Storage Explorer") })
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Column(
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
                displayedChildren = listOf(
                    com.thxios.storagetree.domain.model.FileNode(
                        name = "Downloads",
                        path = "/Downloads",
                        sizeBytes = 500_000_000L,
                        isDirectory = true
                    )
                )
            ),
            onNodeClick = {}
        )
    }
}
