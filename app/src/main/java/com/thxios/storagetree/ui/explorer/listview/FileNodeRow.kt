package com.thxios.storagetree.ui.explorer.listview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.thxios.storagetree.data.scanner.FileSizeFormatter
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.ui.components.SizeProgressBar
import com.thxios.storagetree.ui.theme.StorageTreeTheme

private val formatter = FileSizeFormatter()

@Composable
fun FileNodeRow(
    node: FileNode,
    totalSize: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = if (totalSize > 0) node.sizeBytes.toFloat() / totalSize else 0f
    val percentage = (fraction * 100).toInt()
    val typeLabel = if (node.isDirectory) "[D]" else "[F]"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = typeLabel,
            style = MaterialTheme.typography.bodySmall,
            color = if (node.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatter.format(node.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            SizeProgressBar(
                fraction = fraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FileNodeRowDirectoryPreview() {
    StorageTreeTheme {
        FileNodeRow(
            node = FileNode(name = "Downloads", path = "/storage/emulated/0/Downloads", sizeBytes = 524288000L, isDirectory = true),
            totalSize = 1073741824L,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FileNodeRowFilePreview() {
    StorageTreeTheme {
        FileNodeRow(
            node = FileNode(name = "video.mp4", path = "/storage/emulated/0/video.mp4", sizeBytes = 104857600L, isDirectory = false),
            totalSize = 1073741824L,
            onClick = {}
        )
    }
}
