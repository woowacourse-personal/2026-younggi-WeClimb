---
title: 'We-Climb 시안 충실도 UI 재구현'
type: 'feature'
created: '2026-07-24'
status: 'verifying'
baseline_commit: 'a4148c3'
test_command: 'cd frontend && ./gradlew :shared:jvmTest :androidApp:testDebugUnitTest :androidApp:connectedDebugAndroidTest'
loop_iteration: 6
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

### S6: 세션 종료는 실제 세션 요약을 보여준 확인 다이얼로그 뒤에만 실행된다

- **Given** 활성 세션에 성공, 실패, 미분류 또는 정리가 필요한 영상 Attempt가 있다
- **When** 사용자가 `04-session-board`에서 운동 종료를 요청한다
- **Then** 완등 수, 미분류를 포함한 전체 시도 수, `TRIM_PENDING`, `TRIM_FAILED`,
  `TRIM_PROCESSING`을 합친 정리 필요 수를 요약한 확인 다이얼로그가 표시되고,
  존재하지 않는 레벨 값은 표시하지 않으며 명시적으로 확정하기 전에는 세션이 종료되지 않는다
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

### S15: 촬영 화면에서 선택한 홀드 색상으로 시도를 분류한다

- **Given** 활성 세션의 촬영 화면에 선택 가능한 홀드 색상이 표시된다
- **When** 사용자가 홀드 색상을 바꾸고 녹화한 영상을 성공 또는 실패로 분류한다
- **Then** 분류된 Attempt는 사용자가 선택한 색상을 가지며 선택하지 않은 기본 색상으로
  고정되지 않는다
- **Verification:** auto

### S16: 시스템 뒤로가기는 진행 중인 녹화를 종료하고 분류 단계로 이동한다

- **Given** CameraX 녹화가 진행 중인 촬영 화면이다
- **When** 사용자가 Android 시스템 뒤로가기 버튼 또는 제스처를 실행한다
- **Then** 녹화가 정상 종료되고 완성된 영상은 캐시에 유지되며 촬영 화면을 벗어나지
  않은 채 성공 또는 실패 분류 UI가 표시된다
- **Verification:** auto

### S17: 분류 전 뒤로가기는 녹화한 시도를 미분류 상태로 보관한다

- **Given** 녹화를 마쳐 캐시 영상이 있고 아직 홀드 색상과 성공 또는 실패를 확정하지
  않은 분류 화면이다
- **When** 사용자가 Android 시스템 뒤로가기 버튼 또는 제스처를 실행한다
- **Then** 영상과 Attempt는 `UNCLASSIFIED` 상태로 저장되고 세션 보드로 돌아가며,
  해당 Attempt는 완등 또는 실패 수에 포함되지 않는다
- **Verification:** auto

### S18: 트리밍 시작 전 뒤로가기는 작업을 나중으로 미룬다

- **Given** `trimPending` 또는 `trimFailed` 성공 Attempt의 트리밍 화면에서 변환을
  시작하지 않았다
- **When** 사용자가 뒤로가기를 실행한다
- **Then** 원본 영상과 Attempt는 보존되고 미디어 상태는 `TRIM_PENDING`으로 저장되며
  트리밍 화면을 벗어난다
- **Verification:** auto

### S19: 트리밍 처리 중 뒤로가기는 변환을 취소하고 작업을 나중으로 미룬다

- **Given** Media3 Transformer가 성공 Attempt의 트리밍 결과를 생성하고 있다
- **When** 사용자가 뒤로가기를 실행한다
- **Then** 실행 중인 변환이 취소되고 부분 결과가 제거되며 원본 영상은 보존되고
  Attempt의 미디어 상태는 `TRIM_PENDING`으로 저장된 뒤 트리밍 화면을 벗어난다
- **Verification:** auto

### S20: 아카이브에서 미분류 시도를 다시 열어 분류한다

