# StorageTree — Handover Document
> 작성일: 2026-04-21 | plan.md + 이 파일만으로 작업을 재개할 수 있도록 작성됨

---

## 1. 현재 상태

**완료된 Phase: 0 ~ 4b, 5a ~ 5c**
**남은 Phase: 5d, 5e, 5f**

- 단위 테스트 (`./gradlew test`): ✅ 전체 통과
- APK 빌드 (`./gradlew assembleDebug`): ✅ 성공
- Instrumented 테스트 (`./gradlew connectedAndroidTest`): ❌ API 37 에뮬레이터에서 Espresso 호환성 문제 (API 34 AVD 사용 권장)

### Phase 완료 현황

| Phase | 이름 | 완료일 |
|-------|------|--------|
| 0 | Project Scaffolding | 2026-04-19 |
| 1a | Domain Models & Scanner | 2026-04-19 |
| 1b | Repository & Hilt Wiring | 2026-04-20 |
| 1c | Permission Screen | 2026-04-19 |
| 1d | List View | 2026-04-20 |
| 1e | Treemap View | 2026-04-20 |
| 1f | Navigation & Integration | 2026-04-20 |
| 2a | File Deletion | 2026-04-20 |
| 2b | File Categorization | 2026-04-20 |
| 2c | View Toggle | 2026-04-20 |
| 3a | UX Improvements | 2026-04-20 |
| 3b | Scan Root Selection & Partial Scan Display | 2026-04-20 |
| 3c | Deep Scan UX & File Filter | 2026-04-20 |
| 3d | Installed Apps Virtual Folder | 2026-04-20 |
| 3e | Installed Apps Size Accuracy Fix | 2026-04-20 |
| 4a | Bug Fixes - Permission Flicker & Back Navigation | 2026-04-20 |
| 4b | Settings Page + Installed Apps Toggle + Sort Order | 2026-04-20 |
| 5a | Splash Screen | 2026-04-21 |
| 5b | Scan Once + Reload Button | 2026-04-21 |
| 5c | Start Folder Picker | 2026-04-21 |

---

## 2. 전체 파일 구조

```
app/src/main/java/com/thxios/storagetree/
├── StorageTreeApplication.kt
├── MainActivity.kt                         — @AndroidEntryPoint + AppNavGraph
├── domain/model/
│   ├── FileNode.kt
│   ├── ScanState.kt                        — sealed class (Idle, Scanning, Done, Error)
│   ├── FileCategory.kt                     — enum + companion object { of(filename) }
│   ├── ViewMode.kt                         — enum (LIST, TREEMAP)
│   └── SortOrder.kt                        — enum (SIZE_DESC, NAME_ASC, DATE_DESC, NATURAL_NAME_ASC)
├── domain/repository/
│   └── StorageRepository.kt               — interface (scan, deleteNode)
├── domain/usecase/
│   ├── ScanDirectoryUseCase.kt
│   ├── DeleteNodeUseCase.kt
│   └── CategorizeFilesUseCase.kt
├── data/scanner/
│   ├── FileScanner.kt                      — Flow<ScanState>, throttle 기반 partial emit
│   ├── FileSizeFormatter.kt
│   └── InstalledAppScanner.kt             — APK + StorageStatsManager(UUID_DEFAULT) + OBB walkTopDown
├── data/repository/
│   └── StorageRepositoryImpl.kt
├── data/storage/
│   └── StorageVolumeHelper.kt             — 내부/외부 저장소 루트 목록
├── data/preferences/
│   └── PreferencesRepository.kt          — DataStore, AppSettings(showInstalledApps, sortOrder)
├── di/
│   └── StorageModule.kt
└── ui/
    ├── navigation/
    │   ├── AppDestination.kt              — sealed class (Splash, Permission, Explorer, Settings)
    │   └── AppNavGraph.kt                 — startDestination=Splash, 전체 라우트
    ├── splash/
    │   ├── SplashViewModel.kt             — init{}에서 즉시 권한 체크 → NavTarget emit
    │   └── SplashScreen.kt               — 앱 이름 + CircularProgressIndicator
    ├── permission/
    │   ├── PermissionViewModel.kt
    │   └── PermissionScreen.kt
    ├── settings/
    │   ├── SettingsViewModel.kt
    │   └── SettingsScreen.kt             — showInstalledApps 토글 + sortOrder RadioButton
    ├── explorer/
    │   ├── ExplorerUiState.kt            — showFolderPicker, pickerCurrentPath, pickerEntries 포함
    │   ├── ExplorerViewModel.kt          — scanStarted 플래그, startScanIfNeeded/reloadScan,
    │   │                                   reloadInstalledAppsIfPermissionChanged,
    │   │                                   openFolderPicker/closeFolderPicker/navigatePickerInto/
    │   │                                   startScanFromPicker/loadPickerEntries 포함
    │   ├── ExplorerScreen.kt             — Refresh 버튼, FolderOpen 버튼, FolderPickerSheet 조건부 표시
    │   ├── FolderPickerSheet.kt          — ModalBottomSheet + 경로 breadcrumb + 서브폴더 LazyColumn
    │   ├── listview/
    │   │   ├── FileNodeRow.kt
    │   │   └── StorageListView.kt
    │   └── treemap/
    │       ├── SquarifyAlgorithm.kt
    │       └── TreemapView.kt
    └── components/
        ├── ScanProgressBanner.kt
        ├── SizeProgressBar.kt
        ├── ScrollbarModifier.kt
        └── ErrorBanner.kt
```

---

## 3. 핵심 설계 결정 및 주의사항

