# StorageTree — 구현 계획

## 개요
TreeSizeFree의 핵심 기능을 Android로 이식. Kotlin + Jetpack Compose + MVVM + Hilt.

## Phase 구조

| Phase    | 이름                       | 의존성         |
|----------|----------------------------|----------------|
| phase-0  | Project Scaffolding        | 없음           |
| phase-1a | Domain Models & Scanner    | phase-0        |
| phase-1b | Repository & Hilt Wiring   | phase-1a       |
| phase-1c | Permission Screen          | phase-0        |
| phase-1d | List View (ViewModel + UI) | phase-1b       |
| phase-1e | Treemap View               | phase-1a       |
| phase-1f | Navigation & Integration   | 1c, 1d, 1e     |
| phase-2a | File Deletion              | phase-1f       |
| phase-2b | File Categorization        | phase-1f       |
| phase-2c | View Toggle                | phase-1f       |

---

## Phase 상세

### Phase 0: Project Scaffolding

**생성/수정 파일:**
- `gradle/libs.versions.toml` — 아래 항목 추가
- `build.gradle.kts` (root) — plugin aliases 추가
- `app/build.gradle.kts` — plugins + dependencies 추가
- `app/src/main/java/com/thxios/storagetree/StorageTreeApplication.kt` — 신규
- `app/src/main/AndroidManifest.xml` — application name + 권한 추가
- `app/src/main/java/com/thxios/storagetree/MainActivity.kt` — `@AndroidEntryPoint` 추가

**libs.versions.toml 추가:**
```toml
[versions]
ksp = "2.2.10-1.0.29"
hilt = "2.56.1"
hiltNavigationCompose = "1.2.0"
navigationCompose = "2.8.9"
lifecycleViewModel = "2.9.0"
coroutines = "1.10.2"
mockk = "1.14.0"
turbine = "1.2.0"

[libraries]
hilt-android              = { group = "com.google.dagger", name = "hilt-android",               version.ref = "hilt" }
hilt-android-compiler     = { group = "com.google.dagger", name = "hilt-android-compiler",      version.ref = "hilt" }
hilt-navigation-compose   = { group = "androidx.hilt",     name = "hilt-navigation-compose",    version.ref = "hiltNavigationCompose" }
navigation-compose        = { group = "androidx.navigation",name = "navigation-compose",         version.ref = "navigationCompose" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewModel" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleViewModel" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test   = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test",  version.ref = "coroutines" }
mockk                     = { group = "io.mockk",            name = "mockk",                    version.ref = "mockk" }
turbine                   = { group = "app.cash.turbine",    name = "turbine",                  version.ref = "turbine" }

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android",  version.ref = "kotlin" }
hilt           = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp            = { id = "com.google.devtools.ksp",        version.ref = "ksp" }
```

**build.gradle.kts (root) plugins 블록에 추가:**
```kotlin
alias(libs.plugins.kotlin.android) apply false
alias(libs.plugins.hilt)           apply false
alias(libs.plugins.ksp)            apply false
```

**app/build.gradle.kts plugins 블록에 추가:**
```kotlin
alias(libs.plugins.kotlin.android)
alias(libs.plugins.hilt)
alias(libs.plugins.ksp)
```

**app/build.gradle.kts dependencies에 추가:**
```kotlin
implementation(libs.hilt.android)
ksp(libs.hilt.android.compiler)
implementation(libs.hilt.navigation.compose)
implementation(libs.navigation.compose)
implementation(libs.lifecycle.viewmodel.compose)
implementation(libs.lifecycle.runtime.compose)
implementation(libs.kotlinx.coroutines.android)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.mockk)
testImplementation(libs.turbine)
```

**StorageTreeApplication.kt:**
```kotlin
@HiltAndroidApp
class StorageTreeApplication : Application()
```

**AndroidManifest.xml에 추가:**
```xml
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
    tools:ignore="ScopedStorage" />
<!-- <application> 태그에 android:name=".StorageTreeApplication" 추가 -->
```

