# Voucherengine

Voucherengine models a small, API-first subset of Voucherify. 
It focuses on the core voucher lifecycle (issue, validate, redeem) needed for
common discount campaigns.

## Scope
- Spring Boot 4 + Kotlin 2.2, Web MVC, and JPA (PostgreSQL in dev, H2 in tests).
- Persistence layer for campaigns, vouchers, validation rules, customers, and redemptions.
- Liquibase schema in `src/main/resources/db/changelog`.
- Upstream reference docs: `docs/voucherify.json` (Voucherify OpenAPI), `docs/PER-CUSTOMER-VOUCHER.md`, and `docs/MULTI-USE-VOUCHER.md`.

## Status
- Domain model and database schema exist; service and 
- controller layers are intentionally minimal/not implemented yet.
- Two exemplar flows to build next:
  - Per-customer issuance: create/upsert customer → issue a unique voucher
    tied to that customer → redeem (`docs/PER-CUSTOMER-VOUCHER.md`).
  - Multi-use voucher: create a shared code with redemption quantity
    and optional per-customer limits → validate/redeem (`docs/MULTI-USE-VOUCHER.md`).

## Run locally
1) Start PostgreSQL (matching `application.yml`):
```bash
docker compose -f docker/docker-compose.yml up -d
```
2) Launch the app (Liquibase runs on startup):
```bash
./gradlew bootRun
```
3) Run tests (uses in-memory H2):
```bash
./gradlew test
```

## Project layout
- `src/main/kotlin`: Domain entities under `persistence/model/**` plus Spring Boot entrypoint.
- `src/main/resources`: App config and Liquibase changelogs.
- `docs/`: Voucherify OpenAPI spec and the two targeted use-case walkthroughs.

## Contributing
- Keep the surface area tight to the two documented flows while mirroring Voucherify naming where practical.
- Prefer evolving the existing schema via Liquibase migrations and add Kotlin tests around new behavior (H2 profile).
