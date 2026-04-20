package com.thxios.storagetree.ui.explorer

import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.thxios.storagetree.data.scanner.FileSizeFormatter
import com.thxios.storagetree.data.storage.StorageRoot
import com.thxios.storagetree.domain.model.FileCategory
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.domain.model.SortOrder
import com.thxios.storagetree.domain.model.ViewMode
import com.thxios.storagetree.ui.components.ErrorBanner
import com.thxios.storagetree.ui.navigation.AppDestination
import com.thxios.storagetree.ui.components.ScanProgressBanner
import com.thxios.storagetree.ui.explorer.listview.StorageListView
import com.thxios.storagetree.ui.explorer.treemap.TreemapView
import com.thxios.storagetree.ui.theme.StorageTreeTheme

private val formatter = FileSizeFormatter()

@Composable
fun ExplorerScreen(
    viewModel: ExplorerViewModel = hiltViewModel(),
    navController: NavController? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.loadStorageRoots()
        viewModel.openFolderPickerIfNeeded()
    }

    // Re-check usage stats permission when returning from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.reloadInstalledAppsIfPermissionChanged()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val rootPath = uiState.selectedRoot?.path ?: Environment.getExternalStorageDirectory().absolutePath
    val isAtRoot = uiState.currentPath.isEmpty() || uiState.currentPath == rootPath

    BackHandler(enabled = uiState.canGoBack) {
        viewModel.goBack()
    }

    uiState.pendingDeleteNode?.let { node ->
        AlertDialog(
            onDismissRequest = { viewModel.setPendingDelete(null) },
            title = { Text("Delete") },
            text = { Text("Delete \"${node.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteNode() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setPendingDelete(null) }) { Text("Cancel") }
            }
        )
    }

    ExplorerContent(
        uiState = uiState,
        isAtRoot = isAtRoot,
        onNodeClick = { node ->
            if (node.isDirectory) viewModel.navigateTo(node)
        },
        onNodeLongClick = { node -> viewModel.setPendingDelete(node) },
        onAppBack = { viewModel.goBack() },
        onNavigateUp = { viewModel.goToParent() },
        onToggleViewMode = { viewModel.toggleViewMode() },
        onRootSelected = { viewModel.selectRoot(it) },
        onBreadcrumbClick = { viewModel.navigateToAncestor(it) },
        onCategoryFilter = { viewModel.setFilter(it) },
        onOpenUsageSettings = {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent)
        },
        onOpenSettings = {
            navController?.navigate(AppDestination.Settings.route)
        },
        onReload = { viewModel.reloadScan() },
        onOpenFolderPicker = { viewModel.openFolderPicker() },
        onCloseFolderPicker = { viewModel.closeFolderPicker() },
        onNavigatePickerInto = { viewModel.navigatePickerInto(it) },
        onStartScanFromPicker = { viewModel.startScanFromPicker(it) },
        onSortOrderChanged = { viewModel.setSortOrder(it) }
    )
}

