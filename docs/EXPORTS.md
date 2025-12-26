# Export Architecture

## Overview

The export system provides asynchronous bulk data exports for various entity types (vouchers, orders, customers, etc.) with results stored in S3 and accessible via presigned URLs.

## Architecture

### High-Level Flow

```
┌──────────────────┐
│ POST /v1/exports │  1. Client creates export request
└────────┬─────────┘
         │
         ▼
┌──────────────────────┐
│  ExportController    │  2. Validates request
└────────┬─────────────┘
         │
         ▼
┌──────────────────────┐
│   ExportService      │  3. Creates Export entity (status=SCHEDULED)
│                      │  4. Publishes async command to SQS
└────────┬─────────────┘
         │
         ▼
┌──────────────────────┐
│  AsyncJobPublisher   │  5. Persists AsyncJob, sends to SQS
└────────┬─────────────┘
         │
         ▼ (SQS Message)
         │
┌────────┴─────────────┐
│  AsyncJobListener    │  6. Receives message, deserializes command
└────────┬─────────────┘     (transaction boundary: REQUIRES_NEW)
         │
         ▼
┌─────────────────────────────────────┐
│ VoucherExportCommand.execute()      │  7. Executes polymorphic command
│ OrderExportCommand.execute()        │
│ PlaceholderExportCommand.execute()  │
└─────────┬───────────────────────────┘
          │
          ▼
┌─────────────────────────┐
│ VoucherExportService    │  8. Query DB, generate CSV/JSON
│ OrderExportService      │  9. Upload to S3, get presigned URL
│ (other XxxExportService)│ 10. Update Export entity (status=DONE, resultUrl)
└─────────┬───────────────┘ 11. Update AsyncJob (status=COMPLETED)
          │
          ▼
┌─────────────────────────┐
│      S3Service          │  12. Upload file, generate presigned URL
└─────────────────────────┘
          │
          ▼
      [ S3 Bucket ]
      voucherengine-exports/
        {tenant}/exports/
          {year}/{month}/
            vouchers-{uuid}.csv
```

### Client Poll Flow

```
GET /v1/exports/{id}  →  {status: "SCHEDULED"}
   ↓ (poll every few seconds)
GET /v1/exports/{id}  →  {status: "IN_PROGRESS", progress: 50, total: 100}
   ↓
GET /v1/exports/{id}  →  {status: "DONE", result: {url: "https://s3..."}}
   ↓
Download from S3 URL  →  CSV/JSON content
```

## Components

### 1. ExportController
**Responsibility**: HTTP API endpoints for export CRUD operations.

**Endpoints**:
- `POST /v1/exports` - Create new export (returns SCHEDULED status)
- `GET /v1/exports` - List exports (paginated)
- `GET /v1/exports/{id}` - Get export details
- `DELETE /v1/exports/{id}` - Delete export
- `GET /v1/exports/{id}/download?token={token}` - Download
  This endpoint will later be deprecated and returns 303 redirect to S3 URL.
  For now it should proxy to the S3 bucket and be an alternative to using pre-signed S3 URLs.

**Note**: Download endpoint is kept for backward compatibility but clients should use the S3 presigned URL from `result.url` directly.

### 2. ExportService
**Responsibility**: Business logic for export entity management and async job orchestration.

**Key Methods**:
- `createExport()` - Creates Export entity with SCHEDULED status, publishes async command
- `listExports()` - Paginated list with sorting
- `getExport()` - Single export details
- `deleteExport()` - Delete export entity
- `downloadExport()` - Returns 303 redirect to S3 URL

**Dependencies**:
- `ExportRepository` - Persist Export entities
- `TenantService` - Validate tenant
- `AsyncJobPublisher` - Publish commands to SQS

**No longer depends on**: VoucherRepository, OrderRepository, etc. (moved to dedicated export services)

### 3. AsyncJobCommand Hierarchy

**Base Interface**: `AsyncJobCommand`
- Jackson polymorphic serialization with `jobType` discriminator
- `toAsyncJob()` - Create AsyncJob entity
- `execute(ApplicationContext)` - Perform the actual export work

