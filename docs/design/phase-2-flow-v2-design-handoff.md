# Phase 2 Flow v2 디자인 인수인계

작성일: 2026-08-04
기준 커밋: `18fa309` (`checkpoint/ui-rebuild-flow`)
작업 브랜치: `Giyoul/phase2-design-v2`
브리프: [phase-2-flow-v2-design-requirements.md](phase-2-flow-v2-design-requirements.md)

---

## 1. Claude Design 프로젝트

| 항목 | 값 |
|---|---|
| 신규 프로젝트명 | **We-Climb Phase 2 Flow v2** |
| projectId | `2ebf0d36-9d28-4119-9efd-9c4fc13a44c6` |
| 소유자 | KimGiyoung |
| 기존 프로젝트 (미변경) | **We-Climb** · `ab43a926-940c-4b1f-bd66-c94391a5771d` |

기존 승인 디자인은 **읽기조차 하지 않았고 쓰기 대상에서 완전히 제외**했다.
`finalize_plan`의 `projectId`가 신규 프로젝트로 고정돼 있어 구조적으로 덮어쓸 수 없다.

### MCP 도구 호출 이력

| # | 메서드 | 대상 | 결과 |
|---|---|---|---|
| 1 | `list_projects` | — | `We-Climb` 1건 확인 (가용성 검증) |
| 2 | `create_project` | `We-Climb Phase 2 Flow v2` | `2ebf0d36-9d28-4119-9efd-9c4fc13a44c6` |
| 3 | `finalize_plan` | 신규 프로젝트 | `plan_2ebf0d369d284119_1b8e5dcad635`, writes 20 / deletes 0 |
| 4 | `write_files` | 신규 프로젝트 | `written: 20` |
| 5 | `list_files` | 신규 프로젝트 | 20개 경로 확인 |

`register_assets`는 호출하지 않았다. 모든 HTML 첫 줄에 `<!-- @dsCard group="…" -->`
마커가 있어 Design System 패널이 카드 인덱스를 자동 구성한다.

### MCP 표면에 대한 사실 기록

이 환경에 연결된 Claude Design MCP는 **`DesignSync`** 이며, 노출하는 메서드는
`list_projects` / `get_project` / `list_files` / `get_file` / `create_project` /
`finalize_plan` / `write_files` / `delete_files` / `register_assets` /
`unregister_assets` / `report_validate` 다.

**서버 측 생성(generation) 메서드는 존재하지 않는다.** 따라서 이 MCP에서
"디자인 생성"이란 프로젝트/파일 연산을 통해 산출물을 해당 프로젝트에 **저작·게시**
하는 것을 뜻한다. 산출물은 §1의 브리프를 계약으로 삼아 작성됐고, 위 호출 체인을
통해 신규 프로젝트에 반영됐다. 별도의 "생성 API 응답"은 존재하지 않으므로 그런
것이 있었던 것처럼 기록하지 않는다.

Figma MCP(`use_figma`, `generate_figma_design` 등)도 연결돼 있으나, 이 저장소의
정본 산출물 규약이 `design-bundle/`의 화면별 자립형 HTML이므로 사용하지 않았다.

---

## 2. 생성된 화면 · 상태 인벤토리

총 **20개 파일 / 60개 상태 프레임**.

### 기반 (`group="Foundations"`)

| 파일 | 프레임 | 내용 |
|---|---|---|
| `00-foundations.html` | — | 토큰 · 색 · 타이포 · **모션 토큰 표(라이브 표본)** · **실제 값 vs 미리보기 시각 규칙** · 축소 모션 대체 |
| `00-common.html` | A–H (8) | 초기 로딩 · 성공 배너 · **진행 배너(신규)** · 오류 배너+재시도 · 권한 거부 → 설정 · **리스트 로딩/빈/오류 3종(신규)** |
| `_base.css` | — | 공통 토큰 + **모션 토큰** + `prefers-reduced-motion` 축약 |

### 화면 (`group="Screens"`)

