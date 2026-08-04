# Design System Master File

> **LOGIC:** When building a specific page, first check `design-system/pages/[page-name].md`.
> If that file exists, its rules **override** this Master file.
> If not, strictly follow the rules below.

---

**Project:** We-Climb
**Generated:** 2026-07-15 15:43:12
**Category:** Fitness/Gym App
**Design Dials:** Variance 6/10 (Balanced / Modern) | Motion 6/10 (Standard) | Density 5/10 (Standard)

---

## Global Rules

### Color Palette

| Role | Hex | CSS Variable |
|------|-----|--------------|
| Primary | `#EA580C` | `--color-primary` |
| On Primary | `#FFFFFF` | `--color-on-primary` |
| Secondary | `#F97316` | `--color-secondary` |
| Accent/CTA | `#059669` | `--color-accent` |
| Background | `#0F172A` | `--color-background` |
| Foreground | `#FFFFFF` | `--color-foreground` |
| Muted | `#201C27` | `--color-muted` |
| Border | `rgba(255,255,255,0.08)` | `--color-border` |
| Destructive | `#DC2626` | `--color-destructive` |
| Ring | `#EA580C` | `--color-ring` |

**Color Notes:** Energetic orange + pace green on dark

### Typography

- **Heading Font:** Barlow Condensed
- **Body Font:** Barlow
- **Mood:** sports, fitness, athletic, energetic, condensed, action
- **Google Fonts:** [Barlow Condensed + Barlow](https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@400;500;600;700&family=Barlow:wght@300;400;500;600;700&display=swap)

**CSS Import:**
```css
@import url('https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@400;500;600;700&family=Barlow:wght@300;400;500;600;700&display=swap');
```

### Spacing Variables

*Density: 5/10 — Standard*

| Token | Value | Usage |
|-------|-------|-------|
| `--space-xs` | `4px` / `0.25rem` | Tight gaps |
| `--space-sm` | `8px` / `0.5rem` | Icon gaps, inline spacing |
| `--space-md` | `16px` / `1rem` | Standard padding |
| `--space-lg` | `24px` / `1.5rem` | Section padding |
| `--space-xl` | `32px` / `2rem` | Large gaps |
| `--space-2xl` | `48px` / `3rem` | Section margins |
| `--space-3xl` | `64px` / `4rem` | Hero padding |

### Shadow Depths

| Level | Value | Usage |
|-------|-------|-------|
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,0.05)` | Subtle lift |
| `--shadow-md` | `0 4px 6px rgba(0,0,0,0.1)` | Cards, buttons |
| `--shadow-lg` | `0 10px 15px rgba(0,0,0,0.1)` | Modals, dropdowns |
| `--shadow-xl` | `0 20px 25px rgba(0,0,0,0.15)` | Hero images, featured cards |

---

## Component Specs

### Buttons

```css
/* Primary Button */
.btn-primary {
  background: #059669;
  color: white;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  transition: all 200ms ease;
  cursor: pointer;
}

.btn-primary:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

/* Secondary Button */
.btn-secondary {
  background: transparent;
  color: #EA580C;
  border: 2px solid #EA580C;
  padding: 12px 24px;
  border-radius: 8px;
  font-weight: 600;
  transition: all 200ms ease;
  cursor: pointer;
}
```

### Cards

```css
.card {
  background: #0F172A;
  border-radius: 12px;
  padding: 24px;
  box-shadow: var(--shadow-md);
  transition: all 200ms ease;
  cursor: pointer;
}

.card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
}
```

### Inputs

```css
.input {
  padding: 12px 16px;
  border: 1px solid #E2E8F0;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 200ms ease;
}

.input:focus {
  border-color: #EA580C;
  outline: none;
  box-shadow: 0 0 0 3px #EA580C20;
}
```

### Modals

```css
.modal-overlay {
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
}

