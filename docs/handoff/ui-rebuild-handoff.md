# UI 재구현 핸드오프

갱신일: 2026-08-04

## 현재 위치

정본은 `docs/scenarios/scenario-ui-rebuild.md`다. 현재 상태는 `verifying`,
`loop_iteration: 6`, 기준 커밋은 `a4148c3`이다. `<frozen-after-approval>` 안의
행동 계약은 사용자 승인 없이 변경하지 않는다.

시나리오 상태는 다음과 같다.

- S2-S5, S11-S14, S16, S18: `verified`
- S6, S15, S17, S19-S24: `covered`
- S1, S7-S10: `pending`
- S13: Galaxy S23 시각, CameraX, Media3 수동 검토까지 `verified`

`covered` 행은 해당 focused 테스트가 통과했지만, 최신 변경을 포함한 전체 frozen
명령을 함께 실행하지 않아 아직 `verified`가 아니다. `pending` 행은 기존 테스트가
일부 근거를 제공하더라도 Then 절 전체를 증명하지 못하거나 구현 공백이 남아 있다.

## 이번에 추가된 행동

- 녹화 완료 뒤 분류 화면에서 시스템 뒤로가기를 실행하면 영상을
  `UNCLASSIFIED` Attempt로 보관한다.
- 아카이브의 `분류 필요` 항목에서 미분류 시도를 성공 또는 실패로 다시 분류한다.
- 분류 입력은 최초 선택만 처리하고 저장 중에는 성공, 실패 행동을 비활성화한다.
- 미디어 선택 바텀시트를 뒤로가기, 바깥 영역 누르기, 아래로 밀기로 닫으면
  `나중에`와 동일하게 `TRIM_PENDING`을 유지한다.
- `SAVE_PENDING` Attempt가 있으면 세션 종료를 보류하고 저장 재시도 또는
  `영상만 폐기하고 기록 유지`를 제공한다.
- 세션 종료 요약은 가짜 `최고 Lv.5` 대신 완등, 전체 시도, 정리 필요 수를 표시한다.
  정리 필요는 `TRIM_PENDING`, `TRIM_FAILED`, `TRIM_PROCESSING`을 합산한다.

## 현재 코드

주 진입점은 아래다.

- `frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt`
  - Room, CameraX, MediaStore, Media3와 화면 상태 연결
- `frontend/androidApp/src/main/kotlin/com/weclimb/android/AppState.kt`
  - 화면 상태와 분류, 트림, 저장 재시도 순수 상태 전이
- `frontend/androidApp/src/main/kotlin/com/weclimb/android/RebuiltSessionLoopUi.kt`
  - 화면 상태를 Compose renderer로 연결
- `frontend/androidApp/src/main/kotlin/com/weclimb/android/ArchivePlaybackUi.kt`
  - 아카이브 미분류 카드, 미디어 선택, 세션 종료 요약과 저장 대기 처리
- `shared/src/commonMain/kotlin/com/weclimb/session/AttemptService.kt`
  - `UNCLASSIFIED`, `SAVE_PENDING`, 성공과 실패 분류 전이
- `shared/src/commonMain/kotlin/com/weclimb/session/AttemptSummary.kt`
  - 완등, 전체 시도, 정리 필요 집계

기존 UI 범위는 유지된다.

- `00-01`: 로딩, 온보딩, 권한 상태, 인라인 배너
- `02`: 홈, 정적 레벨 스냅샷, 최근 영상, 최근 리포트 카드
- `03`: 암장 검색, 개인 암장 추가와 관리, 선택
- `04-08`: 보드, CameraX preview, 성공 처리 sheet, 트림, 아카이브, 재생 overlay
- `09-11`: 정적 Phase 3 화면. 저장, 공유, 통계 필터는 비활성 UI다.

## 검증 근거

2026-07-29 최신 변경 이전의 전체 frozen 명령은 Galaxy S23에서 connected 테스트
23개가 통과했다. S15와 S19의 강화된 focused 테스트도 통과했다.

2026-08-04에는 이번 변경과 직접 관련된 instrumentation 메서드 6개만 선택해
Galaxy S23(SM-S911N)에서 실행했고 모두 통과했다.

- S6, S24: 실제 종료 요약
- S17: 분류 전 뒤로가기와 미분류 저장
- S20: 아카이브 미분류 재분류
- S21: 분류 중복 입력 차단
- S22: 미디어 선택 sheet 닫기
- S23: 저장 대기 중 종료 보류와 영상 폐기

같은 작업 트리에서 아래 검증도 통과했다.

