---
title: 'Phase 2 미디어 흐름과 현재 화면 UI'
type: 'feature'
created: '2026-07-23'
status: 'done'
baseline_commit: '15331e9d2a1e617e068bbc017431b070c68438c6'
test_command: 'cd frontend && ./gradlew :shared:jvmTest :androidApp:testDebugUnitTest :androidApp:connectedDebugAndroidTest'
---

<frozen-after-approval reason="human-owned contract - only the human changes this after approval">

## Intent

**Problem:** 성공 영상은 현재 원본 URI로만 남아 있어 사용자가 자르거나 나중에
처리할 수 없고, 앱 안에서 다시 재생할 아카이브도 없다. 실제 앱 화면 역시
design-bundle의 다크 시각 체계를 아직 반영하지 않는다.

**Approach:** 성공 Attempt에 영속적인 영상 처리 상태와 원본·표시 영상 URI를
추가한다. 원본은 항상 보존하고 트리밍 결과는 별도 MediaStore 영상으로 만들며,
현재 열리는 Compose 화면과 새 트리밍·아카이브 화면에 design-bundle의 Compose
토큰을 적용한다.

## Scenarios

### S1: 성공 영상을 저장한 직후 처리 방법을 선택한다

- **Given** 읽을 수 있는 MediaStore 원본 URI를 가진 성공 Attempt가 있다
- **When** 사용자가 성공 분류를 마친다
- **Then** `자르기`, `나중에`, `원본 유지` 중 하나를 선택할 수 있는 처리 선택지가
  표시되고, 선택 전 Attempt는 원본 URI를 유지한다
- **Verification:** auto

### S2: 나중에 처리할 영상을 재진입해 트리밍한다

- **Given** 원본 URI와 `trimPending` 상태로 저장된 성공 Attempt가 있다
- **When** 사용자가 세션 보드 또는 아카이브에서 해당 Attempt를 연다
- **Then** 트리밍 화면이 원본을 대상으로 열리고, 앱을 다시 시작한 뒤에도 같은
  `trimPending` Attempt가 조회된다
- **Verification:** auto

### S3: 원본 유지 선택은 공유 가능한 원본을 보존한다

- **Given** 읽을 수 있는 원본 URI를 가진 처리 대기 성공 Attempt가 있다
- **When** 사용자가 `원본 유지`를 선택한다
- **Then** Attempt 상태는 `originalKept`로 저장되고 표시·공유 URI는 원본 URI이며
  원본 MediaStore 항목은 삭제되지 않는다
- **Verification:** auto

### S4: 유효한 구간을 별도 트리밍 영상으로 저장한다

- **Given** 읽을 수 있는 원본 MediaStore URI와 유효한 시작·종료 시각이 있다
- **When** 사용자가 트리밍 완료를 요청한다
- **Then** 원본 URI는 유지한 채 비어 있지 않은 별도 MediaStore 결과 URI가
  `trimmed` Attempt의 표시·공유 URI로 저장된다
- **Verification:** auto

### S5: 트리밍 오류는 원본과 재시도 경로를 보존한다

- **Given** 처리 대기 Attempt가 범위를 벗어난 요청을 받거나 Transformer 출력에
  실패한다
- **When** 트리밍을 요청한다
- **Then** 손상되거나 빈 출력 MediaStore 항목은 남지 않고 원본 URI와 Attempt는
  유지되며 사용자는 오류 메시지와 재시도 또는 원본 유지 선택지를 본다
- **Verification:** auto

### S6: 아카이브는 성공 영상만 최신순으로 보여 준다

- **Given** 서로 다른 날짜, 암장, 색상, 처리 상태를 가진 성공·실패 Attempt가 있다
- **When** 사용자가 아카이브를 연다
- **Then** 성공 Attempt만 최신순으로 표시되고 각 항목에는 날짜, 암장명, 색상,
  원본·대기·트리밍 상태가 보인다
- **Verification:** auto

### S7: 아카이브 영상을 재생하거나 읽기 불가 상태를 알린다

- **Given** 아카이브 Attempt의 표시 URI가 읽을 수 있거나 기기에서 삭제돼 읽을 수 없다
- **When** 사용자가 영상을 재생한다
- **Then** 읽을 수 있는 URI는 앱 내 플레이어에 전달되고, 읽을 수 없는 URI는 Attempt를
  삭제하지 않은 채 재생 불가 플레이스홀더와 원인 안내를 표시한다
- **Verification:** auto

### S8: 선택한 표시 영상을 Android 공유 시트로 보낸다

