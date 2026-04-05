# DirtyPay Backend

정밀 N/1 정산 서비스의 백엔드 API 서버입니다.

모임, 회식, 여행 등에서 발생하는 비용을 참여자별로 정밀하게 분할 정산하고 관리할 수 있는 서비스입니다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.9 |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| ORM | Spring Data JPA / Hibernate |
| Database | MariaDB 10.11 |
| API Docs | SpringDoc OpenAPI 2.8.5 (Swagger UI) |
| Build | Gradle |

## 프로젝트 구조

도메인 중심 패키지 구조 + Layered Architecture를 따릅니다.

```
src/main/java/com/dirtypay/
├── DirtyPayApplication.java
├── domain/
│   ├── auth/               # 인증 도메인
│   │   ├── controller/     # AuthController (signup, login, refresh, validate, logout)
│   │   ├── dto/            # 요청/응답 DTO
│   │   ├── entity/         # RefreshToken
│   │   ├── repository/     # RefreshTokenRepository
│   │   ├── security/       # JWT, UserDetails, Cookie 설정
│   │   └── service/        # AuthService
│   ├── member/             # 회원 도메인
│   │   ├── controller/     # MemberController (조회, 수정, 삭제)
│   │   ├── dto/            # 요청/응답 DTO
│   │   ├── entity/         # Member, MemberRole
│   │   ├── repository/     # MemberRepository
│   │   └── service/        # MemberService
│   └── session/            # 정산 세션 도메인
│       └── entity/         # Session, SessionStatus
└── global/
    ├── common/             # 공통 DTO, Enum, BaseEntity
    ├── config/             # JPA, Web, Swagger, Security 설정
    └── exception/          # 전역 예외 처리
```

## 실행 방법

### 사전 요구사항

- Java 21
- Docker & Docker Compose

### 1. 환경 변수 설정

`.env.example` 파일을 복사하여 `.env` 파일을 생성합니다.

```bash
cp .env.example .env
```

필요에 따라 `.env` 파일의 값을 수정합니다.

### 2. MariaDB 실행

Docker Compose로 MariaDB 컨테이너를 실행합니다.

```bash
docker compose up -d
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

애플리케이션이 `http://localhost:8080` 에서 실행됩니다.

## 환경 변수

`.env.example` 파일에 정의된 환경 변수 목록입니다.

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `DB_HOST` | 데이터베이스 호스트 | `localhost` |
| `DB_PORT` | 데이터베이스 포트 | `3306` |
| `DB_NAME` | 데이터베이스 이름 | `dirtypay` |
| `DB_USERNAME` | 데이터베이스 사용자명 | `root` |
| `DB_PASSWORD` | 데이터베이스 비밀번호 | `1234` |
| `JWT_SECRET` | JWT 서명에 사용할 비밀키 | - |
| `JWT_EXPIRATION` | JWT Access Token 만료 시간 (ms) | `86400000` (24시간) |

## API 문서

애플리케이션 실행 후 Swagger UI에서 API 명세를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui.html
```

## 주요 API

### Auth (`/api/auth`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인 (Access Token은 Cookie로 설정) |
| POST | `/api/auth/refresh` | Refresh Token으로 Access Token 갱신 |
| GET | `/api/auth/validate` | Refresh Token 유효성 검증 |
| POST | `/api/auth/logout` | 로그아웃 (토큰 무효화) |

### Member (`/api/members`)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/members/{id}` | 회원 조회 |
| PUT | `/api/members/{id}` | 회원 정보 수정 |
| DELETE | `/api/members/{id}` | 회원 삭제 (Soft Delete) |
