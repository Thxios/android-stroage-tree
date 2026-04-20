# StorageTree — Handover Document
> 작성일: 2026-04-20 | 다음 agent가 이 파일만으로 작업을 재개할 수 있도록 작성됨

---

## 1. 현재 상태

**plan.md의 모든 Phase(0 ~ 2C, 3A ~ 3E) 구현 완료.**

- 단위 테스트 (`./gradlew test`): ✅ 전체 통과 (32개)
- APK 빌드 (`./gradlew assembleDebug`): ✅ 성공
- Instrumented 테스트 (`./gradlew connectedAndroidTest`): ❌ 미해결 (아래 알려진 이슈 참고)

### 완료된 Phase 목록

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

---

## 2. 구현된 주요 기능 (Phase별)

- **Phase 0**: Gradle KTS + Hilt + Compose + KSP 프로젝트 초기 구성
- **Phase 1a**: `FileNode`, `ScanState`, `FileCategory`, `ViewMode` 도메인 모델 + `FileScanner` (java.io.File 재귀)
- **Phase 1b**: `StorageRepository` 인터페이스 + `StorageRepositoryImpl` + Hilt `StorageModule`
- **Phase 1c**: `MANAGE_EXTERNAL_STORAGE` 권한 요청 화면 (`PermissionScreen`, `PermissionViewModel`)
- **Phase 1d**: `StorageListView` + `FileNodeRow` (LazyColumn 기반 파일 목록)
- **Phase 1e**: `TreemapView` + `SquarifyAlgorithm` (외부 라이브러리 없이 Squarified 알고리즘 구현)
- **Phase 1f**: `AppNavGraph` + `ExplorerViewModel` (ArrayDeque 백스택 기반 드릴다운 내비게이션)
- **Phase 2a**: `DeleteNodeUseCase` + `ExplorerViewModel.deleteNode()` + AlertDialog 확인
- **Phase 2b**: `CategorizeFilesUseCase` + `CategoryChipRow` 필터 UI (파일 타입별 분류)
- **Phase 2c**: `ViewMode` 토글 (LIST ↔ TREEMAP) + `ExplorerViewModel.toggleViewMode()`
- **Phase 3a**: 스캔 중 실시간 결과 표시, TopAppBar 경로 표시, 시스템 Back vs 상위 폴더 분리
- **Phase 3b**: `StorageVolumeHelper` (내부/외부 저장소 루트 목록), 스캔 루트 선택 UI
- **Phase 3c**: 경로 breadcrumb 클릭 내비게이션, LazyColumn 스크롤바, 파일 타입 필터 동적 갱신
- **Phase 3d**: `InstalledAppScanner` (PackageManager + StorageStatsManager), 가상 FileNode 트리 `/virtual://installed-apps/`, `PACKAGE_USAGE_STATS` 권한 흐름
- **Phase 3e**: OBB 디렉토리 직접 측정, 외부 data 디렉토리 직접 측정, `splitSourceDirs` APK 합산, OBB 별도 자식 노드 표시

---

## 3. 전체 파일 구조

