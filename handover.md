# StorageTree — Handover Document
> 작성일: 2026-04-20 | 다음 agent가 이 파일만으로 작업을 재개할 수 있도록 작성됨

---

## 1. 현재 상태 요약

**plan.md의 모든 Phase(0 ~ 2C) 구현 완료.**
남은 작업은 **Instrumented 테스트 실행 실패 문제 해결** 뿐이다.

- 단위 테스트 (`./gradlew test`): ✅ 전체 통과
- APK 빌드 (`./gradlew assembleDebug`): ✅ 성공
- Instrumented 테스트 (`./gradlew connectedAndroidTest`): ❌ 실패 (아래 원인 참고)

---

## 2. Instrumented 테스트 실패 원인 및 현황

### 원인
사용자의 에뮬레이터가 **Pixel_9_Pro AVD (API 37, Android 17)**이다.  
Espresso(`espresso-core:3.6.1`)는 Android 15(API 35)에서 제거된 `InputManager.getInstance()`를 reflection으로 호출하는데, API 37에서도 동일하게 실패한다.

에러 메시지:
```
java.lang.NoSuchMethodException: android.hardware.input.InputManager.getInstance []
at androidx.test.espresso.base.InputManagerEventInjectionStrategy.initialize
```

### 이미 시도한 것
- `espressoCore = "3.6.1"` (3.5.1 → 3.6.1) 업그레이드 → 미해결
- `junitVersion = "1.2.1"`, `androidTestRunner = "1.6.2"` 추가 → 미해결
- `testInstrumentationRunnerArguments["shellInputCommandInjection"] = "true"` 설정 → 미해결
- `testOptions { animationsDisabled = true }` 설정 → 미해결
- `./gradlew clean` 시도 → build 디렉토리 잠김으로 실패 (Android Studio 프로세스가 파일 점유)

### 현재 테스트 파일 목록 (androidTest)
```
app/src/androidTest/
├── HiltTestRunner.kt               — HiltTestApplication 주입용 커스텀 러너
├── di/
│   ├── FakeStorageRepository.kt   — 테스트용 Fake (scan → Done, delete → success)
│   └── TestStorageModule.kt       — @TestInstallIn으로 StorageModule 대체
└── ui/
    ├── NavigationTest.kt           — @HiltAndroidTest, MainActivity 실행, PermissionScreen 확인
    ├── PermissionScreenTest.kt     — 권한 화면 UI 테스트
    ├── StorageListViewTest.kt      — 리스트 렌더링 및 클릭 테스트
    ├── TreemapViewTest.kt          — Treemap 렌더링 및 클릭 테스트
    └── ViewToggleTest.kt           — 뷰 모드 토글 테스트
```

---

## 3. 다음 agent가 해야 할 일

### Task 1: Instrumented 테스트 통과시키기

**우선 시도: API 34 AVD 사용**
- 사용자에게 API 34 AVD를 생성하도록 요청하거나, 이미 생성되어 있는지 확인:
  ```
  emulator -list-avds
  adb devices
  adb shell getprop ro.build.version.sdk
  ```
- API 34 AVD가 있다면 `./gradlew connectedAndroidTest`로 바로 실행

**대안: 코드 레벨 우회 (API 37 에뮬레이터 유지 시)**

Espresso의 `InputManagerEventInjectionStrategy`를 우회하는 방법:

옵션 A — `UiAutomation` 기반으로 전환 (HiltTestRunner 수정):
```kotlin
override fun onCreate(arguments: Bundle) {
    // Espresso가 InputManager 대신 UiAutomation을 사용하도록 강제
    arguments.putString("clearPackageData", "false")
    super.onCreate(arguments)
}
```

옵션 B — Espresso 최신 스냅샷 버전 사용:
```toml
espressoCore = "3.7.0-alpha01"  # 또는 최신 alpha/beta
```
Maven Central에서 `androidx.test.espresso:espresso-core`의 최신 버전 확인 후 적용.

