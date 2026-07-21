# We-Climb — 기술 결정 기록 (2026-07-16)

> 스파이크 조사(웹 검증 포함) 후 확정. 목표 함수 = **유저 유치** (학습 아님).
> 크루(검증 대상 5명)에 iOS·Android 혼재 → iOS는 빠른 후속 전제.

## 스택

| 층 | 결정 | 근거 / 조건 |
|---|---|---|
| 앱 | **Kotlin + KMP 뼈대** — shared(도메인·기록·통계·repository 인터페이스) + androidApp(Compose). iOS 타깃은 지금 빌드하지 않고 구조만 준비 | +1~2일 보험료로 iOS 확장이 "재작성"→"+1~1.5주"로. 미디어 코드는 어차피 androidMain |
| 백엔드 | **Supabase (MVP 한정)** — 인증(구글)·DB·API | 서버는 메타데이터 CRUD뿐 → 3~5일 절약. **추후 Spring Boot 이관 확정** — 그래서 데이터 접근은 반드시 shared의 repository 인터페이스 뒤에 (Supabase 구현체 → Spring API 클라이언트로 교체 가능하게) |

## 미디어 파이프라인

| 기능 | 결정 | 배제한 대안과 이유 |
|---|---|---|
| 카메라 | **CameraX 직접** (AndroidView interop, expect composable로 감쌈). 비트레이트 직접 설정(720p/5Mbps급 — 용량 관리), 출력은 앱 캐시로 | CameraK: 개인 메인테이너 + 세부 제어 노출 불확실. camera-compose: 아직 알파. Camera2 직접: 기간 초과 |
| 트리밍 | **Media3 Transformer** — 1.8+ MP4 edit-list trim(무손실·즉시) + trim optimization. 기기 이슈 시 폴백 = MediaExtractor/Muxer 수동(키프레임 스냅, 녹화 시 키프레임 간격 1s 설정으로 오차 완화) | FFmpegKit: 2025.1 은퇴·바이너리 삭제·특허 리스크·+30~70MB → 배제 |
| 저장 | **MediaStore** — 실패 영상은 앱 캐시에만, 성공 컷만 `Movies/WeClimb`로 승격 | "실패 일괄 삭제 = 캐시 비우기", 갤러리엔 성공만. 유저가 갤러리서 지우면 앱은 "기록은 남음" 플레이스홀더(기존 정책 일치) |
| 리포트 카드 | Compose `rememberGraphicsLayer()` → `toImageBitmap()` (CMP 공통 API — iOS에서도 동일) | 리스크 낮음, 스파이크 불요 |
| 인스타 공유 | **1차: ACTION_SEND**(마찰 0) → **2차: ADD_TO_STORY**(Meta 앱 등록 + FB App ID, 리포트 카드를 스티커 레이어로 영상 위에) | Graph API: 비즈니스 계정+심사 → 배제 |

## 플로우 제약 (기획 반영 필요)

- **인스타 스토리는 한 번에 1개 asset만** — "세션 영상 일괄 업로드" 불가.
  → 세션 종료 공유 = **리포트 카드(또는 대표 하이라이트 1개)**, 나머지 성공
  영상은 갤러리(`Movies/WeClimb`)에 정리돼 있어 유저가 이어서 올림.
- ADD_TO_STORY는 공유 성공 콜백이 없음 → 공유 지표는 근사치로만.

## 스파이크 계획 (실기기, 1~2일)

검증 질문 순서대로 — 하나라도 깨지면 폴백 경로 확인:

1. **연속 녹화 플로우**: CameraX로 녹화→정지→[성공/실패]→즉시 재녹화가
   끊김 없이 도는가. 캐시 경로 출력·비트레이트 설정 동작 확인
2. **edit-list 트림**: Media3 1.8 trim이 실기기에서 즉시 끝나는가.
   결과물이 (a) 갤러리 플레이어 (b) 인스타 업로드 후에도 잘린 지점이
   정확한가 (edit list 무시 플레이어 이슈 검증)