**Implementations**:
- `VoucherExportCommand` → `VoucherExportService`
- `OrderExportCommand` → `OrderExportService`
- `PlaceholderExportCommand` → inline execution (returns empty CSV with headers)

**Future Implementations** (see below):
- `RedemptionExportCommand` → `RedemptionExportService`
- `PublicationExportCommand` → `PublicationExportService`
- `CustomerExportCommand` → `CustomerExportService`
- `ProductExportCommand` → `ProductExportService`
- `SkuExportCommand` → `SkuExportService`
- `PointsExpirationExportCommand` → `PointsExpirationExportService`
- `VoucherTransactionExportCommand` → `VoucherTransactionExportService`

### 4. XxxExportService Pattern

**Template** (VoucherExportService, OrderExportService):

```kotlin
@Service
class VoucherExportService(
    private val voucherRepository: VoucherRepository,
    private val asyncJobRepository: AsyncJobRepository,
    private val exportRepository: ExportRepository,
    private val s3Service: S3Service,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    fun executeExport(jobId: UUID, tenantName: String, parameters: Map<String, Any?>) {
        // 1. Get AsyncJob, set IN_PROGRESS
        // 2. Parse parameters (format, fields, order, filters)
        // 3. Query DB with pagination (BATCH_SIZE = 100)
        // 4. Generate CSV or JSON
        // 5. Upload to S3 → get presigned URL
        // 6. Update Export entity (status=DONE, resultUrl, resultToken)
        // 7. Update AsyncJob (status=COMPLETED, result with URL)
    }
}
```

**Key Patterns**:
- Batch processing (100 records at a time) to avoid memory issues
- Progress tracking (update AsyncJob.progress periodically)
- Support for both CSV and JSON formats
- Field selection (default fields if none specified)
- Sorting (per Voucherify spec: `-field` for descending)
- Filtering (especially campaign_ids for vouchers)
- Metadata field extraction (`metadata.X` notation)

### 5. S3Service
**Responsibility**: S3 operations and presigned URL generation.

**Methods**:
- `uploadExport(tenantName, fileName, content, contentType)` - Upload to S3, return presigned URL
- `downloadImport(tenantName, fileName)` - Download file from S3 (for imports)
- `generatePresignedUrl(bucket, key)` - Generate time-limited download URL

**S3 Key Structure**:
```
voucherengine-exports/
  {tenant}/exports/{year}/{month}/{filename}
  
Example: voucherengine-exports/acme/exports/2025/12/vouchers-a1b2c3.csv
```

**Presigned URL Duration**: 1 hour (configurable via `aws.s3.presigned-url-duration`)

### 6. Export Entity
**Fields**:
- `exportedObject` - Type of export (voucher, order, etc.)
- `status` - SCHEDULED, IN_PROGRESS, DONE, ERROR
- `channel` - API, WEBSITE
- `resultUrl` - S3 presigned download URL
- `resultToken` - Access token for download endpoint (legacy)
- `parameters` - Export parameters as JSONB (fields, filters, order)
- `tenant` - Foreign key to Tenant

### 7. AsyncJob Entity
**Fields**:
- `type` - VOUCHER_EXPORT, ORDER_EXPORT, etc.
- `status` - PENDING, IN_PROGRESS, COMPLETED, FAILED
- `progress` - Current record count processed
- `total` - Total records to process
- `parameters` - Job parameters as JSONB
- `result` - Job result as JSONB (contains URL, recordCount, expiresAt)
- `tenant` - Foreign key to Tenant

## Supported Export Types

### Currently Implemented ✅

| Export Type | Command | Service | Status |
|-------------|---------|---------|--------|
| `voucher` | VoucherExportCommand | VoucherExportService | ✅ Full async |
| `order` | OrderExportCommand | OrderExportService | ✅ Full async |

### Placeholder Implementation 🟡