```
app/src/main/java/com/thxios/storagetree/
├── StorageTreeApplication.kt           — @HiltAndroidApp
├── MainActivity.kt                     — @AndroidEntryPoint + AppNavGraph
├── domain/model/
│   ├── FileNode.kt                     — data class (name, path, sizeBytes, isDirectory, children)
│   ├── ScanState.kt                    — sealed class (Idle, Scanning, Done, Error)
│   ├── FileCategory.kt                 — enum + companion object { of(filename) }
│   └── ViewMode.kt                     — enum (LIST, TREEMAP)
├── domain/repository/
│   └── StorageRepository.kt            — interface (scan, deleteNode)
├── domain/usecase/
│   ├── ScanDirectoryUseCase.kt
│   ├── DeleteNodeUseCase.kt
│   └── CategorizeFilesUseCase.kt
├── data/scanner/
│   ├── FileScanner.kt                  — Flow<ScanState>, java.io.File 재귀, throttle 기반 partial emit
│   ├── FileSizeFormatter.kt            — bytes → "4.2 GB"
│   └── InstalledAppScanner.kt          — APK(base+splits) + internal data/cache + OBB + external data 측정
├── data/repository/
│   └── StorageRepositoryImpl.kt        — FileScanner 위임, deleteRecursively()
├── data/storage/
│   └── StorageVolumeHelper.kt          — 사용 가능한 저장소 루트 목록 (내부/외부)
├── di/
│   └── StorageModule.kt                — @Binds StorageRepository, InstalledAppScanner @Singleton
└── ui/
    ├── navigation/
    │   ├── AppDestination.kt           — sealed class (Permission, Explorer)
    │   └── AppNavGraph.kt              — NavHost, popUpTo Permission inclusive
    ├── permission/
    │   ├── PermissionViewModel.kt      — hasPermission StateFlow
    │   └── PermissionScreen.kt
    ├── explorer/
    │   ├── ExplorerUiState.kt          — data class (currentPath, displayedChildren, scanState, ...)
    │   ├── ExplorerViewModel.kt        — startScan, navigateTo/Up/ToAncestor, toggleViewMode,
    │   │                                  setPendingDelete, deleteNode, setFilter, loadInstalledApps
    │   ├── ExplorerScreen.kt           — AlertDialog(삭제), CategoryChipRow, breadcrumb, LIST/TREEMAP 분기
    │   ├── listview/
    │   │   ├── FileNodeRow.kt          — combinedClickable (onClick + onLongClick)
    │   │   └── StorageListView.kt      — LazyColumn + 스크롤바
    │   └── treemap/
    │       ├── TreemapRect.kt          — data class (node, left, top, right, bottom)
    │       ├── SquarifyAlgorithm.kt    — pure Kotlin Squarified 알고리즘
    │       └── TreemapView.kt          — Canvas + pointerInput 클릭
    └── components/
        ├── ScanProgressBanner.kt
        ├── SizeProgressBar.kt
        ├── ScrollbarModifier.kt        — LazyColumn 스크롤바 커스텀 Modifier
        └── ErrorBanner.kt

app/src/test/java/com/thxios/storagetree/   — 단위 테스트 (32개, 전체 통과)
app/src/androidTest/java/com/thxios/storagetree/
├── HiltTestRunner.kt
├── di/
│   ├── FakeStorageRepository.kt
│   └── TestStorageModule.kt
└── ui/
    ├── NavigationTest.kt
    ├── PermissionScreenTest.kt
    ├── StorageListViewTest.kt
    ├── TreemapViewTest.kt
    └── ViewToggleTest.kt
```

---

## 4. 알려진 이슈 / 제한사항

### Instrumented 테스트 실패 (미해결)
사용자의 에뮬레이터가 **Pixel_9_Pro AVD (API 37, Android 17)**이다.  
Espresso(`espresso-core:3.6.1`)는 Android 15(API 35)에서 제거된 `InputManager.getInstance()`를 reflection으로 호출하는데, API 37에서도 동일하게 실패한다.

```
java.lang.NoSuchMethodException: android.hardware.input.InputManager.getInstance []
at androidx.test.espresso.base.InputManagerEventInjectionStrategy.initialize
```

**권장 해결 방법**: API 34 AVD 생성 후 `./gradlew connectedAndroidTest` 실행.

### Android 11+ 외부 data 디렉토리 접근 제한
`/storage/emulated/0/Android/data/<packageName>/`은 Android 11 (API 30) 이후 다른 앱에서 접근 불가 (보안 정책). `SecurityException`을 catch하여 0으로 처리함. 따라서 외부 data 크기는 일부 앱에서 정확하지 않을 수 있음.

### PACKAGE_USAGE_STATS 권한 없을 때 data/cache 미표시
`StorageStatsManager.queryStatsForPackage()`는 `PACKAGE_USAGE_STATS` 권한이 필요함. 이 권한이 없으면 내부 data/cache 크기가 0으로 표시됨. OBB와 APK 크기는 권한과 무관하게 측정됨.

