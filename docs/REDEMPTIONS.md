# Redemptions

This document describes stackable redemptions via `POST /v1/redemptions`.

## Endpoint

```
POST /v1/redemptions
```

## Request

Uses the same base payload as validations:

- `redeemables` (required)
- `customer`
- `order`
- `tracking_id`
- `metadata`
- `session`

## Response

Returns:

- `redemptions` (per-applicable redeemable)
- `order` (calculated totals)
- `inapplicable_redeemables`
- `skipped_redeemables`

## Notes / limitations

- Only voucher-based redeemables are supported.
- `promotion_tier` and `promotion_stack` are returned as `SKIPPED` with `promotion_not_supported` during validation.
- If `redeemables_application_mode` is `ALL` and any redeemable is inapplicable/skipped, no redemptions are created.