| 파일 | 프레임 | 상태 |
|---|---|---|
| `01-onboarding.html` | 기존 유지 | 변경 없음 |
| `02-home.html` | A–D (4) | 데이터 있음 · 첫 진입 빈 · **불러오는 중(신규)** · **조회 실패(신규)** |
| `03-gym-select.html` | 기존 유지 | 변경 없음 |
| `04-session-board.html` | A–D (4) | **시도 0건(신규)** · 진행 중(런타임 카운트) · 종료 확인(실제 요약 3값) · **저장 대기 종료 보류(신규)** |
| `05-capture.html` | A–H (8) | PREPARING · **READY(신규)** · RECORDING(타이머+펄스) · **RECORDED(신규)** · CLASSIFYING · **분류 처리 중(신규)** · 분류 전 뒤로가기 · ERROR |
| `05b-result-media-decision.html` | A–H (8) | **전량 신규.** 성공 시트 · **실패 시트** · 폐기 확인 · 닫기 경로 · 결정 완료 · 저장 대기 해소 · 영상만 폐기 확인 · 재저장 반복 실패 |
| `06-trim.html` | A–F (6) | 편집 대기 · **범위 무효(신규)** · 처리 중 · **취소 확인(신규)** · 재시도 가능 실패 · 완료 |
| `07-archive.html` | A–G (7) | 완등×{완료·대기·원본} · **실패×{원본·대기}(신규)** · **분류 필요(신규)** · **트리밍 실패(신규)** · 읽기 불가 · **로딩(신규)** · 빈 · **조회 실패(신규)** · **재분류 시트(신규)** |
| `08-playback.html` | A–E (5) | 재생 중 · **일시정지(신규)** · **버퍼링(신규)** · 컨트롤 숨김 · **재생 오류(신규)** |
| `09-session-end.html` | 미리보기 전용 | **변경 없음** |
| `10-report.html` | 미리보기 전용 | **변경 없음** |
| `11-records.html` | 미리보기 전용 | **변경 없음** |

### 명세 (`group="Specs"`, 전량 신규)

| 파일 | 내용 |
|---|---|
| `spec-interaction-matrix.html` | 도메인 상태 어휘 · 화면×상태×행동 · **뒤로가기/닫기 귀결표** · 파괴적 행동 목록 · 중복 실행 차단 지점 · 미리보기 표면 |
| `spec-flow-map.html` | 세션 주 경로 · 아카이브 경로 · 미리보기 경로 · 화면 전환 모션 · 진입점 요약 |
| `spec-components.html` | 12개 카테고리 컴포넌트·변형 전량 (라이브 표본 포함) + v2 신규 10종 요약 |
| `spec-motion.html` | **24개 모션 × 8항목** (트리거·시작·종료·지속·이징·중단·반복·축소 모션) + 금지 목록 |
| `spec-accessibility.html` | 대비 측정값 · 비색 신호 · 터치 타깃 · TalkBack 문구 · 포커스 순서 · 축소 모션/큰 글자 · 세이프 에어리어 · 인지 부하 · 20항목 체크리스트 |

---

## 3. v2 핵심 설계 결정

### 3.1 두 축 분리

`AttemptOutcome`(결과)과 `VideoAction`(영상 처리)을 독립시켰다.
**`FAILURE`도 `KEEP_ORIGINAL` · `TRIM_NOW` · `TRIM_LATER`를 모두 선택할 수 있다.**
`DISCARD`는 실패의 강조 기본값일 뿐 강제가 아니다.

화면 흐름도 두 단계로 분리했다 — `05`가 결과만 확정하고, `05b`가 성공·실패 모두에
대해 영상 처리를 묻는다.

### 3.2 기능 표면에서 제거한 값

