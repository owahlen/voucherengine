# Validations

This document describes stackable validations via `POST /v1/validations`.

## Endpoint

```
POST /v1/validations
```

## Request

Supports the Stackable Validate payload:

- `redeemables` (required, 1..30, unique by object+id)
- `customer`
- `order`
- `tracking_id`
- `metadata` (used by `redemptions.metadata.<key>` rules)
- `session` (LOCK only; returns a generated `key` if missing)
- `options.expand`: `order`, `redeemable`, `category`, `validation_rules`

## Response

The response follows the Voucherify schema:

- `valid` flag
- `redeemables` with `APPLICABLE`, `INAPPLICABLE`, `SKIPPED`
- `skipped_redeemables`, `inapplicable_redeemables`
- `order` (calculated totals)
- `tracking_id` (hashed)
- `session`
- `stacking_rules`

## Stacking rules (current defaults)

- `redeemables_limit`: 30
- `applicable_redeemables_limit`: 5
- `applicable_redeemables_per_category_limit`: 1
- `redeemables_application_mode`: `ALL`
- `redeemables_sorting_rule`: `REQUESTED_ORDER`
- `exclusive_categories` / `joint_categories`: supported when configured (exclusive blocks non-exclusive unless joint).

## Configuration

Stacking rules are configurable via `application.yml`:

```
voucherengine:
  stacking-rules:
    redeemables-application-mode: PARTIAL
    redeemables-sorting-rule: CATEGORY_HIERARCHY
```

## Notes / limitations

- Only voucher-based redeemables are supported (`voucher`, `gift_card`, `loyalty_card`).
- `promotion_tier` and `promotion_stack` are returned as `SKIPPED` with `promotion_not_supported`.
- Item-level application details are placeholders (empty list).
- Validation sessions are persisted in the database; sending the same `session.key` overwrites existing locks for that tenant.
- When more than one redeemable shares a category, extra redeemables are `SKIPPED` with `applicable_redeemables_per_category_limit_exceeded`.
