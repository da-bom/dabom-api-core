# 실시간 가족 데이터 통합 관리 시스템 - ERD 설계서

> **문서 버전**: v21.3
> **변경 이력**:
> - v21.3 - MISSION_LOG `action_type` ENUM 재정의: 역할 분리 원칙 적용 — 요청 처리 결과는 `mission_request.status`가 담당, 미션 상태 변화 타임라인은 `mission_log.action_type`이 담당. `MISSION_APPROVED`·`MISSION_REJECTED` 제거 (→ `mission_request.status=APPROVED/REJECTED`로 추적), `MISSION_CANCELLED` 추가. 최종 ENUM: `MISSION_CREATED`, `MISSION_REQUESTED`, `MISSION_COMPLETED`, `MISSION_CANCELLED`.
> - v21.2 - POLICY_APPEAL 긴급 요청 동시성 문제 해결: `emergency_grant_month` 컬럼 추가 (DATE, NULL). EMERGENCY 타입일 때 해당 월 1일 값 저장, NORMAL은 NULL. `uk_appeal_emergency_month` UNIQUE 제약 (`requester_id`, `emergency_grant_month`)으로 DB 레벨 월 1회 중복 방지. 기존 `idx_appeal_emergency_monthly` 인덱스는 조회 최적화용으로 유지.
> - v21.1 - BaseEntity 일관성 확보: 전체 21개 엔티티에 created_at/updated_at/deleted_at 3개 필드 통일. 이력성/불변 테이블 deleted_at 예외 조항 변경 (BaseEntity 상속에 따라 컬럼 존재, 운영상 미사용). 13개 테이블에서 총 19개 누락 컬럼 추가.
> - v21.0 - Figma 디자인 반영: REWARD_TEMPLATE에서 default_value·unit 컬럼 제거, thumbnail_url 추가.
>   REWARD에서 template_default_value·value·unit 컬럼 제거, thumbnail_url 추가.
>   category ENUM 간소화 (TIME·ETC 제거 → DATA/GIFTICON).
>   unit ENUM 전체 제거 (보상명에 포맷 포함).
>   REWARD_GRANT 복합 인덱스 추가 (status+created_at).
> - v20.0 - REWARD_TEMPLATE: unit VARCHAR→ENUM(MB/MINUTE/COUNT/NONE), price·is_active 컬럼 추가. REWARD: template_default_value 컬럼 추가, unit VARCHAR→ENUM. REWARD_GRANT 신규 테이블 추가 (보상 지급 이력, 쿠폰 관리). 엔티티 총 개수 20→21개
> - v19.3 - 문서 내부 불일치 9건 일괄 수정: Soft Delete 설계 원칙에 이력성/불변 테이블 예외 명시, ENUM 요약(5.4)에 `notification_log.type` EMERGENCY_APPROVED 및 `policy.require_role` 누락 추가, mermaid 2.5 `action_type` MISSION_ prefix 동기화, 섹션 3.10 API 경로 `GET /admin/audit/logs`로 통일, Read Path(7.2) 테이블 참조 오류 수정(`reward_template`→`reward`, `mission_log` 추가), FAMILY_MEMBER에 `created_at`/`updated_at` 추가(BaseEntity 동기화), POLICY_APPEAL FK ON DELETE 전략 이력 보존 원칙에 맞게 변경(CASCADE→SET NULL/RESTRICT), 엔티티 총 개수 20개 유지
> - v19.2 - CUSTOMER 테이블에서 `profile_image_url` 컬럼 삭제, 엔티티 총 개수 20개 유지
> - v19.1 - POLICY_APPEAL.status ENUM에 `CANCELLED` 추가 (이의제기 취소 기능), POLICY_APPEAL에 `cancelled_at` 컬럼 추가, 이의제기 목록 조회 `type` 필터 제거, 엔티티 총 개수 20개 유지
> - v19.0 - REWARD 엔티티 신규 추가 (REWARD_TEMPLATE에서 name/category/unit 스냅샷, value 커스텀 저장), MISSION_ITEM에서 reward_template_id/reward_value 제거 → reward_id FK 추가, 관계 변경: REWARD_TEMPLATE→REWARD→MISSION_ITEM (1:N:1:1), 엔티티 총 개수 19→20개
> - v18.3 - FAMILY_RECAP_MONTHLY에 `communication_score` 컬럼 추가, `GET /recaps/monthly.communicationScore`와 동기화, `NORMAL` 이의제기와 미션 완료 건수 기반 월간 소통 점수 계산 규칙 명시, `NORMAL` 이의제기 0건이면 미션 완료율로 fallback 하고 이의제기/미션 모두 0건일 때만 `NULL` 반환
> - v18.2 - FAMILY_RECAP_MONTHLY의 이의제기 하이라이트 JSON을 `appeal_highlights_json`으로 재정의 (`topSuccessfulRequester`, `topAcceptedApprover`, 최신 이력 최대 3개), GET /recaps/monthly 응답 구조와 동기화
> - v18.1 - REWARD_TEMPLATE에 updated_at, deleted_at 컬럼 추가 (BaseEntity 일관성 확보, Soft Delete 적용), 엔티티 총 개수 19개 유지
> - v18.0 - MISSION_ITEM에 target_customer_id(대상 자녀) 추가, MISSION_REQUEST에 reject_reason 추가, POLICY_APPEAL.type ENUM APPEAL→NORMAL 리네이밍, FAMILY_REPORT→FAMILY_RECAP_MONTHLY 리네이밍+컬럼 JSON 통합, FAMILY_RECAP_WEEKLY 신규 테이블 추가, 엔티티 총 개수 18→19개
> - v17.0 - POLICY_APPEAL type ENUM APPEAL→NORMAL 리네이밍, API 경로 참조 /policies/appeals → /appeals 업데이트
> - v16.0 - POLICY_APPEAL_LOG 엔티티 전체 제거 (ERD 다이어그램, 관계 매트릭스, FK, ENUM, 인덱스 일괄 삭제), POLICY_ASSIGNMENT에서 reason 컬럼 삭제, POLICY_APPEAL의 reason을 request_reason(이의제기/긴급요청 사유)과 reject_reason(거절 사유, nullable)으로 분리, 엔티티 총 개수 19→18개
> - v15.0 - POLICY_APPEAL 긴급 쿼터 요청 확장: type(APPEAL/EMERGENCY) 컬럼 추가, policy_assignment_id nullable 변경, NOTIFICATION_LOG type에 EMERGENCY_APPROVED 추가, AUDIT_LOG action에 EMERGENCY_QUOTA_GRANTED 추가, idx_appeal_emergency_monthly 인덱스 추가
> - v14.0 - POLICY_APPEAL에 desired_rules(원하는 정책 값 JSON, nullable) 컬럼 추가, 이의제기 도메인 ERD(2.4), 미션 도메인 ERD(2.5) 서브 다이어그램 추가
> - v13.0 - POLICY_APPEAL, POLICY_APPEAL_COMMENT, POLICY_APPEAL_LOG, MISSION_LOG 엔티티 추가 (이의제기 플로우 및 미션 이벤트 타임라인 로그), POLICY_ASSIGNMENT에 reason(적용 사유) 컬럼 추가, notification_log 타입에 APPEAL_CREATED/APPROVED/REJECTED 추가, 엔티티 총 개수 15→19개
> - v12.0 - NEGOTIATION, NEGOTIATION_MESSAGE 엔티티 및 관련 참조 전체 제거 (기획 의도와 맞지 않아 삭제), 엔티티 총 개수 17→15개
> - v11.0 - 2차기획서 Phase 2 기능 반영: 6개 신규 엔티티 추가 (REWARD_TEMPLATE, MISSION_ITEM, MISSION_REQUEST, FAMILY_REPORT 등), CUSTOMER 프로필 컬럼 추가, AUDIT_LOG/INVITE 2차 개발 레이블 제거, NOTIFICATION_LOG 타입 확장
> - v10.4 - CUSTOMER_QUOTA 테이블 created_at 컬럼 추가 (다른 테이블과 일관성 확보, BaseEntity 동기화)
> - v10.3 - FAMILY_MEMBER 테이블 joined_at 컬럼 잔존 참조 제거 (다이어그램-상세 정의 동기화)
> - v10.2 - POLICY 테이블 is_active → is_active 리네이밍 (POLICY_ASSIGNMENT.is_active, API JSON isActive와 일관성 통일)
> - v10.1 - POLICY_ASSIGNMENT 테이블에 created_at, updated_at 컬럼 추가 (다른 테이블과 일관성 확보)
> - v10.0 - web-core 서브도메인 분리 Major 버전 동기화
> - v9.0 - api-spec 최종 동기화: API 경로 참조 업데이트
> - v8.1 - POLICY 테이블 is_active 필드 추가, CUSTOMER/INVITE phone_number VARCHAR(11) 숫자만 형식으로 변경
> - v8.0 - 전체 문서 버전 통일 (공유 Major + 독립 Minor 체계 도입)
> - v6.2 - ADMIN 테이블 phone_number 삭제, email을 NOT NULL UNIQUE 로그인 ID로 변경
> - v6.1 - POLICY 엔티티 ERD 다이어그램에 description, require_role, default_rules 필드 추가 (섹션 3.7 상세 정의와 동기화)
> - v6.0 - FAMILY_GROUP → FAMILY 이름 변경
> - v5.0 - USER→CUSTOMER/ADMIN 분리, MEMBER_QUOTA→CUSTOMER_QUOTA, 일별→월별, FAMILY_QUOTA 삭제→FAMILY_GROUP 통합, POLICY.rules→POLICY_ASSIGNMENT 이동, TINYINT→BOOLEAN, AUDIT_LOG/INVITE 2차 개발 레이블링
> - v4.0 - Soft Delete 전체 적용, API 도메인 그룹핑 반영, REST 알림 API 지원 인덱스 추가, Read Path 업데이트
> - v3.0 - 초기 작성

---

## 1. ERD 개요

### 1.1 설계 원칙

| 원칙 | 설명 |
| --- | --- |
| **Source of Truth** | MySQL이 모든 영속 데이터의 원본 (Redis는 캐시/실시간 상태용) |
| **Write-Behind** | 실시간 경로(Redis) → 비동기 저장(Kafka usage-persist → MySQL) |
| **Idempotency** | `event_id` UNIQUE 제약으로 중복 Insert 방지 |
| **Soft Delete** | 영속 엔티티에 `deleted_at` 컬럼 적용. NULL = 활성, NOT NULL = 삭제. UNIQUE 제약에 `deleted_at` 포함하여 삭제 후 재생성 허용. 모든 엔티티가 `BaseEntity`를 상속하므로 `created_at`, `updated_at`, `deleted_at` 3개 필드를 공통으로 가짐. 이력성/불변 테이블(`POLICY_APPEAL`, `REWARD`, `REWARD_GRANT`, `MISSION_ITEM`, `MISSION_REQUEST`, `MISSION_LOG`, `FAMILY_RECAP_MONTHLY`, `FAMILY_RECAP_WEEKLY`)은 운영상 Soft Delete를 사용하지 않으나, BaseEntity 상속에 따라 `deleted_at` 컬럼은 존재 (항상 NULL) |
| **바이트 단위 통일** | 모든 데이터량 필드는 `BIGINT` 바이트 단위 |

### 1.2 엔티티 목록

| # | 엔티티 | 설명 | 예상 레코드 수 |
| --- | --- | --- | --- |
| 1 | `customer` | 시스템 사용자 (가족 구성원) | ~1,000,000 |
| 2 | `admin` | 백오피스 운영자 | ~100 |
| 3 | `family` | 가족 그룹 | ~250,000 |
| 4 | `family_member` | 가족-사용자 매핑 (N:M 해소) | ~1,000,000 |
| 5 | `customer_quota` | 구성원별 월별 한도/사용량/차단 상태 | ~1,000,000/월 |
| 6 | `usage_record` | 데이터 사용 이력 (Write-Behind 저장) | ~432,000,000/일 |
| 7 | `policy` | 정책 템플릿 정의 | ~100 |
| 8 | `policy_assignment` | 정책 적용 매핑 | ~500,000 |
| 9 | `notification_log` | 알림 발송 이력 | ~수백만/월 |
| 10 | `audit_log` | 감사 로그 (정책 변경, 차단 이력 등) | ~수십만/월 |
| 11 | `invite` | 가족 초대 | ~수만 |
| 12 | `reward_template` | 시스템 제공 보상 템플릿 | ~100 |
| 13 | `reward` | 보상 인스턴스 (템플릿 스냅샷 + 커스텀 값) | ~수만 |
| 14 | `mission_item` | 부모 생성 미션 항목 (대상 자녀 지정) | ~수만 |
| 15 | `mission_request` | 자녀의 미션 보상 요청 | ~수만/월 |
| 16 | `family_recap_monthly` | 월간 가족 리캡 스냅샷 | ~250,000/월 |
| 17 | `policy_appeal` | 자녀의 정책 이의제기 | ~수만 |
| 18 | `policy_appeal_comment` | 이의제기 댓글 (부모-자녀 소통) | ~수만 |
| 19 | `mission_log` | 미션 이벤트 타임라인 로그 | ~수십만 |
| 20 | `family_recap_weekly` | 주간 가족 리캡 스냅샷 (내부 집계용) | ~1,000,000/년 |
| 21 | `reward_grant` | 보상 지급 이력 (쿠폰 코드/URL, 사용 상태 관리) | ~수만 |

---

## 2. ERD 다이어그램

### 2.1 전체 ERD

