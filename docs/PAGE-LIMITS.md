# Voucherify – Pagination & Sorting Support

This document summarizes which Voucherify list endpoints support **pagination** and **sorting**, including **default and maximum page sizes**. It reflects the practical behavior of the Voucherify API and is intended as an integration reference.

---

## Default & maximum page size (important)

- **Default page size (`limit`)**: **10**
- **Maximum page size (`limit`)**: **100** (hard limit)

If no pagination parameters are provided, Voucherify returns **10 items by default**.

Requests that exceed the maximum (e.g. `limit=500`) are either capped to 100 or rejected, depending on the endpoint.

---

## Pagination models used by Voucherify

### Page-based pagination
Used by:
- Customers
- Campaigns
- Vouchers

Parameters:
```
?limit=100&page=1
```

- `limit`: 1–100 (default 10)
- `page`: 1-based index

---

### Page-based pagination with page caps
Used by:
- Publications
- Redemptions

Parameters:
```
?limit=100&page=1
```

- `limit`: 1–100 (default 10)
- `page`: 1-based index
- Publications: **page > 1000** returns `page_over_limit`
- Redemptions: **page > 99** is invalid

---

## Summary table

| Resource | Pagination | Sorting | Notes |
|-------|------------|---------|------|
| Categories | No | No | Small, static configuration list |
| Customers | Yes | Limited | Page-based pagination |
| Campaigns | Yes | Limited | Page-based pagination |
| Vouchers (list) | Yes | Limited | Page-based pagination |
| Publications | Yes | No | Page-based, max page 1000 |
| Validation rules | No | No | Configuration objects |
| Redemptions | Yes | No | Page-based, max page 99 |

---

## Detailed breakdown

### Categories

**Endpoint**
```
/v1/categories
```

- Pagination: ❌ No  
- Sorting: ❌ No  

Categories are treated as configuration metadata and expected to be small.

---

### Customers

**Endpoint**
```
/v1/customers
```

- Pagination: ✅ Yes (`limit`, `page`)
- Sorting: ⚠️ Limited / implicit  

There is no explicit `sort_by` parameter. Ordering is backend-defined (typically newest first).

---

### Campaigns

**Endpoint**
```
/v1/campaigns
```

- Pagination: ✅ Yes
- Sorting: ⚠️ Limited  

Supports pagination and filtering (status, type, category). Sorting is not freely configurable.

---

### Vouchers (voucher list)

**Endpoint**
```
/v1/vouchers
```

- Pagination: ✅ Yes
- Sorting: ⚠️ Limited  

Extensive filtering is supported (campaign, status, customer, category). Client-side sorting is recommended for custom orderings.

---

### Publications

**Endpoint**
```
/v1/publications
```

- Pagination: ✅ Yes (`limit`, `page`, max page 1000)
- Sorting: ❌ No  

Order is fixed (typically newest to oldest).

---

### Validation Rules

**Endpoint**
```
/v1/validation-rules
```

- Pagination: ❌ No  
- Sorting: ❌ No  

Validation rules are treated as configuration and expected to be few in number.

---

### Redemptions

**Endpoint**
```
/v1/redemptions
```

- Pagination: ✅ Yes (`limit`, `page`, max page 99)
- Sorting: ❌ No  

Designed as an audit log with fixed ordering.

---

## Design principle

Voucherify favors **filtering over sorting**.

For advanced needs (multi-field sorting, custom ordering):
- Filter server-side as much as possible
- Sort client-side
- Or export data for reporting/BI tools

---

## TL;DR

- Default page size: **10**
- Maximum page size: **100**
- Pagination exists for: customers, campaigns, vouchers, publications, redemptions
- Sorting is very limited or not supported
- Categories and validation rules are not paginated
- Treat list endpoints as filtered streams, not queryable tables

---

_End of document_
