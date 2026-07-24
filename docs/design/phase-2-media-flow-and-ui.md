# Phase 2 미디어 처리와 UI 적용

## 개요

성공 영상을 자르거나 나중에 처리할 수 있게 하고, 기기에 저장된 성공 영상을 다시
찾아 재생하고 Android 공유 시트로 보낸다. 동시에 실제로 열리는 앱 화면을
design-bundle의 시각 체계로 Compose에 적용한다.

종료 사진, 리포트 카드, 기록 통계는 이번 범위에 넣지 않는다.

## 배경 및 목표

Phase 1은 촬영, 성공/실패 분류, 로컬 저장, 세션 기본 종료까지 검증했다. 그러나
성공 영상은 원본 그대로 저장되고, 사용자는 다시 찾아 정리하거나 공유할 앱 내
흐름이 없다.

이번 단계의 목표는 성공 영상을 찍은 뒤 자르기, 보관, 재생, 공유까지를 앱 안에서
완성하는 것이다.

## 범위

### 포함

- 성공 영상에서 즉시 트리밍 또는 나중 트리밍 선택
- 나중 트리밍 대기열 보존과 재진입
- 자르지 않음을 선택하면 원본 유지
- 성공 영상 아카이브 목록, 재생, 기기에서 삭제된 영상의 플레이스홀더
- 선택한 원본 또는 트리밍 완료 영상을 `ACTION_SEND`으로 공유
- 온보딩, 홈, 암장 선택, 세션 보드, 촬영, 트리밍, 아카이브에 design-bundle 시각
  체계 적용
- 기존 CameraX, Media3 Transformer, MediaStore, Room 계약의 확장

### 제외

- 종료 사진 촬영
- 세션 종료 후처리와 리포트 생성
- 리포트 카드 저장, 리포트 기반 Instagram 공유
- 기록 통계, 성장 곡선, 리포트 목록
- Instagram Stories 전용 `ADD_TO_STORY`
- 서버 동기화, 계정 기능, iOS 구현
- `07-session-end`, `08-report`, `09-records` Compose 화면 구현과 실제 진입
- 기존 Phase 1 설치본의 Room v1 성공 Attempt를 v2 영상 처리 상태로 변환하는
  마이그레이션과 회귀 검증

## 요구사항

### 기능 요구사항

- 성공 영상을 기록한 직후 사용자는 `자르기`, `나중에`, `원본 유지` 중 하나를
  선택할 수 있다.
- `나중에`를 선택한 성공 영상은 트리밍 대기 상태로 저장되고 세션 보드와
  아카이브에서 다시 열 수 있다.
- 트리밍 화면은 원본 재생, 시작과 끝 범위 선택, 완료, 원본 유지, 취소를 제공한다.
- 트리밍 실패 또는 범위 오류는 원본과 시도 기록을 보존하고 사용자가 재시도하거나
  원본을 유지할 수 있게 한다.
- 아카이브는 성공 영상만 최신순으로 보여 주고, 세션 날짜, 암장명, 색상, 원본 또는
  트리밍 상태를 표시한다.
- 선택한 영상은 앱에서 재생할 수 있다.
- MediaStore URI가 더 이상 읽히지 않으면 기록은 유지하고 재생 불가 상태를 보인다.
- 공유는 선택한 영상 URI를 `video/*`, 읽기 권한이 포함된 `ACTION_SEND`으로 보낸다.
- 화면 UI는 브랜드, 시맨틱 색상과 홀드 데이터 색상을 분리하고 `00-foundations`,
  `01`부터 `06` design-bundle을 Compose 기준으로 적용한다.

### 비기능 요구사항

- 모든 영상 처리 상태 변경은 사용자 화면 전환 전에 Room에 반영한다.
- 트리밍 출력 실패 시 손상되거나 빈 파일을 MediaStore에 남기지 않는다.
- 트리밍 중 앱이 종료되어도 원본과 대기 상태는 복구 가능해야 한다.
- 영상 재생, 트리밍, 공유는 실제 Android 기기에서 검증한다.
- 텍스트 대비와 터치 영역은 Compose 접근성 기준을 만족한다.
- 대용량 영상 처리 중 UI는 진행 상태와 재시도 가능한 오류를 보여야 한다.

## 설계 개요

성공 `Attempt`에 영상 처리 상태를 추가한다.

```text
촬영 성공
  -> 원본 MediaStore 저장
  -> trimPending | originalKept | trimProcessing | trimmed | trimFailed
  -> 아카이브 표시
  -> 재생 또는 ACTION_SEND 공유
```

Phase 1 성공 영상은 MediaStore Content URI로 남는다. 이번 구현은 이 URI를
Media3 입력으로 직접 사용하고, 원본을 보존한 별도 MediaStore 결과 URI를
`displayVideoUri`로 저장한다.

아카이브는 별도 리포트 엔터티를 만들지 않고, 종료 여부와 무관하게 성공 `Attempt`의
영상 상태를 읽어 구성한다. 영상이 기기에서 삭제돼도 Attempt와 세션 메타데이터는
유지한다.

UI는 공통 디자인 토큰을 적용해 현재 실제 화면을 구성한다. 기능이 아직 없는 종료,
리포트, 기록 화면은 route와 화면 구현을 추가하지 않는다.

## 영향 범위

- `shared/src/commonMain/kotlin/com/weclimb/session/`
  - `Attempt`, repository 계약, 처리 상태와 아카이브 조회
- `shared/src/commonMain/kotlin/com/weclimb/media/`
  - 트리밍 요청과 결과 상태, 공유 요청
- `frontend/androidApp/src/main/kotlin/com/weclimb/android/`
  - Room entity와 DAO migration
  - Media3 URI 입력과 출력 저장
  - 영상 재생과 Android 공유
  - Compose 화면, navigation, 디자인 토큰
- `frontend/androidApp/src/test/`
  - Room 상태 전이, Media3 gateway, 공유 Intent 테스트
- `design-system/we-climb/MASTER.md`
- `design-bundle/01-onboarding.html`부터 `design-bundle/06-trim.html`

## 구현 작업 목록

- [x] #1 트리밍 가능한 성공 영상의 영속 모델과 상태 전이를 설계하고 shared 테스트를
  작성한다.
- [x] #2 MediaStore URI와 Media3 Transformer를 연결하는 Android 트리밍 경계를
  스파이크와 테스트로 검증한다.
- [x] #3 Room entity, DAO, repository에 트리밍 상태와 아카이브 조회를 추가한다.
- [x] #4 즉시 트리밍, 나중에 처리, 원본 유지 흐름을 세션 보드와 트리밍 화면에
  연결한다.
- [x] #5 아카이브 목록, 영상 재생, 삭제된 영상 플레이스홀더를 구현한다.
- [x] #6 선택 영상의 `ACTION_SEND` 공유를 아카이브와 트리밍 화면에 연결한다.
- [ ] #7 기존 bundle의 토큰 방향 적용은 완료했다. 새 bundle을 받은 뒤
  [UI 재구현 계약](ui-rebuild-contract.md)에 따라 시안 충실도 Compose UI를
  재구현한다.
- [x] #8 단위, Room 통합, Android Intent 테스트와 실기기 트리밍, 재생, 공유
  walkthrough를 완료한다.

## 리스크 및 미해결 질문

- 기존 Phase 1 설치본의 Room v1 성공 Attempt를 현재 모델로 변환할지와, 변환 시
  기존 영상 URI를 어떻게 보존할지 후속 결정 필요
- 아카이브 영상 썸네일 생성 방식과 대용량 목록 성능 정책은 미정
