package com.thxios.storagetree.ui.explorer.treemap

import com.thxios.storagetree.domain.model.FileNode

data class TreemapRect(
    val node: FileNode,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)
