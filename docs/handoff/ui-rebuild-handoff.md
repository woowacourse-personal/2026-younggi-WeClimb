# UI 재구현 핸드오프

갱신일: 2026-07-29

## 현재 결과

UI 충실도 패스와 S1–S12, S14의 자동 검증은 완료됐다. 2026-07-29에
`shared:jvmTest`, Android 단위 테스트, Galaxy S23 연결 테스트 15개가 모두
통과했다. 이 문서는 더 이상 구현 재개 지침이 아니라 후속 S13 수동 검토의
시작점을 기록한다.

## 고정된 계약과 최근 사용자 결정

정본은 `docs/scenarios/scenario-ui-rebuild.md`다. `<frozen-after-approval>` 안은
사용자만 변경한다. 단, 사용자가 2026-07-27에 직접 결정한 내용을 반영해 S14가 이미
추가돼 있다.

- S1–S12: 현재 UI 재구현 계약
- S13: Galaxy S23의 시각/네이티브 미디어 수동 검토는 **별도 follow-up goal**로 보류
- S14: 세션 보드의 색 행 탭은 영상 URI·cachePath 없는 `SUCCESS` Attempt,
  `AttemptMediaState.NONE`으로 저장한다.

S14는 UI 충실도 패스 뒤 구현됐고 도메인, Room, 실제 보드 탭 instrumentation으로
검증됐다.

## 현재 코드 상태

UI 재구현 작업은 `d81b24d`에서 시작했고 scenario-dev Step 5의
`feat(android): rebuild session loop ui` 로컬 커밋으로 정리됐다.

주 진입점은 아래다.

- `frontend/androidApp/src/main/kotlin/com/weclimb/android/MainActivity.kt`
  - 상태, 저장소, CameraX와 Media3 연결
- `frontend/androidApp/src/main/kotlin/com/weclimb/android/RebuiltSessionLoopUi.kt`
  - 화면 상태를 각 Compose renderer로 연결
- `OnboardingHomeUi.kt`, `GymSessionUi.kt`, `CaptureTrimUi.kt`,
  `ArchivePlaybackUi.kt`, `StaticPhaseThreeUi.kt`
  - 화면군별 실제 Compose 구현. 호출되지 않던 legacy composable은 제거됨

이미 반영된 UI 구조:

- `00–01`: 로딩, 온보딩, 권한 상태, 인라인 배너
- `02`: 390dp 세로 스크롤 홈, 정적 레벨 스냅샷·최근 영상·최근 리포트 카드
- `03`: 검색, 개인 암장 추가/관리 sheet, 선택 카드와 하단 시작 CTA
- `04–08`: 보드·CameraX preview·성공 처리 sheet·트림·아카이브·재생 overlay
- `09–11`: static Phase 3 화면. 저장/공유/통계 필터는 비활성 UI로만 둔다.

## 남은 일

남은 계약은 S13 하나다. 별도 follow-up goal에서 Galaxy S23의 실제 앱 화면을
bundle 캡처와 나란히 보고 CameraX 구도, Media3 트리밍과 재생, 주요 터치 영역을
사람이 승인한다. HTML의 가짜 기기 프레임과 상태바는 비교 대상에서 제외한다.

`08-playback`의 탭으로 컨트롤 숨김/복귀는 frozen scenario에 없으므로 지금 구현하지
않는다.

## 검증 이력

마지막으로 확실히 성공한 전체 계약 명령:

```sh
cd frontend
./gradlew :shared:jvmTest :androidApp:testDebugUnitTest :androidApp:connectedDebugAndroidTest
```

2026-07-29 Galaxy S23(SM-S911N)에서 connected instrumentation 15개가 0 failures로
통과했다. 동일 실행에서 `shared:jvmTest`와 Android 단위 테스트도 통과했다.

## 작업 트리와 커밋 규칙

- 다른 변경을 reset/checkout하지 않는다.
- design-bundle HTML은 사용자가 만든 기준 산출물이므로 수정하지 않는다.
- 후속 S13에서 생기는 자잘한 시각 수정은 별도 계약과 커밋으로 남긴다.
