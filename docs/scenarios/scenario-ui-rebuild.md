---
title: 'We-Climb 시안 충실도 UI 재구현'
type: 'feature'
created: '2026-07-24'
status: 'auto-verified'
baseline_commit: 'd81b24d'
test_command: 'cd frontend && ./gradlew :shared:jvmTest :androidApp:testDebugUnitTest :androidApp:connectedDebugAndroidTest'
loop_iteration: 3
---

<frozen-after-approval reason="human-owned contract - only the human changes this after approval">

## Intent

**Problem:** 현재 앱의 Phase 2 미디어 동작은 검증됐지만, Compose 화면은 새
design-bundle의 구조, 상태, 컴포넌트를 충실히 표현하지 못한다. 새 bundle에는
기능이 있는 화면과 아직 비활성인 Phase 3 화면이 함께 있으므로, 동작 경계를
명확히 하지 않으면 시각 재구현 중 기능 범위가 넓어진다.

**Approach:** Galaxy S23의 390dp 앱 내부 콘텐츠를 기준으로 `00`부터 `08`은 기존
동작을 보존한 시안 충실도 Compose 화면으로 재구성한다. `09`부터 `11`은 정적 예시
데이터를 보이는 화면으로만 구현하고, 모든 제품 행동은 비활성으로 유지한다.

## Scenarios

### S1: 공통 상태와 권한 온보딩을 시안 구조로 표시한다

- **Given** 앱이 초기 로딩, 권한 요청 전, 권한 거부, 또는 권한 허용 상태다
- **When** 해당 상태의 온보딩 또는 공통 상태 화면을 연다
- **Then** `00-common`과 `01-onboarding`에 대응하는 로딩, 인라인 상태 배너,
  권한 안내, 설정 열기 또는 시작하기 행동이 각각 접근 가능한 UI 노드로 표시된다
- **Verification:** auto

### S2: 홈은 세션 시작과 영상 아카이브의 실제 진입점을 보인다

- **Given** 홈 화면에 암장 목록과 영상 아카이브 진입점이 있다
- **When** 사용자가 세션 시작 또는 영상 탭을 선택한다
- **Then** `02-home` 구조에서 각각 암장 선택 또는 아카이브에 진입한다
- **Verification:** auto

### S3: 암장 선택과 관리 바텀시트는 기존 저장소 동작을 보존한다

- **Given** 검색 가능한 암장 목록과 개인 암장을 추가할 수 있는 상태다
- **When** 사용자가 `03-gym-select`에서 암장을 추가, 선택, 이름 수정 또는 숨기기를 요청한다
- **Then** 해당 바텀시트 또는 관리 UI가 표시되고 기존 repository의 추가, 선택, 수정,
  숨기기 결과가 화면에 반영된다
- **Verification:** auto

### S4: 세션 보드에서 실제 촬영과 성공 처리 선택 시트를 연결한다

- **Given** 활성 세션과 색상별 성공 Attempt가 있다
- **When** 사용자가 세션 보드에서 촬영을 시작하고 CameraX 녹화를 멈춘다
- **Then** `04-session-board`, `05-capture` 구조로 실제 프리뷰와 녹화 상태가
  표시되고, 성공 또는 실패 분류 뒤 성공 영상은 `자르기`, `나중에`, `원본 유지`
  선택지를 가진 바텀시트로 이어진다
- **Verification:** auto

### S5: 실패 분류는 기존 기록 규칙을 보존한다

- **Given** 녹화한 시도가 성공으로 분류되지 않았다
- **When** 사용자가 `05-capture`의 실패 행동을 선택한다
- **Then** 실패 Attempt와 캐시 정리 규칙은 기존대로 적용되고 세션 보드가 표시된다
- **Verification:** auto

### S6: 세션 종료는 시안의 확인 다이얼로그 뒤에만 실행된다

- **Given** 활성 세션과 정리할 실패 영상이 있다
- **When** 사용자가 `04-session-board`에서 운동 종료를 요청한다
- **Then** 완등, 최고 레벨, 트리밍 대기를 요약한 확인 다이얼로그가 표시되고,
  명시적으로 확정하기 전에는 세션이 종료되지 않는다
- **Verification:** auto

### S7: 트리밍 화면은 실제 미디어 처리 상태를 시안에 맞게 표시한다

- **Given** 읽을 수 있는 원본 URI와 `trimPending` 또는 `trimFailed` 성공 Attempt가 있다
- **When** 사용자가 트리밍을 열고 유효하거나 유효하지 않은 범위를 제출한다
- **Then** `06-trim`의 실제 Media3 프리뷰, 범위 선택, 처리 중 중복 실행 차단,
  오류와 재시도, 원본 유지, 나중에, 공유 행동이 표시되고 기존 Attempt 상태 전이는
  유지된다
- **Verification:** auto

### S8: 아카이브와 재생 오버레이는 실제 영상 상태를 보존한다