```mermaid
erDiagram
    CUSTOMER ||--|{ FAMILY_MEMBER : "belongs to"
    FAMILY ||--|{ FAMILY_MEMBER : "contains"
    CUSTOMER ||--o{ CUSTOMER_QUOTA : "has monthly quota"
    FAMILY ||--o{ CUSTOMER_QUOTA : "scoped to"
    CUSTOMER ||--o{ USAGE_RECORD : "generates"
    FAMILY ||--o{ USAGE_RECORD : "belongs to"
    POLICY ||--o{ POLICY_ASSIGNMENT : "applied as"
    FAMILY ||--o{ POLICY_ASSIGNMENT : "receives"
    CUSTOMER ||--o{ POLICY_ASSIGNMENT : "targeted by"
    CUSTOMER ||--o{ NOTIFICATION_LOG : "receives"
    FAMILY ||--o{ NOTIFICATION_LOG : "scoped to"
    FAMILY ||--o{ INVITE : "has"
    CUSTOMER ||--o{ AUDIT_LOG : "performs"
    FAMILY ||--o{ MISSION_ITEM : "owns"
    CUSTOMER ||--o{ MISSION_ITEM : "creates"
    CUSTOMER ||--o{ MISSION_ITEM : "assigned to"
    REWARD_TEMPLATE ||--o{ REWARD : "template of"
    REWARD ||--|| MISSION_ITEM : "rewards"
    REWARD ||--o{ REWARD_GRANT : "grants"
    CUSTOMER ||--o{ REWARD_GRANT : "receives"
    MISSION_ITEM ||--o{ REWARD_GRANT : "issued for"
    MISSION_ITEM ||--o{ MISSION_REQUEST : "requested as"
    CUSTOMER ||--o{ MISSION_REQUEST : "requests"
    CUSTOMER ||--o{ MISSION_REQUEST : "approves"
    FAMILY ||--o{ FAMILY_RECAP_MONTHLY : "monthly snapshot"
    FAMILY ||--o{ FAMILY_RECAP_WEEKLY : "weekly snapshot"
    POLICY_ASSIGNMENT |o--o{ POLICY_APPEAL : "challenged by"
    CUSTOMER ||--o{ POLICY_APPEAL : "requests"
    CUSTOMER ||--o{ POLICY_APPEAL : "resolves"
    POLICY_APPEAL ||--o{ POLICY_APPEAL_COMMENT : "has"
    CUSTOMER ||--o{ POLICY_APPEAL_COMMENT : "authors"
    MISSION_ITEM ||--o{ MISSION_LOG : "tracks"
    CUSTOMER ||--o{ MISSION_LOG : "performed by"

    CUSTOMER {
        bigint id PK "AUTO_INCREMENT"
        varchar phone_number UK "NOT NULL, 숫자만 11자리 (01012345678, 로그인 ID)"
        varchar password_hash "NOT NULL, BCrypt 해시"
        varchar name "NOT NULL, 사용자 이름"
        varchar email "NULL, 이메일"
        boolean is_onboarded "NOT NULL DEFAULT FALSE, 온보딩 완료 여부"
        datetime terms_agreed_at "NULL, 약관 동의 시각"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    ADMIN {
        bigint id PK "AUTO_INCREMENT"
        varchar email UK "NOT NULL, 이메일 (로그인 ID)"
        varchar password_hash "NOT NULL, BCrypt 해시"
        varchar name "NOT NULL, 운영자 이름"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    FAMILY {
        bigint id PK "AUTO_INCREMENT"
        varchar name "NOT NULL, 가족 그룹명"
        bigint created_by_id FK "NOT NULL → customer.id, 그룹 최초 생성자 (이력/감사 전용)"
        bigint total_quota_bytes "NOT NULL, DEFAULT 100GB"
        bigint used_bytes "NOT NULL DEFAULT 0, 현재 월 사용량(바이트)"
        date current_month "NOT NULL, 현재 과금 월 (매월 1일 기준)"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    FAMILY_MEMBER {
        bigint id PK "AUTO_INCREMENT"
        bigint family_id FK "NOT NULL → family.id"
        bigint customer_id FK "NOT NULL → customer.id"
        enum role "MEMBER | OWNER"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    CUSTOMER_QUOTA {
        bigint id PK "AUTO_INCREMENT"
        bigint customer_id FK "NOT NULL → customer.id"
        bigint family_id FK "NOT NULL → family.id"
        bigint monthly_limit_bytes "NULL = 무제한"
        bigint monthly_used_bytes "NOT NULL DEFAULT 0"
        date current_month "NOT NULL, 해당 월 (매월 1일 기준)"
        boolean is_blocked "NOT NULL DEFAULT FALSE"
        varchar block_reason "NULL, 차단 사유 코드"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    USAGE_RECORD {
        bigint id PK "AUTO_INCREMENT"
        varchar event_id UK "NOT NULL, Idempotency 키"
        bigint customer_id FK "NOT NULL → customer.id"
        bigint family_id FK "NOT NULL → family.id"
        bigint bytes_used "NOT NULL, 사용 바이트"
        varchar app_id "NULL, 앱 식별자"
        datetime event_time "NOT NULL, 이벤트 발생 시각"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    POLICY {
        bigint id PK "AUTO_INCREMENT"
        varchar name "NOT NULL, 정책 이름"
        varchar description "NULL, 정책 설명"
        enum require_role "NOT NULL, DEFAULT 'MEMBER', 최소 요구 역할"
        enum type "MONTHLY_LIMIT | TIME_BLOCK | APP_BLOCK | MANUAL_BLOCK"
        json default_rules "NOT NULL, 기본 정책 규칙 JSON"
        boolean is_system "DEFAULT FALSE, 시스템 정책 여부"
        boolean is_active "NOT NULL DEFAULT TRUE, 정책 활성화 여부"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    POLICY_ASSIGNMENT {
        bigint id PK "AUTO_INCREMENT"
        bigint policy_id FK "NOT NULL → policy.id"
        bigint family_id FK "NOT NULL → family.id"
        bigint target_customer_id FK "NULL → customer.id (NULL=가족 전체)"
        bigint applied_by_id FK "NOT NULL → customer.id, 적용자"
        json rules "NOT NULL, 정책 규칙 JSON"
        boolean is_active "NOT NULL DEFAULT TRUE"
        datetime applied_at "DEFAULT CURRENT_TIMESTAMP"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    NOTIFICATION_LOG {
        bigint id PK "AUTO_INCREMENT"
        bigint customer_id FK "NOT NULL → customer.id"
        bigint family_id FK "NOT NULL → family.id"
        enum type "THRESHOLD_ALERT | BLOCKED | UNBLOCKED | POLICY_CHANGED | MISSION_CREATED | REWARD_REQUESTED | REWARD_APPROVED | REWARD_REJECTED | APPEAL_CREATED | APPEAL_APPROVED | APPEAL_REJECTED | EMERGENCY_APPROVED"
        text message "NOT NULL, 알림 메시지"
        json payload "NULL, 추가 데이터"
        boolean is_read "NOT NULL DEFAULT FALSE"
        datetime sent_at "DEFAULT CURRENT_TIMESTAMP"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    AUDIT_LOG {
        bigint id PK "AUTO_INCREMENT"
        bigint actor_id FK "NULL → customer.id, 수행자"
        varchar action "NOT NULL, 수행 액션"
        varchar entity_type "NOT NULL, 대상 엔티티 종류"
        bigint entity_id "NOT NULL, 대상 엔티티 ID"
        json old_value "NULL, 변경 전 값"
        json new_value "NULL, 변경 후 값"
        varchar ip_address "NULL, 요청 IP"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    INVITE {
        bigint id PK "AUTO_INCREMENT"
        bigint family_id FK "NOT NULL → family.id"
        varchar phone_number "NOT NULL, 숫자만 11자리, 초대 대상 전화번호"
        enum role "MEMBER | OWNER"
        enum status "PENDING | ACCEPTED | EXPIRED | CANCELLED"
        datetime expires_at "NOT NULL, 만료 시각"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    POLICY_APPEAL {
        bigint id PK "AUTO_INCREMENT"
        enum type "NORMAL | EMERGENCY, DEFAULT NORMAL"
        bigint policy_assignment_id FK "NULL → policy_assignment.id (EMERGENCY는 NULL)"
        bigint requester_id FK "NOT NULL → customer.id, 요청자(자녀)"
        text request_reason "NOT NULL, 이의제기/긴급요청 사유"
        text reject_reason "NULL, 거절 사유 (REJECTED 시)"
        json desired_rules "NULL, 원하는 정책 값 JSON (EMERGENCY: additionalBytes)"
        enum status "PENDING | APPROVED | REJECTED | CANCELLED"
        date emergency_grant_month "NULL, EMERGENCY 시 해당 월 1일 (UK)"
        bigint resolved_by_id FK "NULL → customer.id (EMERGENCY는 NULL=시스템)"
        datetime resolved_at "NULL, 처리 시각"
        datetime cancelled_at "NULL, 취소 시각"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    POLICY_APPEAL_COMMENT {
        bigint id PK "AUTO_INCREMENT"
        bigint appeal_id FK "NOT NULL → policy_appeal.id"
        bigint author_id FK "NOT NULL → customer.id"
        text comment "NOT NULL, 댓글 내용"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    MISSION_LOG {
        bigint id PK "AUTO_INCREMENT"
        bigint mission_item_id FK "NOT NULL → mission_item.id"
        bigint actor_id FK "NULL → customer.id, 수행자(NULL=시스템)"
        enum action_type "MISSION_CREATED | MISSION_REQUESTED | MISSION_COMPLETED | MISSION_CANCELLED"
        varchar message "NOT NULL VARCHAR(500), 로그 메시지"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    REWARD_TEMPLATE {
        bigint id PK "AUTO_INCREMENT"
        varchar name "NOT NULL, 보상명 (100)"
        enum category "NOT NULL, DATA | GIFTICON"
        varchar thumbnail_url "NULL, 상품 이미지 (500)"
        int price "NOT NULL, 단가(원)"
        boolean is_system "NOT NULL DEFAULT TRUE"
        boolean is_active "NOT NULL DEFAULT TRUE"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    REWARD {
        bigint id PK "AUTO_INCREMENT"
        bigint reward_template_id FK "NOT NULL → reward_template.id"
        varchar name "NOT NULL, 보상명 (스냅샷)"
        enum category "NOT NULL, DATA | GIFTICON"
        varchar thumbnail_url "NULL, 이미지 (스냅샷)"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    REWARD_GRANT {
        bigint id PK "AUTO_INCREMENT"
        bigint reward_id FK "NOT NULL → reward.id"
        bigint customer_id FK "NOT NULL → customer.id"
        bigint mission_item_id FK "NOT NULL → mission_item.id"
        varchar coupon_code "NULL, 쿠폰 코드 (100)"
        varchar coupon_url "NULL, 쿠폰 URL (255)"
        enum status "ISSUED | USED | EXPIRED"
        datetime expired_at "NULL, 만료 시각"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    MISSION_ITEM {
        bigint id PK "AUTO_INCREMENT"
        bigint family_id FK "NOT NULL → family.id"
        bigint created_by_id FK "NOT NULL → customer.id"
        bigint target_customer_id FK "NOT NULL → customer.id, 대상 자녀"
        bigint reward_id FK "NOT NULL → reward.id"
        text mission_text "NOT NULL, 미션 내용"
        enum status "ACTIVE | COMPLETED | CANCELLED"
        datetime completed_at "NULL, 완료 시각"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    MISSION_REQUEST {
        bigint id PK "AUTO_INCREMENT"
        bigint mission_item_id FK "NOT NULL → mission_item.id"
        bigint requester_id FK "NOT NULL → customer.id"
        enum status "PENDING | APPROVED | REJECTED"
        text reject_reason "NULL, 거절 사유"
        bigint resolved_by_id FK "NULL → customer.id"
        datetime resolved_at "NULL, 처리 시각"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    FAMILY_RECAP_MONTHLY {
        bigint id PK "AUTO_INCREMENT"
        bigint family_id FK "NOT NULL → family.id"
        date report_month "NOT NULL, 리측 월 (예: 2026-03-01)"
        bigint total_used_bytes "NOT NULL"
        bigint total_quota_bytes "NOT NULL"
        decimal usage_rate_percent "NOT NULL"
        json usage_by_weekday "NULL, 월간 요일별 사용 비율"
        json peak_usage "NULL, {startHour, endHour, mostUsedWeekday}"
        json mission_summary_json "NULL, {totalMissionCount, completedMissionCount, rejectedRequestCount}"
        json appeal_summary_json "NULL, {totalAppeals, approvedAppeals, rejectedAppeals}"
        json appeal_highlights_json "NULL, {topSuccessfulRequester, topAcceptedApprover}"
        decimal communication_score "NULL, 0~100 소통 점수"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }

    FAMILY_RECAP_WEEKLY {
        bigint id PK "AUTO_INCREMENT"
        bigint family_id FK "NOT NULL → family.id"
        date week_start_date "NOT NULL, 주 시작일 (월요일 기준)"
        bigint total_used_bytes "NOT NULL"
        bigint total_quota_bytes "NOT NULL"
        decimal usage_rate_percent "NOT NULL"
        json usage_by_weekday "NOT NULL, 7요일 고정"
        json peak_usage "NULL, {startHour, endHour, peakBytes}"
        int mission_created_count "NOT NULL DEFAULT 0"
        int mission_completed_count "NOT NULL DEFAULT 0"
        int mission_rejected_count "NOT NULL DEFAULT 0"
        int appeal_count "NOT NULL DEFAULT 0"
        datetime created_at "DEFAULT CURRENT_TIMESTAMP"
        datetime updated_at "DEFAULT CURRENT_TIMESTAMP"
        datetime deleted_at "NULL, Soft Delete"
    }
```

### 2.2 핵심 도메인 ERD (Core Domain)

사용자-가족-쿼터 핵심 관계만 추출한 다이어그램:

```mermaid
erDiagram
    CUSTOMER ||--|{ FAMILY_MEMBER : "1:N"
    FAMILY ||--|{ FAMILY_MEMBER : "1:N"
    CUSTOMER ||--o{ CUSTOMER_QUOTA : "1:N"
    CUSTOMER ||--o{ USAGE_RECORD : "1:N"

    CUSTOMER {
        bigint id PK
        varchar phone_number UK
        varchar name
        datetime deleted_at
    }

    FAMILY {
        bigint id PK
        varchar name
        bigint created_by_id FK
        bigint total_quota_bytes
        bigint used_bytes
        date current_month
        datetime deleted_at
    }

    FAMILY_MEMBER {
        bigint id PK
        bigint family_id FK
        bigint customer_id FK
        enum role
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    CUSTOMER_QUOTA {
        bigint id PK
        bigint customer_id FK
        bigint family_id FK
        bigint monthly_limit_bytes
        bigint monthly_used_bytes
        boolean is_blocked
        datetime deleted_at
    }

    USAGE_RECORD {
        bigint id PK
        varchar event_id UK
        bigint customer_id FK
        bigint family_id FK
        bigint bytes_used
        datetime event_time
        datetime deleted_at
    }
```

### 2.3 정책 도메인 ERD (Policy Domain)

