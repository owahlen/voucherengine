
# Voucherify Rule Semantics – Full Technical Specification

This document is a **comprehensive, implementation-grade specification** of Voucherify's rule semantics.
It is written so that a developer can:
- generate rules programmatically
- validate rules before sending them to Voucherify
- reason about rule correctness and edge cases

This document intentionally avoids UI wording and focuses on **formal behavior**.

---

## 1. Rule Engine Model

Voucherify rules are **pure boolean predicates** evaluated against a runtime context.

Formally:

    rule(context) -> boolean

Rules:
- are side-effect free
- do not mutate state
- are evaluated synchronously
- are deterministic

If the final rule expression evaluates to `false`, the incentive is **not applicable**.

---

## 2. Rule Set Structure

A rule set consists of:

```json
{
  "rules": {
    "1": { ... },
    "2": { ... }
  },
  "logic": "(1 and 2)"
}
```

### Components

| Field | Description |
|------|-------------|
| rules | Map of rule identifiers to rule definitions |
| logic | Boolean expression combining rule identifiers |

Rule identifiers:
- must be strings
- must be unique
- have no semantic meaning beyond reference

---

## 3. Boolean Logic Grammar

### Supported Operators
- `and`
- `or`

### Grammar

```
expression := term | expression and term | expression or term
term       := number | "(" expression ")"
```

### Characteristics
- No `not` operator
- No operator precedence (use parentheses)
- Left-to-right evaluation
- Short-circuit evaluation

---

## 4. Rule Definition

Each rule definition:

```json
{
  "name": "<rule.path>",
  "conditions": {
    "<operator>": <operand>
  }
}
```

### Constraints
- Exactly one operator per rule
- Operand type must match rule value type
- Invalid operator results in request rejection

---

## 5. Operator Semantics (Exhaustive)

### 5.1 Equality Operators

| Operator | Operand | Meaning |
|--------|---------|--------|
| `$eq` | scalar | value equals operand |
| `$ne` | scalar | value does not equal operand |

Scalar types:
- string
- number
- boolean

---

### 5.2 Numeric Comparison

| Operator | Operand | Meaning |
|--------|---------|--------|
| `$gt` | number | value > operand |
| `$gte` | number | value >= operand |
| `$lt` | number | value < operand |
| `$lte` | number | value <= operand |

Notes:
- Currency amounts are always in **minor units**
- Numeric comparison against non-numeric values evaluates to false

---

### 5.3 Set Membership

| Operator | Operand | Meaning |
|--------|---------|--------|
| `$is` | array | value is one of the listed values |
| `$is_not` | array | value is not in the listed values |

Used for:
- segments
- SKUs
- IDs
- enums

---

### 5.4 Array Containment

Assumes evaluated value is an array.

| Operator | Operand | Meaning |
|--------|---------|--------|
| `$contains` | scalar | array contains operand |
| `$contains_any` | array | array contains at least one element |
| `$contains_all` | array | array contains all elements |

---

### 5.5 Boolean Operators

| Operator | Meaning |
|--------|--------|
| `$true` | value is true |
| `$false` | value is false |

---

### 5.6 String Matching

- `$eq` is exact match
- `$contains` is substring match
- Case sensitivity: implementation-defined (assume case-sensitive)

---

## 6. Rule Namespaces

### 6.1 Customer Rules

| Rule name | Value type |
|---------|------------|
| customer.id | string |
| customer.email | string |
| customer.segment | string |
| customer.metadata.<key> | scalar |

Example:

```json
{
  "name": "customer.metadata.tier",
  "conditions": { "$eq": "gold" }
}
```

---

### 6.2 Order Rules

| Rule name | Value type |
|----------|-----------|
| order.amount | number |
| order.currency | string |
| order.items.count | number |
| order.metadata.<key> | scalar |

---

### 6.3 Order Item Rules

Order item rules are evaluated **existentially**:
- rule is true if **any item** satisfies the condition

| Rule name | Value |
|----------|------|
| order.items.sku | array[string] |
| order.items.quantity | number |
| order.items.price | number |
| order.items.metadata.<key> | array |

---

### 6.4 Voucher Rules

| Rule name | Value |
|----------|------|
| voucher.code | string |
| voucher.metadata.<key> | scalar |

---

### 6.5 Campaign Rules

| Rule name | Value |
|----------|------|
| campaign.id | string |
| campaign.metadata.<key> | scalar |

---

### 6.6 Redemption Rules

| Rule name | Value |
|----------|------|
| redemptions.count.total | number |
| redemptions.count.per_customer | number |
| redemptions.metadata.<key> | scalar |

---

## 7. Metadata Evaluation Rules

- Missing metadata key → rule evaluates to false
- Type mismatch → rule evaluates to false
- Metadata keys are case-sensitive
- Metadata is not coerced

---

## 8. Error Handling

| Scenario | Result |
|-------|--------|
| Invalid operator | Request rejected |
| Invalid logic expression | Request rejected |
| Missing value | Rule false |
| Type mismatch | Rule false |

---

## 9. Rules vs Limits

Rules:
- boolean
- stateless

Limits:
- stateful
- enforced outside rule engine

Do NOT reimplement limits using rules.

---

## 10. Programmatic Best Practices

- Always normalize money to integers
- Generate deterministic rule IDs
- Validate operand types client-side
- Prefer segments over large lists
- Keep logic expressions simple

---

## 11. Full Example

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
      "conditions": { "$contains_any": ["SKU_A", "SKU_B"] }
    },
    "4": {
      "name": "customer.metadata.country",
      "conditions": { "$eq": "DE" }
    }
  },
  "logic": "(1 and 2) and (3 or 4)"
}
```

---

## 12. Explicit Non-Features

- No negation operator
- No arithmetic expressions
- No cross-rule value comparison
- No temporal math

---

## 13. Guarantees

- Deterministic evaluation
- No side effects
- Stable operator semantics
- Portable across campaigns

---

End of specification.
