# Exports

This document describes the export endpoints available under `/v1/exports`.

## Supported export types

Currently supported:
- `voucher`
- `redemption`
- `publication`
- `customer`
- `order`
- `points_expiration`
- `voucher_transactions`
- `product`
- `sku`

Unsupported types return `400`:
- `campaign_transactions`

## Create export

```
POST /v1/exports
```

Request body:
```
{
  "exported_object": "voucher",
  "parameters": {
    "fields": ["code", "voucher_type", "value", "discount_type"],
    "order": "-created_at",
    "filters": { "metadata.source": "web" }
  }
}
```

If `fields` is omitted, defaults are used per object type (see `docs/voucherify.json`).

## List exports

```
GET /v1/exports?limit=10&page=1&order=-created_at
```

Supported ordering: `created_at`, `-created_at`, `status`, `-status`.

## Get export

```
GET /v1/exports/{exportId}
```

## Download export

```
GET /v1/exports/{exportId}?token=...
```

Returns CSV content with the requested fields as headers.

## Notes / limitations

- Filtering supports `metadata.*` equality only.
- CSV generation is synchronous and happens on download.
