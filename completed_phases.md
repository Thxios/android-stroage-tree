# StorageTree — 완료된 구현 이력

## Phase 0 · Project Scaffolding (2026-04-19)
Hilt, Navigation Compose, KSP, Coroutines, MockK, Turbine 의존성 추가. `@HiltAndroidApp` Application, `@AndroidEntryPoint` MainActivity.

## Phase 1A · Domain Models & Scanner (2026-04-19)
`FileNode`, `ScanState(Idle/Scanning/Done/Error)` 모델. `FileSizeFormatter`(1024-base). `FileScanner`: `java.io.File` 재귀, `Flow<ScanState>`, `flowOn(IO)`, null-safe.

## Phase 1B · Repository & Hilt Wiring (2026-04-20)
`StorageRepository` interface, `StorageRepositoryImpl`, `ScanDirectoryUseCase`, `StorageModule(@Binds @Singleton)`.

## Phase 1C · Permission Screen (2026-04-19)
`PermissionViewModel`: API 30+ `isExternalStorageManager`, 26-29 `READ_EXTERNAL_STORAGE`. `PermissionScreen`: 권한 허용 버튼.

## Phase 1D · List View (2026-04-20)
`ExplorerViewModel`(@HiltViewModel) + `ExplorerUiState`. `StorageListView`(LazyColumn) + `FileNodeRow`. `ScanProgressBanner`, `SizeProgressBar`, `ErrorBanner`. 드릴다운 내비게이션: `ArrayDeque<FileNode>` 백스택.

## Phase 1E · Treemap View (2026-04-20)
`SquarifyAlgorithm`: pure Kotlin Squarified 알고리즘. `TreemapView`: Canvas + pointerInput 클릭 감지.

## Phase 1F · Navigation & Integration (2026-04-20)
`AppDestination`(sealed class), `AppNavGraph`(NavHost). Permission→Explorer 전환.

## Phase 2A · File Deletion (2026-04-20)
`DeleteNodeUseCase`. `FileNodeRow` long-press → AlertDialog → `deleteRecursively()`. 가상 경로 삭제 차단.

## Phase 2B · File Categorization (2026-04-20)
`FileCategory` enum (IMAGE/VIDEO/AUDIO/DOCUMENT/APK/ARCHIVE/OTHER). `CategorizeFilesUseCase`: 재귀 집계. `CategoryChipRow` 필터 UI.

## Phase 2C · View Toggle (2026-04-20)
`ViewMode`(LIST/TREEMAP). SegmentedButton → `StorageListView`/`TreemapView` 전환.

## Phase 3A · UX Improvements (2026-04-20)
스캔 중 실시간 `displayedChildren` 업데이트. TopAppBar breadcrumb(클릭 가능). 스크롤바. `goBack()`/`goToParent()` 분리.

## Phase 3B · Scan Root Selection (2026-04-20)
`StorageVolumeHelper`: 내부/외부 저장소 루트 목록. 루트 선택 드롭다운 UI. 부분 스캔 결과 실시간 표시.

## Phase 3C · Deep Scan UX & File Filter (2026-04-20)
`/storage/emulated` 경로 접두사 제거 표시. throttle 기반 partial emit. 파일 타입 필터(CategoryChipRow).

## Phase 3D · Installed Apps Virtual Folder (2026-04-20)
`InstalledAppScanner`: PackageManager + StorageStatsManager. `virtual://installed-apps` 가상 FileNode 트리(APK/데이터/캐시). `PACKAGE_USAGE_STATS` 권한. 스캔 완료 후 root children에 병합.

## Phase 3E · Installed Apps Size Accuracy (2026-04-20)
`splitSourceDirs` 포함 APK 크기. OBB/외부data walkTopDown 직접 측정.

## Phase 4A · Bug Fixes (2026-04-20)
`PermissionViewModel` init{}에서 즉시 권한 체크(깜빡임 제거). `canGoBack` UiState 필드. `BackHandler`에서 `navController.popBackStack()` 제거. `usageStatsPermissionChecked` 필드(배너 초기 깜빡임 제거).

## Phase 4B · Settings Page (2026-04-20)
`SortOrder` enum(SIZE_DESC/NAME_ASC/DATE_DESC/NATURAL_NAME_ASC). `AppSettings` + `PreferencesRepository`(DataStore). `SettingsScreen`: showInstalledApps 토글, sortOrder 선택. natural sort 구현.

## Phase 5A · Splash Screen (2026-04-21)
`SplashViewModel`: init{}에서 권한 체크 → `NavTarget` emit. `AppNavGraph` startDestination=Splash. 권한 보유 시 깜빡임 없이 Explorer로 직행.

## Phase 5B · Scan Once + Reload (2026-04-21)
`scanStarted` 플래그. `startScanIfNeeded()` / `reloadScan()`. ON_RESUME 시 권한 변경 시에만 재로드. TopAppBar Refresh 버튼.

## Phase 5C · Start Folder Picker (2026-04-21)
`FolderPickerSheet`(ModalBottomSheet): breadcrumb + 서브폴더 목록 + "여기서 스캔 시작". `openFolderPicker`/`startScanFromPicker`.

## Phase 5D · Virtual Path Parent Navigation Fix (2026-04-21)
`goToParent()`에서 `VIRTUAL_APPS_PATH` 특별 처리. `findVirtualNode()` 헬퍼.

## Phase 5E · Sort Order In-Screen (2026-04-21)
`SortOrderDropdown` → TopAppBar `SortOrderIconMenu`(아이콘 버튼 + DropdownMenu)로 교체. SettingsScreen에서 정렬 UI 제거.

## Phase 5F · Multi-Volume StorageStatsManager (2026-04-21)
`getAllVolumeStats()`: 모든 StorageVolume UUID 순회 합산. OBB/외부data walkTopDown 제거. 자식 노드 APK/데이터/캐시로 단순화.

## Phase 6A · Sort Order Icon Menu (2026-04-21)
`SortOrderIconMenu` composable: `Icons.Filled.Sort` 아이콘 버튼 → DropdownMenu. 현재 선택 항목에 체크마크.

## Phase 6B · Initial Folder Picker on Startup (2026-04-21)
Explorer 첫 진입 시 `FolderPickerSheet` 자동 표시. 선택 시 해당 경로로 스캔, 취소 시 기본 경로로 스캔.

## Phase 7A · Fix appsNode Disappearing on goBack (2026-04-21)
`buildRootChildren()` 헬퍼 추가. `goBack()`/`goToParent()` 루트 복귀 시 backStack 복원 대신 `scanRoot + appsNode`에서 재계산.

## Phase 7B · Fix User-Installed Apps Visibility (2026-04-21)
`QUERY_ALL_PACKAGES` 권한 추가(Android 11+ 패키지 가시성). `getInstalledPackages` API 33+ 분기. 자기 자신(StorageTree) 목록 제외.
