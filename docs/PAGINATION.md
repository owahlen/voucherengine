# Voucherify – Pagination & Sorting Support

This document summarizes which Voucherify list endpoints support **pagination** and **sorting**, and to what extent. It reflects the practical behavior of the Voucherify API and is intended as an integration reference.

---

## Summary table

| Resource | Pagination | Sorting | Notes |
|-------|------------|---------|------|
| Categories | No | No | Small, static configuration list |
| Customers | Yes | Limited | Implicit order (usually creation date) |
| Campaigns | Yes | Limited | Filterable by status/type |
| Vouchers (list) | Yes | Limited | Filtering preferred over sorting |
| Publications | Yes | No | Cursor-based pagination |
| Validation rules | No | No | Configuration objects |
| Redemptions | Yes | No | Audit log, fixed order |

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

- Pagination: ✅ Yes (cursor-based)
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

- Pagination: ✅ Yes (cursor-based)
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

- Pagination exists for: customers, campaigns, vouchers, publications, redemptions
- Sorting is very limited or not supported
- Categories and validation rules are not paginated
- Treat list endpoints as filtered streams, not queryable tables

---

_End of document_