**검증:** `./gradlew assembleDebug` 성공
**커밋:** `chore: project scaffolding - add Hilt, Navigation, KSP`

---

### Phase 1A: Domain Models & Scanner

**생성 파일:**
- `app/src/test/.../domain/FileSizeFormatterTest.kt` ← **먼저 작성**
- `app/src/test/.../domain/FileScannerTest.kt` ← **먼저 작성**
- `app/src/main/.../domain/model/FileNode.kt`
- `app/src/main/.../domain/model/ScanState.kt`
- `app/src/main/.../data/scanner/FileSizeFormatter.kt`
- `app/src/main/.../data/scanner/FileScanner.kt`

**구현 내용:**
- `FileNode`: `data class(name, path, sizeBytes: Long, isDirectory: Boolean, children: List<FileNode> = emptyList())`
- `ScanState`: `sealed class { Idle, Scanning(currentPath: String, rootNode: FileNode?), Done(rootNode: FileNode), Error(message: String) }`
- `FileSizeFormatter`: bytes → "4.2 GB" (1024-based, B/KB/MB/GB 단위)
- `FileScanner`: `java.io.File` 재귀, `Flow<ScanState>`, `flowOn(Dispatchers.IO)`, `listFiles()` null 시 빈 children

**FileScannerTest 케이스 (시스템 temp dir 사용, 모킹 없음):**
1. 빈 디렉토리 → sizeBytes=0, children 없음
2. 단일 파일 → 정확한 sizeBytes
3. 파일 3개 → children 크기 내림차순 정렬
4. 2단계 중첩 → 총 크기 = 모든 파일 합
5. 읽기 불가 디렉토리 → 빈 children, 크래시 없음
6. scan() → Scanning emit 후 Done emit

**FileSizeFormatterTest 케이스:** 0B, 1023B, 1024B(→"1.0 KB"), 1.5MB, 2GB 경계값

**커밋:** `test: file scanner and formatter tests` → `feat: domain models and file scanner`

---

### Phase 1B: Repository & Hilt Wiring

**생성 파일:**
- `app/src/test/.../data/StorageRepositoryImplTest.kt` ← **먼저 작성**
- `app/src/test/.../domain/ScanDirectoryUseCaseTest.kt` ← **먼저 작성**
- `app/src/main/.../domain/repository/StorageRepository.kt`
- `app/src/main/.../data/repository/StorageRepositoryImpl.kt`
- `app/src/main/.../domain/usecase/ScanDirectoryUseCase.kt`
- `app/src/main/.../di/StorageModule.kt`

**구현 내용:**
- `StorageRepository` interface: `fun scan(path: String): Flow<ScanState>`
- `StorageRepositoryImpl`: `@Inject constructor(private val scanner: FileScanner)`, scan 위임
- `ScanDirectoryUseCase`: `operator fun invoke(path: String): Flow<ScanState>`
- `StorageModule`: `@Module @InstallIn(SingletonComponent::class)`, `@Binds @Singleton`

**테스트 케이스:**
- `StorageRepositoryImplTest`: scan → Done (real temp dir), 없는 경로 → Error emit 확인
- `ScanDirectoryUseCaseTest`: MockK로 repository mocking, invoke() → repository.scan() 위임 확인

**커밋:** `test: repository and use case tests` → `feat: repository layer and Hilt module`

---

### Phase 1C: Permission Screen

**생성 파일:**
- `app/src/androidTest/.../ui/PermissionScreenTest.kt` ← **먼저 작성**
- `app/src/main/.../ui/permission/PermissionViewModel.kt`
- `app/src/main/.../ui/permission/PermissionScreen.kt`

**구현 내용:**
- `PermissionViewModel`: `hasPermission: StateFlow<Boolean>`
  - API 30+: `Environment.isExternalStorageManager()`
  - API 26-29: `READ_EXTERNAL_STORAGE` 권한 확인
  - `requestPermission(activity)`: API 30+ → `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` intent
- `PermissionScreen`: 앱 설명 + "권한 허용" 버튼, `@Preview`

