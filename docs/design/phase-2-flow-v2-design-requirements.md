# Phase 2 Flow v2 디자인 요구사항 (산출물 계약)

작성일: 2026-08-04
기준 커밋: `18fa309`
대상 Claude Design 프로젝트: **We-Climb Phase 2 Flow v2** (기존 `We-Climb` 프로젝트는 덮어쓰지 않는다)

이 문서는 Phase 2 Flow v2 디자인 생성의 **브리프이자 수용 기준**이다. 생성된 결과는
이 문서의 모든 항목을 만족해야 하며, 만족하지 못한 항목은 인수인계 문서에
미해결로 남긴다.

---

## 0. 소유 범위와 금지 사항

이 작업 트리는 **디자인 산출물만** 소유한다.

**소유:**

- `design-bundle/**`
- `design-system/we-climb/MASTER.md`
- `docs/design/phase-2-flow-v2-design-requirements.md` (이 문서)
- `docs/design/phase-2-flow-v2-design-handoff.md`

**금지:**

- Kotlin, Gradle, Room, `shared/` 도메인 코드, 테스트 수정
- `docs/scenarios/**`의 `<frozen-after-approval>` 블록 수정
- 승인된 범위를 넘는 제품 행동 발명
- 기존 시각 아이덴티티(색, 서체, 카드 구조, 프레임 규격) 변경 — 플로우 변경이
  새 컴포넌트를 강제하는 경우에만 신규 컴포넌트를 추가한다

---

## 1. 핵심 모델 변경: 시도 결과와 영상 처리의 분리

v1 시안은 "성공=영상 보관 / 실패=영상 삭제"를 한 결정으로 묶었다. v2는 이를 **두
개의 독립 축**으로 분리한다.

### 축 A — Attempt 결과 (`AttemptOutcome`)

| 값 | 의미 | 런타임 소스 |
|---|---|---|
| `SUCCESS` | 완등 | `shared/session/AttemptService.kt` |
| `FAILURE` | 실패 | 〃 |
| `UNCLASSIFIED` | 분류 전 이탈로 보류 | 〃 |
| `SAVE_PENDING` | 결과는 정해졌으나 MediaStore 저장 실패 | 〃 |

### 축 B — 영상 처리 결정 (`VideoAction`, 신규 · 결정 시점 값)

| 값 | 결과 미디어 상태 | 의미 |
|---|---|---|
| `TRIM_NOW` | `TRIM_PROCESSING` → `TRIMMED` / `TRIM_FAILED` | 지금 자르기 |
| `TRIM_LATER` | `TRIM_PENDING` | 나중에 자르기 (대기함) |
| `KEEP_ORIGINAL` | `ORIGINAL_KEPT` | 원본 그대로 보관 |
| `DISCARD` | `NONE` | 영상만 폐기, Attempt 기록은 유지 |

`AttemptMediaState`는 `shared/media/AttemptMediaService.kt`의 기존 enum
(`NONE`, `TRIM_PENDING`, `ORIGINAL_KEPT`, `TRIM_PROCESSING`, `TRIMMED`,
`TRIM_FAILED`)을 그대로 사용한다.

### 필수 요구 — 실패 Attempt의 영상 보관

시안은 **`FAILURE`에서도 `KEEP_ORIGINAL`, `TRIM_NOW`, `TRIM_LATER`를 선택할 수
있어야 한다.** 즉 축 A와 축 B의 조합은 다음과 같이 열려 있다.

| 결과 \ 영상 | TRIM_NOW | TRIM_LATER | KEEP_ORIGINAL | DISCARD |
|---|---|---|---|---|
| `SUCCESS` | ✅ | ✅ | ✅ | ✅ |
| `FAILURE` | ✅ | ✅ | ✅ | ✅ (기본 제안) |
| `UNCLASSIFIED` | — | 보류 중 유지 | — | — |

`DISCARD`는 실패의 **기본 제안**이지 강제가 아니다. 실패 영상을 보관하면
아카이브에 `실패` 배지와 함께 나타난다.

> 구현 함의(디자인 소유 아님, 인수인계에 기록): 현재
> `AttemptService.classifyUnclassifiedFailure`와 `SessionFinisher.finish`는
> `FAILURE` 캐시를 삭제한다. 실패 영상 보관을 지원하려면 도메인·Room 변경이
> 필요하다. 이 시안은 그 UI 계약을 고정하고, 코드 변경은 구현 트리가 수행한다.

