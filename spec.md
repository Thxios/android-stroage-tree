# Project Specification: Android Storage Analyzer (TreeSizeFree for Android)

## 1. Project Overview
TreeSizeFree의 핵심 기능을 안드로이드로 이식하는 스토리지 분석 앱 개발 프로젝트입니다. 현대적인 안드로이드 기술 스택을 활용하며, 데이터 기반의 체계적인 스토리지 관리 기능을 제공합니다.

## 2. Technical Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Declarative UI)
- **Architecture**: MVVM (Model-View-ViewModel) + UDF (Unidirectional Data Flow)
- **Async & Data Stream**: Kotlin Coroutines & Flow
- **Dependency Injection**: Dagger Hilt
- **Build System**: Gradle Kotlin DSL (build.gradle.kts) + KSP
- **Permission**: `MANAGE_EXTERNAL_STORAGE` (Targeting Personal Use)

## 3. Core Features (Phase 1 & 2)

### Phase 1: Storage Scanning & Visualization
- **Recursive Scanning**: 루트 디렉토리부터 하위 폴더까지 재귀적으로 스캔하여 용량 계산.
- **TreeSize Style Layout (Default)**:
    - 계층형 리스트 구조.
    - 폴더명, 용량, 전체 대비 비율(%), 프로그레스 바 표시.
    - 용량 큰 순서대로 자동 정렬.
- **Alternative Visualization (Treemap)**:
    - Jetpack Compose Canvas를 이용한 사각형 분할 방식의 시각화.
- **Navigation**: 리스트 또는 차트 클릭 시 해당 폴더 내부로 드릴다운(Drill-down).

### Phase 2: Management & Filtering
- **File Management**: 앱 내에서 즉각적인 파일/폴더 삭제 기능 (Permission 확인 필수).
- **Categorization**: 파일 확장자 기반 카테고리(이미지, 영상, 문서, APK 등) 분석.
- **UI Customization**: TreeSize 스타일 리스트와 Treemap 간의 전환 토글 제공.

## 4. Development Guidelines

### Git Workflow
- 모든 기능 개발은 Git을 기반으로 관리함.
- **Commit Message**: `feat:`, `fix:`, `refactor:`, `test:` 등의 접두사 사용.
- 의미 있는 단위로 커밋을 쪼개어 작업 이력을 남김.

### Test-Driven Development (TDD)
- **Rule**: 기능 비즈니스 로직 작성 전, 해당 기능을 검증하는 테스트 코드를 먼저 작성함.
- **Unit Tests**: 파일 용량 계산 로직, 정렬 알고리즘, 카테고리 분류 로직 검증.
- **UI Tests**: Compose Test Rule을 사용하여 UI 컴포넌트(리스트, 차트)의 렌더링 검증.

## 5. UI/UX Requirements
- **Standardized UI**: TreeSizeFree의 윈도우 인터페이스를 안드로이드 머티리얼 디자인 3(Material 3) 스타일로 재해석.
- **Performance**: 파일 스캔 시 UI가 프리징되지 않도록 상태(Flow) 업데이트 최적화.
- **Real-time Feedback**: 스캔 중 현재 진행 상태(스캔 중인 경로 등)를 사용자에게 표시.

## 6. Implementation Notes for Claude Code
- 파일 시스템 접근 시 `java.io.File` 또는 `java.nio.file` 라이브러리의 성능을 고려하여 최적의 방식을 제안할 것.
- `MANAGE_EXTERNAL_STORAGE` 권한 획득을 위한 시스템 인텐트 호출 로직을 포함할 것.
- 모든 UI 컴포넌트는 Preview를 지원하도록 작성할 것.