**테스트 케이스 (ComposeTestRule, fake state 직접 주입):**
1. `hasPermission=false` → "권한 허용" 버튼 표시됨
2. 버튼 클릭 → `onNavigateToExplorer` 콜백 미호출 (권한 요청만 트리거)
3. `hasPermission=true` → 즉시 `onNavigateToExplorer` 콜백 호출

**커밋:** `test: permission screen tests` → `feat: permission gate screen`

---

### Phase 1D: List View (ViewModel + UI)

**생성 파일:**
- `app/src/test/.../ui/ExplorerViewModelTest.kt` ← **먼저 작성**
- `app/src/androidTest/.../ui/StorageListViewTest.kt` ← **먼저 작성**
- `app/src/main/.../ui/explorer/ExplorerUiState.kt`
- `app/src/main/.../ui/explorer/ExplorerViewModel.kt`
- `app/src/main/.../ui/explorer/listview/FileNodeRow.kt`
- `app/src/main/.../ui/explorer/listview/StorageListView.kt`
- `app/src/main/.../ui/components/ScanProgressBanner.kt`
- `app/src/main/.../ui/components/SizeProgressBar.kt`
- `app/src/main/.../ui/components/ErrorBanner.kt`
- `app/src/main/.../ui/explorer/ExplorerScreen.kt` (stub, Phase 1F에서 완성)

**구현 내용:**
- `ExplorerUiState`: `currentPath, displayedChildren: List<FileNode>, scanState: ScanState, isScanning: Boolean, scanningCurrentPath: String, error: String?, pendingDeleteNode: FileNode? = null, categorySummary: Map<FileCategory, Long> = emptyMap(), viewMode: ViewMode = ViewMode.LIST`
- `ExplorerViewModel`: `@HiltViewModel`, `startScan(rootPath)`, `navigateTo(node)`, `navigateUp()`, 백스택 `ArrayDeque<FileNode>`
- `FileNodeRow`: 폴더/파일 아이콘 | 이름 | 사이즈 | % | `SizeProgressBar`, `@Preview`
- `StorageListView`: `LazyColumn` of `FileNodeRow`, `@Preview`
- `ScanProgressBanner`: 스캔 중 현재 경로 표시, `@Preview`

**ExplorerViewModelTest 케이스 (StandardTestDispatcher + Turbine):**
1. 초기 상태 = Idle
2. `startScan()` → Scanning → Done 전이
3. Done 후 `displayedChildren` = rootNode.children (크기 내림차순)
4. `navigateTo(child)` → `displayedChildren` = child.children
5. `navigateTo` 후 `navigateUp()` → 부모 children 복원
6. 루트에서 `navigateUp()` → no-op
7. 스캔 에러 → `error` 필드 non-null

**커밋:** `test: explorer viewmodel and list view tests` → `feat: storage list view`

---

### Phase 1E: Treemap View

**생성 파일:**
- `app/src/test/.../ui/SquarifyAlgorithmTest.kt` ← **먼저 작성**
- `app/src/androidTest/.../ui/TreemapViewTest.kt` ← **먼저 작성**
- `app/src/main/.../ui/explorer/treemap/SquarifyAlgorithm.kt`
- `app/src/main/.../ui/explorer/treemap/TreemapView.kt`

**구현 내용:**
- `TreemapRect`: `data class(node: FileNode, left: Float, top: Float, right: Float, bottom: Float)`
- `SquarifyAlgorithm`: Android import 없는 pure Kotlin. `fun compute(nodes: List<FileNode>, width: Float, height: Float): List<TreemapRect>`
- `TreemapView`: `Canvas` + `pointerInput` click detection, 색상 순환, 소형 노드 레이블 생략, `@Preview`

**SquarifyAlgorithmTest 케이스 (pure JVM):**
1. 단일 아이템 → 전체 영역
2. 동일 크기 2개 → 각각 절반
3. 전체 rect 면적 합 = 캔버스 면적 (float epsilon 허용)
4. 어떤 rect도 경계 밖으로 벗어나지 않음
5. rect 상호 미겹침 (10개 샘플)
6. 빈 리스트 → 빈 결과, 크래시 없음
7. 크기 0 아이템 → 면적 0, division-by-zero 없음