| Export Type | Command | Status |
|-------------|---------|--------|
| `redemption` | PlaceholderExportCommand | 🟡 Returns empty CSV |
| `publication` | PlaceholderExportCommand | 🟡 Returns empty CSV |
| `customer` | PlaceholderExportCommand | 🟡 Returns empty CSV |
| `product` | PlaceholderExportCommand | 🟡 Returns empty CSV |
| `sku` | PlaceholderExportCommand | 🟡 Returns empty CSV |
| `points_expiration` | PlaceholderExportCommand | 🟡 Returns empty CSV |
| `voucher_transactions` | PlaceholderExportCommand | 🟡 Returns empty CSV |

## Export Field Specifications

### Voucher Export
**Default Fields**: `code`, `voucher_type`, `value`, `discount_type`

**All Available Fields**:
- **Identifiers**: `id`, `code`, `campaign`, `campaign_id`, `category`, `category_id`
- **Type & Value**: `voucher_type`, `value`, `discount_type`, `discount_amount_limit`, `discount_unit_type`, `discount_unit_effect`
- **Balances**: `gift_balance`, `loyalty_balance`
- **Redemptions**: `redemption_quantity`, `redemption_count`
- **Dates**: `start_date`, `expiration_date`, `created_at`, `updated_at`
- **Validity**: `validity_timeframe_interval`, `validity_timeframe_duration`, `validity_day_of_week`
- **Status**: `active`, `is_referral_code`
- **Assets**: `qr_code`, `bar_code`
- **Customer**: `customer_id`, `customer_source_id`
- **Metadata**: `metadata`, `metadata.{key}`, `additional_info`

### Order Export
**Default Fields**: `id`, `source_id`, `status`

**All Available Fields**:
- **Identifiers**: `id`, `source_id`
- **Status**: `status`
- **Amounts**: `amount`, `discount_amount`, `items_discount_amount`, `total_discount_amount`, `total_amount`
- **Dates**: `created_at`, `updated_at`
- **Relationships**: `customer_id`, `referrer_id`
- **Metadata**: `metadata`, `metadata.{key}`

### Redemption Export
**Default Fields**: `id`, `object`, `voucher_code`, `customer_id`, `date`, `result`

**All Available Fields**:
- **Identifiers**: `id`, `object`, `voucher_code`, `campaign`, `promotion_tier_id`, `tracking_id`
- **Customer**: `customer_id`, `customer_source_id`, `customer_name`
- **Amounts**: `order_amount`, `gift_amount`, `loyalty_points`
- **Result**: `result`, `failure_code`, `failure_message`
- **Dates**: `date`
- **Metadata**: `metadata`, `metadata.{key}`

### Publication Export
**Default Fields**: `code`, `customer_id`, `date`, `channel`

**All Available Fields**:
- **Identifiers**: `voucher_code`/`code`, `campaign`
- **Customer**: `customer_id`, `customer_source_id`
- **Details**: `date`, `channel`, `is_winner`
- **Metadata**: `metadata`, `metadata.{key}`

### Customer Export
**Default Fields**: `name`, `source_id`

**All Available Fields**:
- **Identifiers**: `id`, `source_id`, `name`, `email`, `description`, `phone`
- **Address**: `address_city`, `address_state`, `address_line_1`, `address_line_2`, `address_country`, `address_postal_code`
- **Dates**: `created_at`, `updated_at`, `birthday`, `birthdate`
- **Aggregates** (redemptions): `redemptions_total_redeemed`, `redemptions_total_failed`, `redemptions_total_succeeded`, `redemptions_total_rolled_back`, `redemptions_total_rollback_failed`, `redemptions_total_rollback_succeeded`
- **Aggregates** (orders): `orders_total_amount`, `orders_total_count`, `orders_average_amount`, `orders_last_order_amount`, `orders_last_order_date`
- **Aggregates** (loyalty): `loyalty_points`, `loyalty_referred_customers`
- **Metadata**: `metadata`, `metadata.{key}`

### Points Expiration Export
**Default Fields**: `id`, `campaign_id`, `voucher_id`, `status`, `expires_at`, `points`

**All Available Fields**:
- **Identifiers**: `id`, `campaign_id`, `voucher_id`
- **Status**: `status` (ACTIVE, EXPIRED)
- **Details**: `expires_at`, `points`

