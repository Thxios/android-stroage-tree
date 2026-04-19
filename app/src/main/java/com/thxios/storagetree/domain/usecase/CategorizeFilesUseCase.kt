package com.thxios.storagetree.domain.usecase

import com.thxios.storagetree.domain.model.FileCategory
import com.thxios.storagetree.domain.model.FileNode
import javax.inject.Inject

class CategorizeFilesUseCase @Inject constructor() {
    operator fun invoke(root: FileNode): Map<FileCategory, Long> {
        val result = mutableMapOf<FileCategory, Long>()
        collectLeaves(root, result)
        return result
    }

    private fun collectLeaves(node: FileNode, acc: MutableMap<FileCategory, Long>) {
        if (!node.isDirectory) {
            val category = FileCategory.of(node.name)
            acc[category] = (acc[category] ?: 0L) + node.sizeBytes
        } else {
            node.children.forEach { collectLeaves(it, acc) }
        }
    }
}
