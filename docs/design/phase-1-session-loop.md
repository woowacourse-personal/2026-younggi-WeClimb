# Phase 1 세션 루프 구현 설계

## 개요

Phase 1은 권한을 완료한 사용자가 서울 암장을 고르고, 진행 중 세션에서 촬영한
각 시도를 성공 또는 실패로 분류하며, 앱을 재시작해도 세션을 이어 가는 Android
우선 기능이다. 기존 미디어 스파이크의 CameraX, MediaStore, 캐시 처리 구현을
제품 흐름에 연결한다.

## 배경 및 목표

미디어 스파이크는 녹화, 성공 영상 저장, 실패 영상 처리, 공유의 기술 가능성을
검증했다. 이제 실제 사용자가 운동을 시작하고 기록하는 화면 흐름으로 바꾼다.
사용자는 카메라와 마이크 권한을 완료한 뒤 암장을 선택하고, 여러 시도를
기록한 후 운동 종료를 통해 홈으로 돌아간다.

## 범위

### 포함

- 로컬 게스트 온보딩과 카메라, 마이크 권한 게이트
- 현재 서울 시드 7개와 사용자 추가 암장을 포함한 암장 선택
- 이름만 받는 개인 암장 추가, 수정, 숨기기
- 활성 세션의 로컬 복원과 홈 이동 차단
- 색상별 완등 카운트를 보이는 세션 보드
- 개별 시도 기록, 성공 또는 실패 분류, 성공 영상의 즉시 MediaStore 저장
- 운동 종료 확인, 세션 종료 상태 저장, 홈 복귀
- Phase 1 전용 Given, When, Then 시나리오 계약과 자동 테스트

### 제외

- Supabase 로그인, 서버 세션 동기화, RLS
- 주소와 GPS 기반 암장 중복 탐지, 시드와 개인 암장의 병합
- 지도, 장소 검색 API, 좌표 보강
- 트리밍 UI, 세션 종료 사진 선택, 리포트, 인스타 공유 UI
- 다른 사용자의 콘텐츠, 크루, iOS 구현
- 암장 색상과 앱 레벨의 정식 매핑

## 요구사항

### 기능 요구사항

- 온보딩은 카메라와 마이크 권한이 모두 허용될 때까지 다음 단계로 진행할 수
  없다. 거부 시 재요청을 제공하고, 재요청할 수 없으면 Android 설정 열기를
  제공한다.
- 활성 세션이 있으면 앱 재시작 후 세션 보드로 복원한다. 활성 세션 중에는
  홈으로 이동할 수 없다.
- 암장 선택은 [암장 선택 화면](../../design-bundle/03-gym-select.html)을 기준으로
  서울 시드와 활성 개인 암장을 이름 부분 일치로 필터링한다.
- 검색 결과가 없으면 이름만으로 개인 암장을 생성할 수 있다. 개인 암장은
  수정할 수 있고, 연결된 세션이 있으면 숨기기만 가능하다.
- 세션은 선택한 암장, 시작 시각, 상태를 가진다. 새 세션은 활성 세션이 없을
  때만 시작할 수 있다.
- [세션 보드](../../design-bundle/04-session-board.html)는 색상별 성공 카운트와
  전체 성공 수를 개별 시도 기록에서 집계해 표시한다.
- [촬영 화면](../../design-bundle/05-capture.html)에서 각 시도는 선택한 색상,
  시각, 결과를 가진다. 성공은 즉시 `Movies/WeClimb`에 저장하고 콘텐츠 URI를
  시도에 기록한다.
- 실패 영상은 앱 캐시의 삭제 대상으로 남긴다. 운동 종료 시 실패 캐시 파일만
  정리한다.
- 운동 종료는 확인 뒤 세션 상태를 `ended`로 바꾸고 홈으로 복귀한다. 트리밍과
  리포트는 시작하지 않는다.

### 비기능 요구사항

- 세션, 암장, 시도 변경은 사용자 동작이 끝나기 전에 로컬 저장에 반영한다.
- 성공 영상 저장 실패 시 시도는 성공 후보와 캐시 원본을 보존하고, 사용자가
  같은 세션에서 재시도할 수 있어야 한다.
- Android 권한, CameraX, MediaStore 오류는 사용자에게 원인과 가능한 다음
  행동을 보인다.
- 기존 미디어 스파이크의 자동 테스트가 계속 통과해야 한다.
- UI는 `01-onboarding.html`, `03-gym-select.html`, `04-session-board.html`,
  `05-capture.html`과 `design-system/we-climb/MASTER.md`를 기준으로 Compose로
  구현한다.

## 설계 개요

```text
온보딩
  -> 권한 게이트
  -> 홈
  -> 암장 선택
  -> 활성 세션 생성
  -> 세션 보드 <-> 촬영
  -> 성공 또는 실패 시도 저장
  -> 운동 종료
  -> 홈
```