- **Given** 정상, 트리밍 대기, 원본 유지, 또는 읽기 불가 상태의 성공 Attempt가 있다
- **When** 사용자가 `07-archive`에서 영상을 열거나 재생과 공유를 선택한다
- **Then** 카드의 상태 라벨과 접근 가능한 행동이 표시되고, 재생 가능한 URI는
  `08-playback` Media3 오버레이로 전달되며 읽기 불가 기록은 삭제되지 않는다
- **Verification:** auto

### S9: 오류와 처리 결과는 화면 안의 상태 배너로 안내한다

- **Given** 카메라 바인딩, 저장, 트리밍, 공유 또는 영상 읽기에서 오류나 성공 결과가 있다
- **When** 해당 비동기 작업이 끝난다
- **Then** `00-common`의 인라인 배너가 원인과 가능한 재시도 행동을 표시하며,
  기존 원본 보존과 재시도 경로는 유지된다
- **Verification:** auto

### S10: 새 시안의 예시 데이터는 아직 제품 기능을 만들지 않는다

- **Given** 홈 최근 영상, 아카이브 필터와 썸네일, 레벨 표기가 있는 화면이다
- **When** 사용자가 이 시각 요소를 본다
- **Then** 해당 값은 예시 데이터 또는 비활성 표기로만 나타나고 새 query, 필터,
  썸네일 생성, 색상-레벨 매핑은 호출되지 않는다
- **Verification:** auto

### S11: Phase 3 화면은 정적 UI로만 열리고 행동하지 않는다

- **Given** 사용자가 `09-session-end`, `10-report`, `11-records`의 화면 진입점을 연다
- **When** 정적 예시 데이터와 `Phase 3, 비활성` 뱃지가 있는 화면을 본다
- **Then** 화면은 시안과 동일한 구조로 표시되지만 저장, 조회, 공유, 통계 필터,
  세션 종료 후처리 행동은 실행되지 않는다
- **Verification:** auto

### S12: 모든 화면은 시안 구현을 검증할 수 있는 의미론적 표면을 제공한다

- **Given** `00`부터 `11`의 각 승인된 상태가 테스트 데이터로 재현 가능하다
- **When** instrumentation 테스트가 각 화면과 상태를 연다
- **Then** 화면, 핵심 CTA, 상태 배너, 바텀시트, 다이얼로그, 플레이어 영역은
  안정적인 접근성 라벨 또는 test tag로 식별 가능하며 390dp 기준 스크린샷을 캡처할 수 있다
- **Verification:** auto

### S13: Galaxy S23에서 시안 충실도와 네이티브 미디어 영역을 사람이 확인한다

- **Given** S1부터 S12의 자동 검증을 통과한 debug 앱과 Galaxy S23이 있다
- **When** 사용자가 새 bundle의 `00`부터 `11` 상태를 기준 스크린샷과 나란히 보고,
  CameraX 촬영, Media3 트리밍과 재생, 접근 가능한 주요 터치 행동을 수행한다
- **Then** 앱 내부 콘텐츠가 390dp 시안의 구조, 여백, 서체, 색, 상태 배치와 일치하고
  실제 CameraX와 Media3 영역이 그 레이아웃 안에서 동작한다
- **Verification:** manual - 화면의 시각 일치와 실제 카메라 프리뷰의 구도는 자동
  테스트로 완전히 판정할 수 없다. S4, S7, S8, S12가 자동화 가능한 상태 전이와
  UI 표면을 먼저 검증한다.

### S14: 색 탭 완등은 영상 없는 성공 Attempt로 기록한다

- **Given** 활성 세션의 `04-session-board`에 색상별 완등 행이 표시된다
- **When** 사용자가 색상 행을 탭해 완등을 기록한다
- **Then** 해당 색의 `SUCCESS` Attempt가 영상 URI와 캐시 경로 없이 `NONE` 미디어
  상태로 저장되고, 세션 보드의 색상별 수와 완등 요약이 즉시 갱신된다
- **Verification:** auto

## Out of Scope

- 트리밍을 앱 종료 뒤에도 완료하는 WorkManager 또는 foreground service 처리
- 기존 Phase 1 설치본의 Room v1 영상 상태 마이그레이션
- 세션 종료 사진, 리포트 생성과 저장, 통계 조회와 필터, 리포트 기반 공유의 기능 구현
- 홈 최근 영상, 아카이브 필터와 썸네일, 색상-레벨 매핑을 위한 데이터 모델 또는 query
- `09`부터 `11`의 정적 화면을 넘어선 제품 행동
- HTML의 가짜 기기 프레임과 가짜 상태바 구현

</frozen-after-approval>

## Coverage Map

