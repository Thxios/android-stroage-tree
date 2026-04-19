package com.thxios.storagetree.ui.explorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.domain.model.ScanState
import com.thxios.storagetree.domain.usecase.ScanDirectoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val scanUseCase: ScanDirectoryUseCase
) : ViewModel() {

    private val backStack = ArrayDeque<List<FileNode>>()

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    fun startScan(rootPath: String) {
        viewModelScope.launch {
            scanUseCase(rootPath).collect { state ->
                when (state) {
                    is ScanState.Scanning -> {
                        _uiState.update {
                            it.copy(
                                isScanning = true,
                                scanningCurrentPath = state.currentPath,
                                scanState = state
                            )
                        }
                    }
                    is ScanState.Done -> {
                        backStack.clear()
                        val sorted = state.rootNode.children.sortedByDescending { it.sizeBytes }
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                scanState = state,
                                displayedChildren = sorted,
                                currentPath = rootPath
                            )
                        }
                    }
                    is ScanState.Error -> {
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                scanState = state,
                                error = state.message
                            )
                        }
                    }
                    ScanState.Idle -> Unit
                }
            }
        }
    }

    fun navigateTo(node: FileNode) {
        backStack.addLast(_uiState.value.displayedChildren)
        val sorted = node.children.sortedByDescending { it.sizeBytes }
        _uiState.update {
            it.copy(
                displayedChildren = sorted,
                currentPath = node.path
            )
        }
    }

    fun navigateUp() {
        if (backStack.isEmpty()) return
        val parent = backStack.removeLast()
        _uiState.update {
            it.copy(displayedChildren = parent)
        }
    }
}
