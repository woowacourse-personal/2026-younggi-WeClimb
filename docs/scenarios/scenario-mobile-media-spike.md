---
title: '모바일 미디어 스파이크 검증'
type: 'feature'
created: '2026-07-20'
status: 'done'
baseline_commit: 'f5fe32b4248f16dc8eea9f221c10e8b4d9a8d047'
test_command: 'cd frontend && ./gradlew :shared:jvmTest :androidApp:testDebugUnitTest :androidApp:connectedDebugAndroidTest'
loop_iteration: 2
---

<frozen-after-approval reason="human-owned contract - only the human changes this after approval">

## Intent

**Problem:** We-Climb의 Phase 1은 촬영, 미디어 저장, 외부 공유에 의존하지만
아직 이 조합이 실제 Android 기기에서 동작하는지 검증되지 않았다. 실패한 기술 선택을
기반으로 제품 화면을 구현하는 위험을 제거해야 한다.

**Approach:** 최소 Android 스파이크에서 각 외부 경계를 자동 검증 가능한 부분과
실기기 또는 Instagram 관찰이 필요한 부분으로 분리해 검증한다.

## Scenarios

### S1: 권한이 있는 상태에서 녹화를 시작한다

- **Given** 카메라와 마이크 권한이 허용되고 캐시 출력 경로를 만들 수 있다
- **When** 사용자가 녹화 시작을 요청한다
- **Then** CameraX가 캐시 경로를 출력 대상으로 하는 활성 녹화 세션을 시작한다
- **Verification:** auto

### S2: 녹화 후 성공 선택으로 다음 녹화를 준비한다

- **Given** 캐시에 저장된 녹화 결과가 있고 CameraX 녹화가 중지됐다
- **When** 사용자가 성공을 선택한다
- **Then** 결과가 성공 후보로 등록되고 녹화 상태가 새 녹화 시작 가능 상태가 된다
- **Verification:** auto

### S3: 녹화 후 실패 선택은 캐시에만 보관한다

- **Given** 캐시에 저장된 녹화 결과가 있고 CameraX 녹화가 중지됐다
- **When** 사용자가 실패를 선택한다
- **Then** 결과에 MediaStore URI를 만들지 않고 실패 삭제 대상으로 등록한다
- **Verification:** auto

### S4: 권한 거부 또는 녹화 오류를 복구 가능하게 알린다

- **Given** 카메라 또는 마이크 권한이 없거나 CameraX가 녹화를 시작하지 못한다
- **When** 사용자가 녹화 시작을 요청한다
- **Then** 녹화 파일이나 성공 후보를 만들지 않고 원인을 구분한 오류 상태를 반환한다
- **Verification:** auto

### S5: 성공 영상을 MediaStore에 저장한다

- **Given** 성공 후보인 캐시 MP4가 읽을 수 있고 MediaStore 쓰기가 허용된다
- **When** 저장을 요청한다
- **Then** `Movies/WeClimb`의 읽을 수 있는 콘텐츠 URI가 반환되고 성공 기록은 그 URI를 참조한다
- **Verification:** auto

### S6: 실패 영상을 일괄 삭제한다

- **Given** 삭제 대상으로 등록된 실패 캐시 파일이 여러 개 있고 성공 후보 파일도 있다
- **When** 실패 영상 일괄 삭제를 요청한다
- **Then** 실패 파일만 제거되고 성공 후보 파일과 MediaStore에 저장된 영상은 남아 있다
- **Verification:** auto

### S7: MediaStore 저장 실패는 캐시 원본을 보존한다

- **Given** 성공 후보인 캐시 MP4가 있고 MediaStore 쓰기가 실패한다
- **When** 저장을 요청한다
- **Then** 실패 원인을 반환하고 캐시 원본과 성공 후보 상태를 유지한다
- **Verification:** auto

### S8: 유효한 구간으로 edit-list 트리밍 결과를 만든다

- **Given** 재생 가능한 MP4와 시작 시각보다 큰 종료 시각이 있다
- **When** Media3 Transformer에 edit-list 트리밍을 요청한다
- **Then** 요청한 시작과 종료 시각을 가진 완료된 MP4 결과를 반환한다
- **Verification:** auto

### S9: 잘못된 트리밍 구간은 변환을 시작하지 않는다