```mermaid
erDiagram
    POLICY ||--o{ POLICY_ASSIGNMENT : "1:N"
    FAMILY ||--o{ POLICY_ASSIGNMENT : "1:N"
    CUSTOMER ||--o{ POLICY_ASSIGNMENT : "targeted (0:N)"

    POLICY {
        bigint id PK
        varchar name
        varchar description
        enum require_role
        enum type
        json default_rules
        boolean is_system
        boolean is_active
        datetime deleted_at
    }

    POLICY_ASSIGNMENT {
        bigint id PK
        bigint policy_id FK
        bigint family_id FK
        bigint target_customer_id FK "NULL = 가족 전체 적용"
        bigint applied_by_id FK
        json rules
        boolean is_active
        datetime deleted_at
    }

    FAMILY {
        bigint id PK
        varchar name
        datetime deleted_at
    }

    CUSTOMER {
        bigint id PK
        varchar name
        datetime deleted_at
    }
```

### 2.4 이의제기 도메인 ERD (Policy Appeal Domain)

```mermaid
erDiagram
    POLICY_ASSIGNMENT |o--o{ POLICY_APPEAL : "challenged by"
    CUSTOMER ||--o{ POLICY_APPEAL : "requests"
    CUSTOMER ||--o{ POLICY_APPEAL : "resolves"
    POLICY_APPEAL ||--o{ POLICY_APPEAL_COMMENT : "has"
    CUSTOMER ||--o{ POLICY_APPEAL_COMMENT : "authors"

    POLICY_ASSIGNMENT {
        bigint id PK
        bigint policy_id FK
        bigint family_id FK
        bigint target_customer_id FK
        bigint applied_by_id FK
        json rules
        boolean is_active
        datetime deleted_at
    }

    POLICY_APPEAL {
        bigint id PK
        enum type "NORMAL | EMERGENCY"
        bigint policy_assignment_id FK "NULL (EMERGENCY)"
        bigint requester_id FK
        text request_reason
        text reject_reason "NULL"
        json desired_rules "NULL"
        enum status "PENDING | APPROVED | REJECTED | CANCELLED"
        date emergency_grant_month "NULL, EMERGENCY 시 해당 월 1일 (UK)"
        bigint resolved_by_id FK "NULL (EMERGENCY=시스템)"
        datetime resolved_at
        datetime cancelled_at "NULL"
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    POLICY_APPEAL_COMMENT {
        bigint id PK
        bigint appeal_id FK
        bigint author_id FK
        text comment
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    CUSTOMER {
        bigint id PK
        varchar name
        datetime deleted_at
    }
```

### 2.5 미션 도메인 ERD (Mission Domain)

```mermaid
erDiagram
    FAMILY ||--o{ MISSION_ITEM : "owns"
    CUSTOMER ||--o{ MISSION_ITEM : "creates"
    CUSTOMER ||--o{ MISSION_ITEM : "assigned to"
    REWARD_TEMPLATE ||--o{ REWARD : "template of"
    REWARD ||--|| MISSION_ITEM : "rewards"
    REWARD ||--o{ REWARD_GRANT : "grants"
    CUSTOMER ||--o{ REWARD_GRANT : "receives"
    MISSION_ITEM ||--o{ REWARD_GRANT : "issued for"
    MISSION_ITEM ||--o{ MISSION_REQUEST : "requested as"
    CUSTOMER ||--o{ MISSION_REQUEST : "requests"
    CUSTOMER ||--o{ MISSION_REQUEST : "approves"
    MISSION_ITEM ||--o{ MISSION_LOG : "tracks"
    CUSTOMER ||--o{ MISSION_LOG : "performed by"

    REWARD_TEMPLATE {
        bigint id PK
        varchar name
        enum category "DATA | GIFTICON"
        varchar thumbnail_url "NULL"
        int price
        boolean is_system
        boolean is_active
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    REWARD {
        bigint id PK
        bigint reward_template_id FK
        varchar name
        enum category "DATA | GIFTICON"
        varchar thumbnail_url "NULL"
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    REWARD_GRANT {
        bigint id PK
        bigint reward_id FK
        bigint customer_id FK
        bigint mission_item_id FK
        varchar coupon_code "NULL"
        varchar coupon_url "NULL"
        enum status "ISSUED | USED | EXPIRED"
        datetime expired_at "NULL"
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    MISSION_ITEM {
        bigint id PK
        bigint family_id FK
        bigint created_by_id FK
        bigint target_customer_id FK
        bigint reward_id FK
        text mission_text
        enum status "ACTIVE | COMPLETED | CANCELLED"
        datetime completed_at
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    MISSION_REQUEST {
        bigint id PK
        bigint mission_item_id FK
        bigint requester_id FK
        enum status "PENDING | APPROVED | REJECTED"
        text reject_reason "NULL"
        bigint resolved_by_id FK
        datetime resolved_at
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    MISSION_LOG {
        bigint id PK
        bigint mission_item_id FK
        bigint actor_id FK "NULL = 시스템"
        enum action_type "MISSION_CREATED | MISSION_REQUESTED | MISSION_COMPLETED | MISSION_CANCELLED"
        varchar message
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    FAMILY {
        bigint id PK
        varchar name
        datetime deleted_at
    }

    CUSTOMER {
        bigint id PK
        varchar name
        datetime deleted_at
    }
```

---

## 3. 엔티티 상세 정의

### 3.1 CUSTOMER (사용자)

시스템의 모든 일반 사용자(가족 구성원)를 관리하는 중심 엔티티.

**설계 의도**: 인증과 권한의 단일 진입점. CUSTOMER/ADMIN 구분은 테이블 자체로 분리하고 JWT role 클레임으로 식별. 전화번호를 로그인 ID로 사용하여 모바일 중심 UX를 지원.

**데이터 생명주기**:
- **생성**: 회원가입 시 (또는 초대 수락 시 자동 생성)
- **조회**: 로그인(`POST /customers/login` — CUSTOMER 전용 엔드포인트)
- **수정**: 프로필 변경 시 `updated_at` 갱신
- **삭제**: Soft Delete — 탈퇴 시 `deleted_at` 설정, 동일 전화번호 재가입 허용

**핵심 설계 결정**:
- `phone_number`를 UNIQUE로 설정하여 로그인 ID 역할 (이메일은 선택 필드)
- JWT 토큰 발급 시 `customer.id`와 `family_member` 테이블에서 추론한 `familyId`를 페이로드에 포함
- BCrypt 해시 저장 (`password_hash`) — 평문 비밀번호는 시스템에 저장되지 않음

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 사용자 고유 ID |
| `phone_number` | VARCHAR(11) | NOT NULL, UNIQUE | 전화번호 (숫자만 11자리, 01012345678, 로그인 ID로 사용) |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt 해시된 비밀번호 |
| `name` | VARCHAR(100) | NOT NULL | 사용자 이름 |
| `email` | VARCHAR(255) | NULL | 이메일 (선택) |
| `is_onboarded` | BOOLEAN | NOT NULL, DEFAULT FALSE | 온보딩 완료 여부 |
| `terms_agreed_at` | DATETIME | NULL | 약관 동의 시각 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성) |

**인덱스**:
- `idx_customer_phone` : `phone_number` (로그인 조회)
- `idx_customer_email` : `email` (이메일 조회)

**Soft Delete UNIQUE**: `UNIQUE(phone_number, deleted_at)` — 삭제된 사용자의 전화번호 재사용 허용 (MySQL에서 NULL은 UNIQUE 제약에서 중복 허용)

### 3.2 ADMIN (백오피스 운영자)

백오피스 운영을 위한 독립 엔티티. 가족 도메인과 FK 관계 없음.

**설계 의도**: 가족 도메인과 분리된 독립 운영자 테이블. email 기반 로그인으로, 전용 엔드포인트(`POST /admin/login`)를 통해 접근.

**데이터 생명주기**:
- **생성**: 내부 운영 절차에 따라 생성
- **조회**: 관리자 로그인(`POST /admin/login` — ADMIN 전용 엔드포인트)
- **수정**: 프로필 변경 시 `updated_at` 갱신
- **삭제**: Soft Delete — `deleted_at` 설정

**핵심 설계 결정**:
- email 기반 로그인 (CUSTOMER의 phone_number 기반과 독립된 인증 체계)
- JWT 토큰 발급 시 `admin.id`와 role 클레임을 페이로드에 포함

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 운영자 고유 ID |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | 이메일 (로그인 ID로 사용) |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt 해시된 비밀번호 |
| `name` | VARCHAR(100) | NOT NULL | 운영자 이름 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성) |

**인덱스**:
- `idx_admin_email` : `email` (로그인 조회)

**Soft Delete UNIQUE**: `UNIQUE(email, deleted_at)` — 삭제된 운영자의 이메일 재사용 허용

**비즈니스 규칙**:
- 백오피스 API(`/admin/*`) 전용 접근
- 정책 템플릿 CRUD 관리

### 3.3 FAMILY (가족)

데이터를 공유하는 가족 단위. 최대 10명까지 구성 가능.

**설계 의도**: 시스템의 핵심 도메인 엔티티. 가족 단위로 데이터 할당량을 공유하고 정책을 적용하는 기준점. 모든 쿼터, 정책, 알림은 가족 그룹을 기준으로 스코핑되며, Kafka 파티션 키(`familyId`)로도 사용되어 같은 가족의 이벤트는 순서가 보장됨. 기존 FAMILY_QUOTA 1:1 테이블을 통합하여 관리 단순화.

**데이터 생명주기**:
- **생성**: 사용자가 그룹 생성 시 → `created_by_id` 자동 설정, 생성자는 `family_member`에 `role='OWNER'`로 자동 등록
- **조회**: 가족 대시보드(`GET /families/dashboard/usage`), 관리자 가족 목록(`GET /families`), 관리자 가족 상세(`GET /families/{familyId}`)
- **수정**: 할당량 변경(`PATCH /families/policies`) 시, `used_bytes`는 Write-Behind 패턴으로 비동기 업데이트
- **삭제**: Soft Delete — 가족 해체 시 `deleted_at` 설정, 연관 `FAMILY_MEMBER` 등 하위 엔티티도 함께 Soft Delete

**핵심 설계 결정**:
- `created_by_id`는 그룹 최초 생성자(이력/감사 전용)이며, 삭제 불가(`ON DELETE RESTRICT`) — 그룹보다 먼저 탈퇴할 수 없음. **OWNER 권한 판단은 `family_member.role='OWNER'`로만 수행** (복수 OWNER 허용)
- `total_quota_bytes`는 계약 수준 할당량이며, 실시간 잔여량은 Redis(`family:{id}:remaining_bytes`)에서 관리
- `used_bytes`는 Write-Behind 패턴으로 비동기 업데이트 (실시간 값은 Redis 기준)
- 잔여량 = `total_quota_bytes` - `used_bytes`
- 매월 1일 Batch Job으로 `current_month` 갱신 + `used_bytes` 0 리셋 (Redis + MySQL 동시)

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 가족 고유 ID |
| `name` | VARCHAR(100) | NOT NULL | 가족 그룹명 |
| `created_by_id` | BIGINT | NOT NULL, FK → customer.id | 그룹 최초 생성자 (이력/감사 전용) |
| `total_quota_bytes` | BIGINT | NOT NULL, DEFAULT 107374182400 | 총 할당량 (기본 100GB) |
| `used_bytes` | BIGINT | NOT NULL, DEFAULT 0 | 현재 월 총 사용량 (Write-Behind 비동기 갱신) |
| `current_month` | DATE | NOT NULL | 현재 과금 월 (매월 1일 기준) |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성) |

**인덱스**:
- `idx_family_created_by` : `created_by_id`

**비즈니스 규칙**:
- `created_by_id`는 그룹 생성 시 자동 설정 (이력/감사 전용, 권한 판단에 사용하지 않음)
- **다중 OWNER 지원**: `family_member.role='OWNER'`인 구성원이 복수 존재 가능 (role 컬럼에 UNIQUE 제약 없음)
- **정책 충돌 해결 (Last Write Wins)**: 복수 OWNER가 동일 정책을 수정할 경우, 마지막 수정이 적용되며 `audit_log`에 변경 이력이 기록됨- `used_bytes`는 Write-Behind 패턴으로 비동기 업데이트 (실시간 값은 Redis 기준)
- 잔여량 = `total_quota_bytes` - `used_bytes`
- 매월 1일 Batch Job으로 `current_month` 갱신 + `used_bytes` 0 리셋 (Redis + MySQL 동시)

### 3.4 FAMILY_MEMBER (가족 구성원)

CUSTOMER와 FAMILY 간 N:M 관계를 해소하는 매핑 테이블.

**설계 의도**: 한 사용자가 여러 가족에 속할 수 있고, 한 가족에 여러 사용자가 속할 수 있는 다대다 관계를 해소. `role` 필드로 일반 구성원(MEMBER)과 Owner 계정(OWNER)의 권한 수준을 분리하여, JWT 토큰 발급 시 API 접근 권한(member/owner)을 결정하는 기준이 됨. **복수 OWNER 허용** — `role='OWNER'`인 구성원이 여러 명 존재할 수 있으며, OWNER 권한 판단은 이 테이블의 `role` 컬럼으로만 수행.

**데이터 생명주기**:
- **생성**: 그룹 생성 시 owner가 자동 등록(OWNER) / 초대 수락(`INVITE.status=ACCEPTED`) 시 생성
- **조회**: JWT familyId 추론 시 참조, 가족 상세(`GET /families/{familyId}`) 응답에 구성원 목록 포함
- **수정**: 역할 변경(MEMBER ↔︎ OWNER) 시 `role` 업데이트 → `AUDIT_LOG` 기록- **삭제**: Soft Delete — 탈퇴 시 `deleted_at` 설정, 동일 사용자가 같은 가족에 재가입 가능

**핵심 설계 결정**:
- 최대 10명 제한은 애플리케이션 레벨에서 검증 (DB 제약이 아닌 비즈니스 규칙)
- `UNIQUE(family_id, customer_id, deleted_at)` — Soft Delete 후 동일 조합으로 재가입 허용
- `role` 변경 시 Redis 캐시(`family:{id}:policy:version`) 무효화 트리거

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 구성원 고유 ID |
| `family_id` | BIGINT | NOT NULL, FK → family.id | 가족 그룹 |
| `customer_id` | BIGINT | NOT NULL, FK → customer.id | 사용자 |
| `role` | ENUM | NOT NULL, DEFAULT ‘MEMBER’ | 역할 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 (가입 시점) |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성) |

**제약조건**:
- UNIQUE(`family_id`, `customer_id`, `deleted_at`) : 동일 가족에 중복 가입 방지 (삭제 후 재가입 허용)

**ENUM 값**:

| role | 설명 |
| --- | --- |
| `MEMBER` | 일반 가족 구성원 (데이터 조회만 가능) |
| `OWNER` | Owner 계정 (정책 수정 권한, 복수 OWNER 가능) |

