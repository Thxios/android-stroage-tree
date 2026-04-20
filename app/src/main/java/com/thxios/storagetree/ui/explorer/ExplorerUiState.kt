package com.thxios.storagetree.ui.explorer

import com.thxios.storagetree.data.storage.StorageRoot
import com.thxios.storagetree.domain.model.FileCategory
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.domain.model.ScanState
import com.thxios.storagetree.domain.model.ViewMode

data class ExplorerUiState(
    val currentPath: String = "",
    val displayedChildren: List<FileNode> = emptyList(),
    val scanState: ScanState = ScanState.Idle,
    val isScanning: Boolean = false,
    val scanningCurrentPath: String = "",
    val error: String? = null,
    val pendingDeleteNode: FileNode? = null,
    val categorySummary: Map<FileCategory, Long> = emptyMap(),
    val viewMode: ViewMode = ViewMode.LIST,
    val availableRoots: List<StorageRoot> = emptyList(),
    val selectedRoot: StorageRoot? = null,
    val selectedCategory: FileCategory? = null,
    val hasUsageStatsPermission: Boolean = false,
    val usageStatsPermissionChecked: Boolean = false,
    val canGoBack: Boolean = false,
    val showFolderPicker: Boolean = false,
    val pickerCurrentPath: String = "",
    val pickerEntries: List<String> = emptyList(),
)