- **Given** 종료 시각이 시작 시각 이하이거나 영상 범위 밖인 트리밍 요청이 있다
- **When** 트리밍을 요청한다
- **Then** 출력 파일을 만들지 않고 입력 오류를 반환한다
- **Verification:** auto

### S10: 트리밍 결과가 갤러리와 Instagram에서 정확히 재생된다

- **Given** S8로 만든 결과 MP4와 원본에서 식별 가능한 시작과 종료 프레임이 있다
- **When** 실제 Android 기기의 갤러리에서 재생하고 Instagram에 업로드한다
- **Then** 두 재생 환경 모두 요청한 시작 프레임에서 시작하고 종료 프레임 뒤의 내용이 없다
- **Verification:** manual - S8의 출력 생성은 자동화한다. 갤러리와 제3자 앱의 실제 edit-list 해석은 기기와 Instagram에 의존한다.

### S11: 공유 인텐트가 단일 동영상 URI를 제공한다

- **Given** MediaStore에 저장된 읽을 수 있는 동영상 URI가 있다
- **When** 사용자가 Instagram 공유를 요청한다
- **Then** 앱은 `ACTION_SEND`, `video/*` MIME 유형, 읽기 URI 권한, 단일 동영상 스트림을 가진 인텐트를 만든다
- **Verification:** auto

### S12: Instagram 스토리 편집기가 공유 대상을 연다

- **Given** Instagram이 설치되어 있고 S11의 공유 인텐트가 전송된다
- **When** 실제 Android 기기에서 공유를 실행한다
- **Then** Instagram 스토리 편집기가 동영상을 로드한 상태로 열린다
- **Verification:** manual - S11의 인텐트 형식은 자동화한다. Instagram의 설치 상태와 편집기 동작은 제3자 앱 표면이다.

## Out of Scope

- 온보딩, 암장 선택, 세션 보드, 리포트, 기록 탭의 제품 UI
- 트리밍 편집 UI와 미룬 트리밍 큐
- Instagram 공유 성공 콜백 수집과 `ADD_TO_STORY` 연동
- iOS 빌드과 iOS 미디어 동작
- 프로덕션 Supabase 스키마, RLS 정책, 암장 데이터 시딩
- Supabase 구글 로그인과 세션 메타데이터 왕복(S13-S15, 사용자 결정으로 보류)

</frozen-after-approval>

## Coverage Map

| Scenario | Verification | Status | Test | Implementation |
|----------|--------------|--------|------|----------------|
| S1 | auto | verified | `RecordingCoordinatorTest.startsRecordingWithCacheOutputWhenPermissionsAreGranted`; `../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt:31` | `RecordingCoordinator`, `CameraRecordingController` |
| S2 | auto | verified | `RecordingCoordinatorTest.successfulRecordingReturnsToReadyStateAndKeepsCandidate`; `../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt:40` | `RecordingCoordinator`, `MainActivity` |
| S3 | auto | verified | `RecordingCoordinatorTest.failedRecordingReturnsToReadyStateAndQueuesOnlyFailedCandidate`; `../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt:31` | `RecordingCoordinator`, `MainActivity` |
| S4 | auto | verified | `RecordingCoordinatorTest.missingCameraPermissionReturnsCameraPermissionErrorWithoutRecording`, `missingMicrophonePermissionReturnsMicrophonePermissionErrorWithoutRecording`; `../../frontend/androidApp/src/test/kotlin/com/weclimb/android/CameraRecordingStartGuardTest.kt:10` | `RecordingCoordinator`, `../../frontend/androidApp/src/main/kotlin/com/weclimb/android/CameraRecordingController.kt:51` |
| S5 | auto | verified | `VideoPersistenceTest.promotesSuccessfulCacheVideoToMediaStore`; `../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/AndroidMediaGatewaysInstrumentationTest.kt:21` | `VideoPersistence`, `AndroidMediaStoreGateway` |
| S6 | auto | verified | `VideoPersistenceTest.deletesOnlyFailedCacheVideos`; `../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/AndroidMediaGatewaysInstrumentationTest.kt:44` | `VideoPersistence`, `AndroidCacheGateway` |
| S7 | auto | verified | `VideoPersistenceTest.preservesSuccessfulCacheVideoWhenMediaStoreWriteFails`; `../../shared/src/commonTest/kotlin/com/weclimb/session/AttemptServiceTest.kt:31`; `../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/AndroidMediaGatewaysInstrumentationTest.kt:64` | `VideoPersistence`, `AndroidMediaStoreGateway`, `AttemptService` |
| S8 | auto | verified | `TrimAndShareTest.sendsValidTrimRequestWithEditListMode`, `AndroidEditListTrimGatewayTest.startsEditListExportAndForwardsCompletion`; `../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/Media3TrimInstrumentationTest.kt:18` | `TrimService`, `../../frontend/androidApp/src/main/kotlin/com/weclimb/android/AndroidEditListTrimGateway.kt:35` |
| S9 | auto | verified | `TrimAndShareTest.rejectsTrimRangeWithNonPositiveDuration`, `rejectsTrimRangeOutsideVideoDuration` | `TrimService` |
| S10 | manual | verified | 2026-07-21 device walkthrough | `Media3EditListExporter`, `MainActivity` |
| S11 | auto | verified | `TrimAndShareTest.createsSingleVideoShareRequestWithReadPermission`; `../../frontend/androidApp/src/test/kotlin/com/weclimb/android/AndroidShareLauncherTest.kt:15`; `../../shared/src/commonTest/kotlin/com/weclimb/media/AttemptShareServiceTest.kt:10`; `../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt:40` | `ShareRequestFactory`, `AttemptShareService`, `../../frontend/androidApp/src/main/kotlin/com/weclimb/android/AndroidShareLauncher.kt:17` |
| S12 | manual | verified | 2026-07-20 device walkthrough; 2026-07-22 app chooser walkthrough | `AndroidShareLauncher`, `MainActivity` |

