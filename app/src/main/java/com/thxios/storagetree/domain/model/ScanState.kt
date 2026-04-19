package com.thxios.storagetree.domain.model

sealed class ScanState {
    object Idle : ScanState()
    data class Scanning(val currentPath: String, val rootNode: FileNode? = null) : ScanState()
    data class Done(val rootNode: FileNode) : ScanState()
    data class Error(val message: String) : ScanState()
}