옵션 C — `createComposeRule()` 대신 Espresso 의존 없는 방식:
일부 테스트를 Compose의 `runComposeUiTest { }` (Espresso 없이 동작) 블록으로 재작성.
단, `createAndroidComposeRule<Activity>`는 Espresso 필수 → NavigationTest는 예외.

### Task 2: 이슈 없을 시 최종 정리
- 모든 instrumented 테스트 통과 확인
- `git status` 확인하여 미커밋 파일 없는지 점검
- 필요 시 deprecated icon 경고 수정:
  - `FileNodeRow.kt`: `Icons.Filled.InsertDriveFile` → `Icons.AutoMirrored.Filled.InsertDriveFile`
  - `ExplorerScreen.kt`: `Icons.Filled.ViewList` → `Icons.AutoMirrored.Filled.ViewList`

---

## 4. 전체 구현된 파일 구조

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
│   ├── FileScanner.kt                  — Flow<ScanState>, java.io.File 재귀
│   └── FileSizeFormatter.kt            — bytes → "4.2 GB"
├── data/repository/
│   └── StorageRepositoryImpl.kt        — FileScanner 위임, deleteRecursively()
├── di/
│   └── StorageModule.kt                — @Binds StorageRepository
└── ui/
    ├── navigation/
    │   ├── AppDestination.kt           — sealed class (Permission, Explorer)
    │   └── AppNavGraph.kt              — NavHost, popUpTo Permission inclusive
    ├── permission/
    │   ├── PermissionViewModel.kt      — hasPermission StateFlow
    │   └── PermissionScreen.kt
    ├── explorer/
    │   ├── ExplorerUiState.kt          — data class (currentPath, displayedChildren, scanState, ...)
    │   ├── ExplorerViewModel.kt        — startScan, navigateTo, navigateUp, toggleViewMode,
    │   │                                  setPendingDelete, deleteNode, categorizeUseCase 호출
    │   ├── ExplorerScreen.kt           — AlertDialog(삭제), CategoryChipRow, LIST/TREEMAP 조건부 렌더링
    │   ├── listview/
    │   │   ├── FileNodeRow.kt          — combinedClickable (onClick + onLongClick)
    │   │   └── StorageListView.kt      — LazyColumn
    │   └── treemap/
    │       ├── TreemapRect.kt          — data class (node, left, top, right, bottom)
    │       ├── SquarifyAlgorithm.kt    — pure Kotlin Squarified 알고리즘
    │       └── TreemapView.kt          — Canvas + pointerInput 클릭
    └── components/
        ├── ScanProgressBanner.kt
        ├── SizeProgressBar.kt
        └── ErrorBanner.kt
```

---

## 5. 주요 의존성 버전 (libs.versions.toml 현재 상태)

```toml
agp = "9.1.1"
kotlin = "2.2.10"
ksp = "2.2.10-2.0.2"
hilt = "2.59.2"
composeBom = "2026.02.01"
espressoCore = "3.6.1"
junitVersion = "1.2.1"
androidTestRunner = "1.6.2"
mockk = "1.14.0"
turbine = "1.2.0"
coroutines = "1.10.2"
```

---

## 6. 알려진 경고 (에러 아님)

- `Icons.Filled.InsertDriveFile` deprecated → `Icons.AutoMirrored.Filled.InsertDriveFile` 권장
- `Icons.Filled.ViewList` deprecated → `Icons.AutoMirrored.Filled.ViewList` 권장
- `./gradlew clean` 실패: Android Studio가 build 디렉토리를 점유 중일 때 발생. Android Studio를 닫고 실행하면 해결됨.

---

## 7. 참고: Hilt 테스트 구조

- `HiltTestRunner` (`androidTest/`): `AndroidJUnitRunner`를 상속, `HiltTestApplication` 주입
- `TestStorageModule` (`androidTest/di/`): `@TestInstallIn(replaces = [StorageModule::class])` — 모든 androidTest에서 자동으로 `FakeStorageRepository` 사용
- `FakeStorageRepository`: `scan()` → 즉시 `Done(빈 root)` emit, `deleteNode()` → `Result.success`
- `@HiltAndroidTest` 테스트는 `HiltAndroidRule`을 `@get:Rule(order = 0)`으로 선언해야 함