## Verification Log

S10, 2026-07-21의 갤러리와 Instagram 재생 walkthrough는 식별 가능한 시작, 종료
프레임과 edit-list 출력 길이를 확인했다. S12, 2026-07-20의 Instagram Stories
walkthrough는 공유된 단일 MP4가 스토리 편집기에 로드된 것을 확인했다.

2026-07-20, SM-S911N(API 36.1-ext21)에서 CameraX 녹화와 성공/실패 분류,
성공 영상 MediaStore 저장, 실패 캐시 영상 삭제를 확인했다. 저장된 단일 MP4는
Android 공유 시트에 전달됐고 Instagram Stories 편집기에서 로드됐다. 실패 선택 후
캐시의 해당 녹화 파일이 삭제되는 것도 확인했다.

2026-07-21, `cd frontend && ./gradlew :shared:jvmTest :androidApp:testDebugUnitTest`와
`:androidApp:assembleDebug`가 통과했다. S9는 잘못된 구간이 트리밍 게이트웨이를
호출하지 않는지도 검사한다. S4의 CameraX 녹화 시작 실패와 S11의 Android Intent
필드는 아직 Android 자동 테스트로 검증하지 않았다.

2026-07-21, SM-S911N(API 36.1-ext21)에서 1초-4초 edit-list 트리밍을 실행했다.
캐시 출력 MP4를 만들고 `Movies/WeClimb`에 저장했으며, MediaStore가 보고한 길이는
3,000ms였다. Samsung 포토 플레이어는 `0:00 / 0:03`으로 재생했고, 사용자가 갤러리와
Instagram에서 시작과 종료 프레임을 수동 확인했다.

S8은 실제 기기에서 MP4 출력을 확인했지만, fixture를 사용해 Media3 출력 생성을
자동 검증하는 Android instrumentation test는 아직 없다. 그래서 자동 검증 상태는
`partial`로 유지한다.

2026-07-22, `CameraRecordingStartGuardTest`와 `AndroidShareLauncherTest`를 추가했고
`cd frontend && ./gradlew :shared:jvmTest :androidApp:testDebugUnitTest :androidApp:compileDebugAndroidTestKotlin`이
통과했다. S8의 `Media3TrimInstrumentationTest`는 실제 MP4 fixture를 만들고 Media3
출력 길이를 검사하도록 추가했으나, 실행 시점에 연결된 Android 기기가 없어
`connectedDebugAndroidTest`는 아직 실행하지 못했다.

