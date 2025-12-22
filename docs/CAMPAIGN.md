# Voucherify – Campaign & Voucher API Flow

Dieses Dokument beschreibt den vollständigen API-Flow mit **Voucherify**, um:

1. Nutzer (Customers) per API anzulegen  
2. Eine Campaign zu erstellen  
3. Pro Nutzer einen Voucher mit **30 Tagen Laufzeit ab Veröffentlichung** zu generieren  
4. Den Voucher-Code aus dem API-Response auszuspielen  

---

## 0) Gemeinsame Basis

### Base URL
```
https://{cluster}.voucherify.io
```

### Header (für alle Requests)
```
Authorization: Bearer <token>
Content-Type: application/json
X-App-Id: <app-id>
X-App-Token: <app-token>
```

> Hinweis: Die Campaign-Erstellung ist **asynchron**. Direkt nach dem Anlegen kann der Status noch `IN_PROGRESS` sein.

---

## 1) Customer (Nutzer) anlegen

**POST** `/v1/customers`

```bash
curl --request POST \
  --url https://{cluster}.voucherify.io/v1/customers \
  --header 'Authorization: Bearer <token>' \
  --header 'Content-Type: application/json' \
  --header 'X-App-Id: <app-id>' \
  --header 'X-App-Token: <app-token>' \
  --data '{
    "source_id": "user-123",
    "name": "Max Mustermann",
    "email": "max@example.com",
    "metadata": {
      "plan": "pro"
    }
  }'
```

**Wichtig**
- `source_id` sollte eure **eigene eindeutige User-ID** sein (DB / CRM).
- Customers können später eindeutig für Publications referenziert werden.

---

## 2) Campaign anlegen (Voucher-Pool)

**POST** `/v1/campaigns`

Beispiel: Discount-Campaign mit **AUTO_UPDATE** und **30 Tage Laufzeit ab Veröffentlichung**.

```bash
curl --request POST \
  --url https://{cluster}.voucherify.io/v1/campaigns \
  --header 'Authorization: Bearer <token>' \
  --header 'Content-Type: application/json' \
  --header 'X-App-Id: <app-id>' \
  --header 'X-App-Token: <app-token>' \
  --data '{
    "name": "Welcome-30D-2025-12",
    "campaign_type": "DISCOUNT_COUPONS",
    "type": "AUTO_UPDATE",
    "join_once": true,
    "start_date": "2025-12-22T00:00:00Z",
    "expiration_date": "2026-12-31T00:00:00Z",

    "vouchers_count": 1000,

    "voucher": {
      "type": "DISCOUNT_VOUCHER",
      "discount": {
        "type": "PERCENT",
        "percent_off": 10
      },
      "redemption": {
        "quantity": 1
      },
      "code_config": {
        "pattern": "WELCOME-#######"
      }
    },

    "activity_duration_after_publishing": "P30D"
  }'
```

### Zentrale Felder
- `type: "AUTO_UPDATE"` – ideal für fortlaufendes Generieren von Codes
- `join_once: true` – ein Customer bekommt immer denselben Code
- `activity_duration_after_publishing: "P30D"` – Voucher ist **30 Tage ab Publication** gültig
- `code_config.pattern` – definiert das Code-Format

---

## 3) Voucher pro Nutzer veröffentlichen (Publication)

**POST** `/v1/publications`

Dieser Request:
- weist einem Customer **einen Voucher aus der Campaign** zu
- liefert im Response direkt den **Voucher-Code**

```bash
curl --request POST \
  --url 'https://{cluster}.voucherify.io/v1/publications?join_once=true' \
  --header 'Authorization: Bearer <token>' \
  --header 'Content-Type: application/json' \
  --header 'X-App-Id: <app-id>' \
  --header 'X-App-Token: <app-token>' \
  --data '{
    "campaign": { "name": "Welcome-30D-2025-12" },
    "customer": {
      "source_id": "user-123",
      "name": "Max Mustermann",
      "email": "max@example.com"
    },
    "channel": "api",
    "metadata": {
      "reason": "welcome"
    }
  }'
```

### Response (relevant)
```json
{
  "voucher": {
    "code": "WELCOME-A1B2C3D"
  }
}
```

➡️ **`voucher.code` ist der Code, den ihr an den Nutzer ausspielt.**

---

## Typischer Gesamt-Flow

1. User registriert sich  
2. Backend erstellt / upsertet Customer  
3. Backend erstellt (einmalig) die Campaign  
4. Backend erstellt eine Publication pro User  
5. Voucher-Code wird im Frontend, per Mail oder Push angezeigt  

---

## Best Practices

- **Idempotenz:** `join_once=true` verhindert doppelte Codes pro User
- **Source IDs:** immer mit eigenen User-IDs arbeiten
- **Gültigkeit:** Laufzeit immer über `activity_duration_after_publishing` steuern
- **Monitoring:** Campaign-Status nach dem Anlegen prüfen (async!)

---

## Erweiterungen (optional)

- Personalisierte Voucher-Metadaten
- Unterschiedliche Campaigns je Plan / Segment
- Kombination mit Voucherify Validation & Redemption API

---

_Ende der Datei_
