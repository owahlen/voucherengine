# Vouchers

## Operational endpoints

### Enable voucher

```
POST /v1/vouchers/{code}/enable
```

Sets `active=true` for the voucher and returns the voucher object.

### Disable voucher

```
POST /v1/vouchers/{code}/disable
```

Sets `active=false` for the voucher and returns the voucher object.