3. **승격·삭제**: 캐시→MediaStore 승격, 캐시 일괄 삭제
4. **공유**: ACTION_SEND로 인스타 스토리까지 도달 확인
5. **Supabase**: supabase-kt(KMP 클라이언트)로 구글 로그인 + 세션 메타 1건
   왕복

## 보류 카드 (시점이 오면 꺼낼 것)

- **하이브리드(네이티브 + 인앱 웹뷰) + 프론트엔드 합류**: MVP엔 손해(코어
  루프가 전부 네이티브 필수 영역, 웹뷰가 가져갈 화면이 기록 탭 정도뿐인데
  브리지 세팅 2~4일). **3단(암장 단위 소셜)부터 정석** — 루트 페이지·집계·
  랭킹·B2B 대시보드는 콘텐츠 화면이라 웹뷰 적합, 앱 심사 없이 업데이트 가능.
  그때 분업 = 네이티브(촬영·기록 도구) / 웹(콘텐츠·커뮤니티).
  지금 준비할 것: 인증 토큰을 웹뷰에 전달 가능한 형태로(Supabase 세션 —
  repository 인터페이스 뒤라 추가 비용 거의 없음).

## 배제 기록 (다시 논의하지 않기 위해)

- React Native: 카메라(vision-camera)는 우수하나 이점의 실체는 "iOS 공수
  4~6일 절약"뿐 — React 경험 없이는 학습 비용이 그걸 압도. 트리밍은 RN도
  네이티브 직접 작성이라 이점 0 → 배제
- Flutter: 공식 카메라 플러그인 품질 이슈 + 트리밍은 FFmpegKit 의존이
  은퇴로 붕괴, 결국 네이티브 2벌 → 이 앱에 최악 → 배제

---

## 프로젝트 현황 (2026-07-21 기준)

**완료**: 문제 검증(인터뷰 6건) → 기획 문서 3종 → 디자인 시스템 + 화면
10장(Claude Design 동기화, 피드백 4라운드 반영) → 클릭형 프로토타입 공유
링크 → 기술 스택 확정 → Android/KMP 스파이크 셸과 실기기 촬영·저장·공유 검증.

**스파이크 결과 (SM-S911N, API 36.1-ext21)**:

- 통과: CameraX 권한 획득과 녹화, 녹화 중지 후 성공/실패 분류, 성공 영상의
  MediaStore 저장, 실패 캐시 영상 삭제, `ACTION_SEND` 공유 시트, Instagram
  Stories 편집기 영상 로드, Media3 edit-list 1초-4초 트리밍 MP4 생성,
  갤러리와 Instagram의 시작·종료 프레임 재생
- 남음: Media3 fixture 기반 Android instrumentation test, CameraX 시작 실패와
  Android 공유 Intent의 자동 테스트
- 보류: Supabase 구글 로그인과 세션 메타데이터 왕복. Phase 1의 기록 로컬
  흐름을 우선 검증한 뒤 다시 편입

**다음 스텝 (순서대로)**:
1. **design-doc** — Phase 1 범위 기술 설계 (KMP 모듈 구조 · 데이터 모델 ·
   로컬 저장 정책 · repository 경계)
2. **스파이크 자동화 보강** - Media3 fixture, CameraX 시작 실패, 공유 Intent의
   Android 테스트
3. **Phase 1 구현** - 온보딩·홈·암장선택·세션보드·인앱촬영,
   완료 신호 = "실제 암장에서 앱으로만 찍고 완등이 기록된다"
4. Phase 2 - 트리밍·세션종료·리포트·인스타 공유 = MVP

**참고 자산**:
- 화면 목업: `design-bundle/*.html` (Claude Design "We-Climb" 프로젝트와 동기)
- 디자인 토큰: `design-system/we-climb/MASTER.md` (Compose 절이 정본)
- 클릭형 프로토타입: `docs/prototype-artifact.html` (Artifact로 호스팅)