internal fun formatDisplayPath(path: String, maxLen: Int = 40): String {
    val stripped = path.removePrefix("/storage/emulated")
    if (stripped.length <= maxLen) return stripped
    val truncated = stripped.takeLast(maxLen - 3)
    val slashIdx = truncated.indexOf('/')
    return "..." + if (slashIdx >= 0) truncated.substring(slashIdx) else truncated
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExplorerContent(
    uiState: ExplorerUiState,
    isAtRoot: Boolean,
    onNodeClick: (FileNode) -> Unit,
    onNodeLongClick: (FileNode) -> Unit,
    onAppBack: () -> Unit,
    onNavigateUp: () -> Unit,
    onToggleViewMode: () -> Unit,
    onRootSelected: (StorageRoot) -> Unit = {},
    onBreadcrumbClick: (String) -> Unit = {},
    onCategoryFilter: (FileCategory?) -> Unit = {},
    onOpenUsageSettings: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onReload: () -> Unit = {},
    onOpenFolderPicker: () -> Unit = {},
    onCloseFolderPicker: () -> Unit = {},
    onNavigatePickerInto: (String) -> Unit = {},
    onStartScanFromPicker: (String) -> Unit = {},
    onSortOrderChanged: (SortOrder) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.currentPath.isEmpty()) {
                        Text(text = "Storage", maxLines = 1)
                    } else {
                        BreadcrumbRow(
                            path = uiState.currentPath,
                            selectedRootPath = uiState.selectedRoot?.path ?: "",
                            onSegmentClick = onBreadcrumbClick
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onAppBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!isAtRoot) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = "Go to parent folder"
                            )
                        }
                    }
                    if (!uiState.isScanning) {
                        IconButton(onClick = onOpenFolderPicker) {
                            Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = "폴더 선택해서 스캔"
                            )
                        }
                    }
                    SortOrderIconMenu(
                        sortOrder = uiState.sortOrder,
                        onSortOrderChanged = onSortOrderChanged
                    )
                    IconButton(onClick = onReload, enabled = !uiState.isScanning) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reload scan"
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "설정"
                        )
                    }
                    IconButton(onClick = onToggleViewMode) {
                        Icon(
                            imageVector = if (uiState.viewMode == ViewMode.LIST)
                                Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "Toggle view"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isScanning) {
                ScanProgressBanner(currentPath = uiState.scanningCurrentPath)
            }
            if (uiState.usageStatsPermissionChecked && !uiState.hasUsageStatsPermission) {
                UsageStatsPermissionBanner(
                    onOpenSettings = onOpenUsageSettings,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (uiState.availableRoots.size > 1) {
                StorageRootPicker(
                    roots = uiState.availableRoots,
                    selected = uiState.selectedRoot,
                    onRootSelected = onRootSelected,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            if (uiState.error != null) {
                ErrorBanner(message = uiState.error)
            }
            if (uiState.categorySummary.isNotEmpty()) {
                CategoryChipRow(
                    summary = uiState.categorySummary,
                    selected = uiState.selectedCategory,
                    onCategoryClick = onCategoryFilter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            when (uiState.viewMode) {
                ViewMode.LIST -> StorageListView(
                    nodes = uiState.displayedChildren,
                    onNodeClick = onNodeClick,
                    onNodeLongClick = onNodeLongClick,
                    modifier = Modifier.fillMaxSize()
                )
                ViewMode.TREEMAP -> TreemapView(
                    nodes = uiState.displayedChildren,
                    onNodeClick = onNodeClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (uiState.showFolderPicker) {
        FolderPickerSheet(
            currentPath = uiState.pickerCurrentPath,
            entries = uiState.pickerEntries,
            onDismiss = onCloseFolderPicker,
            onNavigateInto = onNavigatePickerInto,
            onStartScan = onStartScanFromPicker
        )
    }
}

@Composable
private fun UsageStatsPermissionBanner(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "앱 데이터/캐시 크기를 보려면 사용 정보 접근 권한이 필요합니다",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onOpenSettings) { Text("허용") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UsageStatsPermissionBannerPreview() {
    StorageTreeTheme {
        UsageStatsPermissionBanner(onOpenSettings = {})
    }
}

@Composable
private fun BreadcrumbRow(
    path: String,
    selectedRootPath: String,
    onSegmentClick: (fullPath: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Strip /storage/emulated prefix for display only
    val displayPath = path.removePrefix("/storage/emulated")
    val segments = displayPath.split("/").filter { it.isNotEmpty() }

    // Rebuild actual paths for click targets using original path
    val rootPrefix = if (path.startsWith("/storage/emulated")) "/storage/emulated" else ""

    LazyRow(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        items(segments.indices.toList()) { i ->
            val segmentPath = rootPrefix + "/" + segments.subList(0, i + 1).joinToString("/")
            val isLast = i == segments.lastIndex
            Text(
                text = segments[i],
                style = MaterialTheme.typography.titleSmall,
                color = if (isLast) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.primary,
                modifier = if (!isLast) Modifier.clickable { onSegmentClick(segmentPath) }
                           else Modifier,
                maxLines = 1
            )
            if (!isLast) {
                Text(
                    text = " / ",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BreadcrumbRowPreview() {
    StorageTreeTheme {
        BreadcrumbRow(
            path = "/storage/emulated/0/DCIM/Camera",
            selectedRootPath = "/storage/emulated/0",
            onSegmentClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorageRootPicker(
    roots: List<StorageRoot>,
    selected: StorageRoot?,
    onRootSelected: (StorageRoot) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.label ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("저장소") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            roots.forEach { root ->
                DropdownMenuItem(
                    text = { Text(root.label) },
                    onClick = { onRootSelected(root); expanded = false }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StorageRootPickerPreview() {
    StorageTreeTheme {
        StorageRootPicker(
            roots = listOf(
                StorageRoot("/storage/emulated/0", "내부 저장소", true),
                StorageRoot("/storage/sdcard1", "SD 카드", false)
            ),
            selected = StorageRoot("/storage/emulated/0", "내부 저장소", true),
            onRootSelected = {}
        )
    }
}

@Composable
private fun SortOrderIconMenu(
    sortOrder: SortOrder,
    onSortOrderChanged: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = mapOf(
        SortOrder.SIZE_DESC to "크기 (큰 순)",
        SortOrder.NAME_ASC to "이름 (가나다순)",
        SortOrder.DATE_DESC to "날짜 (최신순)",
        SortOrder.NATURAL_NAME_ASC to "이름 (자연 순서)"
    )
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.Sort,
                contentDescription = "정렬 순서"
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(labels[order] ?: order.name) },
                    leadingIcon = {
                        if (order == sortOrder) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null
                            )
                        }
                    },
                    onClick = { onSortOrderChanged(order); expanded = false }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SortOrderIconMenuPreview() {
    StorageTreeTheme {
        SortOrderIconMenu(
            sortOrder = SortOrder.SIZE_DESC,
            onSortOrderChanged = {}
        )
    }
}

@Composable
private fun CategoryChipRow(
    summary: Map<FileCategory, Long>,
    selected: FileCategory?,
    onCategoryClick: (FileCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier) {
        items(summary.entries.sortedByDescending { it.value }.toList()) { (category, size) ->
            FilterChip(
                selected = selected == category,
                onClick = { onCategoryClick(if (selected == category) null else category) },
                label = { Text("${category.name} ${formatter.format(size)}") },
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExplorerScreenPreview() {
    StorageTreeTheme {
        ExplorerContent(
            uiState = ExplorerUiState(
                currentPath = "/storage/emulated/0",
                displayedChildren = listOf(
                    FileNode(name = "Downloads", path = "/storage/emulated/0/Downloads", sizeBytes = 500_000_000L, isDirectory = true),
                    FileNode(name = "DCIM", path = "/storage/emulated/0/DCIM", sizeBytes = 300_000_000L, isDirectory = true)
                ),
                categorySummary = mapOf(
                    FileCategory.IMAGE to 200_000_000L,
                    FileCategory.VIDEO to 100_000_000L
                )
            ),
            isAtRoot = true,
            onNodeClick = {},
            onNodeLongClick = {},
            onAppBack = {},
            onNavigateUp = {},
            onToggleViewMode = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExplorerScreenSubDirPreview() {
    StorageTreeTheme {
        ExplorerContent(
            uiState = ExplorerUiState(
                currentPath = "/storage/emulated/0/Downloads/very/long/path/to/show/truncation",
                displayedChildren = listOf(
                    FileNode(name = "file.zip", path = "/storage/emulated/0/Downloads/very/long/path/to/show/truncation/file.zip", sizeBytes = 100_000_000L, isDirectory = false)
                )
            ),
            isAtRoot = false,
            onNodeClick = {},
            onNodeLongClick = {},
            onAppBack = {},
            onNavigateUp = {},
            onToggleViewMode = {}
        )
    }
}