.modal {
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: var(--shadow-xl);
  max-width: 500px;
  width: 90%;
}
```

---

## Style Guidelines

**Style:** Vibrant & Block-based

**Keywords:** Bold, energetic, playful, block layout, geometric shapes, high color contrast, duotone, modern, energetic

**Best For:** Startups, creative agencies, gaming, social media, youth-focused, entertainment, consumer

**Key Effects:** Large sections (48px+ gaps), animated patterns, bold hover (color shift), scroll-snap, large type (32px+), 200-300ms

### Page Pattern

**Pattern Name:** Hero + Testimonials + CTA

- **Conversion Strategy:** Social proof before CTA. Use 3-5 testimonials. Include photo + name + role. CTA after social proof.
- **CTA Placement:** Hero (sticky) + Post-testimonials
- **Section Order:** 1. Hero, 2. Problem statement, 3. Solution overview, 4. Testimonials carousel, 5. CTA

---

## Motion

**Stagger List** (Standard) — Trigger: load or scroll | Duration: 300-450ms | Easing: `back.out(1.4)`

```js
gsap.from('.grid-item', { opacity: 0, scale: 0.92, y: 16, duration: 0.4, stagger: { each: 0.06, from: 'start', grid: 'auto' }, ease: 'back.out(1.4)' });
```

**Framework notes:** grid: 'auto' lets GSAP infer rows/columns from a CSS grid layout for a natural wave stagger

- ✅ Combine with from: 'center' for a bento-grid layout to draw the eye inward first
- ❌ Don't use back.out on dense data tables; the overshoot reads as sloppy on informational UI
- ⚡ Group DOM writes; avoid interleaving layout reads (getBoundingClientRect) between staggered tweens

---

## Anti-Patterns (Do NOT Use)

- ❌ Static design
- ❌ No gamification

### Additional Forbidden Patterns

- ❌ **Emojis as icons** — Use SVG icons (Heroicons, Lucide, Simple Icons)
- ❌ **Missing cursor:pointer** — All clickable elements must have cursor:pointer
- ❌ **Layout-shifting hovers** — Avoid scale transforms that shift layout
- ❌ **Low contrast text** — Maintain 4.5:1 minimum contrast ratio
- ❌ **Instant state changes** — Always use transitions (150-300ms)
- ❌ **Invisible focus states** — Focus states must be visible for a11y

---

## Pre-Delivery Checklist (web-generated — superseded by Compose section below)

이 아래는 스킬이 자동 생성한 웹 기준 체크리스트다. **We-Climb는 Jetpack
Compose(KMP) 네이티브 앱이므로, 실제 기준은 아래 "Stack Adaptation" 절이
정본**이다. cursor-pointer / hover / 반응형 브레이크포인트 같은 웹 항목은 무시.

- [ ] No emojis used as icons (use vector assets instead)
- [ ] Text contrast 4.5:1 minimum
- [ ] Motion 150-300ms
- [ ] `prefers-reduced-motion` respected

---

# Stack Adaptation — Jetpack Compose (KMP) · 다크 온리 MVP

> **정본.** 위쪽 웹/CSS/GSAP 예제는 참고용이고, 실제 구현 규칙은 여기부터다.
> MVP는 다크 테마 단일(라이트 모드는 이후). 모든 값은 Material3 토큰으로.

## 두 축 분리 (2026-08-04 · Phase 2 Flow v2 핵심)

**시도 결과**와 **영상 처리 결정**은 독립된 두 축이다. v1처럼 "성공=보관 /
실패=삭제"로 묶지 않는다. **실패한 시도의 영상도 남기거나 자를 수 있다.**

| 축 | 값 | 소스 |
|---|---|---|
| 결과 `AttemptOutcome` | `SUCCESS` · `FAILURE` · `UNCLASSIFIED` · `SAVE_PENDING` | `shared/session/AttemptService.kt` |
| 영상 결정 `VideoAction` (신규) | `TRIM_NOW` · `TRIM_LATER` · `KEEP_ORIGINAL` · `DISCARD` | UI 결정 → `AttemptMediaService` |
| 미디어 상태 `AttemptMediaState` | `NONE` · `TRIM_PENDING` · `ORIGINAL_KEPT` · `TRIM_PROCESSING` · `TRIMMED` · `TRIM_FAILED` | `shared/media/AttemptMediaService.kt` |

`VideoAction` → `AttemptMediaState` 매핑: `TRIM_NOW → TRIM_PROCESSING`,
`TRIM_LATER → TRIM_PENDING`, `KEEP_ORIGINAL → ORIGINAL_KEPT`, `DISCARD → NONE`.

`SUCCESS`·`FAILURE` 모두 네 가지 `VideoAction`을 전부 쓸 수 있다. 강조된 기본값
(성공=`TRIM_NOW`, 실패=`DISCARD`)은 제안일 뿐 잠금이 아니다. `UNCLASSIFIED`는
결과가 미정이므로 영상 처리를 묻지 않고 캐시를 보류 유지한다.

화면은 이 두 축을 **각각 따로** 표시한다 — 결과 배지와 미디어 상태 pill을 한
배지로 합치지 않는다. 결과 확정과 영상 처리도 **두 단계로 분리**한다
(`05` 결과 → `05b` 영상 처리).

## 색: 브랜드 · 시맨틱 vs 데이터 (핵심 구분)

이 앱은 **홀드 색깔이 곧 데이터**다. 그래서 색을 두 네임스페이스로 분리한다 —
섞으면 "브랜드 오렌지"와 "빨강 홀드"가 충돌한다.

### A. 브랜드 · 시맨틱 (UI 크롬)

| Role | Hex | 쓰는 곳 |
|------|-----|--------|
| Brand / Primary CTA | `#EA580C` | 세션 시작, 촬영, 인스타 공유 버튼, 브랜드 마크 |
| Success | `#059669` | 성공 버튼, 완등 확정 |
| Danger | `#DC2626` | 실패 버튼, 삭제 |
| Background | `#0B0F1A` | 최하단 배경 |
| Surface | `#141A26` | 카드·시트 (배경과 반드시 분리 — 생성값 #0F172A 중복 오류 교정) |
| Surface Variant | `#1E2634` | 칩·입력·눌린 상태 |
| On Surface | `#FFFFFF` | 본문 텍스트 (≥4.5:1) |
| On Surface Muted | `#94A3B8` | 보조 텍스트 (≥3:1) |
| Outline | `rgba(255,255,255,0.10)` | 구분선·테두리 (다크에서도 보이게) |
| Deferred / Inert | `#C084FC` | **Phase 3, 비활성** 뱃지 - 정적 예시 데이터로 화면만 제공하고 기능은 연결하지 않은 UI 표시. 배경 `rgba(168,119,210,.14)`, 테두리 `rgba(168,119,210,.40)`. 브랜드 오렌지와 레벨색과 충돌하지 않도록 보라 계열로 분리 |

### B. 앱 레벨 색 (Lv.1~9 — 통계·차트·칩)

앱 표준 난이도 스케일. 빨=쉬움(Lv.1) → 검=어려움(Lv.9). 암장 실제 테이프
색은 이 레벨로 매핑되고, 통계 화면은 이 색을 쓴다(세션 보드·상세는 암장 실제
색으로 표시).

**채도 기준 (ui-ux-pro-max 반영)**: 다크 표면(`#0B0F1A`)에서 순색/네온은
진동·눈부심을 유발하므로 **중채도로 낮추고 명도를 균형**한다(순노랑은 톤다운,
남·갈·검은 밝게/테두리). 각 색 ≥3:1 대비, **색만으로 의미 전달 금지** — 칩·
차트에 항상 라벨(Lv + 색이름) 병기.

| Lv | 색 | Hex |
|----|----|-----|
| 1 | 빨 | `#E45D5D` |
| 2 | 주 | `#E68A4C` |
| 3 | 노 | `#DDBB44` |
| 4 | 초 | `#46AE72` |
| 5 | 파 | `#5A8FDB` |
| 6 | 남 | `#7C7FDB` |
| 7 | 보 | `#A877D2` |
| 8 | 갈 | `#AC7B52` |
| 9 | 검 | `#1E293B` (테두리 `#64748B`) |

> 브랜드 오렌지(`#EA580C`)·실패 레드(`#DC2626`)는 레벨 색과 다른 화면에 쓰여
> 충돌하지 않는다(레벨색=통계 화면, 액션/실패=촬영·종료 화면). 암장의 색→
> 레벨 매핑은 프리셋+직접이며 하드코딩 금지.

> **Phase 2 Flow v2 (2026-08-04) 제약** — 색→레벨 매핑에는 아직 런타임 소스가
> 없다. 따라서 **기능 표면(02 홈 실데이터 영역, 04 보드, 05 촬영, 05b 결정,
> 06 트리밍, 07 아카이브, 08 재생)에서 `Lv.` 표기를 전부 제거**하고 **암장 실제
> 색 이름(파랑·초록…)만** 표시한다. 레벨 스케일은 미리보기 전용 블록(홈 스냅샷,
> 09-11)에서만 예시로 쓴다.

## 타이포 (Compose)

- Barlow Condensed → 숫자·헤드라인 (리포트의 "14 완등" 같은 큰 수치, 스포츠 감성)
- Barlow → 본문·라벨
- Compose: `FontFamily`로 두 폰트 등록, `MaterialTheme.typography` 토큰만 사용
  (sp 하드코딩 금지). displayLarge/Medium을 Condensed로, body/label을 Barlow로 매핑.
- 리포트의 완등 수치는 `displayLarge` 이상으로 크게 — 이 앱의 얼굴이다.

## 컴포넌트 (Compose 기준)

- **버튼**: Material3 `Button`/`FilledTonalButton`, radius 12dp. 눌림 피드백은
  ripple(기본) — 레이아웃 밀림(translateY) 금지. 최소 터치 48×48dp.
- **카드**: `Surface` tonalElevation으로 배경과 분리(그림자 대신 색 대비).
  hover 개념 없음 — press ripple만.
- **색깔 칩(세션 보드)**: 44dp+ 정사각, 홀드 데이터 색 배경 + 한글 라벨 +
  카운트 뱃지. 탭 = 카운트 +1, 즉각 ripple + 숫자 애니메이션(150–250ms).
- **성공/실패 오버레이**: 화면 하단 절반을 덮는 큰 두 버튼(엄지 도달권),
  Success/Danger 색. 오탭 방지로 둘 사이 간격 ≥8dp.
- **바텀 탭**: 홈 / 영상(아카이브) / 기록 **3개**. Material3 `NavigationBar`,
  세이프에어리어 준수. 영상=Phase 2(작동), 기록=Phase 3(비활성). 활성 탭 아이콘은
  브랜드 오렌지, 나머지는 muted.

## 신규 컴포넌트 (2026-07-24 시안 확정 — design-bundle 기준)

Phase 2 재구현 시안에서 굳은 컴포넌트. Compose 구현의 기준.

- **바텀시트** (`ModalBottomSheet`): 성공 영상 처리 선택(지금 자르기·나중에·원본
  유지), 개인 암장 추가, 암장 관리 메뉴에 사용. 상단 grab 핸들, radius 24dp,
  Surface 색, 뒤 배경은 scrim `rgba(0,0,0,.55~.6)`. 주 액션 옵션은 브랜드
  테두리로 강조.
- **다이얼로그** (`AlertDialog` 대체 커스텀): 세션 종료 확인. 중앙 정렬, radius
  22dp, 요약 타일(완등·최고 레벨·트리밍 대기) + 세로 버튼 스택(주 액션=브랜드,
  보조=아웃라인).
- **상태 배너** (인라인): 성공=green 계열 배경+체크, 오류=danger 배경+경고
  아이콘+재시도 버튼. 화면 상단에 인라인 삽입(토스트 아님). `00-common` 참조.
- **아카이브 카드 상태 pill**: 트리밍 완료(success), 트리밍 대기(brand), 원본
  유지(surface-variant/muted), 읽기 불가(muted+점선 테두리, 재생 불가). 색만으로
  구분 금지 — 라벨 병기.
- **Phase 3, 비활성 뱃지**: Deferred/Inert 색(`#C084FC`) 사용. 정적 예시 데이터로
  화면만 제공하는 UI(리포트 공유, 통계, 세션 종료 사진, 홈 스냅샷)에 부착. 이
  뱃지가 붙은 요소는 저장, 조회, 공유 같은 행동을 연결하지 않는다.
- **미디어 영역 표기**: 실제 Android 뷰가 들어가는 자리 명시 — CameraX
  `PreviewView`(촬영 전체 화면), Media3 Player Surface(트리밍 프리뷰·앱 내 재생
  오버레이). 시안에선 점선 라벨, 구현 시 해당 네이티브 뷰로 대체.
- **타임라인/트리머**: 프레임 스트립 + 좌우 핸들 범위 선택, 브랜드 색 선택 영역.
  범위 오류 시 danger 색으로 전환. 처리 중=진행 링(중복 실행 차단), 실패=재시도.

## 신규 컴포넌트 (2026-08-04 · Phase 2 Flow v2 확정)

플로우 변경이 강제한 컴포넌트만 추가했다. 기존 시각 아이덴티티는 유지된다.
전량 인벤토리는 `design-bundle/spec-components.html`.

- **결과 배지** (`AttemptOutcome`): `완등`(success), `실패`(danger),
  `분류 필요`(deferred `#C084FC`). 아이콘 + 한글 라벨 병기. 카드 **상단**에 배치.
- **미디어 상태 pill** (`AttemptMediaState`): `트리밍 완료`(success),
  `원본 유지`(surface-variant), `트리밍 대기`(brand), `트리밍 실패`(danger),
  `기기에서 삭제됨`(muted + 점선). 카드 **메타 하단**에 배치.
  결과 배지와 **절대 한 배지로 합치지 않는다.**
- **실패 영상 처리 시트**: 실패 Attempt도 `지금 자르기`/`나중에`/`원본 그대로`/
  `영상 지우기`를 모두 갖는다. `영상 지우기`가 강조 기본값이지만 강제가 아니다.
- **저장 대기 해소 시트**: `SAVE_PENDING`의 `다시 저장` / `영상만 폐기하고 기록 유지`.
- **미분류 재분류 시트**: 아카이브 `분류 필요` 카드에서 성공·실패를 확정하고
  영상 처리 시트로 이어진다.
- **파괴적 확인 다이얼로그**: **남는 것 / 사라지는 것**을 분리해 나열한다.
  "정말 삭제할까요?" 같은 모호한 문구 금지. 초기 포커스는 취소.
- **진행 배너** (인라인): 불확정 진행 막대 + "완료 전까지 같은 행동은 잠깁니다".
  화면 전체를 덮지 않고 중복 실행 차단 상태를 알린다.
- **리스트 상태 3종**: 로딩(실제 카드 높이 스켈레톤) / 빈(다음 행동 CTA 1개) /
  오류(원인 + 보존 사실 + 복구 2행동). **빈과 오류를 합치지 않는다.**
- **미리보기 블록 래퍼**: 점선 보라 테두리 + 불투명도 .72 + 회전 워터마크 +
  우상단 `Phase 3 · 비활성` 배지. 네 신호를 **동시에** 준다.
- **색 파생 썸네일 플레이스홀더**: `ArchiveAttempt`에 썸네일·duration 컬럼이
  없으므로 가짜 썸네일과 길이 배지를 제거하고 색 그라데이션 + 비디오 아이콘을 쓴다.
- **타임라인 재생헤드**: 실제 `Player.currentPosition`을 범위 선택과 함께 표시.
- **색상 카운트 행 색 이름**: 스와치 옆에 **한글 색 이름**을 명시적으로 추가한다.

## UI 폴리시

- 기능 표면의 모든 동적 값은 **런타임 소스 · 형식 · 빈 · 로딩 · 오류** 다섯 가지가
  시안에 주석으로 붙어 있다. 소스가 없는 값은 기능 표면에 표시하지 않는다.
- 기능 표면에서 금지: 가짜 레벨(`Lv.5`), 고정 길이(`0:12`), 고정 시도 수(`26 완등`),
  가짜 타임스탬프. 미리보기 전용 블록 안에서만 예시로 남는다.
- 홈 레벨 스냅샷·성장 차트·리포트 카드, 아카이브 필터 칩, `09`-`11`은 미리보기
  전용이다. 새 query, 썸네일 생성, 필터, 레벨 매핑을 추론해 구현하지 않는다.
- **뒤로가기·닫기는 절대 데이터를 삭제하지 않는다.** 시트 닫기는 `TRIM_PENDING`,
  분류 전 이탈은 `UNCLASSIFIED`, 저장 대기는 유지로 수렴한다. 삭제는 명시적 파괴
  버튼 + 확인 다이얼로그를 통해서만 일어난다.
- 시트·다이얼로그는 **닫기 결과를 미리 문장으로 고지**한다. 닫은 뒤 알리지 않는다.
- 트리밍의 백그라운드 완료 보장은 후속 기술 범위다. 처리 중 문구에 "화면을 벗어나도
  이어진다"고 쓰지 않는다 — 사실이 아니다. 중단 시 `TRIM_FAILED`로 복구된다.
- 모든 오류 상태는 **원인 + 보존 사실 + 출구 2개 이상**을 갖는다. 예외 원문·코덱명·
  실패 횟수는 노출하지 않는다.
- bundle의 텍스트 기호와 임시 아이콘은 화면 구조가 안정된 뒤 SVG 또는 Compose 벡터
  자산으로 교체한다. 그 전에는 시안을 임의로 다시 그리지 않는다.

## 모션 (2026-08-04 · Phase 2 Flow v2 토큰 확정)

전체 8항목 명세(트리거 · 시작 · 종료 · 지속 · 이징 · 중단 · 반복 · 축소 모션)는
`design-bundle/spec-motion.html`이 정본이다. 여기에는 토큰과 원칙만 둔다.

| 토큰 | 값 | Compose | 용도 |
|---|---|---|---|
| `--dur-press` | `90ms` | `90` | ripple · 눌림 |
| `--dur-fast` | `150ms` | `150` | 색 · 불투명도 · 아이콘 교체 |
| `--dur-standard` | `220ms` | `220` | 컨트롤 토글 · 숫자 · 진행 링 |
| `--dur-emphasized` | `320ms` | `320` | 시트 · 다이얼로그 · 화면 전환 진입 |
| `--dur-exit` | `200ms` | `200` | 시트 · 오버레이 이탈 |
| `--dur-pulse` | `1200ms` | `1200` | 녹화 인디케이터 1주기 |

| 이징 | 곡선 | Compose |
|---|---|---|
| `--ease-standard` | `cubic-bezier(.2,0,0,1)` | `CubicBezierEasing(.2f,0f,0f,1f)` |
| `--ease-decelerate` | `cubic-bezier(0,0,0,1)` | `CubicBezierEasing(0f,0f,0f,1f)` |
| `--ease-accelerate` | `cubic-bezier(.3,0,1,1)` | `CubicBezierEasing(.3f,0f,1f,1f)` |
| `--ease-pulse` | `cubic-bezier(.4,0,.6,1)` | `CubicBezierEasing(.4f,0f,.6f,1f)` |

원칙:

- `Animatable`/`animate*AsState` 사용. 무한 반복은 **상태를 나타낼 때만**
  (녹화 펄스 · 스피너 · shimmer). 무한 장식 애니메이션 금지.
- **중단 규칙** — 전환 중 반대 입력이 오면 처음부터 재생하지 않고 **현재 값에서
  역방향으로 이어간다.** 시트 드래그는 진행 중인 트윈에서 위치·속도를 인수한다.
- **직접 조작(드래그 · 스크럽 · 핸들)은 1:1 추종.** 보간·관성 없음.
- **금지**: 오류 흔들림(shake), 숫자 오버슈트·바운스(`back.out` 계열),
  컨페티·반짝임, 초 단위 타이머 롤링, 눌림 시 레이아웃 이동, 모션에만 실린 상태.

### 축소 모션 (reduced motion)

판정은 `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`.

- 모든 `--dur-*` → **0ms**. 시작 상태 없이 종료 상태를 즉시 적용.
- 무한 반복 → **정적 표시 + 텍스트 라벨**. 녹화 펄스는 고정 점 + `"REC"`,
  회전 스피너는 정적 아이콘 + `"준비 중"`, 진행 링은 퍼센트 숫자, shimmer는 단색.
- **직접 조작은 축소 대상이 아니다** — 손가락 추종은 애니메이션이 아니라 입력 반영.
- ripple은 유지 — 입력 확인 피드백이며 장식이 아니다.
- **정보를 모션에만 싣지 않는다.** 녹화 중 · 처리 중 · 버퍼링 · 로딩 네 상태는
  모션이 유일한 신호가 되기 쉬우므로 텍스트 라벨을 상시 병기한다.

## Pre-Delivery Checklist (모바일 앱 — 정본)

- [ ] 아이콘은 벡터(Compose ImageVector, 예: Material Icons/Phosphor), 이모지 금지
- [ ] 홀드 색 차트·칩에 한글 라벨 병기 (색만으로 의미 전달 금지)
- [ ] 터치 타깃 ≥48×48dp, 성공/실패 버튼 간격 ≥8dp
- [ ] 탭 피드백 ripple 80–150ms, 눌림이 레이아웃을 밀지 않음
- [ ] 다크 대비: 본문 ≥4.5:1, 보조 ≥3:1, 구분선 가시성 확인
- [ ] Surface가 Background와 색으로 분리됨 (그림자 의존 X)
- [ ] 세이프에어리어: 상단 상태바 / 하단 탭바·제스처 인디케이터 침범 X
- [ ] 4/8dp 스페이싱 리듬
- [ ] 스크롤 컨텐츠가 고정 바텀 탭 뒤에 가리지 않음 (contentPadding)
- [ ] TalkBack: 색깔 칩·버튼에 contentDescription (예: "파랑 완등 3개")
- [ ] reduced-motion / 큰 글자 크기에서 레이아웃 안 깨짐
- [ ] 375dp 소형 폰 + 세로 기준 확인
- [ ] Phase 3 미작동 요소에 `Phase 3·비활성` 뱃지(보라 #C084FC) 부착
- [ ] 바텀시트/다이얼로그 뒤 scrim + 세이프에어리어, 처리 중 중복 실행 차단

### Phase 2 Flow v2 추가 항목 (2026-08-04)

- [ ] 기능 표면에 가짜 값 없음 — 레벨 `Lv.`, 고정 길이, 고정 시도 수, 가짜 타임스탬프
- [ ] 모든 동적 값에 런타임 소스 · 형식 · 빈 · 로딩 · 오류 다섯 상태가 정의됨
- [ ] 결과 배지와 미디어 상태 pill이 카드에서 분리 표시됨
- [ ] `FAILURE` + 영상 보관/트리밍 경로가 UI에 존재
- [ ] `SAVE_PENDING` 재시도 · 영상만 폐기 경로가 UI에 존재
- [ ] 모든 뒤로가기·닫기 경로가 데이터를 삭제하지 않음
- [ ] 파괴적 행동에 남는 것/사라지는 것 확인 다이얼로그
- [ ] 시트·다이얼로그가 닫기 결과를 미리 고지
- [ ] 모든 오류 상태에 출구 2개 이상 (막다른 길 없음)
- [ ] 목록 화면에 로딩(스켈레톤) · 빈 · 오류 3종 상태가 분리 구현됨
- [ ] 미리보기 블록이 `clearAndSetSemantics`로 내부 가짜 값을 TalkBack에서 숨김
- [ ] 축소 모션에서 정보 손실 없음 (모든 모션 상태에 텍스트 라벨)
- [ ] 모션 8항목이 `spec-motion.html` 명세와 일치 (특히 중단 동작)
- [ ] 타임라인 핸들 · 카드 액션 · 플레이어 아이콘의 히트박스 ≥48dp 확장