| 값 | 위치 | 사유 |
|---|---|---|
| `Lv.5 파랑`, `Lv.6까지 78%` | 02 · 04 · 07 · 08 | 색→레벨 매핑 런타임 소스 없음 |
| `최고 Lv.5` 요약 타일 | 04 종료 다이얼로그 | 동일 |
| `26 완등` 고정 문구 | 04 하단 | `summarizeAttempts()`로 대체 |
| 카드 길이 배지 `0:12`·`0:24`·`0:31` | 02 · 07 | `ArchiveAttempt`에 duration 컬럼 없음 |
| 가짜 썸네일 | 02 · 07 | 썸네일 생성 미구현 → 색 파생 플레이스홀더로 대체 |
| `기영 님` 인사말 | 02 | 사용자 프로필 소스 없음 (`guest_profiles`는 id만) |
| "백그라운드에서 이어져요" | 06 처리 중 | **사실이 아님.** WorkManager 미구현, 중단 시 `TRIM_FAILED` |

레벨·리포트 예시 값은 미리보기 전용 블록(점선 보라 + 워터마크 + 배지) 안에만 남았다.

### 3.3 세션 보드 진행 막대 — 바퀴 덧칠 (v1 규칙 복원)

v2 1차 작업에서 진행 막대를 "최다 색 대비 비율"로 잘못 바꿨다가 **v1의 바퀴 덧칠
규칙으로 되돌렸다.** `LAP = 5완등`, `lap = count/5`, `rem = count%5`.
`lap ≥ 1`이면 바탕 100%를 shade `lap−1`로 깔고 그 위에 `rem/5`를 shade `lap`으로
덧칠한다.

비율 막대는 다른 색을 올릴 때마다 내 막대가 **줄어드는** 문제가 있었다 — 기록은
늘었는데 시각적으로 후퇴한다. 절대량 방식이 맞다.

바퀴 shade는 `RGB × 0.70`이며 표준 색 램프(lap 0~3)를 `spec-components.html`과
MASTER.md에 표로 고정했다. 초록·파랑 lap1~2는 v1 승인값을 그대로 유지했고,
검정은 배경과 가까워 어둡게 할 수 없으므로 slate 램프로 **밝아지는** 예외를 둔다.
바퀴 넘김 모션(차오름 → 승격 → 리셋)은 `spec-motion.html` #12에 8항목으로 명세했다.

### 3.4 안전 기본값

모든 뒤로가기·닫기가 데이터를 삭제하지 않는다. 시트 닫기 → `TRIM_PENDING`,
분류 전 이탈 → `UNCLASSIFIED`, 저장 대기 → 유지. 삭제는 명시적 파괴 버튼 +
**남는 것 / 사라지는 것**을 나열한 확인 다이얼로그를 통해서만 일어난다.

---

## 4. 미해결 · 구현 트리 결정 필요

### 4.1 실패 영상 보관 — 도메인 변경 필요 (가장 중요)

시안은 `FAILURE` + 영상 보관/트리밍을 UI 계약으로 고정했으나, 현재 코드는 이를
지원하지 않는다.

- `AttemptService.classifyUnclassifiedFailure()`는 `cache.delete(cachePath)`로
  실패 영상을 즉시 삭제한다.
- `SessionFinisher.finish()`는 `outcome == FAILURE`인 Attempt의 캐시를 삭제하고
  **결과 목록에서 해당 Attempt 자체를 제외**(`filterNot`)한다.
- `ArchiveAttemptRow` 쿼리가 실패 Attempt를 포함하는지 확인이 필요하다.

**필요한 변경(디자인 소유 아님):** 실패 Attempt의 `AttemptMedia` 유지, 세션 종료
시 조건부 삭제(사용자가 `DISCARD`를 고른 경우에만), 아카이브 쿼리 확장.
이는 데이터 의미 변경이므로 구현 착수 전에 사용자 확인을 권한다.

### 4.2 그 밖의 미해결 항목