**인덱스**:
- `idx_member_family` : `family_id` (가족별 구성원 조회)
- `idx_member_customer` : `customer_id` (사용자의 가족 조회)

### 3.5 CUSTOMER_QUOTA (구성원 월별 할당량)

구성원별 월별 데이터 한도와 사용량, 차단 상태를 관리.

**설계 의도**: 개인별 월별 데이터 한도 및 차단 상태를 월 단위로 스냅샷하는 엔티티. Owner가 자녀에게 월 5GB 한도를 설정하거나, 시간대 차단/수동 차단을 적용한 결과가 이 테이블에 반영됨. Redis의 실시간 상태를 Write-Behind로 동기화하여 이력 조회와 리포트 생성을 지원.

**데이터 생명주기**:
- **생성**: 해당 월에 첫 데이터 사용 이벤트 발생 시 자동 생성 (월별 1건)
- **조회**: 마이페이지(`GET /customers/usage`), 대시보드(`GET /families/dashboard/usage`)
- **수정**: processor-usage가 Write-Behind로 `monthly_used_bytes`, `is_blocked`, `block_reason` 업데이트. Owner의 즉시 차단(`PATCH /families/policies`) 시 `is_blocked`/`block_reason` 직접 변경
- **삭제**: Soft Delete — 일반적으로 삭제되지 않으나 데이터 보정 시 사용

**핵심 설계 결정**:
- 월별 레코드 생성(`current_month`) — 시계열 조회 최적화, 월별 한도 리셋이 자연스러움
- `monthly_limit_bytes = NULL`이면 무제한 사용 허용 (애플리케이션에서 NULL 체크)
- `is_blocked`와 `block_reason`을 분리하여 차단 여부와 차단 사유를 독립적으로 추적
- Redis 키(`family:{id}:user:{uid}:blocked`)와 동기화 — 실시간 차단 판단은 Redis에서 수행

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 레코드 고유 ID |
| `customer_id` | BIGINT | NOT NULL, FK → customer.id | 사용자 |
| `family_id` | BIGINT | NOT NULL, FK → family.id | 가족 그룹 |
| `monthly_limit_bytes` | BIGINT | NULL | 월별 한도 (NULL = 무제한) |
| `monthly_used_bytes` | BIGINT | NOT NULL, DEFAULT 0 | 월별 사용량 (바이트) |
| `current_month` | DATE | NOT NULL | 해당 월 (매월 1일 기준) |
| `is_blocked` | BOOLEAN | NOT NULL, DEFAULT FALSE | 차단 여부 |
| `block_reason` | VARCHAR(50) | NULL | 차단 사유 코드 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성) |

**제약조건**:
- UNIQUE(`customer_id`, `family_id`, `current_month`, `deleted_at`) : 사용자-가족-월 유일성 (삭제 후 재생성 허용)

**차단 사유 코드 (`block_reason`)**:

| 코드 | 설명 |
| --- | --- |
| `MONTHLY_LIMIT_EXCEEDED` | 월별 한도 초과 |
| `FAMILY_QUOTA_EXCEEDED` | 가족 할당량 소진 |
| `TIME_BLOCK` | 시간대 차단 정책 |
| `MANUAL` | Owner에 의한 수동 차단 |
| `APP_BLOCK` | 앱별 차단 정책 (MVP 제외) |

**인덱스**:
- `idx_cquota_customer_month` : (`customer_id`, `current_month`) (월별 한도 조회)
- `idx_cquota_family` : `family_id` (가족별 구성원 상태 조회)

### 3.6 USAGE_RECORD (데이터 사용 이력)

데이터 사용 이벤트의 영속 저장소. processor-usage가 `usage-persist` 토픽을 자기소비하여 Bulk Insert.

**설계 의도**: 시스템에서 가장 높은 쓰기 부하를 받는 이벤트 로그 테이블. 실시간 경로(Redis)에서는 집계값만 관리하고, 개별 이벤트 원본은 이 테이블에 비동기 저장하여 상세 리포트와 감사 추적을 지원. Idempotency 키(`event_id`)로 Kafka 재처리 시 중복 Insert를 방지.

**데이터 생명주기**:
- **생성**: processor-usage → Kafka `usage-persist` 토픽 → 자기소비 → MySQL Bulk Insert (5초 또는 100건 배치)
- **조회**: 개인 사용량(`GET /customers/usage`), 가족 리포트(`GET /families/reports/usage`), 관리자 대시보드(`GET /admin/dashboard`)
- **수정**: 불변(Immutable) — 한 번 저장된 이벤트는 수정되지 않음
- **삭제/아카이브**: 90일 후 S3(Parquet)로 아카이브 후 MySQL에서 파티션 단위 DROP

**핵심 설계 결정**:
- `event_id` UNIQUE는 `deleted_at`를 포함하지 않음 — Idempotency는 삭제 여부와 관계없이 전역적으로 보장
- 월별 RANGE 파티셔닝(`event_time`) — 시간 범위 쿼리에서 파티션 프루닝으로 성능 최적화
- 3계층 보관(Hot/Warm/Cold): 7일(Redis+MySQL) → 90일(MySQL) → S3(Parquet)
- `app_id`는 앱별 사용량 분석용 (MVP에서는 NULL, 향후 앱별 차단 정책과 연동 예정)

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 레코드 고유 ID |
| `event_id` | VARCHAR(50) | NOT NULL, UNIQUE | 이벤트 ID (Idempotency 키) |
| `customer_id` | BIGINT | NOT NULL, FK → customer.id | 사용자 |
| `family_id` | BIGINT | NOT NULL, FK → family.id | 가족 그룹 |
| `bytes_used` | BIGINT | NOT NULL | 사용 바이트 수 |
| `app_id` | VARCHAR(100) | NULL | 앱 식별자 |
| `event_time` | DATETIME | NOT NULL | 이벤트 발생 시각 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | DB 저장 시각 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성) |

**파티셔닝**: 월별 RANGE 파티셔닝 (`event_time` 기준)

**데이터 보관 정책**:

| 계층 | 기간 | 저장소 |
| --- | --- | --- |
| Hot | 7일 | Redis + MySQL |
| Warm | 90일 | MySQL |
| Cold | 90일+ | S3 (Parquet) |

**인덱스**:
- `idx_usage_family_time` : (`family_id`, `event_time`) (가족별 사용량 집계)
- `idx_usage_customer_time` : (`customer_id`, `event_time`) (개인 사용량 조회)
- `idx_usage_event_id` : `event_id` (Idempotency 검증)

### 3.7 POLICY (정책)

데이터 사용에 적용되는 규칙 템플릿 정의. 백오피스 운영자가 관리.

**설계 의도**: 정책을 “정의(Policy)”와 “적용(PolicyAssignment)”으로 분리하는 템플릿 패턴. 운영자가 재사용 가능한 정책 템플릿을 생성하면, Owner가 이를 가족이나 특정 구성원에게 적용하는 2단계 구조. 정책 템플릿은 이름과 유형만 정의. 세부 규칙(rules)은 적용 시점에 POLICY_ASSIGNMENT에서 관리.

**데이터 생명주기**:
- **생성**: 운영자가 관리자 API(`POST /policies`)로 정책 템플릿 생성
- **조회**: 정책 목록(`GET /policies`), processor-usage가 실시간 정책 평가 시 Redis 캐시 참조
- **수정**: 정책 이름/유형 변경 시 `updated_at` 갱신 → Redis 캐시 무효화(`policy:version` 증가)
- **삭제**: Soft Delete — 이미 적용 중인 정책(`POLICY_ASSIGNMENT` 존재)은 삭제 불가(API 레벨 검증, 에러코드 `POLICY_TEMPLATE_IN_USE`)

**핵심 설계 결정**:
- `is_system = TRUE`인 시스템 기본 정책은 삭제/수정 불가 (기본 월별 한도 등)
- `is_active = FALSE`이면 정책 목록에서 제외되며 신규 적용 불가 (Soft Delete와 별개로 운영자가 일시 비활성화 가능)
- `APP_BLOCK` 타입은 MVP 범위 외이나, 확장성을 위해 ENUM에 미리 포함
- `type`별 rules 스키마는 Backend/Frontend에서 하드코딩으로 추론

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 정책 고유 ID |
| `name` | VARCHAR(100) | NOT NULL | 정책 이름 |
| `description` | VARCHAR(255) | NULL | 정책 설명 |
| `require_role`  | ENUM | NOT NULL, DEFAULT ‘MEMBER’ | 최소 요구 역할 |
| `type` | ENUM | NOT NULL | 정책 유형 |
| `default_rules` | JSON | NOT NULL | 기본 정책 규칙 JSON |
| `is_system` | BOOLEAN | DEFAULT FALSE | 시스템 기본 정책 여부 |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | 정책 활성화 여부 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성) |

**ENUM 값 (`require_role`)**:

| require_role | 설명 |
| --- | --- |
| `MEMBER` | 일반 구성원도 적용 가능 (기본값) |
| `OWNER` | Owner만 적용 가능 |

**ENUM 값 (`type`)**:

| type | 설명 |
| --- | --- |
| `MONTHLY_LIMIT` | 월별 한도 |
| `TIME_BLOCK` | 시간대 차단 |
| `MANUAL_BLOCK` | 즉시 차단 |
| `APP_BLOCK` | 앱별 차단 (MVP 제외) |

### 3.8 POLICY_ASSIGNMENT (정책 적용)

정책을 특정 가족/구성원에게 매핑하는 테이블.

**설계 의도**: POLICY 템플릿을 실제 가족/구성원에게 연결하는 브릿지 테이블. `target_customer_id = NULL`이면 가족 전체에 적용, 특정 사용자 ID면 해당 구성원에게만 적용. `is_active` 플래그로 삭제 없이 일시 비활성화를 지원하고, `applied_by_id`로 누가 정책을 적용했는지 추적. 세부 규칙은 적용 단위(가족/개인)별로 다를 수 있으므로 POLICY_ASSIGNMENT에서 관리. 동일 정책 타입이라도 대상에 따라 다른 한도/시간대를 설정 가능.

**데이터 생명주기**:
- **생성**: Owner가 정책 적용(`PATCH /families/policies`) 시 생성 → `AUDIT_LOG` 기록 → Kafka `policy-updated` 이벤트 발행
- **조회**: 가족 정책 조회(`GET /families/policies`), 개인 정책 조회(`GET /customers/policies`), processor-usage 실시간 정책 평가
- **수정**: 활성화/비활성화(`is_active` 토글) → Redis 캐시 무효화 트리거
- **삭제**: Soft Delete — 정책 적용 해제 시

