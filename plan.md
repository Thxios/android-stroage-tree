# StorageTree — 구현 계획

## 개요
TreeSizeFree의 핵심 기능을 Android로 이식. Kotlin + Jetpack Compose + MVVM + Hilt.

## Phase 구조 및 담당 sub-agent

| Phase   | 이름                        | 의존성               | 상태     |
|---------|-----------------------------|----------------------|----------|
| phase-0 | Project Scaffolding         | 없음                 | pending  |
| phase-1a| Domain Models & Scanner     | phase-0              | pending  |
| phase-1b| Repository & Hilt Wiring    | phase-1a             | pending  |
| phase-1c| Permission Screen           | phase-0              | pending  |
| phase-1d| List View (ViewModel + UI)  | phase-1b             | pending  |
| phase-1e| Treemap View                | phase-1a             | pending  |
| phase-1f| Navigation & Integration    | phase-1c,1d,1e       | pending  |
| phase-2a| File Deletion               | phase-1f             | pending  |
| phase-2b| File Categorization         | phase-1f             | pending  |
| phase-2c| View Toggle                 | phase-1f             | pending  |

## Phase 상세

### Phase 0: Project Scaffolding
**목표**: Hilt, KSP, Navigation 의존성 추가 + Application 클래스 + 권한 설정
- `gradle/libs.versions.toml` 업데이트 (hilt 2.56.1, ksp 2.2.10-1.0.29, navigation 2.8.9 등)
- `build.gradle.kts` (root): hilt/ksp/kotlin.android plugins
- `app/build.gradle.kts`: plugins 적용 + dependencies 추가
- `StorageTreeApplication.kt`: `@HiltAndroidApp`
- `AndroidManifest.xml`: `MANAGE_EXTERNAL_STORAGE` 권한, application name
- `MainActivity.kt`: `@AndroidEntryPoint` + 컴파일 통과하는 stub
- commit: `chore: project scaffolding - add Hilt, Navigation, KSP`

### Phase 1A: Domain Models & Scanner
**TDD 순서**: 테스트 → 구현
- **테스트 먼저**: `FileScannerTest`, `FileSizeFormatterTest`
- `FileNode.kt` — immutable data class (name, path, sizeBytes, isDirectory, children)
- `ScanState.kt` — sealed class (Idle, Scanning, Done, Error)
- `FileSizeFormatter.kt` — bytes → "4.2 GB" (1024-based)
- `FileScanner.kt` — `java.io.File` 재귀, `Flow<ScanState>`, `Dispatchers.IO`
- commit: `test: file scanner and formatter tests` → `feat: domain models and file scanner`

### Phase 1B: Repository & Hilt Wiring
**TDD 순서**: 테스트 → 구현
- **테스트 먼저**: `StorageRepositoryImplTest`, `ScanDirectoryUseCaseTest`
- `StorageRepository.kt` — interface (scan, deleteNode)
- `StorageRepositoryImpl.kt` — FileScanner 위임
- `ScanDirectoryUseCase.kt`, `DeleteNodeUseCase.kt`
- `StorageModule.kt` — @Binds @Singleton
- commit: `test: repository and use case tests` → `feat: repository layer and Hilt module`

### Phase 1C: Permission Screen
**TDD 순서**: 테스트 → 구현
- **테스트 먼저**: `PermissionScreenTest` (ComposeTestRule)
- `PermissionViewModel.kt` — `Environment.isExternalStorageManager()` (API 30+), fallback API 26-29
- `PermissionScreen.kt` — @Composable + @Preview
- commit: `test: permission screen tests` → `feat: permission gate screen`

### Phase 1D: List View (ViewModel + UI)
**TDD 순서**: 테스트 → 구현
- **테스트 먼저**: `ExplorerViewModelTest` (Turbine, 7 케이스), `StorageListViewTest`
- `ExplorerUiState.kt`, `ExplorerViewModel.kt` — `ArrayDeque<FileNode>` 백스택
- `FileNodeRow.kt`, `StorageListView.kt` (LazyColumn)
- `ScanProgressBanner.kt`, `SizeProgressBar.kt`
- `ExplorerScreen.kt` stub
- commit: `test: explorer viewmodel and list view tests` → `feat: storage list view`

### Phase 1E: Treemap View
**TDD 순서**: 테스트 → 구현
- **테스트 먼저**: `SquarifyAlgorithmTest` (pure JVM, 7 케이스), `TreemapViewTest`
- `SquarifyAlgorithm.kt` — pure Kotlin, no Android imports (~80줄)
- `TreemapView.kt` — Canvas + pointerInput click detection
- commit: `test: squarify algorithm tests` → `feat: squarified treemap view`

### Phase 1F: Navigation & Integration
**TDD 순서**: 테스트 → 구현
- **테스트 먼저**: `NavigationTest` (createAndroidComposeRule<MainActivity>)
- `AppDestination.kt`, `AppNavGraph.kt`
- `MainActivity.kt` 최종: NavHost 연결
- `ExplorerScreen.kt` 완성: BackHandler + TopAppBar
- commit: `test: navigation tests` → `feat: full navigation wiring`

### Phase 2A: File Deletion
**TDD 순서**: 테스트 → 구현
- **테스트 먼저**: `DeleteNodeUseCaseTest`, ViewModel 추가 테스트
- `DeleteNodeUseCase.kt`, Repository `deleteNode()`, `File.deleteRecursively()`
- ViewModel `deleteNode()`, AlertDialog 확인창
- FileNodeRow long-press 트리거
- commit: `test: deletion tests` → `feat: in-app file deletion`

### Phase 2B: File Categorization
**TDD 순서**: 테스트 → 구현
- **테스트 먼저**: `FileCategoryTest`, `CategorizeFilesUseCaseTest`
- `FileCategory.kt` — enum (IMAGE, VIDEO, AUDIO, DOCUMENT, APK, ARCHIVE, OTHER)
- `CategorizeFilesUseCase.kt` — returns `Map<FileCategory, Long>`
- FilterChipRow UI in ExplorerScreen
- commit: `test: categorization tests` → `feat: file categorization`

### Phase 2C: View Toggle
**TDD 순서**: 테스트 → 구현
- **테스트 먼저**: ViewModel toggle tests, `ViewToggleTest`
- `ViewMode.kt` — enum (LIST, TREEMAP)
- ViewModel `toggleViewMode()`, SegmentedButton in TopAppBar
- commit: `test: view toggle tests` → `feat: list/treemap view toggle`

## 의존성 요약
```
ksp = "2.2.10-1.0.29"
hilt = "2.56.1"
hiltNavigationCompose = "1.2.0"
navigationCompose = "2.8.9"
lifecycleViewModel = "2.9.0"
coroutines = "1.10.2"
mockk = "1.14.0"
turbine = "1.2.0"
```
