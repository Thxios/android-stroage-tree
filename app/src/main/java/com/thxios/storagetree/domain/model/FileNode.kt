package com.thxios.storagetree.domain.model

data class FileNode(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val isDirectory: Boolean,
    val children: List<FileNode> = emptyList()
)