- **Given** 활성 또는 종료된 세션에 캐시 영상이 보존된 `UNCLASSIFIED` Attempt가 있다
- **When** 사용자가 아카이브의 `분류 필요` 항목을 열고 홀드 색상과 성공 또는 실패를
  선택한다
- **Then** 성공이면 영상을 MediaStore에 저장한 성공 Attempt로, 실패면 캐시 영상을
  제거한 실패 Attempt로 한 번만 전이되고 아카이브와 세션 요약에 결과가 반영된다
- **Verification:** auto

### S21: 분류 입력은 최초 선택만 처리한다

- **Given** 녹화를 마친 시도의 색상과 성공 또는 실패를 선택할 수 있는 화면이다
- **When** 사용자가 저장 완료 전에 같은 분류 버튼을 반복해서 누르거나 성공과 실패를
  연속해서 누른다
- **Then** 최초 입력 뒤 분류 행동이 비활성화되고 하나의 Attempt와 하나의 결과만 저장된다
- **Verification:** auto

### S22: 미디어 선택 시트를 닫으면 트리밍을 나중으로 미룬다

- **Given** 성공 영상을 저장한 뒤 `지금 자르기`, `나중에`, `원본 그대로`를 고르는
  미디어 선택 바텀시트가 표시된다
- **When** 사용자가 시스템 뒤로가기, 바깥 영역 누르기 또는 아래로 밀기로 시트를 닫는다
- **Then** `나중에`를 선택한 것과 동일하게 원본은 보존되고 미디어 상태는
  `TRIM_PENDING`으로 저장된다
- **Verification:** auto

### S23: 저장 대기 영상이 있으면 해결하기 전까지 세션 종료를 보류한다

- **Given** 성공으로 분류했지만 MediaStore 저장에 실패해 캐시 영상이 남은
  `SAVE_PENDING` Attempt가 있다
- **When** 사용자가 세션 종료를 확정하려 한다
- **Then** 세션은 종료되지 않고 저장 재시도와 `영상만 폐기하고 기록 유지` 행동이
  표시되며, 재시도 성공 또는 명시적 폐기로 Attempt가 성공 상태가 된 뒤에만 종료할 수 있다
- **Verification:** auto

### S24: 정리가 필요한 모든 트리밍 상태를 종료 요약에 합산한다

- **Given** 활성 세션에 `TRIM_PENDING`, `TRIM_FAILED`, `TRIM_PROCESSING` 상태의 영상이
  각각 있다