`shared`에는 불변 도메인 모델과 repository 인터페이스를 둔다.

- `Gym`: `id`, `name`, `source`, `visibility`
- `Session`: `id`, `gymId`, `startedAt`, `endedAt`, `status`
- `Attempt`: `id`, `sessionId`, `color`, `recordedAt`, `outcome`, `videoUri`,
  `cachePath`
- `GymSource`: `seeded`, `userAdded`
- `SessionStatus`: `active`, `ended`
- `AttemptOutcome`: `success`, `failure`, `savePending`

`frontend/androidApp`에는 Android Room 기반 local gateway, 시드 import, 권한
gateway, Compose navigation과 CameraX adapter를 둔다. Room은 Android 로컬
데이터의 정본이며, `shared`는 Room 구현에 의존하지 않는다.

`docs/data/seoul-gym-seed.csv`의 7개 지점은 앱 최초 실행 시 한 번 import한다.
시드 행은 업데이트 가능하지만, 사용자 추가 항목과 자동 병합하지 않는다.

기존 `RecordingCoordinator`, `VideoPersistence`, `CameraRecordingController`는
새 `SessionAttemptCoordinator`에서 감싼다. 이 coordinator만 시도 저장 순서를
결정한다.

1. 녹화 완료 후 임시 `Attempt`를 만든다.
2. 실패 선택은 캐시 삭제 대기 상태로 저장한다.
3. 성공 선택은 MediaStore 승격을 요청한다.
4. 승격 성공 시 콘텐츠 URI를 시도에 저장한다.
5. 승격 실패 시 캐시 경로와 `savePending` 상태를 보존한다.
6. 운동 종료 시 실패 캐시만 삭제하고 세션을 `ended`로 저장한다.

## 시나리오 계약

`docs/scenarios/scenario-phase-1-session-loop.md`를 새로 만들고 사용자 승인 뒤
고정한다. 기존 `scenario-mobile-media-spike.md`는 기술 스파이크 계약이므로
수정하지 않는다.

- 자동: 권한 미완료 시 온보딩 진행 차단
- 자동: 활성 세션의 앱 재시작 복원과 홈 이동 차단
- 자동: 시드와 개인 암장 검색, 개인 암장 수정과 숨기기
- 자동: 세션 생성, 개별 시도 저장, 색상별 집계
- 자동: 성공 URI 저장 실패 시 캐시 원본 보존
- 자동: 운동 종료 시 실패 캐시 삭제와 세션 종료
- 수동: 실제 기기에서 Android 권한 팝업과 Compose 화면 흐름 확인

수동 항목은 권한 게이트의 UI 관찰만 남기고, 권한 상태별 상태 전이와 기록
차단은 자동 테스트로 분리한다.

## 영향 범위

- `shared/src/commonMain/kotlin/com/weclimb/session/`
- `shared/src/commonMain/kotlin/com/weclimb/media/`
- `shared/src/commonTest/kotlin/com/weclimb/`
- `frontend/androidApp/src/main/kotlin/com/weclimb/android/`
- `frontend/androidApp/src/test/kotlin/com/weclimb/android/`
- `docs/data/seoul-gym-seed.csv`
- `docs/scenarios/scenario-phase-1-session-loop.md`

## 구현 작업 목록

- [ ] #1 Phase 1 시나리오 계약을 작성하고 사용자 승인 뒤 고정한다.
- [ ] #2 shared에 암장, 세션, 시도 도메인 모델과 repository 인터페이스를 추가한다.
- [ ] #3 Android Room gateway와 서울 시드 import를 구현한다.
- [ ] #4 권한 게이트를 가진 온보딩과 활성 세션 복원을 구현한다.
- [ ] #5 암장 검색, 개인 암장 추가, 수정, 숨기기를 구현한다.
- [ ] #6 세션 생성, 세션 보드 집계, 운동 종료를 구현한다.
- [ ] #7 기존 미디어 스파이크를 개별 시도 저장 흐름에 연결한다.
- [ ] #8 Compose 화면을 디자인 자산 기준으로 구현한다.
- [ ] #9 시나리오별 자동 테스트와 필요한 실기기 확인을 완료한다.

## 리스크 및 미해결 질문

- 현재 서울 시드 7개는 Phase 1 개발과 첫 필드 테스트에 유지한다.
- 추가 시드는 크루의 실제 방문권이 넓어질 때만 공식 출처 기준으로 보강한다.
- 실패 캐시 삭제가 앱 강제 종료 뒤에도 보장되는지 확인해야 한다.
- 개인 암장과 시드 암장의 중복 및 병합은 GPS 또는 승인된 지도 제공사 도입 때
  설계한다.
