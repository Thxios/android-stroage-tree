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
        emit(ScanState.Scanning(currentPath = path))
        val rootNode = scanDirectory(rootFile) { currentPath ->
            emit(ScanState.Scanning(currentPath = currentPath))
        }
        emit(ScanState.Done(rootNode))
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