### Navigation
- `backStack: ArrayDeque<Pair<String, List<FileNode>>>` — (이전 경로, 이전 displayedChildren) 쌍 저장
- **`goBack()`**: backStack pop. BackHandler(enabled=canGoBack) + ← 버튼
- **`goToParent()`**: 현재 상태 push → 부모 경로로 이동. ^ 버튼
- **`navigateTo(node)`**: 현재 상태 push → 자식 노드로 이동. 폴더 클릭
- **`navigateToAncestor(path)`**: 현재 상태 push → 대상 경로로 이동. Breadcrumb 클릭
- `canGoBack = backStack.isNotEmpty()`. BackHandler가 비활성화되면 시스템이 back 처리(앱 종료)
- NavController.popBackStack()를 BackHandler에서 호출하지 않음 (상태 오염 방지)

### Splash Screen (Phase 5a)
- `SplashViewModel`: `init{}`에서 동기적으로 권한 체크. NavTarget sealed interface: `Permission`, `Explorer`
- `AppNavGraph`: startDestination=Splash. Splash→Permission 또는 Splash→Explorer (popUpTo Splash inclusive)
- 기존 Permission→Explorer popUpTo 유지

### Scan Once + Reload (Phase 5b)
- `ExplorerViewModel`: `private var scanStarted = false`
- `startScanIfNeeded(rootPath)`: `!scanStarted`일 때만 startScan()
- `reloadScan()`: `scanStarted=false` 후 startScan()
- `reloadInstalledAppsIfPermissionChanged()`: `lastKnownHasUsageStatsPermission`과 현재값 비교
- `ExplorerScreen`: LaunchedEffect → startScanIfNeeded, ON_RESUME → reloadInstalledAppsIfPermissionChanged
- TopAppBar에 Refresh 아이콘 버튼 (enabled = !isScanning)

### Folder Picker (Phase 5c)
- `ExplorerUiState`: `showFolderPicker`, `pickerCurrentPath`, `pickerEntries` 추가
- `FolderPickerSheet`: ModalBottomSheet. breadcrumb (horizontalScroll) + 서브폴더 LazyColumn + "여기서 스캔 시작" 버튼
- `startScanFromPicker(path)`: 피커 닫고 scanStarted=false 후 startScan(path)

### Virtual Path (InstalledAppScanner)
- `VIRTUAL_APPS_PATH = "virtual://installed-apps"` — 실제 파일시스템 경로 아님
- `goToParent()`에서 가상 경로 특별 처리 필요 (Phase 5d에서 완성 예정)
- 가상 노드 child path 형식: `virtual://installed-apps/<packageName>/apk` 등
- 가상 경로 노드는 삭제 불가

### 설정 (Phase 4b)
- `PreferencesRepository`: DataStore `"app_settings"`
- `ExplorerViewModel.init{}`에서 settings collect → 변경 시 refreshDisplayedChildren() 자동 호출
- `showInstalledApps=false`이면 appsNode를 displayedChildren에서 제외

### InstalledAppScanner 크기 측정 (Phase 3e 현재)
- `totalSize = apkSize(base+splits) + internalDataSize + cacheSize + obbSize + externalDataSize`
- `internalData/cache`: StorageStatsManager.queryStatsForPackage() UUID_DEFAULT
- `obbSize`: `/storage/emulated/0/Android/obb/<pkg>/` walkTopDown
- `externalData`: `/storage/emulated/0/Android/data/<pkg>/` walkTopDown (Android 11+ → SecurityException → 0)
- **Phase 5f에서**: 위 로직을 Multi-Volume UUID 방식으로 교체 예정

---

## 4. 남은 Phase 요약

상세 스펙은 plan.md 참조.

### Phase 5d — Virtual Path Parent Navigation Fix
`ExplorerViewModel.goToParent()`에서 가상 경로 처리 추가:
- `currentPath == VIRTUAL_APPS_PATH` → scanRoot children으로 이동
- `currentPath.startsWith(VIRTUAL_APPS_PATH + "/")` → 상위 가상 경로로 이동
- `findVirtualNode(root, path)` private 헬퍼 추가

### Phase 5e — Sort Order In-Screen Dropdown
- `ExplorerUiState`에 `sortOrder: SortOrder` 추가
- `ExplorerViewModel.setSortOrder(SortOrder)` 추가
- `ExplorerScreen`에 `SortOrderDropdown` composable (ExposedDropdownMenuBox) 추가
- `SettingsScreen`에서 sortOrder RadioButton 그룹 제거

### Phase 5f — Multi-Volume StorageStatsManager
`InstalledAppScanner` 수정:
- `getAllVolumeStats(storageStatsManager, storageManager, packageName): Pair<Long, Long>` 추가
- 모든 StorageVolume UUID에 대해 queryStatsForPackage 합산
- 기존 OBB walkTopDown 블록 제거, 외부 data walkTopDown 블록 제거
- 자식 노드: APK / 데이터 / 캐시만 (OBB 노드 제거)

---

## 5. 알려진 이슈

- **Instrumented 테스트**: API 37 에뮬레이터에서 Espresso `InputManager.getInstance()` 실패. API 34 AVD 사용 권장.
- **외부 data 접근**: Android 11+ 에서 `SecurityException` → 0으로 처리 (Phase 5f에서 개선 예정)
- **Deprecated Icons**: `Icons.Filled.InsertDriveFile`, `Icons.Filled.ViewList` → AutoMirrored 권장 (기능 영향 없음)

---

## 6. 주요 의존성 버전

```toml
agp = "9.1.1"
kotlin = "2.2.10"
ksp = "2.2.10-2.0.2"
hilt = "2.59.2"
composeBom = "2026.02.01"
lifecycleViewModel = "2.9.0"
coroutines = "1.10.2"
mockk = "1.14.0"
turbine = "1.2.0"
datastorePreferences = "1.1.4"
```

SDK: compileSdk 36, minSdk 26, targetSdk 36
