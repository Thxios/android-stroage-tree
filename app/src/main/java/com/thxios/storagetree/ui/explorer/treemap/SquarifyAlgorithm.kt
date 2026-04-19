package com.thxios.storagetree.ui.explorer.treemap

import com.thxios.storagetree.domain.model.FileNode
import javax.inject.Inject

class SquarifyAlgorithm @Inject constructor() {

    fun compute(nodes: List<FileNode>, width: Float, height: Float): List<TreemapRect> {
        if (nodes.isEmpty()) return emptyList()
        val totalSize = nodes.sumOf { it.sizeBytes }.toFloat()
        if (totalSize == 0f) {
            return nodes.map { TreemapRect(it, 0f, 0f, 0f, 0f) }
        }
        val result = mutableListOf<TreemapRect>()
        squarify(nodes.sortedByDescending { it.sizeBytes }, totalSize, 0f, 0f, width, height, result)
        return result
    }

    private fun squarify(
        nodes: List<FileNode>,
        totalSize: Float,
        x: Float, y: Float, w: Float, h: Float,
        result: MutableList<TreemapRect>
    ) {
        if (nodes.isEmpty()) return
        if (nodes.size == 1) {
            result.add(TreemapRect(nodes[0], x, y, x + w, y + h))
            return
        }

        val row = mutableListOf<FileNode>()
        var remaining = nodes.toMutableList()

        while (remaining.isNotEmpty()) {
            val candidate = remaining[0]
            val testRow = row + candidate
            if (row.isEmpty() || worst(row, w, h, totalSize) >= worst(testRow, w, h, totalSize)) {
                row.add(candidate)
                remaining = remaining.drop(1).toMutableList()
            } else {
                layoutRow(row, totalSize, x, y, w, h, result)
                val rowSize = row.sumOf { it.sizeBytes }.toFloat()
                val fraction = rowSize / totalSize
                if (w >= h) {
                    val dx = w * fraction
                    squarify(remaining, totalSize - rowSize, x + dx, y, w - dx, h, result)
                } else {
                    val dy = h * fraction
                    squarify(remaining, totalSize - rowSize, x, y + dy, w, h - dy, result)
                }
                return
            }
        }
        layoutRow(row, totalSize, x, y, w, h, result)
    }

    private fun worst(row: List<FileNode>, w: Float, h: Float, totalSize: Float): Float {
        if (row.isEmpty()) return Float.MAX_VALUE
        val rowSize = row.sumOf { it.sizeBytes }.toFloat()
        val side = if (w >= h) h else w
        val area = (rowSize / totalSize) * w * h
        val rowLength = if (w >= h) w else h
        val stripe = area / rowLength
        var maxAspect = 0f
        for (node in row) {
            val nodeArea = (node.sizeBytes.toFloat() / rowSize) * area
            val nodeLen = if (stripe > 0f) nodeArea / stripe else 0f
            val aspect = if (nodeLen > 0f && stripe > 0f)
                maxOf(nodeLen / stripe, stripe / nodeLen) else Float.MAX_VALUE
            if (aspect > maxAspect) maxAspect = aspect
        }
        return maxAspect
    }

    private fun layoutRow(
        row: List<FileNode>,
        totalSize: Float,
        x: Float, y: Float, w: Float, h: Float,
        result: MutableList<TreemapRect>
    ) {
        val rowSize = row.sumOf { it.sizeBytes }.toFloat()
        val fraction = if (totalSize > 0f) rowSize / totalSize else 0f
        var offset = 0f
        if (w >= h) {
            val dx = w * fraction
            for (node in row) {
                val nodeFraction = if (rowSize > 0f) node.sizeBytes.toFloat() / rowSize else 0f
                val dy = h * nodeFraction
                result.add(TreemapRect(node, x, y + offset, x + dx, y + offset + dy))
                offset += dy
            }
        } else {
            val dy = h * fraction
            for (node in row) {
                val nodeFraction = if (rowSize > 0f) node.sizeBytes.toFloat() / rowSize else 0f
                val dx = w * nodeFraction
                result.add(TreemapRect(node, x + offset, y, x + offset + dx, y + dy))
                offset += dx
            }
        }
    }
}
