---
title: 'Phase 1 세션 루프'
type: 'feature'
created: '2026-07-21'
status: 'verifying'
baseline_commit: '54a643e'
test_command: 'cd frontend && ./gradlew :shared:jvmTest :androidApp:testDebugUnitTest'
---

<frozen-after-approval reason="human-owned contract - only the human changes this after approval">

## Intent

**Problem:** 미디어 기능은 검증됐지만, 사용자가 권한을 완료하고 암장을 고른 뒤
세션에서 시도를 기록하고 종료하는 제품 흐름은 아직 없다.

**Approach:** 로컬 게스트와 서울 시드로 단일 활성 세션을 만들고, 각 시도를
독립 기록으로 저장한다. 미디어 입출력은 기존 스파이크 계약의 구현을 연결한다.

## Scenarios

### S1: 권한이 모두 허용되기 전에는 온보딩을 완료하지 않는다

- **Given** 카메라 또는 마이크 권한이 허용되지 않은 온보딩 상태가 있다
- **When** 사용자가 다음 단계로 진행을 요청한다
- **Then** 홈 경로와 세션 시작 경로를 열지 않고 필요한 권한 상태를 반환한다
- **Verification:** auto

### S2: 필요한 권한이 모두 허용되면 온보딩을 완료한다

- **Given** 카메라와 마이크 권한이 모두 허용된 온보딩 상태가 있다
- **When** 사용자가 다음 단계로 진행을 요청한다
- **Then** 로컬 게스트 프로필을 저장하고 홈 경로를 반환한다
- **Verification:** auto

### S3: 활성 세션은 앱 재시작 뒤 세션 보드로 복원한다

- **Given** 로컬 저장소에 활성 세션과 연결된 암장이 있다
- **When** 앱의 초기 경로를 결정한다
- **Then** 홈 대신 그 세션 ID를 가진 세션 보드 경로를 반환한다
- **Verification:** auto

### S4: 활성 세션 중에는 홈으로 이동하지 않는다

- **Given** 활성 세션을 표시하는 세션 보드 상태가 있다
- **When** 사용자가 홈 이동을 요청한다
- **Then** 홈 경로를 반환하지 않고 현재 세션 보드 상태를 유지한다
- **Verification:** auto

### S5: 시드와 활성 개인 암장을 이름으로 검색한다

- **Given** 서울 시드 암장과 숨기지 않은 개인 암장이 저장돼 있다
- **When** 사용자가 이름 일부를 입력한다
- **Then** 이름에 대소문자와 공백을 무시해 일치하는 암장만 반환하고 숨긴 암장은
  반환하지 않는다
- **Verification:** auto

### S6: 검색 결과가 없으면 이름으로 개인 암장을 추가한다

- **Given** 검색 결과가 없고 비어 있지 않은 암장 이름이 있다
- **When** 사용자가 직접 추가를 확정한다
- **Then** 새 내부 ID와 `userAdded` 출처를 가진 활성 암장을 저장하고 선택 결과로
  반환한다
- **Verification:** auto

### S7: 빈 이름으로 개인 암장을 추가하지 않는다

- **Given** 공백만 있는 암장 이름이 있다
- **When** 사용자가 직접 추가를 확정한다
- **Then** 암장을 저장하지 않고 이름 입력 오류를 반환한다
- **Verification:** auto

### S8: 개인 암장의 이름을 수정한다

- **Given** 사용자가 추가한 활성 암장이 있다
- **When** 사용자가 비어 있지 않은 새 이름을 저장한다
- **Then** 같은 내부 ID를 유지한 채 수정된 이름을 저장한다
- **Verification:** auto

### S9: 기록이 있는 개인 암장은 목록에서 숨긴다

- **Given** 사용자가 추가한 암장과 그 암장에 연결된 종료 세션이 있다
- **When** 사용자가 암장 삭제를 요청한다
- **Then** 암장 레코드는 제거하지 않은 채 목록에서만 숨긴다
- **Verification:** auto

### S10: 암장을 선택하면 활성 세션을 하나만 시작한다

- **Given** 선택한 활성 암장과 활성 세션이 없는 로컬 저장소가 있다
- **When** 사용자가 세션 시작을 확정한다
- **Then** 암장 ID와 시작 시각을 가진 활성 세션을 저장하고 세션 보드 경로를
  반환한다
- **Verification:** auto

### S11: 활성 세션이 있으면 두 번째 세션을 시작하지 않는다

- **Given** 로컬 저장소에 활성 세션이 있다
- **When** 사용자가 다른 암장으로 세션 시작을 요청한다
- **Then** 새 세션을 만들지 않고 기존 활성 세션을 반환한다
- **Verification:** auto

### S12: 성공 시도는 영상 URI와 함께 개별 기록으로 저장한다

- **Given** 활성 세션, 선택한 색상, 읽을 수 있는 녹화 캐시 파일, 성공한
  MediaStore 저장 결과가 있다
- **When** 사용자가 성공을 선택한다
- **Then** 성공, 색상, 기록 시각, MediaStore URI를 가진 시도 하나를 저장하고
  해당 색상과 전체 성공 집계를 증가시킨다
- **Verification:** auto

### S13: 성공 영상 저장 실패는 재시도 가능한 시도를 보존한다