**커밋:** `test: squarify algorithm tests` → `feat: squarified treemap view`

---

### Phase 1F: Navigation & Integration

**생성/수정 파일:**
- `app/src/androidTest/.../NavigationTest.kt` ← **먼저 작성**
- `app/src/main/.../ui/navigation/AppDestination.kt`
- `app/src/main/.../ui/navigation/AppNavGraph.kt`
- `app/src/main/.../MainActivity.kt` (최종)
- `app/src/main/.../ui/explorer/ExplorerScreen.kt` (완성)

**구현 내용:**
- `AppDestination`: `sealed class { object Permission : AppDestination("permission"); object Explorer : AppDestination("explorer") }`
- `AppNavGraph`: `NavHost(startDestination=Permission)`, `hiltViewModel()` 사용, Permission→Explorer `popUpTo(inclusive=true)`
- `MainActivity`: `@AndroidEntryPoint`, `rememberNavController()`, `AppNavGraph(navController)`
- `ExplorerScreen`: `BackHandler` → `navigateUp()` (스택 비면 `navController.popBackStack()`), `TopAppBar` (현재 경로 + 뒤로 버튼)

**테스트 케이스 (`createAndroidComposeRule<MainActivity>`):**
1. 앱 실행 → PermissionScreen 표시
2. Explorer 화면 → StorageListView 렌더링 확인
3. 루트에서 뒤로가기 → 앱 종료

**커밋:** `test: navigation tests` → `feat: full navigation wiring`

---

### Phase 2A: File Deletion

**생성/수정 파일:**
- `app/src/test/.../domain/DeleteNodeUseCaseTest.kt` ← **먼저 작성**
- `app/src/main/.../domain/repository/StorageRepository.kt` — `deleteNode` 추가
- `app/src/main/.../data/repository/StorageRepositoryImpl.kt` — `deleteNode` 구현
- `app/src/main/.../domain/usecase/DeleteNodeUseCase.kt`
- `ExplorerViewModel.kt` — `deleteNode()`, `setPendingDelete()` 추가
- `ExplorerScreen.kt` — AlertDialog 추가
- `FileNodeRow.kt` — long-press 트리거

**구현 내용:**
- `StorageRepository`에 `suspend fun deleteNode(node: FileNode): Result<Unit>` 추가
- `StorageRepositoryImpl`: `File(node.path).deleteRecursively()`, 결과를 `Result.success/failure`로 반환
- `DeleteNodeUseCase`: `suspend operator fun invoke(node: FileNode): Result<Unit>`
- ViewModel `deleteNode()`: 성공 시 `displayedChildren`에서 제거, 실패 시 `error` 표시

**테스트 케이스:**
- `DeleteNodeUseCaseTest` (real temp files): 파일 삭제 성공, 디렉토리+하위 삭제, 없는 경로 → `Result.failure`
- ViewModel 테스트 추가: `deleteNode()` 후 `displayedChildren`에서 해당 노드 제거 확인

**커밋:** `test: deletion tests` → `feat: in-app file deletion`

---

### Phase 2B: File Categorization

**생성/수정 파일:**
- `app/src/test/.../domain/FileCategoryTest.kt` ← **먼저 작성**
- `app/src/test/.../domain/CategorizeFilesUseCaseTest.kt` ← **먼저 작성**
- `app/src/main/.../domain/model/FileCategory.kt`
- `app/src/main/.../domain/usecase/CategorizeFilesUseCase.kt`
- `ExplorerViewModel.kt` — 스캔 완료 후 categorize 호출
- `ExplorerScreen.kt` — FilterChipRow 추가

