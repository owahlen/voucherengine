# Multi-use Voucher in Voucherengine (API steps)

Below is a practical, API-first flow to create **one voucher code that can be used by multiple customers**, plus (optionally) a **per-customer limit**.

## 0) Prereqs: base URL + auth headers
All server-side calls use your private keys in headers (e.g. `X-App-Id`, `X-App-Token`).

---

## 1) Create a multi-use voucher (single shared code)
Use **Create Voucher** and set the voucher’s redemption quantity to something > 1 (or unlimited).

```bash
curl -X POST "https://api.voucherengine.io/v1/vouchers" \
  -H "Content-Type: application/json" \
  -H "X-App-Id: YOUR_APP_ID" \
  -H "X-App-Token: YOUR_APP_TOKEN" \
  -d '{
    "code": "SUMMER2026",
    "type": "DISCOUNT_VOUCHER",
    "discount": {
      "type": "PERCENT",
      "percent_off": 10
    },
    "redemption": {
      "quantity": 1000
    }
  }'
```

Example response:
```bash
{
  "id": "v_p9s8JkLm2X",
  "object": "voucher",
  "code": "SUMMER2026",
  "type": "DISCOUNT_VOUCHER",
  "status": "ACTIVE",

  "discount": {
    "type": "PERCENT",
    "percent_off": 10
  },

  "redemption": {
    "quantity": 1000,
    "redeemed_quantity": 0
  },

  "start_date": null,
  "expiration_date": null,

  "campaign": null,
  "customer": null,

  "metadata": {},

  "created_at": "2025-01-12T09:12:45.382Z",
  "updated_at": "2025-01-12T09:12:45.382Z",

  "assets": {
    "qr": {
      "id": "qr_JH82ks91",
      "url": "https://assets.voucherengine.io/qr/SUMMER2026.png"
    },
    "barcode": {
      "id": "bc_92ks82K",
      "url": "https://assets.voucherengine.io/barcode/SUMMER2026.png"
    }
  }
}
```
**What this does:** one code (`SUMMER2026`) can be redeemed up to **1000 times total**.
Voucherengine exposes this as `redemption.quantity` (null = unlimited).

---

## 2) Optional (recommended): limit to 1 redemption per customer
If you want “many users can use it, **but each user only once**”, you do that with **Validation Rules / Campaign Limits** (e.g. “Redemptions per customer per incentive”).

### 2a) Create a validation rule
```bash
curl -X POST "https://api.voucherengine.io/v1/validation-rules" \
  -H "Content-Type: application/json" \
  -H "X-App-Id: YOUR_APP_ID" \
  -H "X-App-Token: YOUR_APP_TOKEN" \
  -d '{
    "name": "One redemption per customer",
    "type": "redemptions",
    "conditions": {
      "redemptions": {
        "per_customer": 1,
        "per_incentive": true
      }
    }
  }'
```

Example response:
```bash
{
  "id": "val_0f8a1c2b",
  "object": "validation_rule",
  "name": "One redemption per customer",
  "type": "redemptions",
  "conditions": {
    "redemptions": {
      "per_customer": 1,
      "per_incentive": true
    }
  },
  "created_at": "2025-01-12T09:31:00Z"
}
```

### 2b) Assign the validation rule to the voucher (or campaign)
```bash
curl -X POST "https://api.voucherengine.io/v1/validation-rules/val_0f8a1c2b/assignments" \
  -H "Content-Type: application/json" \
  -H "X-App-Id: YOUR_APP_ID" \
  -H "X-App-Token: YOUR_APP_TOKEN" \
  -d '{
    "object": "voucher",
    "id": "SUMMER2026"
  }'

```
This endpoint can assign rules to a **voucher** or **campaign**, among others.

---

## 3) Validate at checkout (optional but common)
To check if the code is applicable for a customer/cart context:

### Option A: Validate a single voucher code
```bash
curl -X POST "https://api.voucherengine.io/v1/vouchers/SUMMER2026/validate" \
  -H "Content-Type: application/json" \
  -H "X-App-Id: YOUR_APP_ID" \
  -H "X-App-Token: YOUR_APP_TOKEN" \
  -d '{
    "customer": {
      "source_id": "customer-123",
      "email": "alice@example.com"
    },
    "order": {
      "id": "order-preview-987",
      "amount": 25900,
      "currency": "EUR",
      "items": [
        {
          "product_id": "prod_1",
          "quantity": 1,
          "price": 25900
        }
      ]
    }
  }'
```
Example response:
```json
{
  "valid": true,
  "voucher": {
    "id": "v_p9s8JkLm2X",
    "code": "SUMMER2026",
    "type": "DISCOUNT_VOUCHER",
    "status": "ACTIVE",

    "discount": {
      "type": "PERCENT",
      "percent_off": 10
    },

    "redemption": {
      "quantity": 1000,
      "redeemed_quantity": 42
    }
  },

  "discount": {
    "type": "PERCENT",
    "percent_off": 10,
    "amount_off": 2590
  },

  "order": {
    "amount": 25900,
    "discount_amount": 2590,
    "total_amount": 23310
  }
}
```

Key fields:

| Field                                | Meaning                     |
|--------------------------------------|-----------------------------|
| valid: true                          | 	Voucher is applicable      |
| discount.amount_off                  | 	Calculated discount        |
| order.total_amount                   | 	Final price after discount | 
| voucher.redemption.redeemed_quantity | 	For analytics/UI           |           

Example failed response (HTTP 400)
Case: customer already redeemed the voucher
```json
{
  "valid": false,
  "error": {
    "code": "redemption_limit_per_customer_exceeded",
    "message": "This voucher can be redeemed only once per customer."
  }
}
```

Case: voucher expired
```json
{
  "valid": false,
  "error": {
    "code": "voucher_expired",
    "message": "This voucher has expired."
  }
}
```

Case voucher not found
```json
{
  "valid": false,
  "error": {
    "code": "voucher_not_found",
    "message": "Voucher does not exist."
  }
}
```

---

## 4) Redeem when the order is placed/paid
### Recommended: Redeem via Stackable Discounts
- `POST /v1/redemptions`

This increments redemption counters and records redemption history.

**Example (redeem one voucher)**
```bash
curl -X POST "https://api.voucherengine.io/v1/redemptions" \
  -H "Content-Type: application/json" \
  -H "X-App-Id: YOUR_APP_ID" \
  -H "X-App-Token: YOUR_APP_TOKEN" \
  -d '{
    "redeemables": [
      { "object": "voucher", "id": "SUMMER2026" }
    ],
    "customer": { "source_id": "customer-123" },
    "order": {
      "id": "order-987",
      "amount": 25900,
      "items": [
        { "product_id": "prod_1", "quantity": 1, "price": 25900 }
      ]
    }
  }'
```
