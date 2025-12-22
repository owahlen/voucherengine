# Publications

Publications assign vouchers to customers, typically by drawing a voucher from a campaign pool or by publishing a specific voucher code.

## Create a publication (campaign)
`POST /v1/publications?join_once=true`

```json
{
  "campaign": { "name": "Welcome-30D-2025-12" },
  "customer": { "source_id": "user-123", "email": "user@example.com" },
  "channel": "api",
  "metadata": { "reason": "welcome" }
}
```

Notes:
- `join_once=true` will return the same publication for the same customer + campaign if it already exists.
- The system assigns the next available voucher from the campaign (holder is empty).
- Use `campaign.count` to assign multiple vouchers (1-20) in a single publication.

## Create a publication (specific voucher)
`POST /v1/publications`

```json
{
  "voucher": "WELCOME-1234",
  "customer": { "source_id": "user-123", "email": "user@example.com" },
  "channel": "api"
}
```

## Create a publication (GET)
`GET /v1/publications/create?voucher=CODE&customer[source_id]=customer-123`

```text
/v1/publications/create?voucher=WELCOME-1234&customer[source_id]=customer-123&channel=api
```

Notes:
- `voucher` or `campaign[name]` is required. If both are provided, `voucher` wins.
- Use `campaign[count]=1` to publish one code from a campaign (GET does not support publishing multiple vouchers).

## List publications
`GET /v1/publications?customer=<customerId>&campaign=<campaignName>&voucher=<code>&result=SUCCESS&source_id=...`

```json
{
  "object": "list",
  "data_ref": "publications",
  "publications": [],
  "total": 0
}
```

Additional query options:
- Pagination: `page` (1..1000), `limit` (1..100). `page>1000` returns `page_over_limit`.
- Sorting: `order` supports `id`, `-id`, `voucher_code`, `-voucher_code`, `tracking_id`, `-tracking_id`, `customer_id`, `-customer_id`, `created_at`, `-created_at`, `channel`, `-channel`.
- Filters: `filters[customer_id]`, `filters[voucher_code]`, `filters[campaign_name]`, `filters[result]`, `filters[failure_code]`, `filters[source_id]`, `filters[voucher_type]`, `filters[is_referral_code]`, `filters[parent_object_id]`, `filters[related_object_id]` with `$is`, `$in`, `$is_not`, `$not_in` and optional `filters[junction]=OR`.

## Get publication
`GET /v1/publications/{id}`

Returns a single publication by its UUID.

## List publications for a voucher
`GET /v1/vouchers/{code}/publications?page=1&limit=10`
