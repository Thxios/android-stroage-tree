# StorageTree — Handover Document
> 작성일: 2026-04-21 | 완료 phase 이력: `completed_phases.md` 참조

---

## 1. 현재 상태

**모든 계획된 Phase 완료** (0 ~ 7b). 미완료 Phase 없음.

- 단위 테스트 (`./gradlew test`): ✅ 전체 통과
- APK 빌드 (`./gradlew assembleDebug`): ✅ 성공
- Instrumented 테스트: ⚠️ API 37 에뮬레이터에서 Espresso 호환성 문제 → API 34 AVD 사용 권장

---

## 2. 핵심 설계 결정 및 주의사항

### Navigation (ViewModel 내부 백스택)
- `backStack: ArrayDeque<Pair<String, List<FileNode>>>` — (이전 경로, 이전 displayedChildren)
- **`goBack()`**: backStack pop. 루트 복귀 시 `buildRootChildren()`으로 재계산(appsNode 보장).
- **`goToParent()`**: 현재 상태 push → 부모 경로로 이동. 루트 도착 시 `buildRootChildren()` 사용.
- **`navigateTo(node)`**: 현재 상태 push → 자식 노드 표시.
- **`navigateToAncestor(path)`**: 현재 상태 push → breadcrumb 클릭 대상 경로.
- `canGoBack = backStack.isNotEmpty()`. BackHandler가 비활성이면 시스템 back(앱 종료).
- NavController.popBackStack()는 BackHandler에서 호출 금지(상태 오염).

### Virtual Path (설치된 앱)
- `VIRTUAL_APPS_PATH = "virtual://installed-apps"` — 실제 파일시스템 경로 아님.
- `goToParent()`에서 가상 경로 분기 처리 필요 (코드 내 상단 if-block 참조).
- 가상 노드는 삭제 불가. `setPendingDelete()`에서 가상 경로 차단.
- `buildRootChildren()`: scanRoot.children + appsNode(설정 활성 시)를 항상 재계산.

### 앱 목록 스캔 (InstalledAppScanner)
- `QUERY_ALL_PACKAGES` 권한 필수 (Android 11+ 패키지 가시성).
- `getAllVolumeStats()`: 모든 StorageVolume UUID로 queryStatsForPackage 합산 → OBB 포함.
- 자식 노드: APK / 데이터 / 캐시.

### 스캔 플로우
- Explorer 첫 진입 → `openFolderPickerIfNeeded()` → `FolderPickerSheet` 표시.
- 폴더 선택 또는 취소 → `startScan()`. 이후 재진입 시 `scanStarted` 플래그로 재스캔 방지.
- `reloadScan()`: 명시적 Reload(Refresh 버튼). `reloadInstalledAppsIfPermissionChanged()`: ON_RESUME 시 권한 변경 감지.

### 설정 (DataStore)
- `PreferencesRepository`: `AppSettings(showInstalledApps: Boolean, sortOrder: SortOrder)`.
- `ExplorerViewModel.init{}`에서 settings collect → 변경 시 `refreshDisplayedChildren()` 자동 호출.

### Splash
- `SplashViewModel.init{}`에서 동기적 권한 체크 → `NavTarget(Permission|Explorer)`.
- startDestination=Splash. popUpTo Splash inclusive로 백스택에서 제거.

---

## 3. 알려진 이슈

- **Instrumented 테스트**: API 37 에뮬레이터에서 Espresso `InputManager.getInstance()` 실패. API 34 AVD 사용 권장.
- **외부 data 직접 접근**: Android 11+에서 `SecurityException` → 0 처리(multi-volume StorageStatsManager로 우회됨).

---

## 4. 주요 의존성 버전

```toml
agp = "9.1.1"           kotlin = "2.2.10"      ksp = "2.2.10-2.0.2"
hilt = "2.59.2"         composeBom = "2026.02.01"
lifecycleViewModel = "2.9.0"   coroutines = "1.10.2"
mockk = "1.14.0"        turbine = "1.2.0"      datastorePreferences = "1.1.4"
```

SDK: compileSdk 36, minSdk 26, targetSdk 36
