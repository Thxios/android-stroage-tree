package com.thxios.storagetree.ui.explorer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thxios.storagetree.data.scanner.InstalledAppScanner
import com.thxios.storagetree.data.storage.StorageRoot
import com.thxios.storagetree.data.storage.StorageVolumeHelper
import com.thxios.storagetree.domain.model.FileCategory
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.domain.model.ScanState
import com.thxios.storagetree.domain.model.ViewMode
import com.thxios.storagetree.domain.usecase.CategorizeFilesUseCase
import com.thxios.storagetree.domain.usecase.DeleteNodeUseCase
import com.thxios.storagetree.domain.usecase.ScanDirectoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
    private val categorizeUseCase: CategorizeFilesUseCase,
    private val storageVolumeHelper: StorageVolumeHelper,
    private val installedAppScanner: InstalledAppScanner,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val backStack = ArrayDeque<Pair<String, List<FileNode>>>()
    private var scanRoot: FileNode? = null
    private var appsNode: FileNode? = null
    private var hasUsageStatsPermission = false

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    fun startScan(rootPath: String) {
        viewModelScope.launch {
            scanUseCase(rootPath).collect { state ->
                when (state) {
                    is ScanState.Scanning -> {
                        _uiState.update { current ->
                            current.copy(
                                isScanning = true,
                                scanningCurrentPath = state.currentPath,
                                scanState = state,
                                currentPath = rootPath,
                                displayedChildren = if (state.rootNode != null)
                                    state.rootNode.children.sortedByDescending { it.sizeBytes }
                                else
                                    current.displayedChildren
                            )
                        }
                    }
                    is ScanState.Done -> {
                        backStack.clear()
                        scanRoot = state.rootNode
                        val sorted = state.rootNode.children.sortedByDescending { it.sizeBytes }
                        val summary = categorizeUseCase(state.rootNode)
                        // Merge existing appsNode if already loaded
                        val withApps = if (appsNode != null) {
                            (listOf(appsNode!!) + sorted).sortedByDescending { it.sizeBytes }
                        } else sorted
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                scanState = state,
                                displayedChildren = withApps,
                                currentPath = rootPath,
                                categorySummary = summary,
                                selectedCategory = null
                            )
                        }
                        loadInstalledApps()  // load/refresh apps (runs in coroutine)
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

    fun loadStorageRoots() {
        val roots = storageVolumeHelper.getAvailableRoots(context)
        val selected = roots.firstOrNull()
        _uiState.update { it.copy(availableRoots = roots, selectedRoot = selected) }
    }

    fun selectRoot(root: StorageRoot) {
        _uiState.update { it.copy(selectedRoot = root) }
        startScan(root.path)
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            hasUsageStatsPermission = installedAppScanner.hasUsageStatsPermission(context)
            val node = installedAppScanner.buildVirtualAppsNode(context)
            appsNode = node
            // Merge into current displayed children at root level
            _uiState.update { current ->
                if (current.currentPath == (current.selectedRoot?.path ?: "") || backStack.isEmpty()) {
                    val merged = (listOf(node) + (scanRoot?.children ?: current.displayedChildren))
                        .sortedByDescending { it.sizeBytes }
                    current.copy(
                        displayedChildren = merged,
                        hasUsageStatsPermission = hasUsageStatsPermission
                    )
                } else {
                    current.copy(hasUsageStatsPermission = hasUsageStatsPermission)
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
                currentPath = node.path,
                selectedCategory = null
            )
        }
        // Only update category summary for real filesystem nodes
        if (!node.path.startsWith(InstalledAppScanner.VIRTUAL_APPS_PATH)) {
            updateCategorySummary(node)
        } else {
            _uiState.update { it.copy(categorySummary = emptyMap()) }
        }
    }

    fun navigateUp() {
        if (backStack.isEmpty()) return
        val (path, children) = backStack.removeLast()
        _uiState.update {
            it.copy(displayedChildren = children, currentPath = path, selectedCategory = null)
        }
        if (!path.startsWith(InstalledAppScanner.VIRTUAL_APPS_PATH)) {
            findNode(scanRoot, path)?.let { updateCategorySummary(it) }
        } else {
            _uiState.update { it.copy(categorySummary = emptyMap()) }
        }
    }

    fun navigateToAncestor(targetPath: String) {
        // Find index in backstack where path == targetPath
        val idx = backStack.indexOfFirst { (path, _) -> path == targetPath }
        if (idx >= 0) {
            // Remove all entries after idx (inclusive of idx)
            val (path, children) = backStack[idx]
            while (backStack.size > idx) backStack.removeLast()
            _uiState.update { it.copy(displayedChildren = children, currentPath = path, selectedCategory = null) }
            if (!path.startsWith(InstalledAppScanner.VIRTUAL_APPS_PATH)) {
                findNode(scanRoot, path)?.let { updateCategorySummary(it) }
            } else {
                _uiState.update { it.copy(categorySummary = emptyMap()) }
            }
        } else {
            // targetPath not in backstack — might be the very root (before any navigation)
            // Just pop everything and find the closest match
            while (backStack.isNotEmpty()) {
                val (path, children) = backStack.last()
                if (path == targetPath) {
                    backStack.removeLast()
                    _uiState.update { it.copy(displayedChildren = children, currentPath = path, selectedCategory = null) }
                    if (!path.startsWith(InstalledAppScanner.VIRTUAL_APPS_PATH)) {
                        findNode(scanRoot, path)?.let { updateCategorySummary(it) }
                    } else {
                        _uiState.update { it.copy(categorySummary = emptyMap()) }
                    }
                    return
                }
                backStack.removeLast()
            }
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
        if (node != null && node.path.startsWith(InstalledAppScanner.VIRTUAL_APPS_PATH)) return
        _uiState.update { it.copy(pendingDeleteNode = node) }
    }

    fun deleteNode() {
        val node = _uiState.value.pendingDeleteNode ?: return
        if (node.path.startsWith(InstalledAppScanner.VIRTUAL_APPS_PATH)) {
            _uiState.update { it.copy(pendingDeleteNode = null, error = "설치된 앱은 여기서 삭제할 수 없습니다") }
            return
        }
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

    fun setFilter(category: FileCategory?) {
        val base = if (backStack.isEmpty()) {
            val root = scanRoot ?: return
            val baseChildren = root.children.sortedByDescending { it.sizeBytes }
            // Include apps node at root level
            if (appsNode != null) {
                (listOf(appsNode!!) + baseChildren).sortedByDescending { it.sizeBytes }
            } else baseChildren
        } else {
            // Find current node in tree
            val currentNode = findNode(scanRoot, _uiState.value.currentPath)
            currentNode?.children?.sortedByDescending { it.sizeBytes } ?: _uiState.value.displayedChildren
        }
        val filtered = if (category == null) base
        else base.filter { containsCategory(it, category) }
        _uiState.update { it.copy(selectedCategory = category, displayedChildren = filtered) }
    }

    private fun updateCategorySummary(node: FileNode) {
        val summary = categorizeUseCase(node)
        _uiState.update { it.copy(categorySummary = summary, selectedCategory = null) }
    }

    private fun findNode(root: FileNode?, path: String): FileNode? {
        if (root == null) return null
        if (root.path == path) return root
        return root.children.firstNotNullOfOrNull { findNode(it, path) }
    }

    private fun containsCategory(node: FileNode, category: FileCategory): Boolean {
        if (!node.isDirectory) return FileCategory.of(node.name) == category
        return node.children.any { containsCategory(it, category) }
    }
}