**핵심 설계 결정**:
- `target_customer_id = NULL` 패턴 — 가족 전체 적용과 개인 적용을 하나의 테이블로 통합
- `is_active`와 `deleted_at` 분리 — `is_active=FALSE`는 일시 비활성화(복구 가능), `deleted_at`은 영구 삭제
- processor-usage는 이 테이블을 직접 조회하지 않고 Redis 캐시(`family:{id}:policy:*`)를 통해 참조하여 DB 부하 최소화
- `applied_by_id`는 OWNER(복수 가능)만 가능 — 일반 MEMBER는 정책 적용 불가. 복수 OWNER가 동일 정책을 수정할 경우 **Last Write Wins** 적용 (마지막 수정이 유효, `audit_log`에 전체 이력 기록)
- `rules` JSON은 `policy.type`별로 스키마가 다름 — 애플리케이션에서 타입별 역직렬화 수행. `type`별 스키마는 Backend/Frontend에서 하드코딩으로 추론

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 적용 고유 ID |
| `policy_id` | BIGINT | NOT NULL, FK → policy.id | 정책 |
| `family_id` | BIGINT | NOT NULL, FK → family.id | 대상 가족 |
| `target_customer_id` | BIGINT | NULL, FK → customer.id | 대상 구성원 (NULL = 가족 전체) |
| `applied_by_id` | BIGINT | NOT NULL, FK → customer.id | 적용한 사용자 |
| `rules` | JSON | NOT NULL | 정책 규칙 JSON |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | 활성화 여부 |
| `applied_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 적용 시각 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성) |

**rules JSON 예시 (policy.type 참조)**:

| type (policy.type 참조) | rules JSON 예시 |
| --- | --- |
| `MONTHLY_LIMIT` | `{"limitBytes": 5368709120}` |
| `TIME_BLOCK` | `{"start": "22:00", "end": "07:00", "timezone": "Asia/Seoul"}` |
| `MANUAL_BLOCK` | `{"reason": "MANUAL"}` |
| `APP_BLOCK` | `{"blockedApps": ["com.youtube.app"]}` |

**인덱스**:
- `idx_pa_family` : `family_id` (가족별 정책 조회)
- `idx_pa_target` : `target_customer_id` (구성원별 정책 조회)

**비즈니스 규칙**:
- `target_customer_id`가 NULL이면 해당 가족 전체에 적용
- `applied_by_id`는 OWNER(복수 가능)만 가능
- **Last Write Wins**: 복수 OWNER 간 정책 충돌 시 마지막 수정이 적용됨 (`audit_log`에 변경 이력 기록)

### 3.9 NOTIFICATION_LOG (알림 로그)

발송된 알림 이력. api-notification이 `notification-events` 토픽을 소비하여 저장. SSE 실시간 알림과 병행하여 REST API(`/notifications/*`)로 이력 조회 제공. CUSTOMER 전용 — Admin 알림은 별도 시스템.

**설계 의도**: SSE로 실시간 Push된 알림을 영속 저장하여 이력 조회를 지원하는 이중 채널 구조. 사용자가 오프라인이었거나 SSE 연결이 끊겼을 때 놓친 알림을 REST API로 확인할 수 있음. `type` ENUM으로 Kafka `notification-events` 토픽의 eventType과 1:1 매핑하여 일관된 타입 체계 유지.

**데이터 생명주기**:
- **생성**: api-notification이 `notification-events` 토픽을 소비 → SSE Push + MySQL 저장 동시 수행
- **조회**: 전체 알림(`GET /notifications`), 임계치 알림 필터(`GET /notifications/alert`), 차단 알림 필터(`GET /notifications/block`)
- **수정**: 읽음 처리 시 `is_read = TRUE`로 업데이트
- **삭제**: Soft Delete — 사용자가 알림 삭제 시

**핵심 설계 결정**:
- `type` 기반 필터링을 위해 `idx_notif_customer_type` 복합 인덱스 추가 — REST API 타입별 엔드포인트 성능 보장
- `payload` JSON에 타입별 상세 데이터 저장 (예: THRESHOLD_ALERT → `{"threshold": 50, "remaining": "5GB"}`, BLOCKED → `{"reason": "MONTHLY_LIMIT_EXCEEDED"}`)
- `is_read` 플래그로 읽지 않은 알림 카운트 표시 (PWA 배지 등)
- Kafka eventType(`QUOTA_UPDATED`, `USER_BLOCKED`, `THRESHOLD_ALERT`)과 DB ENUM(`THRESHOLD_ALERT`, `BLOCKED`, `UNBLOCKED`, `POLICY_CHANGED`)은 의미 단위가 다름 — 변환 로직은 api-notification에서 처리

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 알림 고유 ID |
| `customer_id` | BIGINT | NOT NULL, FK → customer.id | 수신자 |
| `family_id` | BIGINT | NOT NULL, FK → family.id | 소속 가족 |
| `type` | ENUM | NOT NULL | 알림 유형 |
| `message` | TEXT | NOT NULL | 알림 메시지 본문 |
| `payload` | JSON | NULL | 추가 데이터 (임계치, 차단 사유 등) |
| `is_read` | BOOLEAN | NOT NULL, DEFAULT FALSE | 읽음 여부 |
| `sent_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 발송 시각 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성) |

**ENUM 값 (`type`)**:

| type | notification-events eventType | 설명 |
| --- | --- | --- |
| `THRESHOLD_ALERT` | THRESHOLD_ALERT | 잔여량 임계치 도달 (50/30/10%) |
| `BLOCKED` | USER_BLOCKED | 사용자 차단됨 |
| `UNBLOCKED` | USER_UNBLOCKED | 사용자 차단 해제됨 |
| `POLICY_CHANGED` | - | 정책 변경 알림 |
| `MISSION_CREATED` | MISSION_CREATED | 새 미션 생성 |
| `REWARD_REQUESTED` | REWARD_REQUESTED | 보상 요청 접수 |
| `REWARD_APPROVED` | REWARD_APPROVED | 보상 승인됨 |
| `REWARD_REJECTED` | REWARD_REJECTED | 보상 거절됨 |
| `APPEAL_CREATED` | APPEAL_CREATED | 이의제기 접수 (부모에게) |
| `APPEAL_APPROVED` | APPEAL_APPROVED | 이의제기 승인됨 (자녀에게) |
| `APPEAL_REJECTED` | APPEAL_REJECTED | 이의제기 거절됨 (자녀에게) |
| `EMERGENCY_APPROVED` | EMERGENCY_APPROVED | 긴급 쿼터 자동 승인됨 (부모에게 사후 알림) |

**인덱스**:
- `idx_notif_customer` : (`customer_id`, `sent_at` DESC) (사용자 알림 목록)
- `idx_notif_customer_type` : (`customer_id`, `type`, `sent_at` DESC) (타입별 알림 필터 — `/notifications/alert`, `/notifications/block`)
- `idx_notif_family` : (`family_id`, `sent_at` DESC) (가족 알림 목록)

---

### 3.10 AUDIT_LOG (감사 로그)

정책 변경, 차단/해제, 권한 변경, 보상/미션 관련 주요 액션에 대한 감사 추적.

**설계 의도**: 시스템의 모든 상태 변경을 불변 이력으로 기록하는 감사 테이블. “누가, 언제, 무엇을, 어떻게 변경했는가”를 `old_value`/`new_value` JSON으로 변경 전후 상태까지 완전히 추적. 컴플라이언스 요구사항 충족과 운영 디버깅을 동시에 지원.

**데이터 생명주기**:
- **생성**: 정책 변경, 사용자 차단/해제, 구성원 추가/삭제, 역할 변경, 할당량 변경 등 주요 액션 발생 시 api-core에서 자동 기록
- **조회**: 관리자 감사 로그(`GET /admin/audit/logs`) — `entity_type`, `action`, `actor_id`별 필터링
- **수정**: 불변(Immutable) — 감사 로그는 한 번 기록되면 수정되지 않음
- **삭제**: Soft Delete — 법적 보관 기간 경과 후에만 삭제 허용

**핵심 설계 결정**:
- `actor_id = NULL`은 시스템 자동 액션 (예: 월별 한도 초과로 자동 차단, Batch 정산 보정)
- `old_value`/`new_value`를 JSON으로 저장하여 엔티티 종류에 관계없이 범용적으로 변경 이력 추적
- `ip_address`는 VARCHAR(45)로 IPv6 주소까지 대응
- 3개 인덱스로 수행자별, 엔티티별, 액션별 조회 경로를 모두 커버
- `actor_id`는 customer.id 참조 (admin 액션은 별도 식별 체계 적용 가능)

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 로그 고유 ID |
| `actor_id` | BIGINT | NULL, FK → customer.id | 수행자 (시스템 = NULL) |
| `action` | VARCHAR(50) | NOT NULL | 수행 액션 |
| `entity_type` | VARCHAR(50) | NOT NULL | 대상 엔티티 종류 |
| `entity_id` | BIGINT | NOT NULL | 대상 엔티티 ID |
| `old_value` | JSON | NULL | 변경 전 값 |
| `new_value` | JSON | NULL | 변경 후 값 |
| `ip_address` | VARCHAR(45) | NULL | 요청 IP (IPv6 대응) |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성) |

**주요 action 값**:

| action | entity_type | 설명 |
| --- | --- | --- |
| `POLICY_CREATED` | POLICY | 정책 생성 |
| `POLICY_UPDATED` | POLICY_ASSIGNMENT | 정책 수정 |
| `POLICY_ASSIGNED` | POLICY_ASSIGNMENT | 정책 적용 |
| `USER_BLOCKED` | CUSTOMER_QUOTA | 사용자 차단 |
| `USER_UNBLOCKED` | CUSTOMER_QUOTA | 사용자 차단 해제 |
| `QUOTA_CHANGED` | FAMILY | 할당량 변경 |
| `REWARD_CREATED` | MISSION_ITEM | 미션 보상 생성 |
| `REWARD_REQUESTED` | MISSION_REQUEST | 보상 요청 |
| `REWARD_APPROVED` | MISSION_REQUEST | 보상 승인 |
| `REWARD_REJECTED` | MISSION_REQUEST | 보상 거절 (사유 포함) |
| `FAMILY_MEMBER_ADDED` | FAMILY_MEMBER | 구성원 추가 (가족 초대) |
| `FAMILY_MEMBER_REMOVED` | FAMILY_MEMBER | 구성원 추방 |
| `ROLE_CHANGED` | FAMILY_MEMBER | 역할 변경 |
| `EMERGENCY_QUOTA_GRANTED` | CUSTOMER_QUOTA | 긴급 쿼터 자동 승인 |

**인덱스**:
- `idx_audit_actor` : (`actor_id`, `created_at` DESC)
- `idx_audit_entity` : (`entity_type`, `entity_id`, `created_at` DESC)
- `idx_audit_action` : (`action`, `created_at` DESC)

### 3.11 INVITE (가족 초대)

전화번호 기반 가족 초대 관리.

**설계 의도**: 기존 회원뿐 아니라 아직 가입하지 않은 사용자도 전화번호로 초대할 수 있도록 지원하는 비동기 초대 흐름. 초대 수락 시 `FAMILY_MEMBER` 레코드가 생성되는 간접 생성 패턴으로, 가입 전 초대와 가입 후 수락을 시간적으로 분리.

**데이터 생명주기**:
- **생성**: 부모(owner)가 초대(`POST /families/{familyId}/invite`) → `status=PENDING` + `expires_at` 설정
- **상태 전이**: `PENDING` → `ACCEPTED`(수락 → `FAMILY_MEMBER` 생성) | `EXPIRED`(만료 시각 경과) | `CANCELLED`(초대자가 취소)
- **조회**: 가족 상세(`GET /families/{familyId}`) 응답에 대기 중 초대 목록 포함 가능
- **삭제**: Soft Delete — 이력 보관

**핵심 설계 결정**:
- `phone_number` 기반 초대 — 미가입 사용자도 초대 가능 (가입 시 전화번호 매칭으로 자동 수락 처리 가능)
- `expires_at`으로 시간 제한 — 만료된 초대는 배치 잡 또는 조회 시점에 `EXPIRED`로 전이
- `role` 필드로 초대 시점에 역할 지정 — 수락 시 `FAMILY_MEMBER.role`로 반영
- `status` ENUM으로 상태 기계(State Machine) 패턴 구현 — 한 방향으로만 전이 가능

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 초대 고유 ID |
| `family_id` | BIGINT | NOT NULL, FK → family.id | 초대 가족 |
| `phone_number` | VARCHAR(11) | NOT NULL | 초대 대상 전화번호 (숫자만 11자리) |
| `role` | ENUM | NOT NULL, DEFAULT ‘MEMBER’ | 초대 역할 |
| `status` | ENUM | NOT NULL, DEFAULT ‘PENDING’ | 초대 상태 |
| `expires_at` | DATETIME | NOT NULL | 만료 시각 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성) |

**ENUM 값 (`status`)**:

| status | 설명 |
| --- | --- |
| `PENDING` | 대기 중 |
| `ACCEPTED` | 수락됨 |
| `EXPIRED` | 만료됨 |
| `CANCELLED` | 취소됨 |

**인덱스**:
- `idx_invite_phone` : (`phone_number`, `status`)
- `idx_invite_family` : (`family_id`, `status`)

### 3.12 REWARD_TEMPLATE (보상 템플릿)

서비스에서 제공하는 보상 종류 정의. 관리자가 관리하며 부모가 미션 생성 시 선택.

**설계 의도**: 보상의 종류와 기본값을 시스템 레벨에서 관리하는 마스터 테이블. 부모가 미션 생성 시 템플릿을 선택하고 값을 커스텀할 수 있음.

**데이터 생명주기**:
- **생성**: 관리자가 보상 템플릿 생성(`POST /admin/rewards/templates`)
- **조회**: 보상 템플릿 목록(`GET /rewards/templates`, `GET /admin/rewards/templates`)
- **수정**: 관리자가 수정(`PUT /admin/rewards/templates/{id}`)
- **삭제**: 관리자가 삭제(`DELETE /admin/rewards/templates/{id}`)

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 템플릿 고유 ID |
| `name` | VARCHAR(100) | NOT NULL | 보상명 (예: 메가커피 아메리카노(ICE), 100MB) |
| `category` | ENUM | NOT NULL | 보상 카테고리 |
| `thumbnail_url` | VARCHAR(500) | NULL | 상품 썸네일 이미지 경로 (S3/R2) |
| `price` | INT | NOT NULL | 단가 (원 단위, 관리/정산용) |
| `is_system` | BOOLEAN | NOT NULL, DEFAULT TRUE | 시스템 제공 여부 |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | 활성 여부 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성) |

**ENUM 값 (`category`)**:

| category | 설명 |
| --- | --- |
| `DATA` | 데이터 |
| `GIFTICON` | 기프티콘 |

**예시 데이터**:

| id | category | name | thumbnail_url | price | is_system |
| --- | --- | --- | --- | --- | --- |
| 1 | GIFTICON | 메가커피 아메리카노(ICE) | /rewards/mega-coffee.jpg | 3000 | true |
| 2 | GIFTICON | 맘스터치 싸이버거 세트 | /rewards/moms-touch.jpg | 3000 | true |
| 3 | DATA | 100MB | NULL | 3000 | true |
| 4 | DATA | 300MB | NULL | 3000 | true |
| 5 | DATA | 500MB | NULL | 3000 | true |
| 6 | DATA | 1GB | NULL | 3000 | true |

### 3.13 REWARD (보상 인스턴스)

미션 생성 시 REWARD_TEMPLATE에서 스냅샷 복사하여 생성되는 보상 인스턴스. 템플릿이 이후 수정/삭제되어도 기존 보상에는 영향 없음.

**설계 의도**: 미션의 보상을 독립적인 엔티티로 분리하여 스냅샷 관리. REWARD_TEMPLATE에서 name, category, thumbnail_url을 스냅샷 복사. category는 스냅샷 전용(오버라이드 불가).

**데이터 생명주기**:
- **생성**: 부모(OWNER)가 미션 생성(`POST /missions`) 시 자동 생성
- **조회**: 미션 조회 시 reward 객체로 포함
- **수정**: 수정하지 않음 (생성 시점의 스냅샷 보존)
- **삭제**: 삭제하지 않음 (이력 보존)

**핵심 설계 결정**:
- REWARD : MISSION_ITEM = 1:1 관계 (미션 하나당 보상 하나)
- REWARD_TEMPLATE의 name, category, thumbnail_url을 스냅샷 복사 — 템플릿 변경 시 기존 보상 불변
- category는 스냅샷 전용으로 API 요청의 rewardCategory 필드 불필요

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 보상 고유 ID |
| `reward_template_id` | BIGINT | NOT NULL, FK → reward_template.id | 원본 보상 템플릿 |
| `name` | VARCHAR(100) | NOT NULL | 보상명 (템플릿에서 스냅샷) |
| `category` | ENUM | NOT NULL | 보상 카테고리 (템플릿에서 스냅샷, 오버라이드 불가) |
| `thumbnail_url` | VARCHAR(500) | NULL | 이미지 경로 스냅샷 (템플릿에서 복사) |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성, 이력 보존 목적으로 운영상 미사용) |

**ENUM 값 (`category`)**:

| category | 설명 |
| --- | --- |
| `DATA` | 데이터 |
| `GIFTICON` | 기프티콘 |

**인덱스**:
- `idx_reward_template` : (`reward_template_id`) (템플릿별 보상 조회)

### 3.14 MISSION_ITEM (미션 항목)

부모가 생성하는 미션 보상 항목. 자녀가 미션을 달성하면 보상을 요청할 수 있음.

**설계 의도**: 부모가 자녀에게 행동 기반 보상을 제공하는 미션 시스템의 핵심 엔티티. 미션은 자유 텍스트로 작성하고, 보상은 REWARD 엔티티(REWARD_TEMPLATE 스냅샷)를 참조. 한 번 생성하면 수정 불가(삭제 후 재생성). 특정 자녀에게 미션을 명시적으로 할당.

**데이터 생명주기**:
- **생성**: 부모(OWNER)가 미션 생성(`POST /missions`) — `status=ACTIVE`, REWARD 인스턴스 동시 생성
- **조회**: 미션 카드 목록(`GET /missions`), 미션 상태 로그(`GET /missions/logs`), 요청 이력(`GET /missions/requests`)
- **상태 전이**: `ACTIVE` → `COMPLETED`(보상 승인 시) 또는 `CANCELLED`(삭제 시)
- **삭제**: `DELETE /missions/{id}` → `status=CANCELLED`

**핵심 설계 결정**:
- 미션은 생성 후 수정 불가 — 삭제(`CANCELLED`) 후 재생성
- MISSION_REQUEST 승인 시 `status=COMPLETED` + `completed_at` 기록
- 한 번 COMPLETED된 미션은 더 이상 보상 요청 불가
- target_customer_id는 해당 가족의 MEMBER 역할 사용자만 가능. OWNER는 대상이 될 수 없음

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 미션 고유 ID |
| `family_id` | BIGINT | NOT NULL, FK → family.id | 가족 |
| `created_by_id` | BIGINT | NOT NULL, FK → customer.id | 생성자 (부모) |
| `target_customer_id` | BIGINT | NOT NULL, FK → customer.id | 대상 자녀 (MEMBER 역할만 가능) |
| `reward_id` | BIGINT | NOT NULL, FK → reward.id | 보상 인스턴스 |
| `mission_text` | TEXT | NOT NULL | 미션 내용 (자유 텍스트) |
| `status` | ENUM | NOT NULL, DEFAULT 'ACTIVE' | 미션 상태 |
| `completed_at` | DATETIME | NULL | 완료 시각 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성, 이력 보존 목적으로 운영상 미사용) |

**ENUM 값 (`status`)**:

| status | 설명 |
| --- | --- |
| `ACTIVE` | 활성 — 보상 요청 가능 |
| `COMPLETED` | 완료 — 보상 승인됨 |
| `CANCELLED` | 취소 — 삭제됨 |

**상태 흐름**:
```
MISSION_ITEM (ACTIVE)
    ├─ MISSION_REQUEST (REJECTED)
    ├─ MISSION_REQUEST (REJECTED)
    └─ MISSION_REQUEST (APPROVED)
           ↓
MISSION_ITEM → COMPLETED
```

**인덱스**:
- `idx_mission_family` : (`family_id`, `status`, `created_at` DESC) (가족별 미션 목록)
- `idx_mission_creator` : (`created_by_id`) (생성자별 미션)
- `idx_mission_target` : (`target_customer_id`, `status`, `created_at` DESC) (대상 자녀별 미션 목록)

### 3.15 MISSION_REQUEST (미션 보상 요청)

자녀가 미션 달성 후 부모에게 보상을 요청하는 엔티티.

**설계 의도**: 미션 완료를 자녀가 자발적으로 신고하고, 부모가 확인 후 승인/거절하는 2단계 검증 구조. 거절 시 재요청 가능, 승인 시 MISSION_ITEM이 COMPLETED로 전이.

**데이터 생명주기**:
- **생성**: 자녀가 보상 요청(`POST /rewards/requests`) — `MISSION_ITEM.status=ACTIVE`인 경우만 가능
- **조회**: 미션 요청 이력(`GET /missions/requests`)에 포함
- **수정**: 부모가 승인/거절(`PUT /rewards/requests/{id}/respond`) — 승인 시 MISSION_ITEM도 COMPLETED로 변경
- **삭제**: 삭제하지 않음 (이력 보존)

**핵심 설계 결정**:
- 거절(`REJECTED`) 시 동일 미션에 대해 재요청 가능
- 승인(`APPROVED`) 시 해당 `MISSION_ITEM.status=COMPLETED` + `completed_at` 동시 업데이트
- 하나의 MISSION_ITEM에 대해 여러 MISSION_REQUEST 생성 가능 (거절 후 재요청)
- 거절(REJECTED) 시 reject_reason에 사유를 기록. POLICY_APPEAL의 reject_reason 패턴과 동일

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 요청 고유 ID |
| `mission_item_id` | BIGINT | NOT NULL, FK → mission_item.id | 미션 항목 |
| `requester_id` | BIGINT | NOT NULL, FK → customer.id | 요청자 (자녀) |
| `status` | ENUM | NOT NULL, DEFAULT 'PENDING' | 요청 상태 |
| `reject_reason` | TEXT | NULL | 거절 사유 (REJECTED 시 OWNER가 작성) |
| `resolved_by_id` | BIGINT | NULL, FK → customer.id | 승인/거절자 (부모) |
| `resolved_at` | DATETIME | NULL | 처리 시각 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성, 이력 보존 목적으로 운영상 미사용) |

**ENUM 값 (`status`)**:

| status | 설명 |
| --- | --- |
| `PENDING` | 대기 중 |
| `APPROVED` | 승인됨 |
| `REJECTED` | 거절됨 |

**인덱스**:
- `idx_mreq_mission` : (`mission_item_id`, `created_at` DESC) (미션별 요청 이력)
- `idx_mreq_requester` : (`requester_id`, `created_at` DESC) (요청자별 이력)

### 3.16 FAMILY_RECAP_MONTHLY (월간 가족 리캡)

월말 배치로 생성되는 가족 데이터 사용 리캡 스냅샷.

**설계 의도**: 월말에 가족의 데이터 사용 현황, 미션 보상 실적, 이의제기 현황 등을 집계하여 스냅샷으로 저장. 주간 리캡(FAMILY_RECAP_WEEKLY) 4~5건을 월간 단위로 통합. 이후 데이터 변경과 무관하게 과거 기록이 유지되어 월말 가족 회의를 지원.

**데이터 생명주기**:
- **생성**: 월말 배치 잡에 의해 자동 생성 — 해당 월의 모든 지표를 집계하여 1건 저장
- **조회**: 월간 리캡(`GET /recaps/monthly?year=&month=`)
- **수정**: 배치 재실행 시 `updated_at` 갱신 (UPSERT)
- **삭제**: 삭제하지 않음 (이력 보존)

**핵심 설계 결정**:
- `UNIQUE(family_id, report_month)` — 가족당 월 1건 보장
- 스냅샷 저장 방식 — 월별 데이터를 집계 시점에 고정하여 과거 수정 영향 없음
- 기존 개별 컬럼(요일별 사용률, 피크 시간대, 미션, 이의제기 하이라이트 등)을 JSON으로 통합하여 스키마 유연성 확보

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 리캡 고유 ID |
| `family_id` | BIGINT | NOT NULL, FK → family.id | 가족 |
| `report_month` | DATE | NOT NULL | 리캡 월 (예: 2026-03-01) |
| `total_used_bytes` | BIGINT | NOT NULL | 월 총 사용량 |
| `total_quota_bytes` | BIGINT | NOT NULL | 월 총 할당량 |
| `usage_rate_percent` | DECIMAL(5,2) | NOT NULL | 사용률 (%) |
| `usage_by_weekday` | JSON | NULL | 월간 요일별 사용 비율 ({monday: N, ...}) |
| `peak_usage` | JSON | NULL | 피크 사용 정보 ({startHour, endHour, mostUsedWeekday}) |
| `mission_summary_json` | JSON | NULL | 미션 요약 ({totalMissionCount, completedMissionCount, rejectedRequestCount}) |
| `appeal_summary_json` | JSON | NULL | 이의제기 요약 ({totalAppeals, approvedAppeals, rejectedAppeals}) |
| `appeal_highlights_json` | JSON | NULL | 이의제기 하이라이트 ({topSuccessfulRequester, topAcceptedApprover}) |
| `communication_score` | DECIMAL(5,2) | NULL | NORMAL 이의제기와 미션 완료 기반 월간 소통 점수 (0~100, 데이터 없으면 NULL) |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성, 이력 보존 목적으로 운영상 미사용) |

**제약조건**:
- UNIQUE(`family_id`, `report_month`) — 가족당 월 1건 보장

**JSON/계산 규칙**:
- `appeal_highlights_json`은 `type='NORMAL' AND status='APPROVED'`인 정책 이의제기만 집계한다. `EMERGENCY`는 제외한다.
- `topSuccessfulRequester`는 월간 승인된 정책 이의제기를 가장 많이 성공한 구성원과 최신 승인 이력 최대 3개(`recentApprovedAppeals`, `requestedAt` 기준 내림차순)를 저장한다.
- `topAcceptedApprover`는 월간 승인된 정책 이의제기를 가장 많이 수락한 구성원과 최신 수락 이력 최대 3개(`recentAcceptedAppeals`, `resolvedAt` 기준 내림차순)를 저장한다.
- `communication_score`는 `type='NORMAL'`인 정책 이의제기와 `mission_summary_json`(`totalMissionCount`, `completedMissionCount`)를 기반으로 계산한다. `EMERGENCY`는 제외한다.
- `approvedNormalAppealCount`는 `type='NORMAL' AND status='APPROVED'`, `rejectedNormalAppealCount`는 `type='NORMAL' AND status='REJECTED'`, `respondedNormalAppealCount`는 둘의 합, `totalNormalAppealCount`는 `type='NORMAL'` 전체 생성 건수다.
- `A = approvedNormalAppealCount / respondedNormalAppealCount`(`respondedNormalAppealCount = 0`이면 `0`), `B = respondedNormalAppealCount / totalNormalAppealCount`, `C = min(completedMissionCount / approvedNormalAppealCount, 1.0)`(`approvedNormalAppealCount = 0`이면 `0`)로 계산한다.
- `totalNormalAppealCount > 0`이면 최종 점수는 `round((A * 0.5 + B * 0.3 + C * 0.2) * 100, 2)`를 사용한다.
- `totalNormalAppealCount = 0 AND totalMissionCount > 0`이면 `communication_score = round((completedMissionCount / totalMissionCount) * 100, 2)`를 사용한다.
- `totalNormalAppealCount = 0 AND totalMissionCount = 0`이면 `communication_score`는 `NULL`이다.

**인덱스**:
- `idx_recap_monthly_family_month` : (`family_id`, `report_month` DESC) (가족별 월간 리캡 조회)

---

### 3.17 POLICY_APPEAL (정책 이의제기 / 긴급 쿼터 요청)

자녀가 부모에게 적용된 정책에 이의를 제기하거나, 긴급 쿼터를 요청하는 엔티티.

**설계 의도**: 두 가지 유형을 지원. (1) **NORMAL**: 자녀가 본인에게 적용된 정책(POLICY_ASSIGNMENT)에 대해 부모에게 이의를 제기하고, 부모가 이를 검토 후 승인/거절하는 2단계 소통 구조. (2) **EMERGENCY**: 자녀가 월 1회, 100~300MB를 부모 승인 없이 사유만 남기고 데이터를 받을 수 있는 긴급 쿼터 요청 (자동 승인 후 부모에게 사후 알림).

**데이터 생명주기**:
- **생성 (NORMAL)**: 자녀가 이의제기(`POST /appeals`) — `status=PENDING`
- **생성 (EMERGENCY)**: 자녀가 긴급 요청(`POST /appeals/emergency`) — `status=APPROVED` (자동)
- **조회**: 이의제기 목록(`GET /appeals`), 상세(`GET /appeals/{id}`)
- **수정**: 부모가 승인/거절 처리 — `status` 전이 + `resolved_by_id`, `resolved_at` 기록 (EMERGENCY는 즉시 APPROVED)
- **삭제**: Soft Delete 없음 — 이력 보존

**핵심 설계 결정**:
- `type`으로 NORMAL(일반 이의제기)과 EMERGENCY(긴급 쿼터)를 구분
- `policy_assignment_id`: NORMAL 시 NOT NULL (어떤 정책에 대한 이의인지), EMERGENCY 시 NULL (특정 정책 대상 아님)
- `resolved_by_id`: NORMAL 시 처리자(부모), EMERGENCY 시 NULL (시스템 자동 승인)
- `desired_rules`: EMERGENCY 시 `{"additionalBytes": 209715200}` 형태
- EMERGENCY는 월 1회 제한 (`uk_appeal_emergency_month` UNIQUE 제약으로 DB 레벨 동시성 안전 중복 방지, `emergency_grant_month`에 해당 월 1일 저장, NORMAL은 NULL이므로 UNIQUE 제약 무관)
- EMERGENCY 허용 범위: 100~300MB (104,857,600 ~ 314,572,800 bytes)
- 무제한 쿼터 사용자(monthly_limit_bytes=NULL)는 EMERGENCY 요청 불가
- 승인(APPROVED) 시 부모가 해당 정책 수정/해제를 별도로 수행 (appeal 자체가 정책을 자동 변경하지 않음)
- `desired_rules = NULL`이면 부모가 직접 수정 (B 방식), 값이 있으면 승인 시 PolicyAssignment에 자동 반영 (A 방식)
- `desired_rules` 스키마는 policy.type별 rules 스키마와 동일
- NORMAL 타입의 PENDING 상태 이의제기만 요청자 본인이 취소 가능 (EMERGENCY 취소 불가)

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 이의제기 고유 ID |
| `type` | ENUM | NOT NULL, DEFAULT 'NORMAL' | 요청 유형 (NORMAL: 이의제기, EMERGENCY: 긴급 쿼터) |
| `policy_assignment_id` | BIGINT | NULL, FK → policy_assignment.id | 대상 정책 적용 (EMERGENCY 시 NULL) |
| `requester_id` | BIGINT | NOT NULL, FK → customer.id | 요청자 (자녀) |
| `request_reason` | TEXT | NOT NULL | 이의제기/긴급요청 사유 |
| `reject_reason` | TEXT | NULL | 거절 사유 (REJECTED 시 부모가 작성) |
| `desired_rules` | JSON | NULL | 원하는 정책 값 (EMERGENCY: `{"additionalBytes": N}`) |
| `status` | ENUM | NOT NULL, DEFAULT 'PENDING' | 처리 상태 |
| `emergency_grant_month` | DATE | NULL | EMERGENCY 시 해당 월 1일 (NORMAL은 NULL). UNIQUE 제약으로 월 1회 중복 방지 |
| `resolved_by_id` | BIGINT | NULL, FK → customer.id | 처리자 (부모, EMERGENCY는 NULL=시스템) |
| `resolved_at` | DATETIME | NULL | 처리 시각 |
| `cancelled_at` | DATETIME | NULL | 취소 시각 (CANCELLED 시 기록) |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성, 이력 보존 목적으로 운영상 미사용) |

**ENUM 값 (`type`)**:

| type | 설명 |
| --- | --- |
| `NORMAL` | 정책 이의제기 (부모 승인 필요) |
| `EMERGENCY` | 긴급 쿼터 요청 (자동 승인, 월 1회, 100~300MB) |

**ENUM 값 (`status`)**:

| status | 설명 |
| --- | --- |
| `PENDING` | 대기 중 |
| `APPROVED` | 승인됨 |
| `REJECTED` | 거절됨 |
| `CANCELLED` | 취소됨 (요청자 본인이 취소, NORMAL만 가능) |

**인덱스**:
- `idx_appeal_assignment` : `policy_assignment_id` (정책 적용별 이의제기 조회)
- `idx_appeal_requester` : `requester_id` (요청자별 이의제기 조회)
- `idx_appeal_emergency_monthly` : (`requester_id`, `type`, `status`, `created_at`) (월별 긴급 요청 조회 최적화)
- `uk_appeal_emergency_month` : UNIQUE (`requester_id`, `emergency_grant_month`) (월 1회 긴급 요청 동시성 안전 중복 방지, NULL은 UNIQUE 제약 미적용)

---

### 3.18 POLICY_APPEAL_COMMENT (이의제기 댓글)

이의제기 건에 대해 부모-자녀가 주고받는 댓글 엔티티.

**설계 의도**: 이의제기 처리 과정에서 부모와 자녀가 의사소통할 수 있는 댓글 스레드. 자유로운 텍스트 소통을 지원하며, 소프트 삭제로 이력 관리.

**데이터 생명주기**:
- **생성**: 부모 또는 자녀가 댓글 작성(`POST /appeals/{id}/comments`)
- **조회**: 이의제기 상세 조회 시 댓글 목록 포함
- **삭제**: Soft Delete — 댓글 삭제 시 `deleted_at` 설정

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 댓글 고유 ID |
| `appeal_id` | BIGINT | NOT NULL, FK → policy_appeal.id | 이의제기 |
| `author_id` | BIGINT | NOT NULL, FK → customer.id | 작성자 |
| `comment` | TEXT | NOT NULL | 댓글 내용 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성) |

**인덱스**:
- `idx_appeal_comment_appeal` : (`appeal_id`, `created_at`) (이의제기별 댓글 목록)
- `idx_appeal_comment_author` : `author_id` (작성자별 댓글)

---

### 3.19 MISSION_LOG (미션 이벤트 로그)

미션의 이벤트 타임라인을 불변 로그로 기록하는 엔티티.

**설계 의도**: 미션 생성부터 완료/취소까지의 모든 이벤트를 타임라인 순서로 기록. 불변(Immutable) 로그로 Soft Delete 없음.

**데이터 생명주기**:
- **생성**: 미션 관련 이벤트 발생 시 자동 기록 (생성, 요청, 완료, 취소)
- **조회**: 미션 상세 조회 시 타임라인으로 표시, 미션 로그(`GET /missions/logs`)
- **수정/삭제**: 불변(Immutable) — 로그는 수정/삭제하지 않음

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 로그 고유 ID |
| `mission_item_id` | BIGINT | NOT NULL, FK → mission_item.id | 미션 항목 |
| `actor_id` | BIGINT | NULL, FK → customer.id | 수행자 (NULL = 시스템) |
| `action_type` | ENUM | NOT NULL | 이벤트 유형 |
| `message` | VARCHAR(500) | NOT NULL | 로그 메시지 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 이벤트 발생 시각 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성, 이력 보존 목적으로 운영상 미사용) |

**ENUM 값 (`action_type`)**:

| action_type | 설명 |
| --- | --- |
| `MISSION_CREATED` | 미션 생성 (부모) |
| `MISSION_REQUESTED` | 보상 요청 (자녀) |
| `MISSION_COMPLETED` | 미션 완료 처리 — 보상 승인 시 시스템이 상태 전이 (actor_id=NULL) |
| `MISSION_CANCELLED` | 미션 삭제/취소 (부모) |

> **역할 분리**: 요청의 처리 결과(승인/거절)는 `mission_request.status`(APPROVED/REJECTED)가 담당하고, `mission_log`는 미션 자체의 상태 변화만 기록합니다. 보상 거절(REJECTED)은 미션 상태 변화가 아니므로 로그에 기록하지 않습니다.

**인덱스**:
- `idx_mission_log_item` : (`mission_item_id`, `created_at`) (미션별 타임라인 조회)
- `idx_mission_log_actor` : `actor_id` (수행자별 로그)

---

### 3.20 FAMILY_RECAP_WEEKLY (주간 가족 리캡)

주간 단위 스냅샷. 월간 리캡 생성 시 4~5개 주간 데이터를 집계하는 중간 테이블. API 노출 없음.

**설계 의도**: 주간 단위 스냅샷. 월간 리캡 생성 시 4~5개 주간 데이터를 집계하는 중간 테이블. API 노출 없음.

**데이터 생명주기**:
- **생성**: 매주 월요일 배치 잡으로 생성
- **조회**: 배치 잡에서만 조회 (월간 리캡 생성 시 집계 소스)
- **수정**: 배치 재실행 시 `updated_at` 갱신 (UPSERT)
- **삭제**: 삭제하지 않음 (이력 보존)

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 주간 리캡 고유 ID |
| `family_id` | BIGINT | NOT NULL, FK → family.id | 가족 |
| `week_start_date` | DATE | NOT NULL | 주 시작일 (월요일 기준) |
| `total_used_bytes` | BIGINT | NOT NULL | 주간 총 사용량 |
| `total_quota_bytes` | BIGINT | NOT NULL | 주간 총 할당량 |
| `usage_rate_percent` | DECIMAL(5,2) | NOT NULL | 사용률 (%) |
| `usage_by_weekday` | JSON | NOT NULL | 7요일 고정 사용 비율 |
| `peak_usage` | JSON | NULL | 피크 사용 정보 ({startHour, endHour, peakBytes}) |
| `mission_created_count` | INT | NOT NULL, DEFAULT 0 | 생성된 미션 수 |
| `mission_completed_count` | INT | NOT NULL, DEFAULT 0 | 완료된 미션 수 |
| `mission_rejected_count` | INT | NOT NULL, DEFAULT 0 | 거절된 미션 요청 수 |
| `appeal_count` | INT | NOT NULL, DEFAULT 0 | 이의제기 수 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 생성일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성, 이력 보존 목적으로 운영상 미사용) |

**제약조건**:
- UNIQUE(`family_id`, `week_start_date`) — 가족당 주 1건 보장

**인덱스**:
- `idx_recap_weekly_family_week` : (`family_id`, `week_start_date` DESC) (가족별 주간 리캡 조회)

### 3.21 REWARD_GRANT (보상 지급)

미션 완료 후 실제 사용자에게 지급된 보상 기록.

**설계 의도**:
보상 지급 이력을 관리하고 기프티콘, 데이터 지급 등의 실제 처리 결과를 저장한다.
Admin 화면에서 **지급 내역** 조회에 사용된다.

**Admin 화면 매핑**:
- 보상 관리 → 지급 내역

**데이터 생명주기**:
- **생성**: MISSION_REQUEST 승인 시 자동 생성
- **조회**: 관리자 지급 내역 조회 (`GET /admin/rewards/grants`)
- **수정**: 사용 상태 변경 (ISSUED → USED 또는 EXPIRED)
- **삭제**: 없음 (이력 보존, Soft Delete 미적용)

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| `id` | BIGINT | PK, AUTO_INCREMENT | 지급 ID |
| `reward_id` | BIGINT | NOT NULL, FK → reward.id | 지급된 보상 |
| `customer_id` | BIGINT | NOT NULL, FK → customer.id | 수령 사용자 |
| `mission_item_id` | BIGINT | NOT NULL, FK → mission_item.id | 완료된 미션 |
| `coupon_code` | VARCHAR(100) | NULL | 쿠폰 코드 |
| `coupon_url` | VARCHAR(255) | NULL | 쿠폰 URL (바코드 등) |
| `status` | ENUM | NOT NULL, DEFAULT 'ISSUED' | 지급 상태 |
| `expired_at` | DATETIME | NULL | 만료일시 |
| `created_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 지급일시 |
| `updated_at` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 수정일시 |
| `deleted_at` | DATETIME | NULL | Soft Delete (NULL = 활성, 이력 보존 목적으로 운영상 미사용) |

**ENUM 값 (`status`)**:

| status | 설명 |
| --- | --- |
| `ISSUED` | 지급됨 |
| `USED` | 사용됨 |
| `EXPIRED` | 만료 |

**인덱스**:

| 인덱스명 | 컬럼 | 용도 |
| --- | --- | --- |
| `idx_reward_grant_customer` | (`customer_id`) | 사용자별 지급 이력 조회 |
| `idx_reward_grant_status` | (`status`) | 상태별 지급 이력 조회 |
| `idx_reward_grant_expired` | (`expired_at`) | 만료 대상 조회 |

---

## 4. 관계 정의

### 4.1 관계 매트릭스

| 부모 엔티티 | 자식 엔티티 | 카디널리티 | FK 컬럼 | 설명 |
| --- | --- | --- | --- | --- |
| CUSTOMER | FAMILY_MEMBER | 1:N | `customer_id` | 사용자는 여러 가족에 속할 수 있음 |
| FAMILY | FAMILY_MEMBER | 1:N | `family_id` | 가족은 여러 구성원 보유 (최대 10명) |
| CUSTOMER | CUSTOMER_QUOTA | 1:N | `customer_id` | 사용자는 월별 쿼터 레코드 보유 |
| FAMILY | CUSTOMER_QUOTA | 1:N | `family_id` | 가족 범위 내 구성원 쿼터 |
| CUSTOMER | USAGE_RECORD | 1:N | `customer_id` | 사용자의 데이터 사용 이력 |
| FAMILY | USAGE_RECORD | 1:N | `family_id` | 가족 범위 내 사용 이력 |
| POLICY | POLICY_ASSIGNMENT | 1:N | `policy_id` | 정책은 여러 곳에 적용 가능 |
| FAMILY | POLICY_ASSIGNMENT | 1:N | `family_id` | 가족에 적용된 정책 목록 |
| CUSTOMER | POLICY_ASSIGNMENT | 0:N | `target_customer_id` | 특정 구성원 대상 정책 (NULL=전체) |
| CUSTOMER | NOTIFICATION_LOG | 1:N | `customer_id` | 사용자에게 발송된 알림 |
| FAMILY | NOTIFICATION_LOG | 1:N | `family_id` | 가족 범위 알림 |
| FAMILY | INVITE | 1:N | `family_id` | 가족의 초대 목록 |
| CUSTOMER | AUDIT_LOG | 0:N | `actor_id` | 사용자의 액션 이력 |
| FAMILY | MISSION_ITEM | 1:N | `family_id` | 가족의 미션 목록 |
| CUSTOMER | MISSION_ITEM | 1:N | `created_by_id` | 부모가 생성한 미션 |
| CUSTOMER | MISSION_ITEM | 1:N | `target_customer_id` | 대상 자녀에게 할당된 미션 |
| REWARD_TEMPLATE | REWARD | 1:N | `reward_template_id` | 보상 템플릿으로 생성된 보상 인스턴스 |
| REWARD | MISSION_ITEM | 1:1 | `reward_id` | 보상에 연결된 미션 |
| MISSION_ITEM | MISSION_REQUEST | 1:N | `mission_item_id` | 미션의 보상 요청 |
| CUSTOMER | MISSION_REQUEST | 1:N | `requester_id` | 자녀의 보상 요청 |
| CUSTOMER | MISSION_REQUEST | 0:N | `resolved_by_id` | 부모의 보상 승인/거절 |
| FAMILY | FAMILY_RECAP_MONTHLY | 1:N | `family_id` | 가족의 월간 리캡 |
| FAMILY | FAMILY_RECAP_WEEKLY | 1:N | `family_id` | 가족의 주간 리캡 |
| POLICY_ASSIGNMENT | POLICY_APPEAL | 0:N | `policy_assignment_id` | 정책 적용에 대한 이의제기 (EMERGENCY 시 NULL) |
| CUSTOMER | POLICY_APPEAL | 1:N | `requester_id` | 자녀의 이의제기 |
| CUSTOMER | POLICY_APPEAL | 0:N | `resolved_by_id` | 부모의 이의제기 처리 |
| POLICY_APPEAL | POLICY_APPEAL_COMMENT | 1:N | `appeal_id` | 이의제기의 댓글 목록 |
| CUSTOMER | POLICY_APPEAL_COMMENT | 1:N | `author_id` | 댓글 작성자 |
| MISSION_ITEM | MISSION_LOG | 1:N | `mission_item_id` | 미션 이벤트 로그 |
| CUSTOMER | MISSION_LOG | 0:N | `actor_id` | 로그 수행자 |
| REWARD | REWARD_GRANT | 1:N | `reward_id` | 보상에 대한 지급 이력 |
| CUSTOMER | REWARD_GRANT | 1:N | `customer_id` | 사용자의 보상 지급 이력 |
| MISSION_ITEM | REWARD_GRANT | 1:N | `mission_item_id` | 미션의 보상 지급 이력 |

### 4.2 자기 참조 관계

`FAMILY` 테이블은 `CUSTOMER`에 대해 하나의 FK를 가짐:

```
FAMILY.created_by_id → CUSTOMER.id  (그룹 최초 생성자, 이력/감사 전용, NOT NULL)
```

> **Note**: `created_by_id`는 이력/감사 전용이며, OWNER 권한 판단은 `family_member.role='OWNER'`로만 수행합니다.
>

`POLICY_ASSIGNMENT` 테이블은 `CUSTOMER`에 대해 두 개의 FK를 가짐:

```
POLICY_ASSIGNMENT.target_customer_id → CUSTOMER.id  (정책 대상, NULL 허용 = 가족 전체)
POLICY_ASSIGNMENT.applied_by_id      → CUSTOMER.id  (정책 적용자, NOT NULL)
```

`MISSION_ITEM` 테이블은 `CUSTOMER`에 대해 두 개의 FK를 가짐:

```
MISSION_ITEM.created_by_id      → CUSTOMER.id  (미션 생성자 = 부모, NOT NULL)
MISSION_ITEM.target_customer_id → CUSTOMER.id  (미션 대상 = 자녀, NOT NULL)
```

`MISSION_REQUEST` 테이블은 `CUSTOMER`에 대해 두 개의 FK를 가짐:

```
MISSION_REQUEST.requester_id    → CUSTOMER.id  (보상 요청자 = 자녀, NOT NULL)
MISSION_REQUEST.resolved_by_id  → CUSTOMER.id  (승인/거절자 = 부모, NULL 허용)
```

`POLICY_APPEAL` 테이블은 `CUSTOMER`에 대해 두 개의 FK를 가짐:

```
POLICY_APPEAL.requester_id   → CUSTOMER.id  (요청자 = 자녀, NOT NULL)
POLICY_APPEAL.resolved_by_id → CUSTOMER.id  (처리자 = 부모, NULL 허용)
```

---

## 5. 제약조건 요약

### 5.1 PRIMARY KEY

| 테이블 | PK | 전략 |
| --- | --- | --- |
| 전체 21개 테이블 | `id` (BIGINT) | AUTO_INCREMENT |

### 5.2 UNIQUE KEY

| 테이블 | UK 컬럼 | 목적 |
| --- | --- | --- |
| `customer` | (`phone_number`, `deleted_at`) | 로그인 ID 유일성 (삭제 후 재사용 허용) |
| `admin` | (`email`, `deleted_at`) | 로그인 ID 유일성 (삭제 후 재사용 허용) |
| `family_member` | (`family_id`, `customer_id`, `deleted_at`) | 중복 가입 방지 (삭제 후 재가입 허용) |
| `customer_quota` | (`customer_id`, `family_id`, `current_month`, `deleted_at`) | 월별 유일성 |
| `usage_record` | `event_id` | Idempotency (중복 Insert 방지, deleted_at 미포함) |
| `family_recap_monthly` | (`family_id`, `report_month`) | 가족당 월 1건 보장 |
| `family_recap_weekly` | (`family_id`, `week_start_date`) | 가족당 주 1건 보장 |

> **Soft Delete와 UNIQUE 제약**: MySQL에서 `deleted_at`이 NULL인 경우 UNIQUE 제약은 중복을 허용함. 따라서 활성 레코드(deleted_at=NULL)는 1건만 가능하고, 삭제된 레코드(deleted_at=timestamp)는 각각 다른 시각으로 구분됨.
>

### 5.3 FOREIGN KEY

| 테이블 | FK 컬럼 | 참조 | ON DELETE |
| --- | --- | --- | --- |
| `family` | `created_by_id` | `customer.id` | RESTRICT |
| `family_member` | `family_id` | `family.id` | CASCADE |
| `family_member` | `customer_id` | `customer.id` | CASCADE |
| `customer_quota` | `customer_id` | `customer.id` | CASCADE |
| `customer_quota` | `family_id` | `family.id` | CASCADE |
| `usage_record` | `customer_id` | `customer.id` | RESTRICT |
| `usage_record` | `family_id` | `family.id` | RESTRICT |
| `policy_assignment` | `policy_id` | `policy.id` | CASCADE |
| `policy_assignment` | `family_id` | `family.id` | CASCADE |
| `policy_assignment` | `target_customer_id` | `customer.id` | CASCADE |
| `policy_assignment` | `applied_by_id` | `customer.id` | RESTRICT |
| `notification_log` | `customer_id` | `customer.id` | CASCADE |
| `notification_log` | `family_id` | `family.id` | CASCADE |
| `audit_log` | `actor_id` | `customer.id` | SET NULL |
| `invite` | `family_id` | `family.id` | CASCADE |
| `mission_item` | `family_id` | `family.id` | CASCADE |
| `mission_item` | `created_by_id` | `customer.id` | RESTRICT |
| `mission_item` | `target_customer_id` | `customer.id` | RESTRICT |
| `mission_item` | `reward_id` | `reward.id` | RESTRICT |
| `reward` | `reward_template_id` | `reward_template.id` | RESTRICT |
| `mission_request` | `mission_item_id` | `mission_item.id` | CASCADE |
| `mission_request` | `requester_id` | `customer.id` | CASCADE |
| `mission_request` | `resolved_by_id` | `customer.id` | SET NULL |
| `family_recap_monthly` | `family_id` | `family.id` | CASCADE |
| `family_recap_weekly` | `family_id` | `family.id` | CASCADE |
| `policy_appeal` | `policy_assignment_id` | `policy_assignment.id` | SET NULL |
| `policy_appeal` | `requester_id` | `customer.id` | RESTRICT |
| `policy_appeal` | `resolved_by_id` | `customer.id` | SET NULL |
| `policy_appeal_comment` | `appeal_id` | `policy_appeal.id` | CASCADE |
| `policy_appeal_comment` | `author_id` | `customer.id` | CASCADE |
| `mission_log` | `mission_item_id` | `mission_item.id` | CASCADE |
| `mission_log` | `actor_id` | `customer.id` | SET NULL |
| `reward_grant` | `reward_id` | `reward.id` | RESTRICT |
| `reward_grant` | `customer_id` | `customer.id` | RESTRICT |
| `reward_grant` | `mission_item_id` | `mission_item.id` | RESTRICT |

### 5.4 ENUM 정의 요약

| 테이블 | 컬럼 | 값 |
| --- | --- | --- |
| `family_member` | `role` | `MEMBER`, `OWNER` |
| `policy` | `require_role` | `MEMBER`, `OWNER` |
| `policy` | `type` | `MONTHLY_LIMIT`, `TIME_BLOCK`, `APP_BLOCK`, `MANUAL_BLOCK` |
| `notification_log` | `type` | `THRESHOLD_ALERT`, `BLOCKED`, `UNBLOCKED`, `POLICY_CHANGED`, `MISSION_CREATED`, `REWARD_REQUESTED`, `REWARD_APPROVED`, `REWARD_REJECTED`, `APPEAL_CREATED`, `APPEAL_APPROVED`, `APPEAL_REJECTED`, `EMERGENCY_APPROVED` |
| `invite` | `role` | `MEMBER`, `OWNER` |
| `invite` | `status` | `PENDING`, `ACCEPTED`, `EXPIRED`, `CANCELLED` |
| `reward_template` | `category` | `DATA`, `GIFTICON` |
| `reward` | `category` | `DATA`, `GIFTICON` |
| `mission_item` | `status` | `ACTIVE`, `COMPLETED`, `CANCELLED` |
| `mission_request` | `status` | `PENDING`, `APPROVED`, `REJECTED` |
| `policy_appeal` | `type` | `NORMAL`, `EMERGENCY` |
| `policy_appeal` | `status` | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` |
| `mission_log` | `action_type` | `MISSION_CREATED`, `MISSION_REQUESTED`, `MISSION_COMPLETED`, `MISSION_CANCELLED` |
| `reward_grant` | `status` | `ISSUED`, `USED`, `EXPIRED` |

---

## 6. 인덱스 전략

### 6.1 인덱스 전체 목록

| 테이블 | 인덱스명 | 컬럼 | 용도 |
| --- | --- | --- | --- |
| `customer` | `idx_customer_phone` | `phone_number` | 로그인 조회 |
| `customer` | `idx_customer_email` | `email` | 이메일 조회 |
| `admin` | `idx_admin_email` | `email` | 로그인 조회 |
| `family` | `idx_family_created_by` | `created_by_id` | 생성자별 그룹 조회 |
| `family_member` | `idx_member_family` | `family_id` | 가족별 구성원 목록 |
| `family_member` | `idx_member_customer` | `customer_id` | 사용자의 가족 목록 |
| `customer_quota` | `idx_cquota_customer_month` | (`customer_id`, `current_month`) | 월별 한도 조회 |
| `customer_quota` | `idx_cquota_family` | `family_id` | 가족별 구성원 상태 |
| `usage_record` | `idx_usage_family_time` | (`family_id`, `event_time`) | 가족별 사용량 집계 |
| `usage_record` | `idx_usage_customer_time` | (`customer_id`, `event_time`) | 개인 사용량 조회 |
| `usage_record` | `idx_usage_event_id` | `event_id` | Idempotency 검증 |
| `policy_assignment` | `idx_pa_family` | `family_id` | 가족별 정책 조회 |
| `policy_assignment` | `idx_pa_target` | `target_customer_id` | 구성원별 정책 조회 |
| `notification_log` | `idx_notif_customer` | (`customer_id`, `sent_at` DESC) | 알림 목록 |
| `notification_log` | `idx_notif_customer_type` | (`customer_id`, `type`, `sent_at` DESC) | 타입별 알림 필터 |
| `notification_log` | `idx_notif_family` | (`family_id`, `sent_at` DESC) | 가족 알림 목록 |
| `audit_log` | `idx_audit_actor` | (`actor_id`, `created_at` DESC) | 수행자별 이력 |
| `audit_log` | `idx_audit_entity` | (`entity_type`, `entity_id`, `created_at` DESC) | 엔티티별 이력 |
| `audit_log` | `idx_audit_action` | (`action`, `created_at` DESC) | 액션별 이력 |
| `invite` | `idx_invite_phone` | (`phone_number`, `status`) | 전화번호별 초대 조회 |
| `invite` | `idx_invite_family` | (`family_id`, `status`) | 가족별 초대 목록 |
| `reward` | `idx_reward_template` | (`reward_template_id`) | 템플릿별 보상 조회 |
| `mission_item` | `idx_mission_family` | (`family_id`, `status`, `created_at` DESC) | 가족별 미션 목록 |
| `mission_item` | `idx_mission_creator` | (`created_by_id`) | 생성자별 미션 |
| `mission_item` | `idx_mission_target` | (`target_customer_id`, `status`, `created_at` DESC) | 대상 자녀별 미션 목록 |
| `mission_request` | `idx_mreq_mission` | (`mission_item_id`, `created_at` DESC) | 미션별 요청 이력 |
| `mission_request` | `idx_mreq_requester` | (`requester_id`, `created_at` DESC) | 요청자별 이력 |
| `family_recap_monthly` | `idx_recap_monthly_family_month` | (`family_id`, `report_month` DESC) | 가족별 월간 리캡 |
| `family_recap_weekly` | `idx_recap_weekly_family_week` | (`family_id`, `week_start_date` DESC) | 가족별 주간 리캡 조회 |
| `policy_appeal` | `idx_appeal_assignment` | `policy_assignment_id` | 정책 적용별 이의제기 조회 |
| `policy_appeal` | `idx_appeal_requester` | `requester_id` | 요청자별 이의제기 조회 |
| `policy_appeal` | `idx_appeal_emergency_monthly` | (`requester_id`, `type`, `status`, `created_at`) | 월별 긴급 요청 조회 최적화 |
| `policy_appeal` | `uk_appeal_emergency_month` | UNIQUE (`requester_id`, `emergency_grant_month`) | 월 1회 긴급 요청 동시성 안전 중복 방지 |
| `policy_appeal_comment` | `idx_appeal_comment_appeal` | (`appeal_id`, `created_at`) | 이의제기별 댓글 목록 |
| `policy_appeal_comment` | `idx_appeal_comment_author` | `author_id` | 작성자별 댓글 조회 |
| `mission_log` | `idx_mission_log_item` | (`mission_item_id`, `created_at`) | 미션별 타임라인 조회 |
| `mission_log` | `idx_mission_log_actor` | `actor_id` | 수행자별 로그 조회 |
| `reward_grant` | `idx_reward_grant_customer` | (`customer_id`) | 사용자별 지급 이력 조회 |
| `reward_grant` | `idx_reward_grant_status` | (`status`) | 상태별 지급 이력 조회 |
| `reward_grant` | `idx_reward_grant_expired` | (`expired_at`) | 만료 대상 조회 |
| `reward_grant` | `idx_reward_grant_status_created` | (`status`, `created_at` DESC) | 미사용+최신순 복합 쿼리 최적화 |

### 6.2 파티셔닝

**대상**: `usage_record` (고용량 테이블, ~432M rows/일)

```sql
PARTITION BY RANGE (YEAR(event_time) * 100 + MONTH(event_time)) (
    PARTITION p2025_01 VALUES LESS THAN (202502),
    PARTITION p2025_02 VALUES LESS THAN (202503),
    ...
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

**효과**: 월별 파티션 프루닝으로 시간 범위 쿼리 성능 최적화, 90일 이후 파티션 단위 아카이브(S3) 가능

---

## 7. 데이터 흐름과 ERD 매핑

### 7.1 Write Path (실시간 → 영속)

```mermaid
flowchart LR
    subgraph "실시간 (Redis)"
        R1["family:{id}:remaining_bytes"]
        R2["family:{id}:user:{uid}:used_bytes_month"]
        R3["family:{id}:user:{uid}:blocked"]
    end

    subgraph "비동기 (MySQL)"
        T1[usage_record]
        T2[customer_quota]
        T3[family]
    end

    R1 -.->|Write-Behind<br/>usage-persist 자기소비| T3
    R2 -.->|Write-Behind<br/>usage-persist 자기소비| T2
    R3 -.->|Write-Behind<br/>usage-persist 자기소비| T2

    style R1 fill:#ff6b6b,color:#fff
    style R2 fill:#ff6b6b,color:#fff
    style R3 fill:#ff6b6b,color:#fff
    style T1 fill:#4ecdc4,color:#fff
    style T2 fill:#4ecdc4,color:#fff
    style T3 fill:#4ecdc4,color:#fff
```

### 7.2 Read Path (조회 경로)

| API 엔드포인트 | 데이터 소스 | 관련 테이블 |
| --- | --- | --- |
| `GET /families/dashboard/usage` | Redis (실시간) → MySQL (Fallback) | `family`, `customer_quota` |
| `GET /customers/usage` | MySQL | `usage_record`, `customer_quota` |
| `GET /customers/policies` | MySQL | `policy`, `policy_assignment` |
| `GET /notifications` | MySQL | `notification_log` |
| `GET /notifications/alert` | MySQL | `notification_log` (type=THRESHOLD_ALERT 필터) |
| `GET /notifications/block` | MySQL | `notification_log` (type=BLOCKED 필터) |
| `GET /families/policies` | MySQL | `policy`, `policy_assignment` |
| `PATCH /families/policies` | MySQL | `policy_assignment` |
| `GET /policies` | MySQL | `policy` |
| `GET /families/reports/usage` | MySQL | `usage_record` (집계 쿼리) |
| `GET /missions` | MySQL | `mission_item`, `reward` |
| `GET /missions/logs` | MySQL | `mission_item`, `mission_log` |
| `GET /missions/requests` | MySQL | `mission_item`, `mission_request`, `reward` |
| `GET /rewards/templates` | MySQL | `reward_template` |
| `GET /recaps/monthly` | MySQL | `family_recap_monthly` |
| `GET /admin/audit/logs` | MySQL | `audit_log` |
| `GET /admin/rewards/templates` | MySQL | `reward_template` |
| `GET /admin/dashboard` | MySQL | 전체 테이블 (통계 집계) |
| `GET /admin/rewards/grants` | MySQL | `reward_grant`, `reward`, `mission_item`, `customer` |

---

## 관련 문서

- [기획서](./SPECIFICATION.md)
- [아키텍처 설계서](./ARCHITECTURE.md)
- [API 명세서](./API_SPECIFICATION.md)
- [데이터 모델](./DATA_MODEL.md)
- [용어집](./GLOSSARY.md)