- **When** 사용자가 세션 종료 확인 다이얼로그를 연다
- **Then** 세 상태의 Attempt가 모두 `정리 필요` 수에 포함되고, 원본 유지 또는 트리밍
  완료 상태는 포함되지 않는다
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
| S1 | auto | pending | `UiRebuildInstrumentationTest`, `SessionLoopServiceTest` (partial) | permission request and app-settings actions still need decisive proof |
| S2 | auto | verified | `UiRebuildInstrumentationTest`, `ArchivePlaybackInstrumentationTest` | `HomeUi`, `BottomTabs` |
| S3 | auto | verified | `UiRebuildInstrumentationTest`, `SessionLoopServiceTest` | search, add-and-select, rename and hide persist through Room |
| S4 | auto | verified | `CameraRecordingFlowInstrumentationTest` | `BoardUi`, `CaptureUi`, `MediaChoiceSheet` |
| S5 | auto | verified | `CameraRecordingFlowInstrumentationTest`, `AttemptServiceTest` | failed Attempt persists, then its cache and record are removed on confirmed end |
| S6 | auto | covered | [focused device test](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/UiRebuildInstrumentationTest.kt#L248), [summary unit test](../../shared/src/commonTest/kotlin/com/weclimb/session/AttemptSummaryTest.kt#L8) | [summary](../../shared/src/commonMain/kotlin/com/weclimb/session/AttemptSummary.kt#L11), [dialog](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/ArchivePlaybackUi.kt#L342) |
| S7 | auto | pending | `CameraRecordingFlowInstrumentationTest`, `AndroidEditListTrimGatewayTest` (partial) | invalid submission and failed retry still need integrated state-transition proof |
| S8 | auto | pending | `ArchivePlaybackInstrumentationTest`, `CameraRecordingFlowInstrumentationTest` (partial) | exact playback URI and every promised card state label still need decisive proof |
| S9 | auto | pending | `UiRebuildInstrumentationTest`, `AttemptServiceTest`, `AttemptSavePresentationTest` (partial) | save retry is covered; camera, share and unreadable-media error severity and retry remain |
| S10 | auto | pending | `UiRebuildInstrumentationTest` (partial) | inert presentation is checked; absence of query, thumbnail and level-mapping work still needs proof |
| S11 | auto | verified | `UiRebuildInstrumentationTest` | disabled Phase 3 actions leave state and repository unchanged |
| S12 | auto | verified | `UiRebuildInstrumentationTest` | all approved states open on the 390dp catalog surface and produce screenshots |
| S13 | manual | verified | walkthrough in Verification Log | Galaxy S23 visual/native-media review |
| S14 | auto | verified | `UiRebuildInstrumentationTest`, `AttemptServiceTest`, `RoomSessionLoopRepositoryTest` | color tap saves and restores a video-less `SUCCESS` / `NONE` Attempt |
| S15 | auto | covered | `CameraRecordingFlowInstrumentationTest` | `CaptureUi` color selector and success/failure classification |
| S16 | auto | verified | `CameraRecordingFlowInstrumentationTest` | recording `BackHandler` stops CameraX and preserves the completed cache file |
| S17 | auto | covered | [focused device test](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt#L165), [domain test](../../shared/src/commonTest/kotlin/com/weclimb/session/AttemptServiceTest.kt#L70) | [capture Back](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L462), [unclassified record](../../shared/src/commonMain/kotlin/com/weclimb/session/AttemptService.kt#L40) |
| S18 | auto | verified | `CameraRecordingFlowInstrumentationTest`, `AttemptMediaServiceTest` | idle trim Back defers to `TRIM_PENDING` and preserves the original URI |
| S19 | auto | covered | `UiRebuildInstrumentationTest`, `TrimExportCoordinatorTest`, `AttemptMediaServiceTest` | processing Back leaves Trim, cancels export, removes partial output and restores `TRIM_PENDING` |
| S20 | auto | covered | [focused device test](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/ArchivePlaybackInstrumentationTest.kt#L75), [domain test](../../shared/src/commonTest/kotlin/com/weclimb/session/AttemptServiceTest.kt#L86) | [archive query](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/SessionLoopRoom.kt#L103), [classification entry](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L230) |
| S21 | auto | covered | [focused device test](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/UiRebuildInstrumentationTest.kt#L365), [state test](../../frontend/androidApp/src/test/kotlin/com/weclimb/android/ClassificationStateTest.kt#L11) | [classification guard](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/AppState.kt#L66), [disabled actions](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/CaptureTrimUi.kt#L238) |
| S22 | auto | covered | [focused device test](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt#L180), [dismiss race test](../../frontend/androidApp/src/test/kotlin/com/weclimb/android/AndroidEditListTrimGatewayTest.kt#L84) | [sheet dismissal](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/RebuiltSessionLoopUi.kt#L190), [choice guard](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/AppState.kt#L13) |
| S23 | auto | covered | [focused device test](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/UiRebuildInstrumentationTest.kt#L331), [domain test](../../shared/src/commonTest/kotlin/com/weclimb/session/AttemptServiceTest.kt#L115) | [pending-save guard](../../shared/src/commonMain/kotlin/com/weclimb/session/AttemptService.kt#L183), [dialog actions](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/ArchivePlaybackUi.kt#L342) |
| S24 | auto | covered | [focused device test](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/UiRebuildInstrumentationTest.kt#L248), [summary unit test](../../shared/src/commonTest/kotlin/com/weclimb/session/AttemptSummaryTest.kt#L24) | [summary](../../shared/src/commonMain/kotlin/com/weclimb/session/AttemptSummary.kt#L11), [dialog](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/ArchivePlaybackUi.kt#L342) |

## Verification Log

- 2026-07-27: `cd frontend && ./gradlew :androidApp:compileDebugAndroidTestKotlin :androidApp:testDebugUnitTest :shared:jvmTest :androidApp:assembleDebug` passed. The suite and APK compile, including the UI reconstruction tests.
- 2026-07-27: after the S3 search and add-sheet correction, `cd frontend && ./gradlew :androidApp:compileDebugKotlin :androidApp:compileDebugAndroidTestKotlin :androidApp:testDebugUnitTest :shared:jvmTest` passed and `git diff --check` was clean.
- 2026-07-27: no ADB device was attached (`adb devices -l` returned no devices), so `connectedDebugAndroidTest` has not been rerun after this UI change. All `ready-device` rows require that execution before they become covered.
- 2026-07-27: S13 is intentionally deferred by the active goal. It is not a completion claim for the Galaxy S23 visual or native-media walkthrough.
- 2026-07-27: the frozen command `cd frontend && ./gradlew :shared:jvmTest :androidApp:testDebugUnitTest :androidApp:connectedDebugAndroidTest` passed on SM-S911N (Galaxy S23). The connected suite finished 13 tests with 0 failures. S1-S12 are covered by that run and their mapped unit tests.
- 2026-07-29: the UI was re-audited against `design-bundle/00-common.html` through
  `11-records.html` on a reproducible 390dp debug catalog. The board proportion fix
  was also rebuilt, installed and captured on SM-S911N.
- 2026-07-29: the frozen command passed again after the fidelity rebuild and
  behavior corrections. `shared:jvmTest`, Android unit tests, and 15 connected
  instrumentation tests on SM-S911N completed with 0 failures. This run directly
  covers S3 add-and-select, S5 cleanup, S6 confirmation, S7 completion retention,
  S10-S12 static/catalog guards, and S14 video-less success.
- 2026-07-29: S13 follow-up installed the current debug APK on SM-S911N and
  captured the approved `00`-`11` catalog states at 390dp. The live walkthrough
  then completed onboarding, gym selection, CameraX preview and recording,
  successful Attempt classification, archive resume, Media3 range preview and
  playback, edit-list trim export, trimmed-result playback, and the Android share
  chooser. Native output reported `0:00 / 0:12`, and the chooser received one
  share item.
- 2026-07-29: the first live `지금 자르기` attempt exposed a bottom-sheet dismiss
  race that replaced the trim destination with the deferred destination. The
  dismiss callback now applies only while the same media choice is still open;
  focused unit coverage and the physical-device
  `trimsSuccessfulVideoThroughTheAppAndKeepsOriginalAttempt` regression passed.
  The frozen command then passed with all 15 connected tests and 0 failures.
- 2026-07-29: the connected in-app browser had no available browser instance, so
  the agent could not render the local HTML bundle for a fresh side-by-side
  browser comparison. The device captures and native-media walkthrough are
  complete; final subjective comparison against the bundle remains the S13 human
  sign-off boundary.
- 2026-07-29: S13 human review rejected the initial trim timeline because the
  default Material `RangeSlider` track and circular thumbs did not match
  `06-trim`. A new red device test first failed because the framed timeline and
  edge handles were absent. The replacement now uses the bundle's continuous
  10-frame strip, 22%-86% orange selection frame, 14×34dp rectangular handles,
  and `선택 0:04 – 0:16` label. The test passed after implementation, a physical
  drag changed the start label to `0:05`, and the frozen command passed with all
  16 connected tests and 0 failures.
- 2026-07-29: the human approved the corrected `06-trim` timeline on the Galaxy
  S23. Together with the completed `00`-`11` catalog capture review and native
  CameraX/Media3 walkthrough above, this closes the manual S13 visual sign-off.
- 2026-07-29: Step 4 reran the frozen command after human S13 approval. Shared
  tests, Android unit tests, and all 16 connected tests passed together on
  SM-S911N with 0 failures. Cold reviewers still found contract and coverage
  gaps, so the artifact remains `verifying` until the findings below are routed.
- 2026-07-29: after S15, S16, S18 and S19 implementation, the frozen command
  passed with shared tests, Android unit tests and all 23 connected tests on
  SM-S911N. Focused follow-up tests also passed after adding failure-classification
  color evidence for S15 and processing-Back screen-exit evidence for S19. Those
  two rows remain `covered` until the strengthened tests pass in the next full
  frozen-command run.
- 2026-08-04: Step 3 tests for the approved S6, S17 and S20-S24 contract first
  failed on the absent `UNCLASSIFIED` transitions, summary, Room archive mapping,
  classification lock and pending-save guard. Their shared and Android unit tests
  pass after implementation; the debug APK, Android instrumentation sources and
  Android lint also build successfully.
- 2026-08-04: the frozen command compiled the app and instrumentation APK and
  completed the shared and Android unit work, but `connectedDebugAndroidTest`
  stopped with `No connected devices!`. The new scenario rows stay `uncovered`
  until their instrumentation tests run on the physical Android device.
- 2026-08-04: the device was attached to the existing ADB server on port `5038`
  rather than the default `5037`. A focused run selected only the six changed
  instrumentation methods for S6, S17 and S20-S24; all 6 passed on SM-S911N
  with 0 failures. The rows are `covered`, not `verified`, because the human
  explicitly deferred the longer full-suite run.

## Review Findings

- 2026-07-27 `missing_scenario` resolved by the human: direct color-tap completion creates a video-less `SUCCESS` Attempt with `NONE` media. It became S14.
- 2026-07-29 `patch`: S3 add-and-select and S7 trim completion retention are implemented and directly covered.
- 2026-07-29 `patch`: the S1, S3, S5-S7, S9-S12 hollow coverage findings are replaced by state, repository, screenshot, CameraX and Media3 evidence.
- 2026-07-29 `patch`: cold-read review removed the accidental color-to-level calculation and keeps `Lv.x` as ordered preview data; selected personal gyms now synchronize after rename and hide.
- 2026-07-29 `patch`: programmatic removal of the media-choice sheet no longer
  triggers the user-dismissed `나중에` path after `지금 자르기`.
- 2026-07-29 `patch`: the `06-trim` timeline no longer exposes the default
  Material slider visuals; its frame strip, selection outline, handles, spacing,
  labels, accessibility nodes, and drag behavior now follow the bundle.
- 2026-07-29 `missing_scenario`: human decision required for capture hold-color
  selection, leaving Capture while recording or awaiting classification, and
  leaving Trim while Transformer processing is active.
- 2026-07-29 `missing_scenario`: the human approved S15, S16, S18 and S19 for
  capture color selection, system Back behavior during recording, and deferring
  idle or active trimming. The design has no separate in-app Capture Back
  control. Leaving Capture while awaiting classification remains a separate
  unresolved product decision.
- 2026-07-29 `unmet_scenario`: S9 save-failure retry is not wired to the pending
  Attempt, and some real save errors are rendered as success banners.
- 2026-07-29 `hollow_test`: strengthen S6 summary values, S7 drag/processing
  guard/failure retry, S10 no-new-data-work, S11 all static Phase 3 surfaces, and
  S12 stable semantic surfaces. Keep S13 manual only for the remaining
  subjective visual and live CameraX residue; move deterministic seams into the
  S7/S12 automation rather than claiming the whole scenario is automatable.
- 2026-07-29 `patch`: S15 now checks the selected color through both success and
  failure classification, and S19 splits its proof across processing-Back screen
  exit, Transformer cancellation and partial-file cleanup, and the persisted
  `TRIM_PENDING` original-preserving state.
- 2026-07-29 `unmet_scenario`: S6 requires a real highest-level summary while
  S10 and Out of Scope prohibit introducing color-to-level mapping. The current
  live dialog displays a hard-coded `Lv.5`; the frozen contract must choose a
  truthful replacement before implementation can close S6.
- 2026-07-29 `unmet_scenario`: S9 still renders unreadable-video and sharing
  failures without explicit error severity or a retry path, and the trim failure
  banner replaces the concrete cause with fixed copy.
- 2026-07-29 `hollow_test`: S1 permission and settings actions, S7 invalid-range
  and retry state transitions, S8 state labels and exact playback URI, and S10
  absence of new data work need stronger deterministic assertions. S2 already
  has real Home-to-Archive navigation evidence in
  `ArchivePlaybackInstrumentationTest`; its Coverage Map entry was corrected.
- 2026-07-29 `missing_scenario`: cold review found five contract gaps requiring
  human routing: Capture Back while awaiting classification, duplicate
  classification taps, media-choice sheet dismissal, session end with a
  `SAVE_PENDING` Attempt, and whether failed or interrupted trims count as
  `트리밍 대기` in the live end-session summary.
- 2026-08-04 `missing_scenario` resolved by the human: S17 and S20 preserve an
  unclassified recording and provide an Archive re-entry point; S21 makes the
  first classification input win; S22 maps every user dismissal of the media
  choice sheet to `나중에`; S23 blocks session end until `SAVE_PENDING` is retried
  or its video is explicitly discarded; S24 counts every unfinished trim state.
- 2026-08-04 `unmet_scenario` resolved by the human: S6 replaces the unsupported
  hard-coded highest level with the truthful total Attempt count and renames the
  trim summary to `정리 필요`.
- 2026-08-04 `patch`: focused Galaxy S23 tests cover S6, S17 and S20-S24 with
  6 tests and 0 failures. The latest full-suite run remains deferred by the human,
  so these rows stay `covered`.
- 2026-08-04 `hollow_test`: S1 still needs decisive permission-request and
  app-settings action proof. S7 still needs an integrated invalid-submission and
  failed-retry transition test. S8 still needs exact playback URI and all promised
  state-label assertions. S10 proves inert presentation and unchanged persisted
  data, but not yet the absence of query, thumbnail or level-mapping work. These
  rows are `pending` until the stronger proof passes.
- 2026-08-04 `unmet_scenario`: S9 save failure now has truthful error severity and
  retry bound to its `SAVE_PENDING` Attempt. Camera binding, sharing and unreadable
  media can still produce a message without the correct error severity or a retry
  of the failed action. S9 remains `pending` until those paths are wired and tested.
- 2026-08-04 `defer`: the human paused the remaining S1 and S7-S10 closure work.
  A separate worktree will first decouple Attempt outcome from video retention,
  replace hardcoded values on functional screens with runtime data, and consume a
  new human-provided design bundle for motion and flow changes. Re-audit these
  pending findings against that result instead of strengthening the old UI now.
- 2026-08-04 `defer`: the functional surfaces for unclassified archive cards,
  classification re-entry, pending-save session end and the truthful end summary
  have no dedicated design-bundle states. Keep their behavior and accessibility
  stable, then run a separate fidelity pass after the next bundle update.
- 2026-07-27 `defer`: playback control hide/show on tap is not frozen in S8. Do not implement until it is explicitly contracted.

## Notes

- 기준 시안: `design-bundle/00-common.html`부터 `design-bundle/11-records.html`
- 구현 기준: `design-system/we-climb/MASTER.md`의 `Stack Adaptation`과 `UI 폴리시`
- 현재 구현은 `frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt`의
  `Screen`과 `AppState`에서 시작한다. UI 재구현에서 화면을 분리할 수 있지만,
  기존 미디어 상태와 repository 계약을 바꾸지 않는다.