2026-07-22, SM-S911N(API 36.1-ext21)에 앱과 instrumentation APK를 설치하고
`Media3TrimInstrumentationTest.createsCompletedMp4ForValidEditListRange`를 직접
실행했다. 실제 MP4 fixture에서 500ms-1,500ms edit-list 트리밍이 완료됐고, 출력 MP4의
길이는 800ms-1,200ms 범위로 확인됐다.

2026-07-22, SM-S911N(API 36.1-ext21)에서 `AndroidMediaGatewaysInstrumentationTest`의
MediaStore 저장, 실패 캐시 삭제, 저장 실패 시 캐시 보존을 확인했다. 강화한
`Media3TrimInstrumentationTest`는 시작과 종료 프레임 밝기를 원본 구간과 비교했고,
`CameraRecordingFlowInstrumentationTest`는 실제 CameraX 녹화 후 실패 분류와 성공
분류, Android chooser 호출까지 통과했다.

2026-07-22, 전체 자동 명령과 두 개의 맥락 없는 리뷰를 실행했다. 리뷰는 스파이크용
순수 서비스 테스트가 현재 Android 앱 경로와 분리돼 S1-S7을 충분히 증명하지 못하며,
S8은 시간 경계 프레임을 식별하지 못하고, 공유 런처가 앱 화면에 연결되지 않아 S11-S12를
충족하지 못한다고 확인했다. S4의 동기 시작 실패 UI 상태와 S7의 실패 이유 반환은
수정했고 각각의 단위 테스트를 추가했다. 남은 계약·구현 방향은 사람 결정이 필요해
모든 영향을 받은 항목을 `partial`로 되돌렸다.

## Notes

## Scenario Traceability

| Scenario | Test or walkthrough | Implementation |
|----------|---------------------|----------------|
| S1: | [Camera flow](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt) | [Camera controller](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/CameraRecordingController.kt) |
| S2: | [Camera flow](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt) | [Activity](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt) |
| S3: | [Camera flow](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/CameraRecordingFlowInstrumentationTest.kt) | [Activity](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt) |
| S4: | [Start guard](../../frontend/androidApp/src/test/kotlin/com/weclimb/android/CameraRecordingStartGuardTest.kt) | [Camera controller](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/CameraRecordingController.kt) |
| S5: | [Media gateway](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/AndroidMediaGatewaysInstrumentationTest.kt) | [Media gateway](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/AndroidMediaGateways.kt) |
| S6: | [Media gateway](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/AndroidMediaGatewaysInstrumentationTest.kt) | [Cache gateway](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/AndroidMediaGateways.kt) |
| S7: | [Media gateway](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/AndroidMediaGatewaysInstrumentationTest.kt) | [Attempt service](../../shared/src/commonMain/kotlin/com/weclimb/session/AttemptService.kt) |
| S8: | [Trim instrumentation](../../frontend/androidApp/src/androidTest/kotlin/com/weclimb/android/Media3TrimInstrumentationTest.kt) | [Media3 exporter](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/AndroidEditListTrimGateway.kt) |
| S9: | [Trim tests](../../shared/src/commonTest/kotlin/com/weclimb/media/TrimAndShareTest.kt) | [Trim service](../../shared/src/commonMain/kotlin/com/weclimb/media/TrimService.kt) |
| S10: | S10 device walkthrough | [Media3 exporter](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/AndroidEditListTrimGateway.kt) |
| S11: | [Share launcher](../../frontend/androidApp/src/test/kotlin/com/weclimb/android/AndroidShareLauncherTest.kt) | [Share launcher](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/AndroidShareLauncher.kt) |
| S12: | S12 device walkthrough | [Share launcher](../../frontend/androidApp/src/main/kotlin/com/weclimb/android/AndroidShareLauncher.kt) |

- 기준 계획: [기술 결정](../planning/03-tech-decisions.md),
  [프로덕트 정의](../planning/01-product.md)
- 이 문서는 기능 배포 계약이 아니라, Phase 1 구현 착수 전 기술 선택을 통과 또는
  폴백으로 판정하기 위한 스파이크 계약이다.
- 개발 순서: S1-S4(녹화 상태와 캐시) → S5-S7(성공 저장과 실패 삭제) →
  S8-S10(트리밍) → S11-S12(공유). 인증과 세션은 별도 스파이크로 보류했다.
