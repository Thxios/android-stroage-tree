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
                name = rootFile.name, path = rootFile.absolutePath,
                sizeBytes = rootFile.length(), isDirectory = false
            )
            emit(ScanState.Done(node))
            return@flow
        }

        val childFiles = rootFile.listFiles() ?: emptyArray()
        val scannedTopLevel = mutableListOf<FileNode>()
        var lastEmitMs = System.currentTimeMillis()
        val THROTTLE_MS = 200L

        for (childFile in childFiles) {
            val childNode = scanDirectory(childFile) { partialChild ->
                // Called whenever any subdirectory completes inside this top-level child
                val now = System.currentTimeMillis()
                if (now - lastEmitMs >= THROTTLE_MS) {
                    lastEmitMs = now
                    val partialList = (scannedTopLevel + listOf(partialChild))
                        .sortedByDescending { it.sizeBytes }
                    emit(ScanState.Scanning(
                        currentPath = partialChild.path,
                        rootNode = FileNode(
                            name = rootFile.name,
                            path = rootFile.absolutePath,
                            sizeBytes = partialList.sumOf { it.sizeBytes },
                            isDirectory = true,
                            children = partialList
                        )
                    ))
                }
            }
            scannedTopLevel.add(childNode)
            // Always emit after each top-level child finishes
            val partialList = scannedTopLevel.sortedByDescending { it.sizeBytes }
            emit(ScanState.Scanning(
                currentPath = rootFile.absolutePath,
                rootNode = FileNode(
                    name = rootFile.name,
                    path = rootFile.absolutePath,
                    sizeBytes = scannedTopLevel.sumOf { it.sizeBytes },
                    isDirectory = true,
                    children = partialList
                )
            ))
            lastEmitMs = System.currentTimeMillis()
        }

        val finalList = scannedTopLevel.sortedByDescending { it.sizeBytes }
        emit(ScanState.Done(FileNode(
            name = rootFile.name,
            path = rootFile.absolutePath,
            sizeBytes = scannedTopLevel.sumOf { it.sizeBytes },
            isDirectory = true,
            children = finalList
        )))
    }.flowOn(Dispatchers.IO)

    // onPartialUpdate is called with the CURRENT node (partial) whenever any child directory inside it completes
    private suspend fun scanDirectory(
        file: File,
        onPartialUpdate: suspend (FileNode) -> Unit
    ): FileNode {
        if (!file.isDirectory) {
            return FileNode(
                name = file.name, path = file.absolutePath,
                sizeBytes = file.length(), isDirectory = false
            )
        }
        val childFiles = file.listFiles() ?: emptyArray()
        val children = mutableListOf<FileNode>()
        for (childFile in childFiles) {
            val childNode = scanDirectory(childFile) { partialDescendant ->
                // Propagate up: rebuild current node with partial children + pass up
                val currentPartial = FileNode(
                    name = file.name, path = file.absolutePath,
                    sizeBytes = children.sumOf { it.sizeBytes } + partialDescendant.sizeBytes,
                    isDirectory = true,
                    children = (children + listOf(partialDescendant)).sortedByDescending { it.sizeBytes }
                )
                onPartialUpdate(currentPartial)
            }
            children.add(childNode)
            if (childFile.isDirectory) {
                // Notify after each child directory completes
                val currentPartial = FileNode(
                    name = file.name, path = file.absolutePath,
                    sizeBytes = children.sumOf { it.sizeBytes },
                    isDirectory = true,
                    children = children.sortedByDescending { it.sizeBytes }
                )
                onPartialUpdate(currentPartial)
            }
        }
        val sorted = children.sortedByDescending { it.sizeBytes }
        return FileNode(
            name = file.name, path = file.absolutePath,
            sizeBytes = children.sumOf { it.sizeBytes },
            isDirectory = true, children = sorted
        )
    }
}
