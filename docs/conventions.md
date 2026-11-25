# 개발 약속

> 상태: 1 · 3 · 4 · 6 확정 (2026-11-26) · 2는 첫 API를 만들 때 · 5는 예약 동시성 설계와 함께 정한다.
> 원칙: 혼자 개발하더라도 안정성 · 보안을 낮추지 않는다. 시간 · 구현 난이도는 결정 기준 중 하나로만 본다.
> 범위: 아키텍처 결정 아래의 코드 수준 약속. 첫 기능이 나머지 기능의 본보기가 되므로 기능 개발 전에 정한다.

## 한눈에

| # | 항목 | 결정 | 되돌리기 |
|---|---|---|---|
| 1 | API 에러 응답 | RFC 9457 Problem Details + `code` | 비쌈 (앱이 의존) |
| 2 | API → 앱 타입 | (첫 API 때) 추천: springdoc → `openapi-typescript` | 쉬움 |
| 3 | ID | 내부 `bigint` + 공개 ID `UUIDv7` | 비쌈 (데이터 이행) |
| 4 | DB 스키마 변경 | Flyway, `ddl-auto=validate` | 중간 |
| 5 | 시간 | (예약 설계 때) 추천: 저장 UTC `timestamptz`, API ISO-8601 오프셋 포함 | 비쌈 |
| 6 | backend 패키지 | 기능(도메인)별 | 중간 |

---

## 1. API 에러 응답

| | A. RFC 9457 Problem Details (추천) | B. 자체 형식 `{success, data, error}` | C. 상태 코드 + 메시지 문자열만 |
|---|---|---|---|
| 근거 | IETF 표준. Spring에 `ProblemDetail` 타입이 있고, Boot에선 `spring.mvc.problemdetails.enabled=true`로 켬 (기본은 꺼짐) | 국내 프로젝트에 흔함 (DPlanner · C.Groove 방식) | 가장 단순 |
| 앱 처리 | `code`로 분기, `detail`은 사람이 읽는 문구 | 성공 응답까지 감싸서 타입이 한 겹 늘어남 | 문자열 비교로 분기 → 깨지기 쉬움 |
| 비용 | 전역 예외 처리기 하나 | 같음 | 0 |

