# Voucherengine

Voucherengine models a focused, API-first subset of Voucherify. It covers the voucher lifecycle (issue, validate, redeem) for two flows described in `docs/PER-CUSTOMER-VOUCHER.md` and `docs/MULTI-USE-VOUCHER.md`, using `docs/voucherify.json` as the contract reference.

## Stack and architecture
- Kotlin 2.3 + Spring Boot 4 (Web MVC, Data JPA, Bean Validation) with virtual threads enabled.
- Persistence: PostgreSQL in dev, H2 in tests; JSONB fields mapped through `ToolsJacksonJsonFormatMapper` using Jackson 3 (`tools.jackson.*`).
- Liquibase migrations live in `src/main/resources/db/changelog/migrations`.
- SpringDoc UI is provided by `springdoc-openapi-starter-webmvc-ui:3.0.0`.

## Capabilities
- DTOs mirror Voucherify payloads for vouchers (discount/gift/loyalty), redemptions, validation rules, and customers; requests are validated with `@Valid`.
- Controllers expose Voucherify-like verbs: voucher CRUD/validate/redeem, customer CRUD, validation-rule CRUD/assignment, and stack validations.
- Services enforce the documented behavior: redemption quantity/per-customer limits (e.g., multi-use 1000 cap), holder checks, and metadata preservation. Responses include voucher assets, redemption counters, and timestamps per the docs.
- All endpoints require a `tenant` header; tenants are managed via `/v1/tenants`.

## Tenant CRUD
- `POST /v1/tenants` with `{ "name": "acme" }` creates a tenant.
- `GET /v1/tenants` lists tenants, `GET /v1/tenants/{id}` fetches one.
- `PUT /v1/tenants/{id}` updates name, `DELETE /v1/tenants/{id}` removes it.
- All tenant endpoints require the `tenant` header (used to scope requests).

## Run locally
1. Start PostgreSQL: `docker compose -f docker/docker-compose.yml up -d`
2. Launch the app: `./gradlew bootRun`
3. Run tests (H2): `GRADLE_USER_HOME=.gradle-tmp ./gradlew test`

## Layout
- `src/main/kotlin`: app entrypoint, controllers under `api/controller`, DTOs under `api/dto`, services under `service`, persistence under `persistence/**`.
- `src/main/resources`: application config and Liquibase changelog.
- `docs`: upstream OpenAPI plus the targeted use-case guides.

## Contributing
- Keep parity with Voucherify naming and payloads; prefer DTOs over entities at the edge.
- Extend the schema via Liquibase migrations only.
- Stay on Jackson 3 (no `com.fasterxml.*` imports); update SpringDoc/Kotlin versions together.
- Add tests for every new endpoint and service branch; use the H2 profile for integration tests.
