# Voucherengine Agent Guide

Purpose: implement a Voucherify-inspired voucher management system using the Kotlin/Spring stack in this repo.

## Architecture at a glance
- Stack: Kotlin 2.3 + Spring Boot 4 (Web MVC, Data JPA, Bean Validation) with virtual threads enabled; SpringDoc starter 3.0.0 for API docs.
- JSON: Jackson 3 (`tools.jackson.*`) only. `ToolsJacksonJsonFormatMapper` is wired via `application.yml` to keep JSONB columns working—avoid any `com.fasterxml.*` imports.
- Data: PostgreSQL in dev (`docker/docker-compose.yml`), H2 in tests; Liquibase migrations live under `src/main/resources/db/changelog/migrations` and are referenced from `db.changelog-master.yaml`.
- Domain: vouchers (discount/gift/loyalty + assets/metadata/redemption counters), customers, validation rules + assignments, campaigns, redemptions/rollbacks, categories.
- Async Jobs: SQS-based async operations via LocalStack. `AsyncJobListener` establishes transaction boundary (`@Transactional(REQUIRES_NEW)`) for each SQS message. Job records (`AsyncJob` entity under `persistence/model/async`) track status/progress in DB for client polling via `/v1/async-actions/{id}`. Commands (under `service/async/command`) are serialized to SQS using Jackson polymorphic serialization with `jobType` discriminator. Each command implements `execute(ApplicationContext)` and delegates to `VoucherAsyncService` handlers which run within the listener's transaction.
- References: Voucherify OpenAPI `docs/voucherify.json` plus the two flow guides `docs/PER-CUSTOMER-VOUCHER.md` (per-customer issuance) and `docs/MULTI-USE-VOUCHER.md` (multi-use with quantity/per-customer limits).

## Campaign-Voucher Operations
- Campaigns group vouchers with shared configuration (code patterns, discount rules, validity).
- POST `/v1/campaigns/{id}/vouchers` - Create vouchers in campaign (supports `vouchers_count` param for bulk)
- POST `/v1/campaigns/{id}/vouchers/{code}` - Add voucher with specific code to campaign
- POST `/v1/campaigns/{id}/import` - Async import of vouchers to campaign (returns async job ID)
- POST `/v1/campaigns/{id}/importCSV` - CSV import (stub, returns 501)
- GET `/v1/campaigns/{id}/vouchers` - List all vouchers in a campaign
- Session release: DELETE `/v1/vouchers/{code}/sessions/{sessionKey}` - Release validation session locks

## Working rules for agents
- Scope: implement Voucherify-compatible endpoints per `voucherify.json`; mirror Voucherify verbs/payloads and keep controllers/DTOs in sync with spec.
- Modeling: extend entities under `persistence/model/**`; preserve UUID PKs, audit fields, and JSONB usage. Any schema change must go through Liquibase migrations.
- Services/controllers: validate request DTOs (`var` properties for Bean Validation); use POST for create, PUT for update; enforce redemption limits (total and per-customer) and holder checks as documented.
- Testing: add Spring Boot + MockMvc tests for every endpoint and service branch; use the H2 profile. Repository tests should live per-entity (e.g., `VoucherRepositoryTest`).
- Operations: run builds/tests with `GRADLE_USER_HOME=.gradle-tmp ./gradlew test` to avoid local Gradle cache contention. No destructive SQL or migration rewrites.

## Security and tenancy
- All endpoints require a `tenant` header; it must match a JWT `tenants` claim entry unless the caller is a manager.
- JWTs are validated against Keycloak (`spring.security.oauth2.resourceserver.jwt.issuer-uri`).
- Use realm roles `ROLE_TENANT` and `ROLE_MANAGER` from `realm_access.roles`; manager implies tenant.
- `/v1/tenants/**` endpoints require role `MANAGER`; all other endpoints require role `TENANT`.
- Local Keycloak configuration lives in `docker/tofu/main.tf` with clients `acme` (ROLE_TENANT + tenants claim) and `manager` (ROLE_MANAGER only).

