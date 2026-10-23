# ALTEREGO 2026 백엔드

## 로컬 실행
Java 17이 필요합니다. 설정 파일을 복사하거나 운영 DB에 접속할 필요는 없습니다.

```bash
./gradlew test bootJar
./gradlew bootRun
```

Windows: `gradlew.bat`. 기본 주소: `http://localhost:8080`.
기본 `local` 프로필은 H2 메모리 DB와 `data.sql` 샘플 상품을 사용합니다.
서버 종료 시 테스트 주문은 사라집니다. 로컬 관리자 비밀번호는 `change-me`입니다.

## 관리자 인증
`POST /api/admin/login`에 `{"password":"..."}`을 보내면 `token`을 반환합니다.
로그인 외 모든 `/api/admin/**` 요청은 `Authorization: Bearer <token>`이 필요합니다.
`POST /api/admin/logout`은 현재 토큰을 폐기합니다. 토큰은 8시간 뒤 만료됩니다.
세션은 단일 서버 메모리에 보관되어 서버 재시작 시 다시 로그인해야 합니다.
다중 서버로 확장할 때는 공유 세션 저장소 또는 표준 인증 서비스로 변경합니다.
프론트의 수정된 관리자 화면과 함께 사용해야 합니다.

## 주문·상품 수정
- 공개 API: `/api/store/teams`, `/api/store/items`, `/api/store/items/{id}`
- 주문: `POST /api/store/checkout`
- 주문 확인: `GET /api/store/receipts/{publicToken}`. UUID 링크는 주문 정보에 접근하는 열쇠이므로 공유하지 않습니다.
- 전화번호만으로 주문을 조회하는 API는 제거했습니다. 삭제된 주문도 조회할 수 없습니다.
- 주문자명·은행명은 필수, 각각 50자 이내입니다. 계좌번호는 숫자·하이픈 5~30자(숫자 최소 5개), 전화번호는 숫자·하이픈 9~15자(숫자 최소 9개)입니다.
- 주문 항목은 1~50개, 상품별 합산 수량은 1~99개입니다. 동일 상품을 여러 줄로 보내도 합산합니다.
- `src/main/resources/store-catalog.json`이 서버의 판매 옵션 기준입니다. 키는 DB 상품명과 정확히 맞춥니다. `{}`는 옵션 없는 상품, 옵션 값 `true`는 판매 가능, `false`는 품절입니다. 등록하지 않은 상품은 주문할 수 없습니다.
- 상품을 바꿀 때 DB 상품 데이터, 위 파일, 프론트 `src/data/storeProductDetails.js`를 함께 맞춥니다. 가격은 DB 기준으로 계산합니다.
- `data.sql`은 로컬 샘플입니다. 운영 데이터를 자동 변경하지 않습니다.

## 운영 설정 참고
이 수정본은 로컬 검증용이며 자동 배포하지 않습니다.
운영 준비 시 `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_PASSWORD`, `PORT`를 별도 환경 변수로 설정합니다.
운영 관리자 비밀번호는 16자 이상이어야 하며, 누락·공백·기본값이면 서버 시작을 거부합니다. HTTPS를 사용합니다.
허용 프론트 주소는 `application.properties`의 `app.cors.allowed-origins`에서 관리합니다.
현재 배포 환경과 DB 호환성은 별도로 확인해야 합니다. 기존 운영 서버·DB에는 이번 변경을 적용하지 않았습니다.

## 회귀 테스트
`./gradlew test`로 인증·로그아웃·만료·주문 입력 검증·품절·삭제·엑셀 내보내기를 확인합니다. 테스트는 로컬 H2를 사용합니다.
