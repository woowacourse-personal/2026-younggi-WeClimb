---
title: 'Phase 1 Android 세션 루프 구현'
type: 'feature'
created: '2026-07-21'
status: 'in-progress'
baseline_commit: '54a643e'
context:
  - 'docs/design/phase-1-session-loop.md'
  - 'docs/scenarios/scenario-phase-1-session-loop.md'
  - 'docs/planning/03-tech-decisions.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Phase 1의 도메인 규칙과 미디어 스파이크는 존재하지만, Android 앱은
영속화되지 않는 실험용 View 화면이라 사용자가 온보딩부터 세션 종료까지 완료할 수 없다.

**Approach:** Room을 Android 로컬 데이터의 정본으로 두고 서울 시드, 게스트,
암장, 세션, 시도를 저장한다. Compose 화면에서 권한 게이트, 암장 선택, 단일 활성
세션, 녹화 결과 분류와 세션 종료를 기존 shared 서비스와 CameraX 구현에 연결한다.

## Boundaries & Constraints

**Always:** 승인된 `scenario-phase-1-session-loop.md`의 S1–S16 계약을 유지한다.
shared는 Android Room에 의존하지 않고, 변경은 불변 모델과 명시적 오류 처리로 만든다.
성공 영상은 MediaStore URI로 보존하고 실패 캐시는 세션 종료 때만 삭제한다.

**Ask First:** Room 스키마를 파괴적으로 변경해야 하거나, 승인된 시나리오 계약 및
Phase 1 범위를 바꿔야 할 경우.

**Never:** Supabase, 서버 동기화, 지도, 트리밍 UI, 리포트, 공유 UI 또는 iOS를
이번 작업에 추가하지 않는다. 기존 스파이크의 승인된 미디어 계약을 약화하지 않는다.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|----------------------------|----------------|
| 첫 실행 | 비어 있는 DB | 시드 7개와 미완료 온보딩을 표시한다 | 시드 읽기 실패는 재시도 가능한 화면 메시지로 표시한다 |
| 온보딩 완료 | 카메라·마이크 권한 허용 | 게스트를 저장하고 홈으로 간다 | 거부된 권한과 설정 이동 행동을 표시한다 |
| 세션 시작 | 활성 암장, 활성 세션 없음 | 하나의 활성 세션과 보드를 저장한다 | 기존 활성 세션이면 그것을 복원한다 |
| 녹화 성공 | 읽을 수 있는 캐시 영상 | MediaStore URI를 가진 성공 시도를 저장한다 | 저장 실패면 캐시 경로를 가진 `SAVE_PENDING` 시도를 보존한다 |
| 운동 종료 | 실패 캐시와 성공 시도 | 실패 캐시만 삭제하고 세션을 종료한다 | 삭제 오류를 사용자에게 알리고 성공 기록은 보존한다 |

</frozen-after-approval>

## Code Map

- `shared/src/commonMain/kotlin/com/weclimb/session/` -- Phase 1 도메인 모델과 순수 서비스
- `frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt` -- 현재 스파이크 진입점, Compose 앱 진입점으로 교체 대상
- `frontend/androidApp/src/main/kotlin/com/weclimb/android/CameraRecordingController.kt` -- CameraX 녹화 어댑터
- `frontend/androidApp/src/main/kotlin/com/weclimb/android/AndroidMediaGateways.kt` -- MediaStore·캐시 경계
- `docs/data/seoul-gym-seed.csv` -- 최초 실행에 import할 7개 암장

## Tasks & Acceptance

**Execution:**
- [x] Android Room 의존성, entity, DAO, database, repository gateway와 시드 import를 추가한다.
- [x] repository gateway를 shared 서비스와 묶어 온보딩, 암장, 활성 세션, 시도 저장을 앱 재시작 뒤에도 복원한다.
- [x] `MainActivity`를 Compose 기반 권한 게이트, 홈, 암장 선택, 세션 보드 및 종료 흐름으로 교체한다.
- [x] CameraX 녹화 완료를 색상 선택과 성공·실패 시도 저장에 연결한다.
- [ ] Room gateway와 Compose 상태 전이를 Android 단위 또는 instrumentation 테스트로 검증한다.

**Acceptance Criteria:**
- Given 첫 앱 실행, when 권한을 모두 허용하면, then 게스트와 시드가 저장되고 홈으로 이동한다.
- Given 활성 세션, when 앱을 다시 열면, then 홈이 아닌 해당 세션 보드를 복원한다.
- Given 선택한 암장과 녹화 영상, when 성공 또는 실패를 분류하면, then 각 결과가 계약에 맞게 저장되고 집계된다.
- Given 종료를 확정한 활성 세션, when 처리가 끝나면, then 실패 캐시만 정리되고 다음 시작은 홈으로 이동한다.

## Spec Change Log

## Design Notes

Room entity는 shared 모델과 별도로 두고 gateway 경계에서 상호 변환한다. DB는 단일
활성 세션을 조회로 강제하고, 앱은 저장 성공 뒤에만 화면 상태를 전환한다.

## Verification

**Commands:**
- `cd frontend && ./gradlew :shared:jvmTest :androidApp:testDebugUnitTest :androidApp:assembleDebug --console=plain --no-daemon` -- 모든 자동 테스트와 debug APK 빌드 성공
- `adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk` -- 실기기 권한 흐름 확인 준비
