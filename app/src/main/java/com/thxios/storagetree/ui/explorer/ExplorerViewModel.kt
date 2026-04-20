package com.thxios.storagetree.ui.explorer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thxios.storagetree.data.preferences.AppSettings
import com.thxios.storagetree.data.preferences.PreferencesRepository
import com.thxios.storagetree.data.scanner.InstalledAppScanner
import com.thxios.storagetree.data.storage.StorageRoot
import com.thxios.storagetree.data.storage.StorageVolumeHelper
import com.thxios.storagetree.domain.model.FileCategory
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.domain.model.ScanState
import com.thxios.storagetree.domain.model.SortOrder
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
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val backStack = ArrayDeque<Pair<String, List<FileNode>>>()
    private var scanRoot: FileNode? = null
    private var appsNode: FileNode? = null
    private var hasUsageStatsPermission = false
    private var currentSettings: AppSettings = AppSettings()

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.appSettings.collect { settings ->
                onSettingsChanged(settings)
            }
        }
    }

    private fun onSettingsChanged(settings: AppSettings) {
        currentSettings = settings
        refreshDisplayedChildren()
    }

    private fun refreshDisplayedChildren() {
        val currentPath = _uiState.value.currentPath
        if (currentPath.isEmpty()) return
        val isAtRoot = backStack.isEmpty()
        val baseNode = if (isAtRoot) scanRoot else findNode(scanRoot, currentPath)
        val baseChildren = baseNode?.children ?: return
        val sorted = sortChildren(baseChildren)
        val withApps = if (isAtRoot && currentSettings.showInstalledApps && appsNode != null) {
            sortChildren(listOf(appsNode!!) + sorted.filter { it.path != appsNode!!.path })
        } else if (isAtRoot) {
            sorted
        } else {
            sorted
        }
        val filtered = if (_uiState.value.selectedCategory != null) {
            withApps.filter { containsCategory(it, _uiState.value.selectedCategory!!) }
        } else withApps
        _uiState.update { it.copy(displayedChildren = filtered) }
    }

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
                                    sortChildren(state.rootNode.children)
                                else
                                    current.displayedChildren
                            )
                        }
                    }
                    is ScanState.Done -> {
                        backStack.clear()
                        scanRoot = state.rootNode
                        val sorted = sortChildren(state.rootNode.children)
                        val summary = categorizeUseCase(state.rootNode)
                        // Merge existing appsNode if already loaded and setting is enabled
                        val withApps = if (appsNode != null && currentSettings.showInstalledApps) {
                            sortChildren(listOf(appsNode!!) + sorted)
                        } else sorted
                        _uiState.update {
                            it.copy(
                                isScanning = false,
                                scanState = state,
                                displayedChildren = withApps,
                                currentPath = rootPath,
                                categorySummary = summary,
                                selectedCategory = null,
                                canGoBack = false
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
            // Merge into current displayed children at root level, respecting the setting
            _uiState.update { current ->
                if ((current.currentPath == (current.selectedRoot?.path ?: "") || backStack.isEmpty()) && currentSettings.showInstalledApps) {
                    val merged = sortChildren(listOf(node) + (scanRoot?.children?.filter { it.path != node.path } ?: current.displayedChildren))
                    current.copy(
                        displayedChildren = merged,
                        hasUsageStatsPermission = hasUsageStatsPermission,
                        usageStatsPermissionChecked = true
                    )
                } else {
                    current.copy(
                        hasUsageStatsPermission = hasUsageStatsPermission,
                        usageStatsPermissionChecked = true
                    )
                }
            }
        }
    }

    fun navigateTo(node: FileNode) {
        backStack.addLast(Pair(_uiState.value.currentPath, _uiState.value.displayedChildren))
        val sorted = sortChildren(node.children)
        _uiState.update {
            it.copy(
                displayedChildren = sorted,
                currentPath = node.path,
                selectedCategory = null,
                canGoBack = true
            )
        }
        // Only update category summary for real filesystem nodes
        if (!node.path.startsWith(InstalledAppScanner.VIRTUAL_APPS_PATH)) {
            updateCategorySummary(node)
        } else {
            _uiState.update { it.copy(categorySummary = emptyMap()) }
        }
    }

    fun goBack() {
        if (backStack.isEmpty()) return
        val (path, children) = backStack.removeLast()
        _uiState.update {
            it.copy(
                displayedChildren = children,
                currentPath = path,
                selectedCategory = null,
                canGoBack = backStack.isNotEmpty()
            )
        }
        if (!path.startsWith(InstalledAppScanner.VIRTUAL_APPS_PATH)) {
            findNode(scanRoot, path)?.let { updateCategorySummary(it) }
        } else {
            _uiState.update { it.copy(categorySummary = emptyMap()) }
        }
    }

    fun goToParent() {
        val currentPath = _uiState.value.currentPath
        if (currentPath.isEmpty()) return
        // Don't navigate above the selected root
        val rootPath = _uiState.value.selectedRoot?.path ?: ""
        if (currentPath == rootPath) return
        val parentPath = currentPath.substringBeforeLast("/", "")
        if (parentPath.isEmpty()) return
        // Push current state to backStack (so back can return here)
        backStack.addLast(Pair(currentPath, _uiState.value.displayedChildren))
        // Find parent node in tree
        val parentNode = findNode(scanRoot, parentPath)
        if (parentNode != null) {
            val sorted = sortChildren(parentNode.children)
            _uiState.update {
                it.copy(
                    displayedChildren = sorted,
                    currentPath = parentPath,
                    selectedCategory = null,
                    canGoBack = true
                )
            }
            if (!parentPath.startsWith(InstalledAppScanner.VIRTUAL_APPS_PATH)) {
                updateCategorySummary(parentNode)
            } else {
                _uiState.update { it.copy(categorySummary = emptyMap()) }
            }
        } else {
            // parentNode not in tree (e.g., at scan root level) — just go back
            goBack()
        }
    }

    fun navigateToAncestor(targetPath: String) {
        val currentPath = _uiState.value.currentPath
        if (currentPath == targetPath) return
        // Push current state to backStack
        backStack.addLast(Pair(currentPath, _uiState.value.displayedChildren))
        // Find target node and navigate there
        val targetNode = findNode(scanRoot, targetPath)
        if (targetNode != null) {
            val sorted = sortChildren(targetNode.children)
            _uiState.update {
                it.copy(
                    displayedChildren = sorted,
                    currentPath = targetPath,
                    selectedCategory = null,
                    canGoBack = true
                )
            }
            if (!targetPath.startsWith(InstalledAppScanner.VIRTUAL_APPS_PATH)) {
                updateCategorySummary(targetNode)
            } else {
                _uiState.update { it.copy(categorySummary = emptyMap()) }
            }
        } else {
            // target not in tree — just pop like before (edge case)
            backStack.removeLast()
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
            val baseChildren = sortChildren(root.children)
            // Include apps node at root level only if setting is enabled
            if (appsNode != null && currentSettings.showInstalledApps) {
                sortChildren(listOf(appsNode!!) + baseChildren)
            } else baseChildren
        } else {
            // Find current node in tree
            val currentNode = findNode(scanRoot, _uiState.value.currentPath)
            currentNode?.children?.let { sortChildren(it) } ?: _uiState.value.displayedChildren
        }
        val filtered = if (category == null) base
        else base.filter { containsCategory(it, category) }
        _uiState.update { it.copy(selectedCategory = category, displayedChildren = filtered) }
    }

    private fun sortChildren(children: List<FileNode>): List<FileNode> {
        return when (currentSettings.sortOrder) {
            SortOrder.SIZE_DESC -> children.sortedByDescending { it.sizeBytes }
            SortOrder.NAME_ASC -> children.sortedBy { it.name.lowercase() }
            SortOrder.DATE_DESC -> children.sortedByDescending { java.io.File(it.path).lastModified() }
            SortOrder.NATURAL_NAME_ASC -> children.sortedWith(naturalOrderComparator)
        }
    }

    private val naturalOrderComparator = Comparator<FileNode> { a, b ->
        compareNatural(a.name, b.name)
    }

    private fun compareNatural(s1: String, s2: String): Int {
        var i1 = 0; var i2 = 0
        while (i1 < s1.length && i2 < s2.length) {
            val c1 = s1[i1]; val c2 = s2[i2]
            if (c1.isDigit() && c2.isDigit()) {
                val num1 = s1.substring(i1).takeWhile { it.isDigit() }
                val num2 = s2.substring(i2).takeWhile { it.isDigit() }
                val cmp = num1.toBigInteger().compareTo(num2.toBigInteger())
                if (cmp != 0) return cmp
                i1 += num1.length; i2 += num2.length
            } else {
                val cmp = c1.lowercaseChar().compareTo(c2.lowercaseChar())
                if (cmp != 0) return cmp
                i1++; i2++
            }
        }
        return s1.length - s2.length
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