### OBB 측정은 MANAGE_EXTERNAL_STORAGE 필요
앱이 이미 `MANAGE_EXTERNAL_STORAGE`를 요청하므로 일반적으로 문제없음. 권한이 없을 경우 `Exception` catch로 0 처리.

### 알려진 경고 (에러 아님)
- `Icons.Filled.InsertDriveFile` deprecated → `Icons.AutoMirrored.Filled.InsertDriveFile` 권장
- `Icons.Filled.ViewList` deprecated → `Icons.AutoMirrored.Filled.ViewList` 권장
- `unsafeCheckOpNoThrow` deprecated (Android Q+) — API 레벨 분기로 처리됨

---

## 5. 주요 의존성 버전 (libs.versions.toml 기준)

```toml
agp = "9.1.1"
kotlin = "2.2.10"
ksp = "2.2.10-2.0.2"
hilt = "2.59.2"
hiltNavigationCompose = "1.2.0"
navigationCompose = "2.8.9"
composeBom = "2026.02.01"
lifecycleViewModel = "2.9.0"
coroutines = "1.10.2"
espressoCore = "3.6.1"
junitVersion = "1.2.1"
androidTestRunner = "1.6.2"
mockk = "1.14.0"
turbine = "1.2.0"
```

SDK: compileSdk 36, minSdk 26, targetSdk 36

---

## 6. 다음 agent 작업 시 주의사항

### Virtual Path 처리
- `InstalledAppScanner.VIRTUAL_APPS_PATH = "virtual://installed-apps"` — 실제 파일시스템 경로가 아님
- `ExplorerViewModel`의 많은 분기문이 `path.startsWith(VIRTUAL_APPS_PATH)`로 가상 경로를 특별 처리함
  - `navigateTo`: 가상 경로 진입 시 `categorySummary = emptyMap()`
  - `setPendingDelete`: 가상 경로 노드는 삭제 불가
  - `navigateUp` / `navigateToAncestor`: 가상 경로에서 되돌아올 때 categorySummary 비움
- 가상 노드의 children path 형식: `virtual://installed-apps/<packageName>/apk` 등

### BackStack 구조
- `backStack: ArrayDeque<Pair<String, List<FileNode>>>` — `(이전 경로, 이전 displayedChildren)` 쌍을 저장
- `NavController`를 사용하지 않음. ViewModel 내부에서 직접 관리
- `navigateToAncestor(targetPath)`: breadcrumb 클릭 시 backStack에서 targetPath 항목까지 pop

### scanRoot vs displayedChildren
- `scanRoot: FileNode?` — 스캔 완료 후 전체 트리의 루트 노드 (불변)
- `displayedChildren` — 현재 화면에 표시되는 자식 노드 목록 (가변, 필터/정렬 반영)
- root level에서는 `appsNode`(가상 설치앱 노드)를 `scanRoot.children`에 병합하여 표시
- `setFilter()` 함수는 backStack이 비어있으면 root level로 간주하고 appsNode를 포함시킴

### InstalledAppScanner 크기 측정 로직 (Phase 3e 이후)
```
totalSize = apkSize(base+splits) + internalDataSize + cacheSize + obbSize + externalDataSize
```
- `internalDataSize` = `stats.dataBytes` (StorageStatsManager, UUID_DEFAULT)
- `obbSize` = `/storage/emulated/0/Android/obb/<pkg>/` 직접 walkTopDown
- `externalDataSize` = `/storage/emulated/0/Android/data/<pkg>/` 직접 walkTopDown (SecurityException → 0)
- child 노드: APK (항상), 데이터 (>0일 때), 캐시 (>0일 때), OBB (>0일 때)

### Hilt 테스트 구조
- `HiltTestRunner`: `AndroidJUnitRunner` 상속, `HiltTestApplication` 주입
- `TestStorageModule` (`@TestInstallIn(replaces = [StorageModule::class])`): androidTest 전체에서 자동으로 `FakeStorageRepository` 사용
- `buildVirtualAppsNode`는 테스트에서 mock되므로 InstalledAppScanner 변경이 테스트에 영향 없음
- `@HiltAndroidTest` 테스트는 `HiltAndroidRule`을 `@get:Rule(order = 0)`으로 선언 필수
