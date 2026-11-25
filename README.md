# SCENE

대학 스트릿댄스 씬의 공동 공간 - 동아리 운영과 학교 간 교류 · 행사 소식이 한곳에.

| 폴더 | 내용 |
|---|---|
| `backend/` | Java 25 · Spring Boot 4.1 · Gradle |
| `app/` | Expo SDK 58 · TypeScript (웹 · iOS · Android) |

## 시작하기

```bash
git config core.hooksPath .githooks   # 커밋 전 gitleaks 검사 (brew install gitleaks)
(cd backend && ./gradlew build)       # JDK 25 필요
(cd app && npm ci && npm run web)     # Node 26 · 웹으로 열기
```

헬스 확인: `./gradlew bootRun` 뒤 `GET /actuator/health`

브랜치 · PR · 커밋 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)

## 레거시

이전에 제가 참여하고 개발 진행했던 프로젝트입니다.
설계만 참고하고 새롭게 개발 중에 있습니다.

- [CGroove/CGroove-BE](https://github.com/CGroove/CGroove-BE) · [CGroove/CGroove-FE](https://github.com/CGroove/CGroove-FE)
- [dpplanner/api](https://github.com/dpplanner/api) · [dpplanner/app](https://github.com/dpplanner/app)