**구현 내용:**
- `FileCategory` enum: `IMAGE(jpg,jpeg,png,gif,webp,bmp,heic,heif)`, `VIDEO(mp4,mkv,avi,mov,wmv,flv,webm,3gp)`, `AUDIO(mp3,aac,flac,ogg,wav,m4a,opus)`, `DOCUMENT(pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md)`, `APK(apk,xapk,apks)`, `ARCHIVE(zip,rar,7z,tar,gz,bz2)`, `OTHER(emptySet())`
  - `companion object { fun of(filename: String): FileCategory }` — 확장자 대소문자 무시
- `CategorizeFilesUseCase`: 리프 노드 재귀 집계 → `Map<FileCategory, Long>`

**테스트 케이스:**
- `FileCategoryTest`: `of("photo.JPG")`→IMAGE, `of("noext")`→OTHER, 대소문자 무관
- `CategorizeFilesUseCaseTest`: 단일 이미지, 혼합 파일, 알 수 없는 확장자→OTHER, 중첩 디렉토리 재귀

**커밋:** `test: categorization tests` → `feat: file categorization`

---

### Phase 2C: View Toggle

**생성/수정 파일:**
- `app/src/test/.../ui/ExplorerViewModelTest.kt` — toggle 테스트 추가 ← **먼저 작성**
- `app/src/androidTest/.../ui/ViewToggleTest.kt` ← **먼저 작성**
- `app/src/main/.../domain/model/ViewMode.kt`
- `ExplorerViewModel.kt` — `toggleViewMode()` 추가
- `ExplorerScreen.kt` — SegmentedButton + 조건부 렌더링

**구현 내용:**
- `ViewMode`: `enum class { LIST, TREEMAP }`
- `ExplorerScreen`: `viewMode`에 따라 `StorageListView` 또는 `TreemapView` 렌더링, `displayedChildren`과 `onNodeClick` 공유

**테스트 케이스:**
- ViewModel: 초기=LIST, `toggleViewMode()`→TREEMAP, 재호출→LIST
- `ViewToggleTest`: 토글 버튼 클릭 후 화면 전환 확인

**커밋:** `test: view toggle tests` → `feat: list/treemap view toggle`

---

### Phase 5A: Splash Screen

**목적:** 권한이 이미 있을 때 Permission 화면이 잠깐 보이는 깜빡임 완전 제거

**생성 파일:**
- `app/src/main/.../ui/splash/SplashViewModel.kt`
- `app/src/main/.../ui/splash/SplashScreen.kt`

**수정 파일:**
- `app/src/main/.../ui/navigation/AppDestination.kt` — `object Splash : AppDestination("splash")` 추가
- `app/src/main/.../ui/navigation/AppNavGraph.kt` — `startDestination = Splash`, Splash 라우트 추가

**구현 내용:**
- `SplashViewModel`: `@HiltViewModel`, `@ApplicationContext` 주입. `init{}`에서 즉시 권한 체크:
  - API 30+: `Environment.isExternalStorageManager()`
  - API 26-29: `ContextCompat.checkSelfPermission(READ_EXTERNAL_STORAGE)`
  - 결과에 따라 `_navTarget: MutableStateFlow<NavTarget?>` 를 `Permission` 또는 `Explorer`로 설정
- `SplashScreen`: 앱 이름 + `CircularProgressIndicator`. `navTarget` 수집 → `LaunchedEffect(navTarget)`으로 즉시 navigate (popUpTo Splash inclusive)
- `AppNavGraph`: Splash → Permission (권한 없음) / Explorer (권한 있음). Permission → Explorer는 기존 popUpTo 유지.

**커밋:** `feat: add splash screen to eliminate permission screen flicker`

---

### Phase 5B: 스캔 1회 실행 + Reload 버튼

**목적:** 설정 → 돌아오기 시 재스캔 방지. Reload 버튼으로 사용자가 명시적으로 재스캔 가능.

**수정 파일:**
- `app/src/main/.../ui/explorer/ExplorerViewModel.kt`
- `app/src/main/.../ui/explorer/ExplorerScreen.kt`

