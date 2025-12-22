# Voucherengine Agent Guide

Purpose: keep agents aligned on a narrow Voucherify-inspired core (issue, validate, redeem vouchers) using the Kotlin/Spring stack in this repo.

## Architecture at a glance
- Stack: Kotlin 2.3 + Spring Boot 4 (Web MVC, Data JPA, Bean Validation) with virtual threads enabled; SpringDoc starter 3.0.0 for API docs.
- JSON: Jackson 3 (`tools.jackson.*`) only. `ToolsJacksonJsonFormatMapper` is wired via `application.yml` to keep JSONB columns working—avoid any `com.fasterxml.*` imports.
- Data: PostgreSQL in dev (`docker/docker-compose.yml`), H2 in tests; Liquibase migrations live under `src/main/resources/db/changelog/migrations` and are referenced from `db.changelog-master.yaml`.
- Domain: vouchers (discount/gift/loyalty + assets/metadata/redemption counters), customers, validation rules + assignments, campaigns, redemptions/rollbacks, categories.
- References: Voucherify OpenAPI `docs/voucherify.json` plus the two flow guides `docs/PER-CUSTOMER-VOUCHER.md` (per-customer issuance) and `docs/MULTI-USE-VOUCHER.md` (multi-use with quantity/per-customer limits).

## Working rules for agents
- Scope: stay within the two documented flows; mirror Voucherify verbs/payloads and keep controllers/DTOs in sync with `voucherify.json`.
- Modeling: extend entities under `persistence/model/**`; preserve UUID PKs, audit fields, and JSONB usage. Any schema change must go through Liquibase migrations.
- Services/controllers: validate request DTOs (`var` properties for Bean Validation); use POST for create, PUT for update; enforce redemption limits (total and per-customer) and holder checks as documented.
- Testing: add Spring Boot + MockMvc tests for every endpoint and service branch; use the H2 profile. Repository tests should live per-entity (e.g., `VoucherRepositoryTest`).
- Operations: run builds/tests with `GRADLE_USER_HOME=.gradle-tmp ./gradlew test` to avoid local Gradle cache contention. No destructive SQL or migration rewrites.