- **Given** 아카이브 또는 트리밍 화면에서 읽을 수 있는 표시 영상 URI가 선택됐다
- **When** 사용자가 공유를 요청한다
- **Then** `ACTION_SEND`, `video/*`, 단일 `EXTRA_STREAM`, 읽기 URI 권한을 가진
  chooser가 선택한 표시 URI로 열린다
- **Verification:** auto

### S9: 처리 중단 뒤에도 안전한 상태로 복구한다

- **Given** `trimPending`, `trimProcessing`, 또는 `trimFailed` 상태의 Attempt가 Room에 있다
- **When** 앱이 종료된 뒤 다시 시작된다
- **Then** 원본 URI와 마지막 안전 상태가 복원되고, `trimProcessing`은 재시도 가능한
  실패 또는 대기 상태로 표시되며 고아 출력은 표시 URI가 되지 않는다
- **Verification:** auto

### S10: 현재 실제 앱 화면에 design-bundle 시각 체계를 적용한다

- **Given** 온보딩, 홈, 암장 선택, 세션 보드, 촬영 상태, 트리밍, 아카이브 화면이 있다
- **When** 사용자가 각 화면을 연다
- **Then** 배경·표면·브랜드·시맨틱 색과 홀드 데이터 색이 분리된 Compose 토큰으로
  표시되고, 각 핵심 행동은 접근 가능한 라벨과 최소 터치 영역을 가진다
- **Verification:** auto

### S11: 실제 기기에서 영상 처리와 UI를 관찰한다

- **Given** S4, S7, S8, S10의 자동 검증을 통과한 debug 앱과 실제 Android 기기가 있다
- **When** 사용자가 성공 영상 트리밍, 아카이브 재생, 공유, 현재 화면 전환을 수행한다
- **Then** 트리밍 결과가 재생되고 Android 공유 시트가 열리며, 주요 화면의 대비·상태·터치
  흐름에 사람이 확인 가능한 결함이 없다
- **Verification:** manual - 실제 카메라, 기기 플레이어와 사람의 시각·터치 판단은 S4, S7, S8, S10의 자동 검증으로 분리한 뒤에도 자동화할 수 없다.

## Out of Scope

- 종료 사진 촬영, 세션 종료 후처리, 리포트 카드·리포트 목록·기록 통계
- `07-session-end`, `08-report`, `09-records`의 Compose route 또는 화면 구현
- Instagram Stories 전용 `ADD_TO_STORY`, 공유 성공 콜백, 서버 동기화·계정·iOS
- 아카이브 썸네일 생성과 대용량 목록 성능 정책
- 기존 Phase 1 설치본의 Room v1 성공 Attempt를 v2 영상 처리 상태로 변환하는
  마이그레이션과 회귀 검증. 기존 사용자의 영상 보존 정책은 후속 작업으로 결정한다.
- 미디어 처리와 독립 배포 가능한 전체 UI 재설계 범위; 사용자의 선택으로 이번
  사용자 여정 계약에는 현재 실제 화면의 design-bundle 적용만 함께 포함한다

</frozen-after-approval>

## Coverage Map