**구현 내용:**
- `ExplorerViewModel`에 `private var scanStarted = false` 추가
- `startScan()` 시작부에 `scanStarted = true` 추가
- `startScanIfNeeded(rootPath: String)`: `if (!scanStarted) startScan(rootPath)`
- `reloadScan()`: `scanStarted = false` 후 `startScan(selectedRoot.path)`
- `reloadInstalledAppsIfPermissionChanged()`: `hasUsageStatsPermission` 값이 이전과 다를 때만 `loadInstalledApps()` 호출 (ON_RESUME 시 불필요한 전체 재로드 방지)
- `ExplorerScreen.kt`:
  - `LaunchedEffect(Unit)`: `startScan` → `startScanIfNeeded`로 변경
  - `DisposableEffect ON_RESUME`: `loadInstalledApps()` → `reloadInstalledAppsIfPermissionChanged()`
  - `TopAppBar` actions에 `Icons.Filled.Refresh` 아이콘 버튼 추가. `enabled = !uiState.isScanning`

**커밋:** `feat: scan once per session with explicit reload button`

---

### Phase 5C: 시작 폴더 선택 (서브폴더 피커)

**목적:** 스캔 시작 전 특정 서브폴더(예: `/0/Downloads`)를 선택해 스캔 범위 제한 가능.

**생성 파일:**
- `app/src/main/.../ui/explorer/FolderPickerSheet.kt`

**수정 파일:**
- `app/src/main/.../ui/explorer/ExplorerUiState.kt` — `showFolderPicker: Boolean = false`, `pickerCurrentPath: String = ""`, `pickerEntries: List<String> = emptyList()` 추가
- `app/src/main/.../ui/explorer/ExplorerViewModel.kt` — picker 관련 메서드 추가
- `app/src/main/.../ui/explorer/ExplorerScreen.kt` — 폴더 아이콘 버튼 + FolderPickerSheet 통합

**구현 내용:**
- `ExplorerViewModel`: `openFolderPicker()`, `closeFolderPicker()`, `navigatePickerInto(path)`, `startScanFromPicker(path)`, `loadPickerEntries(path)` 추가
  - `loadPickerEntries(path)`: `File(path).listFiles()?.filter { it.isDirectory && !it.isHidden }?.sorted()` → `pickerEntries` 업데이트
  - `startScanFromPicker(path)`: 피커 닫고 `scanStarted = false` 후 `startScan(path)`
- `FolderPickerSheet`: `ModalBottomSheet`. 상단 breadcrumb (탭 가능) + 서브디렉토리 `LazyColumn` + "여기서 스캔 시작" `Button`. `@Preview` 포함.
- `ExplorerScreen`: 스캔 중 아닐 때 `Icons.Filled.FolderOpen` 버튼 → `openFolderPicker()`. `uiState.showFolderPicker` 시 `FolderPickerSheet` 표시.

**커밋:** `feat: folder picker sheet for selecting scan start directory`

---

### Phase 5D: Virtual 경로 상위 폴더 이동 수정

**목적:** "설치된 앱" 가상 경로에서 "^" 버튼으로 실제 파일시스템 루트로 이동 가능.

**수정 파일:**
- `app/src/main/.../ui/explorer/ExplorerViewModel.kt`

**구현 내용:**
- `goToParent()` 함수 앞부분에 가상 경로 처리 추가 (기존 파일시스템 로직 앞에 삽입):
  1. `currentPath == VIRTUAL_APPS_PATH`: backStack push → scanRoot children으로 이동 (rootPath로)
  2. `currentPath.startsWith(VIRTUAL_APPS_PATH + "/")`: pkg 하위면 pkg 레벨로, pkg 레벨이면 VIRTUAL_APPS_PATH로 이동
- `findVirtualNode(root: FileNode, path: String): FileNode?` private 헬퍼 추가 (가상 트리 탐색용)

**커밋:** `fix: navigate to fs root from virtual installed-apps path via parent button`

---

### Phase 5E: 정렬 방식 화면 내 드롭다운

**목적:** 정렬 방식 선택을 설정 페이지에서 제거하고 탐색기 화면 내 드롭다운으로 이전.