```sh
cd frontend
./gradlew :shared:jvmTest :androidApp:testDebugUnitTest \
  :androidApp:assembleDebug :androidApp:compileDebugAndroidTestKotlin
./gradlew :androidApp:lintDebug
```

기기는 기본 ADB 포트 `5037`이 아니라 기존 서버 포트 `5038`에 연결돼 있었다.
후속 실기기 명령에서도 `ANDROID_ADB_SERVER_PORT=5038`을 사용한다.

## 남은 일

2026-08-04 사용자 결정으로 아래 항목은 지금 바로 보강하지 않는다. 실제 기능 화면의
하드코딩 제거, 시도 결과와 영상 보관 분리, 새 디자인 기반 모션과 사용자 플로우를
먼저 완료한 뒤 여전히 필요한 검증인지 다시 감사한다.

현재 scenario-dev에 남아 있는 재검토 후보는 다음과 같다.

1. S1에서 실제 권한 요청과 앱 설정 이동 행동을 검증한다.
2. S7에서 잘못된 범위 제출과 실패 뒤 재시도의 실제 상태 전이를 통합 검증한다.
3. S8에서 카드 상태 라벨 전체와 Player에 전달되는 정확한 영상 URI를 검증한다.
4. S9에서 카메라 바인딩, 공유, 읽기 실패를 오류 심각도로 표시하고 실패한 행동을
   다시 실행하도록 연결한 뒤 검증한다. 저장 실패 재시도는 이미 완료됐다.
5. S10에서 예시 필터가 query, 썸네일 생성, 레벨 계산을 만들지 않는다는 결정적
   근거를 추가한다.
6. 새 우선 작업이 끝난 뒤 S1, S7-S10을 현재 구조에 맞게 다시 감사한다.
7. 그때도 유효한 항목만 새 구조에 맞춰 검증한다.
8. 최신 전체 회귀, Scenario Traceability와 scenario 마감을 진행한다.

## 먼저 진행할 별도 작업

별도 worktree에서 아래 순서로 진행한다.

1. 성공, 실패, 미분류의 시도 결과와 영상 보관, 트리밍, 삭제 결정을 독립시킨다.
2. 실제 기능 화면의 시간, 개수, 날짜, 상태와 미디어 메타데이터 하드코딩을 전수
   식별하고 실제 데이터로 연결한다.
3. 사용자와 별도 Claude Design에서 만든 새 design-bundle을 수령한다.
4. 새 시안이 정의한 로딩, 녹화, 저장, 트리밍, 피드백과 화면 전환 모션을 구현한다.
5. 전체 사용자 플로우를 다시 감사하고 기존 scenario의 남은 항목을 재분류한다.

디자인 산출물 수령 전에는 Codex가 임의의 모션이나 시각 구조를 확정하지 않는다.

전체 frozen 명령은 다음과 같다.

```sh
cd frontend
ANDROID_ADB_SERVER_PORT=5038 ./gradlew \
  :shared:jvmTest \
  :androidApp:testDebugUnitTest \
  :androidApp:connectedDebugAndroidTest
```

## 후속 설계와 제품 범위

`UNCLASSIFIED` 아카이브 카드, 분류 재진입, 저장 대기 종료 다이얼로그, 새로운 종료
요약은 기능 계약을 우선해 구현했다. 현재 design-bundle에는 이 상태의 전용 시안이
없다. 기능과 접근성 표면은 유지하되, 다음 design-bundle 갱신에서 시각 구조를
확정하고 Compose 충실도 패스를 다시 진행한다.

현재 계약 밖에서 보류한 항목은 다음과 같다.

- 기존 Room v1 설치본의 영상 상태 마이그레이션과 보존 정책
- 트리밍을 앱 종료 뒤에도 보장하는 WorkManager 또는 foreground service
- 세션 종료 사진과 후처리, 리포트 카드, 기록 통계, 리포트 기반 공유
- 아카이브 썸네일과 대용량 목록 성능 정책
- 임시 텍스트 아이콘의 SVG 또는 Compose vector 교체
- 재생 화면 탭으로 컨트롤을 숨기고 다시 표시하는 행동

## 작업 트리와 커밋 규칙

- 현재 변경은 별도 worktree가 상속할 수 있도록 완료 선언이 아닌 체크포인트
  커밋으로 보존한다. scenario 상태는 계속 `verifying`이다.
- 다른 변경을 reset 또는 checkout하지 않는다.
- design-bundle HTML은 사용자 기준 산출물이므로 이번 작업에서 수정하지 않는다.