| Scenario | Verification | Status | Test | Implementation |
|----------|--------------|--------|------|----------------|
| S1 | auto | verified | `UiRebuildInstrumentationTest`, `SessionLoopServiceTest` | loading, permission request/denial/grant, settings card and status banners |
| S2 | auto | verified | `UiRebuildInstrumentationTest` | `HomeUi`, `BottomTabs` |
| S3 | auto | verified | `UiRebuildInstrumentationTest`, `SessionLoopServiceTest` | search, add-and-select, rename and hide persist through Room |
| S4 | auto | verified | `CameraRecordingFlowInstrumentationTest` | `BoardUi`, `CaptureUi`, `MediaChoiceSheet` |
| S5 | auto | verified | `CameraRecordingFlowInstrumentationTest`, `AttemptServiceTest` | failed Attempt persists, then its cache and record are removed on confirmed end |
| S6 | auto | verified | `UiRebuildInstrumentationTest`, `AttemptServiceTest` | active session survives dismissal and ends only after explicit confirmation |
| S7 | auto | verified | `CameraRecordingFlowInstrumentationTest`, `AndroidEditListTrimGatewayTest` | validation, interrupted recovery, completion state and result actions |
| S8 | auto | verified | `ArchivePlaybackInstrumentationTest`, `CameraRecordingFlowInstrumentationTest` | `ArchiveUi`, `PlaybackOverlay` |
| S9 | auto | verified | `UiRebuildInstrumentationTest`, `AttemptServiceTest` | success/error banners, retry surface and save retry domain path |
| S10 | auto | verified | `UiRebuildInstrumentationTest` | archive filters remain non-clickable example data |
| S11 | auto | verified | `UiRebuildInstrumentationTest` | disabled Phase 3 actions leave state and repository unchanged |
| S12 | auto | verified | `UiRebuildInstrumentationTest` | all approved states open on the 390dp catalog surface and produce screenshots |
| S13 | manual | deferred-follow-up-goal | walkthrough in Verification Log | Galaxy S23 visual/native-media review |
| S14 | auto | verified | `UiRebuildInstrumentationTest`, `AttemptServiceTest`, `RoomSessionLoopRepositoryTest` | color tap saves and restores a video-less `SUCCESS` / `NONE` Attempt |

## Verification Log

- 2026-07-27: `cd frontend && ./gradlew :androidApp:compileDebugAndroidTestKotlin :androidApp:testDebugUnitTest :shared:jvmTest :androidApp:assembleDebug` passed. The suite and APK compile, including the UI reconstruction tests.
- 2026-07-27: after the S3 search and add-sheet correction, `cd frontend && ./gradlew :androidApp:compileDebugKotlin :androidApp:compileDebugAndroidTestKotlin :androidApp:testDebugUnitTest :shared:jvmTest` passed and `git diff --check` was clean.
- 2026-07-27: no ADB device was attached (`adb devices -l` returned no devices), so `connectedDebugAndroidTest` has not been rerun after this UI change. All `ready-device` rows require that execution before they become covered.
- 2026-07-27: S13 is intentionally deferred by the active goal. It is not a completion claim for the Galaxy S23 visual or native-media walkthrough.
- 2026-07-27: the frozen command `cd frontend && ./gradlew :shared:jvmTest :androidApp:testDebugUnitTest :androidApp:connectedDebugAndroidTest` passed on SM-S911N (Galaxy S23). The connected suite finished 13 tests with 0 failures. S1–S12 are covered by that run and their mapped unit tests.
- 2026-07-29: the UI was re-audited against `design-bundle/00-common.html` through
  `11-records.html` on a reproducible 390dp debug catalog. The board proportion fix
  was also rebuilt, installed and captured on SM-S911N.
- 2026-07-29: the frozen command passed again after the fidelity rebuild and
  behavior corrections. `shared:jvmTest`, Android unit tests, and 15 connected
  instrumentation tests on SM-S911N completed with 0 failures. This run directly
  covers S3 add-and-select, S5 cleanup, S6 confirmation, S7 completion retention,
  S10–S12 static/catalog guards, and S14 video-less success.

## Review Findings

- 2026-07-27 `missing_scenario` resolved by the human: direct color-tap completion creates a video-less `SUCCESS` Attempt with `NONE` media. It became S14.
- 2026-07-29 `patch`: S3 add-and-select and S7 trim completion retention are implemented and directly covered.
- 2026-07-29 `patch`: the S1, S3, S5–S7, S9–S12 hollow coverage findings are replaced by state, repository, screenshot, CameraX and Media3 evidence.
- 2026-07-29 `patch`: cold-read review removed the accidental color-to-level calculation and keeps `Lv.x` as ordered preview data; selected personal gyms now synchronize after rename and hide.
- 2026-07-27 `defer`: playback control hide/show on tap is not frozen in S8. Do not implement until it is explicitly contracted.

## Notes

- 기준 시안: `design-bundle/00-common.html`부터 `design-bundle/11-records.html`
- 구현 기준: `design-system/we-climb/MASTER.md`의 `Stack Adaptation`과 `UI 폴리시`
- 현재 구현은 `frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt`의
  `Screen`과 `AppState`에서 시작한다. UI 재구현에서 화면을 분리할 수 있지만,
  기존 미디어 상태와 repository 계약을 바꾸지 않는다.
