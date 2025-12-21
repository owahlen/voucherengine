# Voucherengine Agent Guide

Purpose: keep agents aligned on a narrow Voucherify-inspired core (issue, validate, redeem vouchers)
using the existing Kotlin/Spring Boot stack.

## Architecture at a glance
- Stack: Kotlin 2.2 + Spring Boot 4 (Web MVC, Data JPA),
  Liquibase for schema, PostgreSQL dev DB (`docker/docker-compose.yml`),
  H2 for tests.
- Domain: campaigns, vouchers (discount/gift/loyalty),
  validation rules (with converters for string enums), customers,
  redemptions/rollbacks, categories. JSON-heavy fields are persisted as `jsonb`.
- Migrations: `src/main/resources/db/changelog/db.changelog-master.yaml`
  includes all changesets under `migrations/`.
  Add new DB changes via Liquibase, do not hand-edit tables.
- References: Voucherify OpenAPI `docs/voucherify.json`;
  use-case guides `docs/PER-CUSTOMER-VOUCHER.md` (per-customer issuance)
  and `docs/MULTI-USE-VOUCHER.md` (shared multi-use).

## Working rules for agents
- Scope first: optimize for the two documented flows
  (per-customer issuance, shared multi-use vouchers with optional per-customer limits).
  Avoid feature creep outside those tracks.
- Modeling: extend existing entities under `persistence/model/**` and keep UUID primary keys,
  audit fields, and JSONB usage consistent. Update Liquibase migrations alongside entity changes.
- Services/controllers: prefer clear REST endpoints aligned with Voucherify naming;
  use DTOs rather than exposing entities directly; validate inputs up-front.
- Testing: favor Spring Boot tests against H2; seed data via Liquibase or builders;
  assert both happy-path and limit-enforcement cases.
- Data safety: preserve existing migrations and IDs;
  no destructive SQL or schema rewrites.