### Voucher Transactions Export
**Default Fields**: `id`, `type`, `source_id`, `status`, `reason`, `source`, `balance`, `amount`, `created_at`

**All Available Fields**:
- **Identifiers**: `id`, `source_id`, `voucher_id`, `campaign_id`
- **Type**: `type` (CREDITS_REMOVAL, CREDITS_ADDITION, CREDITS_REFUND, CREDITS_REDEMPTION, POINTS_EXPIRATION, POINTS_ADDITION, POINTS_REMOVAL, etc.)
- **Details**: `reason`, `balance`, `amount`, `source`, `related_transaction_id`
- **Dates**: `created_at`
- **Metadata**: `details` (JSON object)

### Product Export
**Default Fields**: `id`, `name`, `price`, `image_url`, `source_id`, `attributes`, `created_at`

**All Available Fields**:
- **Identifiers**: `id`, `source_id`, `name`
- **Details**: `price`, `image_url`, `attributes`
- **Dates**: `created_at`, `updated_at`
- **Metadata**: `metadata`, `metadata.{key}`

### SKU Export
**Default Fields**: `id`, `sku`, `product_id`, `currency`, `price`, `image_url`, `source_id`, `attributes`, `created_at`

**All Available Fields**:
- **Identifiers**: `id`, `sku`, `product_id`, `source_id`
- **Details**: `currency`, `price`, `image_url`, `attributes`
- **Dates**: `created_at`, `updated_at`
- **Metadata**: `metadata`, `metadata.{key}`

## Next Implementation Steps

### Phase 1: Core Exports (Priority: HIGH)

#### 1. Redemption Export
- [ ] Create `RedemptionExportCommand`
- [ ] Create `RedemptionExportService`
- [ ] Implement field extraction for all redemption fields
- [ ] Add to `AsyncJobCommand` @JsonSubTypes
- [ ] Update `ExportService.createExport()` to use RedemptionExportCommand
- [ ] Add integration tests

#### 2. Publication Export
- [ ] Create `PublicationExportCommand`
- [ ] Create `PublicationExportService`
- [ ] Implement field extraction for all publication fields
- [ ] Add to `AsyncJobCommand` @JsonSubTypes
- [ ] Update `ExportService.createExport()` to use PublicationExportCommand
- [ ] Add integration tests

#### 3. Customer Export (with aggregates)
- [ ] Create `CustomerExportCommand`
- [ ] Create `CustomerExportService`
- [ ] Implement field extraction including complex aggregates:
  - Redemption counts (total_redeemed, total_failed, etc.)
  - Order aggregates (total_amount, count, average, last order)
  - Loyalty aggregates (points, referred_customers)
- [ ] Add to `AsyncJobCommand` @JsonSubTypes
- [ ] Update `ExportService.createExport()` to use CustomerExportCommand
- [ ] Add integration tests

### Phase 2: Product/SKU Exports (Priority: MEDIUM)

#### 4. Product Export
- [ ] Create `ProductExportCommand`
- [ ] Create `ProductExportService`
- [ ] Implement field extraction
- [ ] Add integration tests

#### 5. SKU Export
- [ ] Create `SkuExportCommand`
- [ ] Create `SkuExportService`
- [ ] Implement field extraction
- [ ] Add integration tests

### Phase 3: Advanced Exports (Priority: LOW)

#### 6. Points Expiration Export
- [ ] Create `PointsExpirationExportCommand`
- [ ] Create `PointsExpirationExportService`
- [ ] Requires loyalty points bucket entity (not yet implemented)
- [ ] Add integration tests

#### 7. Voucher Transactions Export
- [ ] Create `VoucherTransactionExportCommand`
- [ ] Create `VoucherTransactionExportService`
- [ ] Requires gift card/loyalty card transaction entity (not yet implemented)
- [ ] Support for both gift card and loyalty card transactions
- [ ] Add integration tests

### Phase 4: Enhancements (Priority: ONGOING)

#### Performance Optimizations
- [ ] Implement streaming for very large exports (>100K records)
- [ ] Add export result caching (avoid re-generating same export)
- [ ] Implement export pagination for downloads (split large files)

