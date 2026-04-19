# CLAUDE.md — StorageTree Project Guidelines

이 파일은 모든 sub-agent가 작업 시작 전 반드시 읽어야 하는 공통 지침입니다.

## Project Overview
TreeSizeFree의 핵심 기능을 Android로 이식하는 스토리지 분석 앱.
- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + UDF
- **DI**: Dagger Hilt (`hilt = "2.56.1"`)
- **Async**: Kotlin Coroutines & Flow
- **Build**: Gradle Kotlin DSL + KSP (`ksp = "2.2.10-1.0.29"`)
- **SDK**: compileSdk 36, minSdk 26, Kotlin 2.2.10, AGP 9.1.1, Compose BOM 2026.02.01

## Sub-agent 작업 원칙

### 1. 작업 전 필수 확인
- 이 파일(`CLAUDE.md`)을 반드시 읽을 것
- `plan.md`를 읽고 자신이 담당하는 Phase를 확인할 것
- `checklists.json`을 읽고 선행 Phase가 완료되었는지 확인할 것

### 2. TDD (Test-Driven Development) — 필수
- **비즈니스 로직 구현 전 반드시 테스트 코드를 먼저 작성**
- 단위 테스트: `app/src/test/` 디렉토리
- UI/Instrumented 테스트: `app/src/androidTest/` 디렉토리
- 테스트가 실패하는 것을 확인한 후 구현 코드 작성

### 3. Git Commit 규칙
커밋 메시지 접두사: `feat:` / `fix:` / `refactor:` / `test:` / `chore:`
의미 있는 단위로 커밋을 분리할 것 (테스트 커밋 + 구현 커밋 분리 권장).

### 4. Compose @Preview 필수
모든 `@Composable` 함수에 `@Preview` 함수를 함께 작성할 것.

### 5. Checklist 업데이트
각 Phase 완료 시 `checklists.json`의 해당 phase `status`를 `"completed"`, `completedAt`을 오늘 날짜로 업데이트 후 커밋:
`chore: update checklist - phase-XX completed`

## Package Structure
```
com.thxios.storagetree/
├── StorageTreeApplication.kt
├── MainActivity.kt
├── domain/model/          FileNode, ScanState, FileCategory, ViewMode
├── domain/repository/     StorageRepository (interface)
├── domain/usecase/        ScanDirectoryUseCase, DeleteNodeUseCase, CategorizeFilesUseCase
├── data/repository/       StorageRepositoryImpl
├── data/scanner/          FileScanner, FileSizeFormatter
├── di/                    StorageModule
└── ui/
    ├── theme/             (기존 유지, 수정 금지)
    ├── navigation/        AppDestination, AppNavGraph
    ├── permission/        PermissionScreen, PermissionViewModel
    ├── explorer/          ExplorerScreen, ExplorerViewModel, ExplorerUiState
    │   ├── listview/      StorageListView, FileNodeRow
    │   └── treemap/       TreemapView, SquarifyAlgorithm
    └── components/        ScanProgressBanner, SizeProgressBar, ErrorBanner
```

## Key Design Decisions
- **파일 시스템**: `java.io.File` 사용 (NIO 아님) — `listFiles()` null 반환으로 권한 오류 처리
- **드릴다운 내비게이션**: ViewModel 내 `ArrayDeque<FileNode>` 백스택 (NavController 미사용)
- **스캔 진행**: `Flow<ScanState>` 에서 `Scanning(currentPath)` emit으로 실시간 표시
- **Treemap**: 외부 라이브러리 없이 순수 Kotlin으로 Squarified 알고리즘 구현

## Build & Test Commands (PowerShell)
```powershell
./gradlew test                   # 단위 테스트 (에뮬레이터 불필요)
./gradlew assembleDebug          # APK 빌드 확인
adb devices                      # 연결 기기 확인
./gradlew connectedAndroidTest   # Instrumented 테스트 (에뮬레이터/실기기 필요)
emulator -list-avds              # AVD 목록
emulator -avd <avd_name>         # 에뮬레이터 실행
```
