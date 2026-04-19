package com.thxios.storagetree.ui.explorer.treemap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.ui.theme.StorageTreeTheme

@Composable
fun TreemapView(
    nodes: List<FileNode>,
    onNodeClick: (FileNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val colorScheme = MaterialTheme.colorScheme
    val tileColors = remember(colorScheme) {
        listOf(
            colorScheme.primary,
            colorScheme.secondary,
            colorScheme.tertiary,
            colorScheme.primaryContainer,
            colorScheme.secondaryContainer,
            colorScheme.tertiaryContainer
        )
    }

    val algorithm = remember { SquarifyAlgorithm() }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val visibleNodes = remember(nodes) { nodes.filter { it.sizeBytes > 0 } }
        val rects = remember(visibleNodes, widthPx, heightPx) {
            algorithm.compute(visibleNodes, widthPx, heightPx)
        }

        val labelMinWidthPx = with(density) { 40.dp.toPx() }
        val labelMinHeightPx = with(density) { 20.dp.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("treemap_canvas")
                .pointerInput(rects) {
                    detectTapGestures { offset ->
                        val hit = rects.lastOrNull { rect ->
                            offset.x >= rect.left && offset.x <= rect.right &&
                                    offset.y >= rect.top && offset.y <= rect.bottom
                        }
                        hit?.let { onNodeClick(it.node) }
                    }
                }
        ) {
            rects.forEachIndexed { index, treemapRect ->
                val fillColor = tileColors[index % tileColors.size]
                val rectLeft = treemapRect.left
                val rectTop = treemapRect.top
                val rectWidth = treemapRect.right - treemapRect.left
                val rectHeight = treemapRect.bottom - treemapRect.top

                if (rectWidth <= 0f || rectHeight <= 0f) return@forEachIndexed

                drawRect(
                    color = fillColor,
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectWidth, rectHeight)
                )

                drawRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectWidth, rectHeight),
                    style = Stroke(width = 1.dp.toPx())
                )

                if (rectWidth > labelMinWidthPx && rectHeight > labelMinHeightPx) {
                    val textLayoutResult = textMeasurer.measure(
                        text = treemapRect.node.name,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp
                        ),
                        constraints = androidx.compose.ui.unit.Constraints(
                            maxWidth = rectWidth.toInt().coerceAtLeast(0)
                        )
                    )
                    val textX = rectLeft + (rectWidth - textLayoutResult.size.width) / 2f
                    val textY = rectTop + (rectHeight - textLayoutResult.size.height) / 2f
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(
                            x = textX.coerceIn(rectLeft, rectLeft + rectWidth),
                            y = textY.coerceIn(rectTop, rectTop + rectHeight)
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TreemapViewPreview() {
    StorageTreeTheme {
        TreemapView(
            nodes = listOf(
                FileNode(name = "Videos", path = "/Videos", sizeBytes = 5_000_000, isDirectory = true),
                FileNode(name = "Photos", path = "/Photos", sizeBytes = 3_000_000, isDirectory = true),
                FileNode(name = "Music", path = "/Music", sizeBytes = 2_000_000, isDirectory = true),
                FileNode(name = "Documents", path = "/Documents", sizeBytes = 1_000_000, isDirectory = true),
                FileNode(name = "Downloads", path = "/Downloads", sizeBytes = 800_000, isDirectory = true),
                FileNode(name = "Apps", path = "/Apps", sizeBytes = 600_000, isDirectory = true)
            ),
            onNodeClick = {},
            modifier = Modifier.size(400.dp, 300.dp)
        )
    }
}