- **Given** 활성 세션, 선택한 색상, 읽을 수 있는 녹화 캐시 파일, 실패한
  MediaStore 저장 결과가 있다
- **When** 사용자가 성공을 선택한다
- **Then** `savePending` 상태와 캐시 경로를 가진 시도를 저장하고 캐시 파일을
  삭제하지 않는다
- **Verification:** auto

### S14: 실패 시도는 성공 집계에 넣지 않고 종료 시 삭제한다

- **Given** 활성 세션에 실패 선택한 캐시 시도가 하나 이상 있고 성공 영상도 있다
- **When** 사용자가 운동 종료를 확정한다
- **Then** 실패 캐시 파일만 삭제하고 성공 영상은 보존하며 실패 시도는 성공
  집계에 포함하지 않는다
- **Verification:** auto

### S15: 운동 종료 후에는 홈으로 돌아가고 세션을 복원하지 않는다

- **Given** 활성 세션과 그 세션의 시도 기록이 있다
- **When** 사용자가 운동 종료를 확정한다
- **Then** 세션에 종료 시각과 `ended` 상태를 저장하고 홈 경로를 반환하며 다음
  앱 시작 시 세션 보드를 반환하지 않는다
- **Verification:** auto

### S16: 실제 Android 권한 팝업을 완료하면 온보딩에서 다음 단계로 갈 수 있다

- **Given** 카메라와 마이크 권한이 없는 실제 Android 기기와 첫 온보딩 화면이 있다
- **When** 사용자가 두 권한을 모두 허용한다
- **Then** 권한 단계의 다음 버튼이 활성화되고 홈으로 진행할 수 있다
- **Verification:** manual - S1과 S2가 권한 상태별 경로와 기록 차단을 자동 검증한다. Android 시스템 권한 팝업과 실제 Compose 화면의 상호작용만 기기 관찰이 필요하다.

## Out of Scope

- Supabase 인증과 서버 동기화
- GPS, 지도, 주소 입력, 암장 중복 탐지와 병합
- 트리밍 UI, 리포트, 인스타 공유 UI
- 두 개 이상의 동시 활성 세션
- 기존 미디어 스파이크의 edit-list 트리밍과 공유 인텐트 계약

</frozen-after-approval>

## Coverage Map

| Scenario | Verification | Status | Test | Implementation |
|----------|--------------|--------|------|----------------|
| S1 | auto | verified | `SessionLoopServiceTest.kt` | `SessionLoopService.kt` |
| S2 | auto | verified | `SessionLoopServiceTest.kt`, `RoomSessionLoopRepositoryTest.kt` | `SessionLoopRoom.kt` |
| S3 | auto | verified | `SessionLoopServiceTest.kt`, `RoomSessionLoopRepositoryTest.kt` | `SessionLoopRoom.kt` |
| S4 | auto | verified | `SessionLoopServiceTest.kt` | `SessionLoopService.kt` |
| S5 | auto | verified | `SessionLoopServiceTest.kt` | `SessionLoopService.kt` |
| S6 | auto | verified | `SessionLoopServiceTest.kt` | `SessionLoopService.kt` |
| S7 | auto | verified | `SessionLoopServiceTest.kt` | `SessionLoopService.kt` |
| S8 | auto | verified | `SessionLoopServiceTest.kt` | `SessionLoopService.kt` |
| S9 | auto | verified | `SessionLoopServiceTest.kt` | `SessionLoopService.kt` |
| S10 | auto | verified | `SessionLoopServiceTest.kt`, `RoomSessionLoopRepositoryTest.kt` | `SessionLoopRoom.kt` |
| S11 | auto | verified | `SessionLoopServiceTest.kt` | `SessionLoopService.kt` |
| S12 | auto | verified | `AttemptServiceTest.kt`, `RoomSessionLoopRepositoryTest.kt` | `AttemptService.kt` |
| S13 | auto | verified | `AttemptServiceTest.kt` | `AttemptService.kt` |
| S14 | auto | verified | `AttemptServiceTest.kt`, `RoomSessionLoopRepositoryTest.kt` | `AttemptService.kt` |
| S15 | auto | verified | `AttemptServiceTest.kt`, `RoomSessionLoopRepositoryTest.kt` | `MainActivity.kt` |
| S16 | manual | pending | walkthrough in Verification Log | - |

## Notes

- 기준 설계: [Phase 1 세션 루프](../design/phase-1-session-loop.md)
- 기존 미디어 경계: [모바일 미디어 스파이크](scenario-mobile-media-spike.md)
- 자동 테스트 기준 명령은 현재 shared JVM 단위 테스트와 Android 단위 테스트다.
  Compose navigation과 Room gateway를 추가하면 같은 명령에 해당 테스트를 포함한다.

## Verification Log

- 2026-07-22: `:shared:jvmTest :androidApp:testDebugUnitTest :androidApp:assembleDebug`
  실행 완료. shared와 Android 테스트 XML에 failures/errors가 없고 debug APK가 생성됐다.
- S16은 실제 Android 시스템 권한 팝업과 Compose 버튼 상호작용을 관찰해야 하므로
  SM-S911N 실기기 walkthrough가 남아 있다.
