---
title: '모바일 미디어 스파이크 검증'
type: 'feature'
created: '2026-07-20'
status: 'in-progress'
baseline_commit: '3119808267a93066c8984064cf6820d73c1bc766'
test_command: 'cd frontend && ./gradlew :shared:jvmTest :androidApp:testDebugUnitTest'
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
| S1 | auto | verified | `RecordingCoordinatorTest.startsRecordingWithCacheOutputWhenPermissionsAreGranted` | `RecordingCoordinator`, `CameraRecordingController` |
| S2 | auto | verified | `RecordingCoordinatorTest.successfulRecordingReturnsToReadyStateAndKeepsCandidate` | `RecordingCoordinator`, `MainActivity` |
| S3 | auto | verified | `RecordingCoordinatorTest.failedRecordingReturnsToReadyStateAndQueuesOnlyFailedCandidate` | `RecordingCoordinator`, `MainActivity` |
| S4 | auto | partial | `RecordingCoordinatorTest.missingCameraPermissionReturnsCameraPermissionErrorWithoutRecording`, `missingMicrophonePermissionReturnsMicrophonePermissionErrorWithoutRecording` | `RecordingCoordinator`, `CameraRecordingController` |
| S5 | auto | verified | `VideoPersistenceTest.promotesSuccessfulCacheVideoToMediaStore` | `VideoPersistence`, `AndroidMediaStoreGateway` |
| S6 | auto | verified | `VideoPersistenceTest.deletesOnlyFailedCacheVideos` | `VideoPersistence`, `AndroidCacheGateway` |
| S7 | auto | verified | `VideoPersistenceTest.preservesSuccessfulCacheVideoWhenMediaStoreWriteFails` | `VideoPersistence`, `AndroidMediaStoreGateway` |
| S8 | auto | partial | `TrimAndShareTest.sendsValidTrimRequestWithEditListMode`, `AndroidEditListTrimGatewayTest.startsEditListExportAndForwardsCompletion` | `TrimService`, `Media3EditListExporter` |
| S9 | auto | verified | `TrimAndShareTest.rejectsTrimRangeWithNonPositiveDuration`, `rejectsTrimRangeOutsideVideoDuration` | `TrimService` |
| S10 | manual | verified | 2026-07-21 device walkthrough | `Media3EditListExporter`, `MainActivity` |
| S11 | auto | partial | `TrimAndShareTest.createsSingleVideoShareRequestWithReadPermission` | `ShareRequestFactory`, `AndroidShareLauncher` |
| S12 | manual | verified | 2026-07-20 device walkthrough | `AndroidShareLauncher`, `MainActivity` |

## Verification Log

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

## Notes

- 기준 계획: [기술 결정](../planning/03-tech-decisions.md),
  [프로덕트 정의](../planning/01-product.md)
- 이 문서는 기능 배포 계약이 아니라, Phase 1 구현 착수 전 기술 선택을 통과 또는
  폴백으로 판정하기 위한 스파이크 계약이다.
- 개발 순서: S1-S4(녹화 상태와 캐시) → S5-S7(성공 저장과 실패 삭제) →
  S8-S10(트리밍) → S11-S12(공유). 인증과 세션은 별도 스파이크로 보류했다.
