package com.thxios.storagetree.ui.explorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.domain.model.ScanState
import com.thxios.storagetree.domain.model.ViewMode
import com.thxios.storagetree.domain.usecase.CategorizeFilesUseCase
import com.thxios.storagetree.domain.usecase.DeleteNodeUseCase
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
    private val scanUseCase: ScanDirectoryUseCase,
    private val deleteUseCase: DeleteNodeUseCase,
    private val categorizeUseCase: CategorizeFilesUseCase
) : ViewModel() {

    private val backStack = ArrayDeque<Pair<String, List<FileNode>>>()

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    fun startScan(rootPath: String) {
        viewModelScope.launch {
            scanUseCase(rootPath).collect { state ->
                when (state) {
                    is ScanState.Scanning -> {
                        _uiState.update { current ->
                            val updatedChildren = if (state.rootNode != null) {
                                state.rootNode.children.sortedByDescending { it.sizeBytes }
                            } else {
                                current.displayedChildren
                            }
                            val updatedPath = if (state.rootNode != null) rootPath else current.currentPath
                            current.copy(
                                isScanning = true,
                                scanningCurrentPath = state.currentPath,
                                scanState = state,
                                displayedChildren = updatedChildren,
                                currentPath = updatedPath
                            )
                        }
                    }
                    is ScanState.Done -> {
                        backStack.clear()
                        val sorted = state.rootNode.children.sortedByDescending { it.sizeBytes }
                        val summary = categorizeUseCase(state.rootNode)
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                scanState = state,
                                displayedChildren = sorted,
                                currentPath = rootPath,
                                categorySummary = summary
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
        backStack.addLast(Pair(_uiState.value.currentPath, _uiState.value.displayedChildren))
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
        val (path, children) = backStack.removeLast()
        _uiState.update {
            it.copy(displayedChildren = children, currentPath = path)
        }
    }

    fun toggleViewMode() {
        _uiState.update { current ->
            current.copy(
                viewMode = if (current.viewMode == ViewMode.LIST) ViewMode.TREEMAP else ViewMode.LIST
            )
        }
    }

    fun setPendingDelete(node: FileNode?) {
        _uiState.update { it.copy(pendingDeleteNode = node) }
    }

    fun deleteNode() {
        val node = _uiState.value.pendingDeleteNode ?: return
        viewModelScope.launch {
            val result = deleteUseCase(node)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        pendingDeleteNode = null,
                        displayedChildren = it.displayedChildren.filter { child -> child.path != node.path }
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        pendingDeleteNode = null,
                        error = result.exceptionOrNull()?.message ?: "Delete failed"
                    )
                }
            }
        }
    }
}