| # | 항목 | 상태 |
|---|---|---|
| 1 | `VideoAction` enum의 실제 배치 위치 (shared vs androidApp) | 미정. 시안은 결정 시점 값으로만 정의 |
| 2 | 아카이브 썸네일 생성과 duration 컬럼 | 미구현. 시안은 플레이스홀더로 우회 |
| 3 | 아카이브 필터 query | 미구현. 칩은 미리보기 표시 |
| 4 | 색 → 레벨 매핑 | 미구현. 기능 표면에서 레벨 제거로 우회 |
| 5 | 트리밍 백그라운드 보장 (WorkManager / foreground service) | 후속 기술 범위. 시안 문구를 사실에 맞게 수정함 |
| 6 | 프레임 썸네일 추출 방식과 성능 | 미정. 추출 실패해도 트리밍은 가능하도록 설계 |
| 7 | Room v1 설치본 마이그레이션 | 기존 미해결 유지 |
| 8 | 스크린샷 비교 허용 오차·도구 | 기존 미해결 유지 |
| 9 | 임시 텍스트 아이콘 → SVG/Compose 벡터 교체 | 기존 미해결 유지 |
| 10 | 세션 시작 시각 필드명 (`startedAtEpochMillis` 가정) | 타이머 주석의 소스명 확인 필요 |

---

## 5. 변경된 파일 (정확 목록)

### 수정 (10)

```
design-bundle/_base.css
design-bundle/00-foundations.html
design-bundle/00-common.html
design-bundle/02-home.html
design-bundle/04-session-board.html
design-bundle/05-capture.html
design-bundle/06-trim.html
design-bundle/07-archive.html
design-bundle/08-playback.html
design-system/we-climb/MASTER.md
```

### 신규 (7)

```
design-bundle/05b-result-media-decision.html
design-bundle/spec-interaction-matrix.html
design-bundle/spec-flow-map.html
design-bundle/spec-components.html
design-bundle/spec-motion.html
design-bundle/spec-accessibility.html
docs/design/phase-2-flow-v2-design-requirements.md
docs/design/phase-2-flow-v2-design-handoff.md
```

### 의도적으로 변경하지 않음

```
design-bundle/01-onboarding.html      새 플로우가 온보딩을 바꾸지 않음
design-bundle/03-gym-select.html      동일
design-bundle/09-session-end.html     미리보기 전용 유지
design-bundle/10-report.html          미리보기 전용 유지
design-bundle/11-records.html         미리보기 전용 유지
frontend/**  shared/**                이 작업 트리의 소유 범위 밖
docs/scenarios/**                     frozen 블록 보존
```

### MASTER.md 변경 요약

- **두 축 분리** 절 신설 (결과 · `VideoAction` · `AttemptMediaState` 매핑)
- 앱 레벨 색에 **기능 표면 `Lv.` 표기 금지** 제약 추가
- **신규 컴포넌트** 절 신설 (v2 11종) + **세션 보드 진행 막대 바퀴 덧칠 규칙과
  10색 shade 램프 표**
- **UI 폴리시** 전면 개정 (동적 값 5상태 · 가짜 값 금지 · 뒤로가기 불변 규칙 ·
  트리밍 백그라운드 문구 정정 · 오류 출구 2개)
- **모션** 절 전면 개정 (토큰표 · Compose 매핑 · 중단 규칙 · 금지 목록 ·
  축소 모션 대체)
- Pre-Delivery 체크리스트에 **Phase 2 Flow v2 14항목** 추가

---

## 6. 다음 단계 (구현 트리)

1. §4.1 실패 영상 보관의 도메인·Room 변경 범위를 사용자와 확정한다.
2. `spec-interaction-matrix.html`의 **결과 상태** 열을 계약으로 `AppState` 전이를 맞춘다.
3. `spec-motion.html`의 24개 모션을 `MotionTokens` 객체로 구현한다
   (중단 동작 — 현재 값에서 역방향 — 이 가장 틀리기 쉽다).
4. `spec-accessibility.html`의 20항목 체크리스트로 Galaxy S23 실기기 검토를 수행한다.
5. `ui-rebuild-contract.md` #9와 `phase-2-media-flow-and-ui.md` #10의 남은 scenario를
   새 구조 기준으로 재감사한다.
