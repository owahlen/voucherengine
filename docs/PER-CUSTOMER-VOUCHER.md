# Per-customer Voucher Issuance in Voucherengine (API steps)

Below is an API-first flow to **issue one unique voucher code per known customer** (e.g., you already know their email addresses).  
This pattern is commonly used for **personalized vouchers** (one-time or limited-use per customer).

---

## 0) Prereqs: base URL + auth headers
All server-side calls use your private keys in headers (e.g. `X-App-Id`, `X-App-Token`).

---

## 1) Create (or upsert) the Customer records
If you already have customers in Voucherengine, you can skip this step. Otherwise, create them first.

```bash
curl -X POST "https://api.voucherengine.io/v1/customers" \
  -H "Content-Type: application/json" \
  -H "X-App-Id: YOUR_APP_ID" \
  -H "X-App-Token: YOUR_APP_TOKEN" \
  -d '{
    "source_id": "customer-123",
    "email": "alice@example.com",
    "name": "Alice Example"
  }'
```

**Notes**
- `source_id` should be your internal customer identifier (recommended for stable linking).
- You can store email in the customer object and also use it in metadata if needed.

---

## 2) Create a voucher code per customer and bind it to that customer
Create a voucher **per customer** and restrict redemption to that customer.

### 2a) One-time voucher per customer (common)
```bash
curl -X POST "https://api.voucherengine.io/v1/vouchers" \
  -H "Content-Type: application/json" \
  -H "X-App-Id: YOUR_APP_ID" \
  -H "X-App-Token: YOUR_APP_TOKEN" \
  -d '{
    "code": "ALICE-10OFF-2026",
    "type": "DISCOUNT_VOUCHER",
    "discount": {
      "type": "PERCENT",
      "percent_off": 10
    },
    "redemption": {
      "quantity": 1
    },
    "customer": {
      "source_id": "customer-123"
    },
    "metadata": {
      "issued_to_email": "alice@example.com"
    }
  }'
```

### 2b) Limited multi-use voucher per customer (optional)
If you want the same customer to be able to use their personal voucher multiple times:
```bash
curl -X POST "https://api.voucherengine.io/v1/vouchers" \
  -H "Content-Type: application/json" \
  -H "X-App-Id: YOUR_APP_ID" \
  -H "X-App-Token: YOUR_APP_TOKEN" \
  -d '{
    "code": "ALICE-5X-2026",
    "type": "DISCOUNT_VOUCHER",
    "discount": {
      "type": "AMOUNT",
      "amount_off": 500,
      "amount_off_type": "FIXED"
    },
    "redemption": {
      "quantity": 5
    },
    "customer": {
      "source_id": "customer-123"
    }
  }'
```

**What this does**
- Each code is **unique** and **bound to a specific customer**.
- `redemption.quantity` controls how many total redemptions that single customer may perform.

---

## 3) (Optional) Validate at checkout
If you want to check if the voucher is applicable for the customer/cart context before redeeming:

### Option A: Validate a single voucher code
- `POST /v1/vouchers/{code}/validate`

**Example**
```bash
curl -X POST "https://api.voucherengine.io/v1/vouchers/ALICE-10OFF-2026/validate" \
  -H "Content-Type: application/json" \
  -H "X-App-Id: YOUR_APP_ID" \
  -H "X-App-Token: YOUR_APP_TOKEN" \
  -d '{
    "customer": { "source_id": "customer-123" },
    "order": { "amount": 25900 }
  }'
```

### Option B: Validate “stackable discounts”
- `POST /v1/validations` (validates up to 30 redeemables at once)

---

## 4) Redeem when the order is placed/paid
Redeem the voucher when you commit the order payment (or when you create the order, depending on your business rules).

### Recommended: Redeem via Stackable Discounts
- `POST /v1/redemptions`

**Example**
```bash
curl -X POST "https://api.voucherengine.io/v1/redemptions" \
  -H "Content-Type: application/json" \
  -H "X-App-Id: YOUR_APP_ID" \
  -H "X-App-Token: YOUR_APP_TOKEN" \
  -d '{
    "redeemables": [
      { "object": "voucher", "id": "ALICE-10OFF-2026" }
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

---

# Minimal “necessary calls” summary

## Per-customer voucher issuance (typical)
1) `POST /v1/customers` (only if customer doesn’t exist yet)  
2) `POST /v1/vouchers` (one unique code per customer + bind to customer)  
3) `POST /v1/redemptions` (when used)

*(Optional)* validate:
- `POST /v1/vouchers/{code}/validate` or `POST /v1/validations`

---

## Practical batching note (when you have many emails)
For issuing vouchers to a whole list of known customers:
- Loop through your customer list:
  1) create/upsert customer (or skip if already present)
  2) create voucher with a unique code and `customer.source_id`

Store the mapping `{source_id/email -> code}` in your system so you can display/send the correct code to each customer.