---

## 2. 화면별 산출물 계약

### 2.1 `00` 공통 상태 + 모션 토큰 (`00-common.html`, `00-foundations.html`)

- 초기 로딩, 성공 배너, 오류 배너(재시도 포함), 권한 거부 → 설정 이동
- **신규**: 인라인 진행 배너(처리 중), 비어 있음 / 불러오는 중 / 오류의 3종
  리스트 상태 템플릿
- **신규**: 모션 토큰 표(§4)를 `00-foundations.html`에 시각 표본과 함께 수록
- 축소 모션(reduced motion) 대체 표기를 각 토큰 옆에 병기

### 2.2 `02` 홈 (`02-home.html`)

- **실제 값과 미리보기 전용 값이 시각적으로 구분되어야 한다.**
  - 실제 값: 실선 테두리 + 정상 대비
  - 미리보기 전용: `Phase 3 · 비활성` 보라 배지 + 점선 테두리 + 불투명도 저하 +
    `미리보기` 워터마크 라벨
- 실제 값: 인사말, 저장된 영상 개수, 최근 영상 카드(암장명·촬영일·색·상태)
- 미리보기 전용: 레벨 스냅샷 링, 성장 차트, 최근 리포트 카드
- **가짜 레벨(`Lv.5 파랑`), 가짜 진행률(`78%`), 가짜 완등 수(`14`)는 기능 표면에서
  제거**하고 미리보기 블록 안으로만 격리한다
- 홈의 상태 변형: 데이터 있음 / 첫 진입 빈 상태 / 불러오는 중 / 조회 실패

### 2.3 `04` 세션 보드 (`04-session-board.html`)

- **런타임 카운트**: 색상별 완등 수는 `attempts` 집계에서 온다. 고정 숫자
  (`26 완등`, `최고 Lv.5`) 금지
- 세션 타이머는 `activeSession.startedAtEpochMillis` 기준 경과 시간
- **세션 종료 요약**은 `summarizeAttempts()`의 세 값만 표시한다
  - `successCount` 완등
  - `totalCount` 전체 시도
  - `mediaActionCount` 정리 필요 (`TRIM_PENDING` + `TRIM_FAILED` + `TRIM_PROCESSING`)
  - **`최고 Lv.` 타일 제거** (런타임 소스 없음)
- 상태 변형: 시도 0건 보드 / 시도 있음 보드 / 종료 확인 다이얼로그 /
  저장 대기(`SAVE_PENDING`) 때문에 종료가 보류된 다이얼로그

### 2.4 `05` 촬영 (`05-capture.html`)

카메라 표면의 상태만 담는다. 필수 상태:

| # | 상태 | 설명 |
|---|---|---|
| 1 | `PREPARING` | CameraX 바인딩 대기 |
| 2 | `READY` | 프리뷰 활성, 녹화 시작 가능 |
| 3 | `RECORDING` | 실시간 타이머 + 펄스 인디케이터 |
| 4 | `RECORDED` | 정지 직후, 마지막 프레임 |
| 5 | `CLASSIFYING` | 결과 선택 입력 대기 |
| 6 | `CLASSIFICATION_IN_PROGRESS` | 저장 중, 중복 입력 차단 |
| 7 | `ERROR` | 바인딩 실패, 재시도/설정 |

- 뒤로가기 경로: `RECORDED`/`CLASSIFYING`에서 시스템 뒤로가기 → `UNCLASSIFIED`
  보관 확인
- 녹화 타이머는 `mm:ss` 실시간 값, 고정 `0:08`/`0:19` 금지

### 2.5 결과 · 미디어 결정 플로우 (`05b-result-media-decision.html`, 신규)

- 1단계: 결과 선택 (`SUCCESS` / `FAILURE`), 저장 중 비활성 상태 포함
- 2단계: 영상 처리 선택 시트 — **성공 변형과 실패 변형 각각**
  - 성공 기본 강조: `TRIM_NOW`
  - 실패 기본 강조: `DISCARD`, 단 `KEEP_ORIGINAL`/`TRIM_LATER`/`TRIM_NOW` 모두 노출
