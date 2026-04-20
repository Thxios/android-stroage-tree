package com.thxios.storagetree.data.scanner

import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.domain.model.ScanState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class FileScanner @Inject constructor() {
    fun scan(path: String): Flow<ScanState> = flow {
        val rootFile = File(path)
        emit(ScanState.Scanning(currentPath = path, rootNode = null))

        if (!rootFile.isDirectory) {
            val node = FileNode(
                name = rootFile.name,
                path = rootFile.absolutePath,
                sizeBytes = rootFile.length(),
                isDirectory = false
            )
            emit(ScanState.Done(node))
            return@flow
        }

        val childFiles = rootFile.listFiles() ?: emptyArray()
        val scannedChildren = mutableListOf<FileNode>()

        for (childFile in childFiles) {
            val childNode = scanDirectory(childFile) { currentPath ->
                emit(ScanState.Scanning(currentPath = currentPath, rootNode = null))
            }
            scannedChildren.add(childNode)
            // After each top-level child completes, emit partial result
            val partialChildren = scannedChildren.sortedByDescending { it.sizeBytes }
            emit(ScanState.Scanning(
                currentPath = childFile.absolutePath,
                rootNode = FileNode(
                    name = rootFile.name,
                    path = rootFile.absolutePath,
                    sizeBytes = scannedChildren.sumOf { it.sizeBytes },
                    isDirectory = true,
                    children = partialChildren
                )
            ))
        }

        val finalChildren = scannedChildren.sortedByDescending { it.sizeBytes }
        emit(ScanState.Done(FileNode(
            name = rootFile.name,
            path = rootFile.absolutePath,
            sizeBytes = scannedChildren.sumOf { it.sizeBytes },
            isDirectory = true,
            children = finalChildren
        )))
    }.flowOn(Dispatchers.IO)

    private suspend fun scanDirectory(
        file: File,
        onProgress: suspend (String) -> Unit
    ): FileNode {
        if (!file.isDirectory) {
            return FileNode(
                name = file.name,
                path = file.absolutePath,
                sizeBytes = file.length(),
                isDirectory = false
            )
        }
        onProgress(file.absolutePath)
        val childFiles = file.listFiles() ?: emptyArray()
        val children = childFiles
            .map { scanDirectory(it, onProgress) }
            .sortedByDescending { it.sizeBytes }
        val totalSize = children.sumOf { it.sizeBytes }
        return FileNode(
            name = file.name,
            path = file.absolutePath,
            sizeBytes = totalSize,
            isDirectory = true,
            children = children
        )
    }
}