#### Additional Features
- [ ] Support for JSON format (currently only CSV is tested)
- [ ] Add compression (gzip) for large exports
- [ ] Email notification when export completes
- [ ] Webhook notification for export completion
- [ ] Export templates (save field selections for reuse)
- [ ] Scheduled recurring exports

#### Observability
- [ ] Metrics for export success/failure rates
- [ ] Metrics for export duration by type
- [ ] Alerting for failed exports
- [ ] Logging improvements (structured logging with export_id, tenant)

## Testing Strategy

### Unit Tests
- `ExportService` - mock async publisher, verify command creation
- `VoucherExportService` - mock repositories, verify CSV/JSON generation
- Field extraction logic - verify all field mappings

### Integration Tests
- `ExportControllerIntegrationTest` - end-to-end via HTTP
- `VoucherExportServiceIntegrationTest` - async job execution with S3
- `OrderExportServiceIntegrationTest` - async job execution with S3

### Key Test Scenarios
- ✅ Export creation returns SCHEDULED status
- ✅ Async job progresses from PENDING → IN_PROGRESS → COMPLETED
- ✅ Export entity updated with resultUrl after completion
- ✅ S3 file uploaded and downloadable via presigned URL
- ✅ CSV content matches expected format with correct headers and data
- ✅ JSON content correctly structured
- ✅ Field selection works (custom fields vs defaults)
- ✅ Sorting works (ascending/descending)
- ✅ Filtering works (campaign_ids, metadata filters)
- ✅ Empty exports handled gracefully (no records)
- ✅ Large exports paginate correctly (batch size = 100)
- ⏳ Placeholder exports return empty CSV with headers

### Test Data Cleanup
**Important**: Integration tests must clean up in reverse dependency order:
```kotlin
// Correct order:
orderRepository.deleteAll()        // or voucherRepository, etc.
campaignRepository.deleteAll()      // if applicable
customerRepository.deleteAll()      // if applicable  
asyncJobRepository.deleteAll()      // must come before exportRepository
exportRepository.deleteAll()        // if injected
tenantRepository.deleteAll()        // last (referenced by everything)
```

## Technical Considerations

### Jackson 3 Usage
- **Use**: `tools.jackson.databind.ObjectMapper`
- **Avoid**: `com.fasterxml.jackson.*` (except annotations like `@JsonTypeInfo`, `@JsonSubTypes`)
- ToolsJacksonJsonFormatMapper configured for JSONB columns

### Transaction Boundaries
- `AsyncJobListener` uses `@Transactional(REQUIRES_NEW)` for each SQS message
- Each export service execution runs in its own transaction
- Export entity and AsyncJob updates happen within same transaction

### Error Handling
- Exceptions in export service mark AsyncJob as FAILED
- Export entity status remains SCHEDULED (not updated to ERROR)
- Error message stored in AsyncJob.result map

### Presigned URL Expiration
- Default: 1 hour
- Configurable via `aws.s3.presigned-url-duration`
- Client should download before expiration or request new export

### Memory Management
- Batch processing (100 records) prevents OutOfMemoryError
- ByteArrayOutputStream for CSV/JSON generation
- Progress tracking allows client to estimate completion time

## Configuration

### Application Properties
```yaml
aws:
  s3:
    bucket:
      exports: voucherengine-exports
      imports: voucherengine-imports
    presigned-url-duration: PT1H  # 1 hour
  sqs:
    queues:
      voucherengine-async-jobs: voucherengine-async-jobs
```

### LocalStack (Development)
- S3Mock on random port, bucket pre-created by S3MockExtension
- ElasticMQ for SQS, queue pre-created by ElasticMqExtension
- Test configuration via S3MockPropertyInitializer and ElasticMqPropertyInitializer

## References

- **Voucherify Spec**: `docs/voucherify.json` - OpenAPI specification
- **Postman Collection**: `http/Voucherify API - Core API Endpoints.postman_collection.json`
- **Flow Guides**: `docs/PER-CUSTOMER-VOUCHER.md`, `docs/MULTI-USE-VOUCHER.md`
- **Async Framework**: AGENTS.md - Agent guide for async job pattern