| Scenario | Verification | Status | Test | Implementation |
|----------|--------------|--------|------|----------------|
| S1 | auto | verified | [Camera flow](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt#L48) | [Session board](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L744) |
| S2 | auto | verified | [Camera flow recreation](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt#L63) | [Trim entry](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L189) |
| S3 | auto | verified | [Media state](../../shared/src/commonTest/kotlin/com/weclimb/media/AttemptMediaServiceTest.kt#L24), [share selection](../../shared/src/commonTest/kotlin/com/weclimb/media/AttemptShareServiceTest.kt#L31) | [Media service](../../shared/src/commonMain/kotlin/com/weclimb/media/AttemptMediaService.kt#L43) |
| S4 | auto | verified | [App trim](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt#L82), [Media3 output](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/Media3TrimInstrumentationTest.kt#L19) | [Trim promotion](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L259) |
| S5 | auto | verified | [Invalid range retry](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt#L99), [state retry](../../shared/src/commonTest/kotlin/com/weclimb/media/AttemptMediaServiceTest.kt#L77) | [Trim failure](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L278) |
| S6 | auto | verified | [Archive query](../../frontend/androidApp/src/test/kotlin/com/weclimb/android/RoomSessionLoopRepositoryTest.kt#L71), [archive UI](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/ArchivePlaybackInstrumentationTest.kt#L59) | [Archive screen](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L822) |
| S7 | auto | verified | [Unreadable archive](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/ArchivePlaybackInstrumentationTest.kt#L59), [URI readability](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/AndroidMediaGatewaysInstrumentationTest.kt#L83) | [Playback branch](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L211) |
| S8 | auto | verified | [Chooser flow](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt#L48), [trim share](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt#L63) | [Share launcher](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/AndroidShareLauncher.kt#L15) |
| S9 | auto | verified | [Room recovery](../../frontend/androidApp/src/test/kotlin/com/weclimb/android/RoomSessionLoopRepositoryTest.kt#L89), [domain recovery](../../shared/src/commonTest/kotlin/com/weclimb/media/AttemptMediaServiceTest.kt#L54) | [Initial recovery](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L111) |
| S10 | auto | verified | [Theme tokens](../../frontend/androidApp/src/test/kotlin/com/weclimb/android/WeClimbThemeTest.kt#L7), [live routes](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt#L115) | [Theme](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/WeClimbTheme.kt#L5), [live screens](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L528) |
| S11 | manual | verified | walkthrough in Verification Log | [Device build](../../frontend/androidApp/build.gradle.kts#L1) |

## Verification Log

- 2026-07-23, S11, SM-S911N (Android 16): debug APK에서 온보딩, 홈, 암장 선택,
  세션 보드, 녹화 결과, 트리밍, 아카이브 흐름을 확인했다. 실제 CameraX 촬영 후
  성공 영상을 0–500ms로 잘라 저장했고, 아카이브에서 재생·공유 chooser와 읽기 불가
  영상의 카드 내 안내를 확인했다. 세션 보드의 긴 난이도 목록은 스크롤해 처리
  선택지까지 접근 가능함을 확인했다.

## Scenario Traceability

| Scenario | Verified by | Implementation |
|----------|-------------|----------------|
| S1: 성공 직후 처리 선택지를 표시한다 | [Camera flow](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt#L48) | [Session board](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L744) |
| S2: 대기 영상을 재시작 뒤에도 트리밍한다 | [Camera flow recreation](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt#L63) | [Trim entry](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L189) |
| S3: 원본 유지는 원본 URI를 공유한다 | [Media state](../../shared/src/commonTest/kotlin/com/weclimb/media/AttemptMediaServiceTest.kt#L24) | [Media service](../../shared/src/commonMain/kotlin/com/weclimb/media/AttemptMediaService.kt#L43) |
| S4: 트리밍 결과는 원본과 별도로 저장된다 | [App trim](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt#L82) | [Trim promotion](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L259) |
| S5: 오류 뒤 재시도와 원본 유지 경로가 남는다 | [Invalid range retry](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt#L99) | [Trim failure](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L278) |
| S6: 아카이브는 성공 시도만 최신순으로 표시한다 | [Archive query](../../frontend/androidApp/src/test/kotlin/com/weclimb/android/RoomSessionLoopRepositoryTest.kt#L71) | [Archive screen](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L822) |
| S7: 읽기 불가 영상은 기록을 유지하고 안내한다 | [Unreadable archive](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/ArchivePlaybackInstrumentationTest.kt#L59) | [Playback branch](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L211) |
| S8: 선택 영상으로 Android 공유 chooser를 연다 | [Chooser flow](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt#L48) | [Share launcher](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/AndroidShareLauncher.kt#L15) |
| S9: 중단된 트리밍은 재시도 가능 상태로 복구한다 | [Room recovery](../../frontend/androidApp/src/test/kotlin/com/weclimb/android/RoomSessionLoopRepositoryTest.kt#L89) | [Initial recovery](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L111) |
| S10: 현재 화면은 design-bundle 토큰을 사용한다 | [Theme tokens](../../frontend/androidApp/src/test/kotlin/com/weclimb/android/WeClimbThemeTest.kt#L7) | [Theme](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/WeClimbTheme.kt#L5) |
| S11: 실기기에서 영상과 화면 흐름을 확인한다 | manual walkthrough (see Verification Log) | [Live screens](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt#L528) |

## Notes

- 기준 설계: [Phase 2 미디어 처리와 UI 적용](../design/phase-2-media-flow-and-ui.md),
  [기술 결정](../planning/03-tech-decisions.md),
  [디자인 시스템](../../design-system/we-climb/MASTER.md)
- S4는 Content URI를 직접 읽을지 임시 앱 캐시로 안전하게 복사할지 구현을 고정하지
  않는다. 어느 경계든 원본 보존과 별도 결과 URI라는 관찰 가능한 계약을 충족해야 한다.
