# Voucherify Events -- Data Model & Deletion Semantics

## Purpose

This document explains how events work in Voucherify, what data they
store about related objects (customers, campaigns, vouchers,
promotions), and what happens to events when those objects are deleted.

------------------------------------------------------------------------

## Core Principles

### Events are immutable

-   Events are append-only historical records.
-   Once created, an event is never modified.
-   Events are not automatically deleted when referenced objects are
    deleted.

### Events are denormalized

-   Events store **snapshots** of selected data at the time the event
    occurred.
-   Events do **not** rely on live joins to customers, campaigns,
    vouchers, or promotions.

------------------------------------------------------------------------

## Deletion Semantics

Deleting a referenced object **does not delete events** that reference
it.

  Deleted Object   Event Deleted?   Event Still Contains Data?
  ---------------- ---------------- ----------------------------
  Customer         No               Yes
  Campaign         No               Yes
  Voucher          No               Yes
  Promotion        No               Yes

Events may reference objects that no longer exist. This is expected and
supported behavior.

------------------------------------------------------------------------

## What Data Is Stored in an Event

Events store: - IDs for traceability - Selected business-critical
attributes - Context needed to understand what happened

They do **not** store full object definitions.

------------------------------------------------------------------------

## Customer Data in Events

Typical fields: - `customer.id` - `customer.email` (if present) -
`customer.source_id` (if present)

Example:

``` json
"customer": {
  "id": "cust_123",
  "email": "user@example.com",
  "source_id": "crm-789"
}
```

If the customer is deleted later: - The event remains unchanged - The
embedded snapshot is still available - The Customer API will return 404

------------------------------------------------------------------------

## Campaign Data in Events

Typical fields: - `campaign.id` - `campaign.name` - `campaign.type`

Example:

``` json
"campaign": {
  "id": "camp_456",
  "name": "SUMMER_SALE",
  "type": "VOUCHER"
}
```

Campaign configuration and rules are not stored.

------------------------------------------------------------------------

## Voucher Data in Events

Typical fields: - `voucher.id` - `voucher.code` - Discount snapshot -
Redemption metadata

Example:

``` json
"voucher": {
  "id": "vch_789",
  "code": "SAVE20",
  "discount": {
    "type": "PERCENT",
    "amount": 20
  }
}
```

Even if the voucher or campaign is deleted, the event still describes
what was redeemed.

------------------------------------------------------------------------

## Promotion Data in Events

Typical fields: - `promotion.id` - `promotion.name` - Applied effects -
Order context

Example:

``` json
"promotion": {
  "id": "promo_123",
  "name": "10% OFF CART",
  "effects": [
    {
      "type": "DISCOUNT",
      "amount": 10,
      "unit": "PERCENT"
    }
  ]
}
```

Promotion rule trees and conditions are not stored.

------------------------------------------------------------------------

## What Events Do NOT Store

Events generally do not store: - Full customer attribute sets - Full
campaign configurations - Full promotion rule logic - Live references to
current object state

------------------------------------------------------------------------

## Mental Model

Think of an event as:

> "At time T, an entity with these attributes caused this outcome."

Not as: \> "A pointer to the current state of a customer or campaign."

------------------------------------------------------------------------

## Implications for Consumers

When consuming events: - Do not assume referenced objects still exist -
Do not re-fetch objects to reconstruct history - Treat the event payload
as the source of truth

------------------------------------------------------------------------

## Compliance & Privacy

-   Object deletion does not remove historical events.
-   GDPR workflows may anonymize fields inside events.
-   Events may remain for audit and analytics purposes.

------------------------------------------------------------------------

## Summary

-   Events are immutable, denormalized snapshots.
-   Deleting customers, campaigns, vouchers, or promotions does not
    delete events.
-   Events remain meaningful even when referenced objects are gone.