모양:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "code": "RESERVATION_SLOT_TAKEN",
  "detail": "이미 예약된 시간입니다."
}
```

- `code`는 `도메인_상황` 대문자. 목록은 backend 한 곳(enum)에 둔다
- 입력 검증 실패는 `code: VALIDATION_FAILED` + `errors: [{field, reason}]`
- 성공 응답은 감싸지 않고 본문 그대로
- 구현: `spring.mvc.problemdetails.enabled=true` + `@RestControllerAdvice` 클래스가 `ResponseEntityExceptionHandler`를 상속. 안 켜면 Boot 기본 에러 JSON(`timestamp` · `error` · `path`)이 섞여 나옴

선택: **A** (11/26)

## 2. API → 앱 타입

| | A. springdoc → openapi-typescript (추천) | B. springdoc → orval (클라이언트 · TanStack Query 훅까지 생성) | C. 손으로 타입 작성 |
|---|---|---|---|
| 생성물 | 타입만. 호출은 `openapi-fetch` 얇은 래퍼 | 호출 함수 · 훅 전부 | 없음 |
| 얻는 것 | 가볍고 이해하기 쉬움 | 코드 가장 적게 씀 | 도구 0 |
| 잃는 것 | 훅은 직접 씀 | 생성 코드가 많아 디버깅 · 면접 설명 부담 | 필드 이름 오타가 런타임 버그 |

- backend가 `/v3/api-docs`를 내고, 앱에서 `npm run gen:api`로 `app/src/api/schema.d.ts` 생성 · 커밋
- CI에서 생성 결과가 커밋본과 다르면 실패 (나중에, 기능 3~4개 뒤)
- springdoc의 Spring Boot 4.1 지원 버전은 첫 API를 만들 때 확인하고 정한다

선택: 첫 API를 만들 때 springdoc 지원 확인 후

## 3. ID

| | A. 내부 `bigint` + 공개 `UUIDv7` (추천) | B. 전부 `UUIDv7` | C. 전부 `bigint` 자동 증가 |
|---|---|---|---|
| URL · 딥링크 | `/clubs/0193…` 추측 불가 | 같음 | `/clubs/3` 순서대로 찍어 보기 · 규모 노출 |
| 조인 · 인덱스 | 내부는 작은 정수 | 16바이트 키, UUIDv7은 시간순이라 인덱스 부담은 작음 | 가장 가벼움 |
| 복잡도 | 엔티티마다 열 2개 | 열 1개 | 열 1개 |

- RFC 9562 UUIDv7. 밖으로 나가는 ID(동아리 · 행사 · 초대 링크)만 공개 ID를 단다. 예약처럼 동아리 안에서만 보이는 건 B · C 어느 쪽이어도 무방 → 엔티티별로 표에 기록
- 초대 링크 토큰은 ID와 따로 (만료 · 회수가 필요)

선택: **A** (11/26). 안정성 · 보안 우선 원칙으로 열 2개 비용을 받아들임

## 4. DB 스키마 변경

| | A. Flyway (추천) | B. Liquibase | C. JPA `ddl-auto=update` |
|---|---|---|---|
| 형식 | 순수 SQL 파일 `V1__create_club.sql` | XML · YAML · SQL | 자동 |
| 얻는 것 | 읽기 쉽고 PostgreSQL 기능 그대로 | 되돌리기(rollback) 기술 | 가장 빠름 |
| 잃는 것 | 되돌리기는 새 파일로 | 문법 학습 | 기록 없음 · 12월 RDS 리허설 불가 · 운영에서 위험 |

- 의존성: `spring-boot-starter-flyway` + `flyway-database-postgresql` (Boot 4는 자동 설정이 모듈로 나뉘어 `flyway-core`만으로는 시작 때 안 돎)
- 파일은 `backend/src/main/resources/db/migration`, 이미 병합된 파일은 고치지 않고 새 파일로
- JPA는 `ddl-auto=validate` (엔티티와 스키마가 다르면 시작 실패)
- 테스트는 Testcontainers PostgreSQL (H2 쓰지 않음, 문법 차이). Boot 4 기준 의존성 좌표는 도입할 때 확인

선택: **A** (11/26)

## 5. 시간

| 층 | 약속 |
|---|---|
| DB | `timestamptz`, 날짜만이면 `date` |
| Java | 시각은 `Instant` · 동아리 시간대가 필요한 계산은 `ZonedDateTime(Asia/Seoul)` · `LocalDateTime` 금지 |
| API | ISO-8601 오프셋 포함 `2026-12-01T19:00:00+09:00` |
| 앱 | 받은 그대로 파싱, 화면에서만 KST 표시 |
| 서버 | JVM · DB 세션 UTC 고정 |

- 예약 시간 칸(30분 단위 등)과 겹침 판정은 예약 동시성 설계에서 같이 정한다
- 대안: 모두 KST 저장 → 단순하지만 서버 · DB 설정에 따라 9시간 어긋나는 사고가 흔함

선택: 예약 동시성 설계에서. 예약 시간 칸은 서울 기준 시각이 본질이라 `LocalDateTime` 금지 여부도 거기서 다시 본다

## 6. backend 패키지 구조

| | A. 기능별 (추천) | B. 계층별 (DPlanner 방식) |
|---|---|---|
| 모양 | `scene/club/{ClubController, ClubService, Club, ClubRepository}` | `scene/controller/…` `scene/service/…` `scene/domain/…` |
| 얻는 것 | 기능 하나가 폴더 하나 · 지울 때 · 읽을 때 한곳 · 나중에 모듈 경계 검사(Spring Modulith) 가능 | 익숙함 · 레거시와 같은 모양 |
| 잃는 것 | 공통 코드 위치를 정해야 함 (`scene/common`) | 기능이 늘수록 폴더마다 26개 파일이 섞임 |

- 다른 기능의 내부(Repository · 엔티티)를 직접 부르지 않고 Service를 부른다
- 패키지 최상위 이름 `scene`은 도메인을 정한 뒤 역도메인으로 바꿀지 결정

선택: **A** (11/26)

---

## 첫 기능 만들면서 정할 것 (기본값)

| 항목 | 기본값 |
|---|---|
| 테스트 | 기능마다 API 통합 테스트 1개 이상, 예약처럼 동시성이 걸린 곳은 동시성 테스트 |
| 설정 · 비밀값 | `application-local.yml`은 git에서 제외, 운영 값은 환경 변수 |
| 포맷 · 린트 | backend Spotless, app ESLint + Prettier → CI에 추가 |
| app 화면 이동 · 서버 상태 | expo-router · TanStack Query |
| 로그 | 요청 ID를 로그에 같이 |
| 의존성 업데이트 | Dependabot 주 1회 |

미룸: API 버전(`/v1`) · 모니터링 · 성능 기준 (파일럿 전까지)
