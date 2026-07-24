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

## UI 폴리시

- 시안의 홈 최근 영상, 아카이브 필터, 썸네일, 레벨은 기능 계약 전까지 예시 데이터로
  표시한다. 새 query, 썸네일 생성, 필터, 레벨 매핑을 추론해 구현하지 않는다.
- 트리밍의 백그라운드 완료 보장은 후속 기술 범위다. 현재 UI는 처리 중 중복 실행을
  막고, 중단 뒤 재시도 가능한 상태를 안내한다.
- bundle의 텍스트 기호와 임시 아이콘은 화면 구조가 안정된 뒤 SVG 또는 Compose 벡터
  자산으로 교체한다. 그 전에는 시안을 임의로 다시 그리지 않는다.

## 모션

- 표준 150–300ms, 네이티브 이징. 카운트 증가·화면 전환에만 의미 있는 모션.
- 리포트 등장은 값들이 순차로 카운트업(stagger)되면 "완성됐다" 감각을 줌.
- `Animatable`/`animate*AsState` 사용. 무한 장식 애니메이션 금지.

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
