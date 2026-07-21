# 초기 암장 시드

`seoul-gym-seed.csv`는 Phase 1에서 사용할 서울 암장 지점 목록이다. 자동
수집으로 채우지 않는다. 한 행은 공식 웹사이트 또는 운영자 확인이 끝난 지점
하나만 나타낸다. 개발 시작에는 5개 이상, 실제 서울 필드 테스트에는 10개
이상을 유지한다.

## 입력 기준

- `name`, `address`는 확인한 값만 입력한다.
- `latitude`, `longitude`는 Phase 1에서 비워 둔다. 별도 승인된 지도 제공사를
  도입하기 전에는 다른 지도 데이터로 보강하지 않는다.
- `gym_id`는 `seoul-`로 시작하는 안정적인 소문자 식별자다. 예: `seoul-climb-one`.
- `source_url`에는 해당 지점을 확인한 공식 페이지 URL을 기록한다.
- `verified_at`은 `YYYY-MM-DD` 형식의 확인일이다.
- `verification_status`는 처음 `approved`만 허용한다. 후보는 앱 데이터가 아니라
  별도 검토 목록에서 관리한다.
- `source_type`은 `official_website`, `operator_channel`,
  `operator_confirmation` 중 하나만 허용한다. `operator_channel`은 운영자가
  관리하는 예약 또는 소셜 채널을 뜻한다.

사진, 리뷰, 운영자 연락처, 지도 제공사 결과는 이 파일에 넣지 않는다. 수정 또는
삭제 요청이 오면 해당 행의 출처와 확인일을 다시 검토한다.
