# Qualifications

This endpoint returns a list of redeemables that are applicable to the provided customer and order context.

## Endpoint

```
POST /v1/qualifications
```

## Supported scenarios

- `ALL` (default)
- `CUSTOMER_WALLET`
- `AUDIENCE_ONLY` (order rules are ignored; only customer-based rules are evaluated)
- `PRODUCTS_BY_CUSTOMER`
- `PRODUCTS_DISCOUNT_BY_CUSTOMER`

Other scenarios are accepted but currently behave like `ALL`.

## Pagination

Qualifications uses cursor-based paging via `options.starting_after` and `options.limit`:

- Default `limit`: 5
- Maximum `limit`: 50
- `starting_after`: uses the `created_at` timestamp of the last returned redeemable

Response includes:

- `redeemables.has_more`
- `redeemables.more_starting_after`

## Filters

Supported `options.filters`:

- `category_id`
- `campaign_id`
- `campaign_type`
- `voucher_type`
- `code`
- `resource_id`
- `resource_type` (only `voucher` is supported)

## Expand

Supported `options.expand` values:

- `redeemable` adds name/campaign/metadata
- `category` adds category details
- `validation_rules` adds `validation_rules_assignments` list metadata

## Notes

- Qualification validation uses the same rules as voucher validation, but **per-customer redemption limits are ignored**, matching the Voucherify spec.
- `metadata` in the request is used for rules that reference `redemptions.metadata.<key>`.
- When `session.type` is `LOCK`, gift card results include `locked_credits` (returns `0` when there is no active lock).
- Only voucher redeemables are returned at the moment.
