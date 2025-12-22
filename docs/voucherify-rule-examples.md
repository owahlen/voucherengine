
# Voucherify Rule Semantics – Examples for All Rules

This document provides **concrete, copy‑pasteable JSON examples** for **every known Voucherify rule type**.
Each example is minimal, valid, and suitable for use in:
- qualification APIs
- campaign validation rules
- promotion eligibility logic
- automated rule generation tests

All amounts are in **minor units** (e.g. cents).

---

## 1. Customer Rules

### 1.1 Customer ID

```json
{
  "name": "customer.id",
  "conditions": { "$is": ["cust_001", "cust_002"] }
}
```

---

### 1.2 Customer Email

```json
{
  "name": "customer.email",
  "conditions": { "$contains": "@example.com" }
}
```

---

### 1.3 Customer Segment

```json
{
  "name": "customer.segment",
  "conditions": { "$is": ["VIP", "LOYAL"] }
}
```

---

### 1.4 Customer Metadata (String)

```json
{
  "name": "customer.metadata.tier",
  "conditions": { "$eq": "gold" }
}
```

---

### 1.5 Customer Metadata (Number)

```json
{
  "name": "customer.metadata.age",
  "conditions": { "$gte": 18 }
}
```

---

## 2. Order Rules

### 2.1 Order Amount (Minimum Spend)

```json
{
  "name": "order.amount",
  "conditions": { "$gte": 5000 }
}
```

---

### 2.2 Order Currency

```json
{
  "name": "order.currency",
  "conditions": { "$eq": "EUR" }
}
```

---

### 2.3 Order Item Count

```json
{
  "name": "order.items.count",
  "conditions": { "$gte": 3 }
}
```

---

### 2.4 Order Metadata

```json
{
  "name": "order.metadata.channel",
  "conditions": { "$eq": "mobile" }
}
```

---

## 3. Order Item Rules

### 3.1 SKU Inclusion (Any Item)

```json
{
  "name": "order.items.sku",
  "conditions": { "$contains_any": ["SKU_A", "SKU_B"] }
}
```

---

### 3.2 SKU Exclusion

```json
{
  "name": "order.items.sku",
  "conditions": { "$contains_all": ["SKU_PROMO"] }
}
```

---

### 3.3 Item Quantity

```json
{
  "name": "order.items.quantity",
  "conditions": { "$gte": 2 }
}
```

---

### 3.4 Item Price

```json
{
  "name": "order.items.price",
  "conditions": { "$gte": 1000 }
}
```

---

### 3.5 Item Metadata

```json
{
  "name": "order.items.metadata.category",
  "conditions": { "$is": ["electronics"] }
}
```

---

## 4. Voucher Rules

### 4.1 Voucher Code

```json
{
  "name": "voucher.code",
  "conditions": { "$eq": "WELCOME10" }
}
```

---

### 4.2 Voucher Metadata

```json
{
  "name": "voucher.metadata.source",
  "conditions": { "$eq": "referral" }
}
```

---

## 5. Campaign Rules

### 5.1 Campaign ID

```json
{
  "name": "campaign.id",
  "conditions": { "$eq": "camp_summer_2025" }
}
```

---

### 5.2 Campaign Metadata

```json
{
  "name": "campaign.metadata.region",
  "conditions": { "$is": ["EU"] }
}
```

---

## 6. Redemption & Usage Rules

### 6.1 Total Redemptions

```json
{
  "name": "redemptions.count.total",
  "conditions": { "$lt": 1000 }
}
```

---

### 6.2 Redemptions per Customer

```json
{
  "name": "redemptions.count.per_customer",
  "conditions": { "$lt": 3 }
}
```

---

### 6.3 Redemption Metadata

```json
{
  "name": "redemptions.metadata.device",
  "conditions": { "$eq": "ios" }
}
```

---

## 7. Boolean Rules

### 7.1 Boolean True

```json
{
  "name": "customer.metadata.marketing_opt_in",
  "conditions": { "$true": true }
}
```

---

### 7.2 Boolean False

```json
{
  "name": "order.metadata.is_test",
  "conditions": { "$false": false }
}
```

---

## 8. Combined Rule Set Example

```json
{
  "rules": {
    "1": {
      "name": "customer.segment",
      "conditions": { "$is": ["VIP"] }
    },
    "2": {
      "name": "order.amount",
      "conditions": { "$gte": 10000 }
    },
    "3": {
      "name": "order.items.sku",
      "conditions": { "$contains_any": ["SKU_A"] }
    }
  },
  "logic": "1 and 2 and 3"
}
```

---

## 9. Notes for Implementers

- Missing values cause rule failure
- Type mismatches cause rule failure
- No negation operator exists
- Logic expressions must be valid

---

End of examples.
