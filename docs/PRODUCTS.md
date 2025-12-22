# Voucherify – Products, SKUs, and Orders Storage Model

This document explains how **Products** and **SKUs** are stored in Voucherify and how they compare to **Orders**, including persistence, mutability, deletion behavior, and practical implications.

---

## Storage model overview

| Object | Persisted in DB | Purpose | Mutable | Typical lifetime |
|------|----------------|--------|---------|------------------|
| Product | Yes | Commercial catalog item | Limited | Long-lived |
| SKU | Yes | Concrete sellable variant | Limited | Long-lived |
| Order | Yes | Transaction record | Partially | Permanent / audit |
| Voucher | Yes | Incentive instrument | Limited | Campaign-bound |

---

## Products

**Endpoint**
```
/v1/products
```

### What they represent
- Logical catalog entities (e.g. “Premium Plan”, “T-Shirt”)
- Optional but strongly recommended when:
  - restricting vouchers to products
  - validating carts or orders

### Persistence
- Stored as durable database records
- Remain until explicitly deleted

### Mutability
- Name, metadata, attributes are mutable
- Historical references are preserved (non-destructive updates)

### Deletion
- Hard delete removes the product object
- Cascades to:
  - SKUs that belong to the product
- Does **not** delete:
  - orders
  - vouchers
  - redemptions

### Impact after deletion
- Voucher validation may fail if it references the deleted product
- Historical redemptions remain valid

---

## SKUs

**Endpoint**
```
/v1/skus
```

### What they represent
- Sellable variants of a product
- Examples:
  - Size or color
  - Billing cycle (monthly / yearly)
  - Plan tier

### Persistence
- Stored as durable database records
- Linked to a parent product

### Mutability
- Attributes and metadata are mutable
- SKU ID is immutable

### Deletion
- Hard delete removes the SKU
- Does **not** cascade to:
  - product
  - orders
  - vouchers

### Impact after deletion
- Vouchers restricted to that SKU will no longer validate
- Existing redemptions remain unchanged

---

## Orders (comparison point)

**Endpoint**
```
/v1/orders
```

### What they represent
- Completed or in-progress purchases
- Used for:
  - validation context
  - redemption context
  - reporting

### Persistence
- Strongly persistent
- Treated as audit records

### Mutability
- Limited (mostly metadata or status)
- Core transactional data should be append-only
- Order items are immutable snapshots with soft references to `product_id` / `sku_id`

### Response fields (high level)
- List responses return an envelope with `object`, `data_ref`, `orders`, and `total`
- Order responses include `items_discount_amount`, `total_discount_amount`, and `total_amount`
- Order items expose calculated values (`amount`, `discount_amount`, `subtotal_amount`) plus snapshots (`product`, `sku`)

### Deletion
- Supported but discouraged in production
- No cascade deletes

---

## Key differences at a glance

| Aspect | Product / SKU | Order |
|-----|--------------|------|
| Represents | Catalog | Transaction |
| Referenced by | Validation rules | Redemptions |
| Safe to delete | Yes (with caution) | Rarely |
| Affects validation | Yes | No |
| Audit relevance | Low | High |

---

## Mental model

- **Products / SKUs** → constraints  
- **Orders** → facts  
- **Vouchers** → stateful instruments  

Deleting a constraint may break future validations, but never rewrites historical facts.

---

## Practical recommendations

### Use Products / SKUs when
- Voucher applicability depends on cart contents
- You need catalog-level discount logic
- You want clean segmentation

### Be careful deleting Products / SKUs if
- Active campaigns reference them
- Validation rules depend on them

### Prefer disabling over deleting when
- Product is temporarily unavailable
- SKU lifecycle is seasonal

---

## TL;DR

- Products and SKUs are persisted in the database like Orders
- They are **not** audit records like Orders
- Deleting a product cascades to its SKUs; deleting a SKU does not cascade
- Deleting them can affect future voucher validation

---

## Product collections

**Endpoint**
```
/v1/product-collections
```

### What they represent
- Named sets of products and/or SKUs
- Used for rules and segmentation

### Types
- `STATIC`: explicit list of products/SKUs
- `AUTO_UPDATE`: stored filter definition (no automatic evaluation yet)

### Notes
- Collections are tenant-scoped
- Listing collection products returns a mix of product and SKU objects

---

## Pagination & sorting

List endpoints support:
- `limit`: page size (default 10)
- `page`: 1-based page index (default 1)
- `order`: sort field, prefix with `-` for descending

Applied to:
- `/v1/products`
- `/v1/orders`
- `/v1/product-collections`

---

_End of document_