**수정 파일:**
- `app/src/main/.../ui/explorer/ExplorerUiState.kt` — `sortOrder: SortOrder = SortOrder.SIZE_DESC` 추가
- `app/src/main/.../ui/explorer/ExplorerViewModel.kt` — `setSortOrder(SortOrder)` 추가, `refreshDisplayedChildren()` 끝에 `sortOrder` UiState 반영
- `app/src/main/.../data/preferences/PreferencesRepository.kt` — `suspend fun updateSortOrder(SortOrder)` 확인/추가
- `app/src/main/.../ui/explorer/ExplorerScreen.kt` — `SortOrderDropdown` composable 추가 + `ExplorerContent`에 통합
- `app/src/main/.../ui/settings/SettingsScreen.kt` — sortOrder RadioButton 그룹 제거

**구현 내용:**
- `SortOrderDropdown`: `ExposedDropdownMenuBox` + `OutlinedTextField(readOnly)` + `DropdownMenuItem`s. label 매핑: `SIZE_DESC→"크기 (큰 순)"`, `NAME_ASC→"이름 (가나다순)"`, `DATE_DESC→"날짜 (최신순)"`, `NATURAL_NAME_ASC→"이름 (자연 순서)"`. `@Preview` 포함.
- `ExplorerContent`: `ScanProgressBanner` 아래, `CategoryChipRow` 위에 `SortOrderDropdown` 배치.
- 정렬 변경 → `PreferencesRepository.updateSortOrder()` → `onSettingsChanged()` 자동 호출 → `refreshDisplayedChildren()`

**커밋:** `feat: move sort order selection to in-screen dropdown`

---

### Phase 5F: Multi-Volume StorageStatsManager

**목적:** 모든 스토리지 볼륨의 UUID로 `StorageStatsManager` 쿼리 → OBB/외부 데이터 정확 측정. 원신 등 대형 게임 크기 정확도 개선.

**수정 파일:**
- `app/src/main/.../data/scanner/InstalledAppScanner.kt`

**API 원리:**
| UUID | 포함 항목 |
|------|----------|
| `UUID_DEFAULT` | `/data/data/<pkg>/` (내부 data + cache) |
| 주 외부 볼륨 UUID | `Android/data/<pkg>/` + `Android/obb/<pkg>/` (OBB 포함!) |
| SD카드 UUID | SD카드의 `Android/data/<pkg>/` |

`StorageManager.getUuidForPath(File)` (API 26+)으로 각 볼륨의 UUID 조회.

**구현 내용:**
- `getAllVolumeStats(storageStatsManager, storageManager, packageName): Pair<Long, Long>` private 메서드 추가:
  ```kotlin
  storageManager.storageVolumes.forEach { volume ->
      val dir = volume.directory ?: return@forEach
      try {
          val uuid = storageManager.getUuidForPath(dir)
          val stats = storageStatsManager.queryStatsForPackage(uuid, packageName, Process.myUserHandle())
          totalData += stats.dataBytes
          totalCache += stats.cacheBytes
      } catch (_: Exception) { }
  }
  ```
- `buildVirtualAppsNode()` 내에서:
  - `context.getSystemService(Context.STORAGE_SERVICE)` 로 `storageManager` 취득
  - 기존 `UUID_DEFAULT` 단일 쿼리 블록 → `getAllVolumeStats()` 호출로 교체
  - OBB `walkTopDown` 블록 (line 83-91) 제거
  - 외부 데이터 `walkTopDown` 블록 (line 94-103) 제거
  - `totalSize = apkSize + totalDataSize + totalCacheSize` (obbSize, externalDataSize 제거)
  - 자식 노드: APK / 데이터(`totalDataSize`) / 캐시(`totalCacheSize`) — OBB 노드 제거

**권한:** `PACKAGE_USAGE_STATS`만으로 충분. 없을 때: data/cache = 0 (기존과 동일).

**커밋:** `feat: multi-volume StorageStatsManager for accurate installed app size`
