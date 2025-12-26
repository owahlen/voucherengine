# Voucherengine

Voucherengine models a focused, API-first subset of Voucherify. It covers the voucher lifecycle (issue, validate, redeem) for two flows described in `docs/PER-CUSTOMER-VOUCHER.md` and `docs/MULTI-USE-VOUCHER.md`, using `docs/voucherify.json` as the contract reference.

Keycloak Admin Console:\
[http://localhost:8180](http://localhost:8180)

API Documentation:\
[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

Postgres Database:\
[jdbc:postgresql://voucherengine:voucherengine@localhost:5432/voucherengine](jdbc:postgresql://voucherengine:voucherengine@localhost:5432/voucherengine)

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
- Publications assign vouchers to customers via `/v1/publications` (see `docs/PUBLICATIONS.md`).

## Security and Keycloak
- JWTs are validated by Spring against the configured Keycloak realm (`spring.security.oauth2.resourceserver.jwt.issuer-uri`).
- The JWT must carry a `tenants` claim (array of strings). The `tenant` header must match one of these entries, except for manager tokens (see below).
- Roles are read from `realm_access.roles`. Use realm roles `ROLE_TENANT` and `ROLE_MANAGER`.
- Access rules: any endpoint requires role `TENANT`; `/v1/tenants/**` requires role `MANAGER` (manager implies tenant).
- Manager tokens may omit the `tenants` claim but must still send the `tenant` header for request scoping.
- Local Keycloak configuration lives in `docker/tofu/main.tf` with clients:
  - `acme`: role `ROLE_TENANT` plus `tenants` claim `["acme"]`.
  - `manager`: role `ROLE_MANAGER` only (no `tenants` claim).

## Tenant CRUD
- `POST /v1/tenants` with `{ "name": "acme" }` creates a tenant.
- `GET /v1/tenants` lists tenants, `GET /v1/tenants/{id}` fetches one.
- `PUT /v1/tenants/{id}` updates name, `DELETE /v1/tenants/{id}` removes it.
- All tenant endpoints require the `tenant` header (used to scope requests).
- Access requires role `MANAGER`; other endpoints require role `TENANT` (manager implies tenant).

## Run locally
1. Start PostgreSQL: `docker compose -f docker/docker-compose.yml up -d`
2. Launch the app: `./gradlew bootRun`
3. Run tests (H2): `GRADLE_USER_HOME=.gradle-tmp ./gradlew test`

## IntelliJ HTTP Client (Docker)
Use the JetBrains HTTP Client CLI container to run collections without IntelliJ:
```
docker run --rm --network host \
  -v "$PWD/http:/work" -w /work \
  jetbrains/intellij-http-client \
  -e development -v http-client.env.json \
  multi-use-voucher-flow.http
```
If host networking is unavailable, add `-D` to rewrite `localhost` to `host.docker.internal`.

## Layout
- `src/main/kotlin`: app entrypoint, controllers under `api/controller`, DTOs under `api/dto`, services under `service`, persistence under `persistence/**`.
- `src/main/resources`: application config and Liquibase changelog.
- `docs`: upstream OpenAPI plus the targeted use-case guides.

## Contributing
- Keep parity with Voucherify naming and payloads; prefer DTOs over entities at the edge.
- Extend the schema via Liquibase migrations only.
- Stay on Jackson 3 (no `com.fasterxml.*` imports); update SpringDoc/Kotlin versions together.
- Add tests for every new endpoint and service branch; use the H2 profile for integration tests.
