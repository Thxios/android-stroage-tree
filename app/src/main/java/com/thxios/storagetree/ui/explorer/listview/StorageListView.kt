package com.thxios.storagetree.ui.explorer.listview

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.ui.components.verticalScrollbar
import com.thxios.storagetree.ui.theme.StorageTreeTheme

@Composable
fun StorageListView(
    nodes: List<FileNode>,
    onNodeClick: (FileNode) -> Unit,
    onNodeLongClick: ((FileNode) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val totalSize = nodes.sumOf { it.sizeBytes }
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier.verticalScrollbar(listState)
    ) {
        items(nodes, key = { it.path }) { node ->
            FileNodeRow(
                node = node,
                totalSize = totalSize,
                onClick = { onNodeClick(node) },
                onLongClick = onNodeLongClick?.let { { it(node) } }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StorageListViewPreview() {
    StorageTreeTheme {
        StorageListView(
            nodes = listOf(
                FileNode(name = "Downloads", path = "/Downloads", sizeBytes = 500_000_000L, isDirectory = true),
                FileNode(name = "DCIM", path = "/DCIM", sizeBytes = 300_000_000L, isDirectory = true),
                FileNode(name = "document.pdf", path = "/document.pdf", sizeBytes = 2_000_000L, isDirectory = false)
            ),
            onNodeClick = {}
        )
    }
}