- `DISCARD` 확정은 **파괴적 확인 다이얼로그**를 거친다
- 시트 닫기 경로(뒤로가기 · 바깥 탭 · 아래로 밀기)는 모두 `TRIM_LATER`와 동일한
  `TRIM_PENDING` 결과로 수렴하며, 시트에 그 사실을 명시한다
- **저장 대기 해소**: `SAVE_PENDING` 상태에서 `다시 저장` / `영상만 폐기하고 기록
  유지` 두 경로. 폐기는 파괴적 확인을 거친다
- 저장 재시도 실패 반복 상태 포함

### 2.6 `06` 트리밍 (`06-trim.html`)

- **실제 재생 길이**: `selectedVideoDurationMillis`가 Media3
  `Player.duration`에서 온다는 주석. 고정 `19_000L` 표기 금지
- **실제 선택 범위**: `lastTrimStartMillis` / `lastTrimEndMillis`
- **프레임 타임라인**: 실제 프레임 썸네일 스트립 자리 표시 + 좌우 핸들
- 필수 상태: `편집(대기)` / `범위 무효(INVALID_RANGE·OUT_OF_BOUNDS)` /
  `처리 중(TRIM_PROCESSING)` / `재시도 가능 실패(TRIM_FAILED)` / `완료(TRIMMED)`
- 처리 중에는 중복 실행 차단, 뒤로가기 시 취소 확인
- 하단 행동: 원본 유지 · 자르고 저장 · 나중에 · (완료 후) 재생 · 공유

### 2.7 `07` 아카이브 (`07-archive.html`)

카드가 표현해야 하는 축:

- 결과 배지: `완등` / `실패` / `분류 필요`
- 미디어 상태 pill: `트리밍 완료(TRIMMED)` / `원본 유지(ORIGINAL_KEPT)` /
  `트리밍 대기(TRIM_PENDING)` / `트리밍 실패(TRIM_FAILED)` / `읽기 불가`
- 필수 카드 조합:
  1. 완등 + 트리밍 완료
  2. 완등 + 트리밍 대기
  3. 완등 + 원본 유지
  4. **실패 + 원본 유지** (신규)
  5. **실패 + 트리밍 대기** (신규)
  6. 분류 필요 (`UNCLASSIFIED`) — 성공/실패 재분류 진입점
  7. 트리밍 실패 — 재시도 진입점
  8. 읽기 불가 — 기록 유지, 재생 불가
- 목록 상태: 빈 목록 / 불러오는 중 / 조회 실패
- 필터 칩은 기능 계약 전까지 미리보기 표시(§2.2 규칙 동일)

### 2.8 `08` 재생 (`08-playback.html`)

- **실제 길이와 진행률**: `Player.duration`, `Player.currentPosition`
- 컨트롤 상태: 재생 중 / 일시정지 / 버퍼링 / 컨트롤 숨김(몰입) / 재생 오류
- 상단 정보는 실제 Attempt 메타데이터(암장·색·촬영 시각·결과 배지)
- 공유 진입점 유지, Android chooser 자체는 시안 밖

### 2.9 `09`–`11`

**미리보기 전용을 유지한다.** 새 플로우가 내비게이션 변경을 강제하지 않으므로
라우트와 화면 구조를 바꾸지 않는다. 기존 `Phase 3 · 비활성` 표기를 유지한다.

---

## 3. 동적 값 주석 규칙

기능 표면의 **모든 동적 값**에 다음 5개를 시안 안 주석 블록으로 병기한다.

| 항목 | 내용 |
|---|---|
| 런타임 소스 | 파일·필드 경로 (예: `AttemptSummary.successCount`) |
| 형식 | 표시 포맷 (예: `mm:ss`, `M월 d일 · a h:mm`, 정수) |
| 빈 상태 | 값이 없을 때 표시 |
| 로딩 상태 | 조회 중 표시 (스켈레톤 / 자리 표시) |
| 오류 상태 | 조회 실패 시 표시와 복구 행동 |

### 제거 대상 (기능 표면에서 금지)

- 가짜 레벨: `Lv.5 파랑`, `Lv.6 남색까지 78%`
- 고정 길이: `0:19`, `0:12`, `0:24`, `0:31`
- 고정 시도 수: `26 완등`, `12개`, `14`
- 가짜 타임스탬프: `7월 22일 · 오후 8:14`, `1시간 24분`

미리보기 전용 블록(`09`–`11`, 홈 스냅샷·리포트) 안에서는 예시 값을 유지하되
`미리보기` 표기를 반드시 동반한다.

---

## 4. 모션 명세 요구

모든 애니메이션·전환에 다음 8개 항목을 명시한다.

1. 트리거 2. 시작 상태 3. 종료 상태 4. 지속 시간 5. 이징
6. 중단 동작 7. 반복 정책 8. 축소 모션 대체

### 필수 커버 대상

| 모션 | 화면 |
|---|---|
| 녹화 펄스 + 타이머 | `05` |
| 로딩 · 카메라 준비 | `00`, `05` |
| 저장 처리 · 트리밍 처리 | `05b`, `06` |
| 성공 · 실패 피드백 | `00`, `05b` |
| 숫자 변화 (완등 카운트, 요약 수치) | `04` |
| 시트 · 오버레이 전환 | `05b`, `06`, `08` |
| 플레이어 컨트롤 · 타임라인 이동 | `08`, `06` |

### 모션 토큰 (신규, `_base.css` + `MASTER.md`에 고정)

| 토큰 | 값 | 용도 |
|---|---|---|
| `--dur-press` | `90ms` | ripple, 눌림 |
| `--dur-fast` | `150ms` | 색·불투명도 변화 |
| `--dur-standard` | `220ms` | 컨트롤 표시/숨김, 배너 |
| `--dur-emphasized` | `320ms` | 시트 진입, 화면 전환 |
| `--dur-exit` | `200ms` | 시트·오버레이 이탈 |
| `--dur-pulse` | `1200ms` | 녹화 인디케이터 1주기 |
| `--ease-standard` | `cubic-bezier(.2,0,0,1)` | 기본 |
| `--ease-decelerate` | `cubic-bezier(0,0,0,1)` | 진입 |
| `--ease-accelerate` | `cubic-bezier(.3,0,1,1)` | 이탈 |
| `--ease-pulse` | `cubic-bezier(.4,0,.6,1)` | 반복 펄스 |

축소 모션에서는 모든 `--dur-*`를 `0ms`로 축약하고, `--dur-pulse` 반복은 정적
표시로 대체한다.

---

## 5. 산출 문서 (5종 + 인수인계)

| 산출물 | 파일 |
|---|---|
| 인터랙션·상태 매트릭스 | `design-bundle/spec-interaction-matrix.html` |
| 화면 플로우 맵 | `design-bundle/spec-flow-map.html` |
| 컴포넌트·변형 인벤토리 | `design-bundle/spec-components.html` |
| 모션 명세 | `design-bundle/spec-motion.html` |
| 접근성 노트 | `design-bundle/spec-accessibility.html` |
| 개발자 인수인계 | `docs/design/phase-2-flow-v2-design-handoff.md` |

---

## 6. 규격과 컨벤션

- 기준 뷰포트 390dp (Galaxy S23), 프레임 높이 844
- 파일당 1화면, 자립형 인라인 CSS, 첫 줄 `<!-- @dsCard group="…" -->` 마커
- 화면은 `group="Screens"`, 토큰/공통은 `group="Foundations"`,
  명세 문서는 `group="Specs"`
- 다크 온리, Barlow / Barlow Condensed 유지
- 색: 브랜드·시맨틱과 홀드 데이터 색 네임스페이스 분리 유지
- 아이콘은 인라인 SVG, 이모지 금지
- 터치 타깃 ≥48dp, 성공/실패 버튼 간격 ≥8dp, 본문 대비 ≥4.5:1

---

## 7. 수용 기준

- [ ] §2의 모든 화면·상태가 존재한다
- [ ] `FAILURE` + 영상 보관/트리밍 경로가 시안에 있다
- [ ] `SAVE_PENDING` 재시도와 영상만 폐기 경로가 시안에 있다
- [ ] 기능 표면에 §3 제거 대상 값이 남아 있지 않다
- [ ] 모든 동적 값에 5개 주석이 붙어 있다
- [ ] 모든 모션에 8개 항목이 명시돼 있다
- [ ] 파괴적 행동에 확인 단계가 있다
- [ ] 모든 뒤로가기·닫기 경로의 결과 상태가 정의돼 있다
- [ ] `09`–`11`은 미리보기 전용으로 남아 있다
- [ ] 5종 명세 문서가 생성돼 있다
- [ ] 기존 시각 아이덴티티가 보존돼 있다
