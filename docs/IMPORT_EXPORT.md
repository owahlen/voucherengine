# Import/Export Architecture

This document describes the architecture for asynchronous import and export operations in the voucher engine.

## Overview

Import and export operations are long-running tasks that process large datasets (orders, vouchers, customers, redemptions). They leverage the existing SQS-based async job infrastructure to handle these operations asynchronously while providing progress tracking and result retrieval.

## Architecture Components

### 1. Async Job Infrastructure (Existing)

The system already has a robust async job infrastructure:

- **AsyncJob Entity** (`persistence/model/async/AsyncJob.kt`)
  - Tracks job status (QUEUED, PROCESSING, COMPLETED, FAILED)
  - Stores progress information (processedCount, totalCount, percentage)
  - Maintains result data in JSONB column
  - Supports error tracking

- **SQS Queue** (LocalStack in dev, AWS SQS in prod)
  - Commands are serialized to SQS using Jackson polymorphic serialization
  - `AsyncJobListener` processes messages with transaction boundary (`@Transactional(REQUIRES_NEW)`)
  - Each command implements `execute(ApplicationContext)` method

- **Status Polling Endpoint** (`/v1/async-actions/{id}`)
  - Clients poll this endpoint to check job status
  - Returns progress percentage and results when complete

### 2. Import Operations

#### Workflow

```
Client → POST /v1/orders/import → Create AsyncJob → Send Command to SQS → Return Job ID
                                                           ↓
Client ← Poll /v1/async-actions/{id} ← Update Progress ← Process Import
                                                           ↓
                                                      Store Results
```

#### Implementation Components

**Command**: `ImportOrdersCommand`
- Serializable command containing import parameters
- Includes: tenantName, source data reference (S3 key or inline data), validation rules
- Implements `execute(ApplicationContext)` to delegate to service

**Service**: `OrderAsyncService.handleImportOrders()`
- Runs within `AsyncJobListener`'s transaction
- Processes orders in batches (e.g., 100 at a time)
- Updates AsyncJob progress after each batch
- Stores validation errors in result data
- Final result includes: successCount, errorCount, errors[]

**Controller**: `OrderController.importOrders()`
- Accepts import request (file upload or data payload)
- Creates AsyncJob record (status=QUEUED)
- Sends ImportOrdersCommand to SQS
- Returns job ID immediately

#### Data Flow

1. **File Upload** (optional pre-step)
   - Client uploads CSV/JSON to S3 (or stores inline for small datasets)
   - Receives S3 key or passes data directly

2. **Job Creation**
   - Controller creates AsyncJob with type=IMPORT_ORDERS
   - Job initially has status=QUEUED, progress=0%

3. **SQS Message**
   - ImportOrdersCommand serialized to SQS
   - Contains job ID and data reference

4. **Processing**
   - AsyncJobListener picks up message
   - Parses/validates data in batches
   - Creates Order entities
   - Updates progress: `processedCount / totalCount * 100`

5. **Completion**
   - Sets status=COMPLETED or FAILED
   - Stores result summary in AsyncJob.result JSONB:
     ```json
     {
       "successCount": 950,
       "errorCount": 50,
       "errors": [
         {"line": 15, "message": "Invalid customer ID"},
         {"line": 42, "message": "Missing required field: amount"}
       ]
     }
     ```

### 3. Export Operations

#### Workflow

```
Client → POST /v1/orders/export → Create AsyncJob → Send Command to SQS → Return Job ID
                                                           ↓
Client ← Poll /v1/async-actions/{id} ← Update Progress ← Query & Build Export
                                                           ↓
                                                      Upload to S3 → Store URL
```

#### Implementation Components

**Command**: `ExportOrdersCommand`
- Contains: tenantName, filter criteria, format (CSV/JSON)
- Implements `execute(ApplicationContext)`

**Service**: `OrderAsyncService.handleExportOrders()`
- Queries orders matching filter criteria
- Generates export file in batches
- Uploads to S3 (or stores inline for small exports)
- Stores download URL in AsyncJob.result

**Controller**: `OrderController.exportOrders()`
- Accepts export request with filters
- Creates AsyncJob record
- Sends ExportOrdersCommand to SQS
- Returns job ID

#### Data Flow

1. **Job Creation**
   - Controller creates AsyncJob with type=EXPORT_ORDERS
   - Stores filter parameters in job metadata

2. **SQS Message**
   - ExportOrdersCommand sent to queue

3. **Processing**
   - Query total count for progress tracking
   - Stream results in batches
   - Build CSV/JSON incrementally
   - Update progress after each batch

4. **File Storage**
   - Upload to S3 with expiring pre-signed URL
   - Store URL in AsyncJob.result:
     ```json
     {
       "url": "https://s3.../exports/orders-20231224-abc123.csv",
       "expiresAt": "2023-12-31T23:59:59Z",
       "recordCount": 1500,
       "format": "CSV"
     }
     ```

5. **Download**
   - Client retrieves URL from `/v1/async-actions/{id}`
   - Downloads file directly from S3

### 4. Supported Operations

Based on Voucherify API spec:

#### Orders
- **POST /v1/orders/import** - Import orders
- **POST /v1/orders/export** - Export orders with filters

#### Vouchers
- **POST /v1/vouchers/import** - Import standalone vouchers
- **POST /v1/vouchers/importCSV** - Import vouchers from CSV
- **POST /v1/vouchers/export** - Export vouchers with filters
- **POST /v1/campaigns/{id}/import** - Import vouchers to campaign (already implemented)
- **POST /v1/campaigns/{id}/importCSV** - Import vouchers to campaign from CSV

#### Customers
- **POST /v1/customers/import** - Import customers
- **POST /v1/customers/importCSV** - Import customers from CSV
- **POST /v1/customers/bulk/async** - Bulk update customers

#### Redemptions
- **POST /v1/exports** - Generic export API (redemptions, publications, etc.)

## Error Handling

### Validation Errors
- Partial success supported: valid records processed, invalid records reported
- Errors stored in AsyncJob.result with line numbers and messages
- Job status = COMPLETED even with validation errors (check errorCount)

### System Errors
- Network failures, S3 errors, DB errors → status = FAILED
- Error message stored in AsyncJob.result
- Client can retry by creating new job

### Transaction Boundaries
- Each batch processed in separate transaction
- Progress updates committed after each batch
- Prevents losing progress on failure

## File Format Support

### CSV
- Standard format with header row
- UTF-8 encoding
- Comma-separated, quoted strings
- Example:
  ```csv
  customer_id,amount,status,created_at
  "cust_001",99.99,"PAID","2023-12-24T10:00:00Z"
  ```

### JSON
- Array of objects or newline-delimited JSON
- Example:
  ```json
  [
    {"customer_id": "cust_001", "amount": 99.99, "status": "PAID"},
    {"customer_id": "cust_002", "amount": 149.99, "status": "PAID"}
  ]
  ```

## Storage Strategy

### Small Datasets (< 1MB)
- Store inline in request/response
- No S3 upload needed
- Suitable for < 1000 records

### Large Datasets (> 1MB)
- Client uploads to S3 pre-signed URL (or direct upload endpoint)
- Pass S3 key to import endpoint
- Export generates S3 URL with expiration

### S3 Bucket Structure
```
{tenant}/imports/{year}/{month}/{job-id}.{format}
{tenant}/exports/{year}/{month}/{job-id}.{format}
```

## Progress Tracking

Progress calculation:
```kotlin
val percentage = if (totalCount > 0) {
    (processedCount.toDouble() / totalCount * 100).toInt()
} else 0

asyncJob.processedCount = processedCount
asyncJob.totalCount = totalCount
asyncJob.progressPercentage = percentage
```

Update frequency:
- Update after each batch (e.g., every 100 records)
- Prevents excessive DB writes
- Provides reasonable client feedback

## Security Considerations

### Tenant Isolation
- All operations scoped to tenant from JWT
- S3 keys include tenant prefix
- Query filters automatically include tenant

### File Access
- S3 pre-signed URLs with short expiration (1 hour)
- URLs are single-use or time-limited
- No public bucket access

### Rate Limiting
- Limit concurrent import/export jobs per tenant
- Reject new jobs if queue depth exceeds threshold

## Example Client Flow

### Import Flow
```typescript
// 1. Create import job
const response = await fetch('/v1/orders/import', {
  method: 'POST',
  headers: { 'tenant': 'acme', 'Authorization': 'Bearer ...' },
  body: JSON.stringify({ orders: [...] })
});
const { id } = await response.json();

// 2. Poll for completion
while (true) {
  const status = await fetch(`/v1/async-actions/${id}`);
  const job = await status.json();
  
  console.log(`Progress: ${job.progressPercentage}%`);
  
  if (job.status === 'COMPLETED') {
    console.log(`Success: ${job.result.successCount}, Errors: ${job.result.errorCount}`);
    break;
  }
  
  await sleep(2000); // Poll every 2 seconds
}
```

### Export Flow
```typescript
// 1. Create export job
const response = await fetch('/v1/orders/export', {
  method: 'POST',
  headers: { 'tenant': 'acme', 'Authorization': 'Bearer ...' },
  body: JSON.stringify({ 
    filters: { status: 'PAID', created_after: '2023-01-01' },
    format: 'CSV'
  })
});
const { id } = await response.json();

// 2. Poll for completion
while (true) {
  const status = await fetch(`/v1/async-actions/${id}`);
  const job = await status.json();
  
  if (job.status === 'COMPLETED') {
    // 3. Download file
    window.location.href = job.result.url;
    break;
  }
  
  await sleep(2000);
}
```

## Implementation Checklist

- [ ] Order import/export endpoints and commands
- [ ] Voucher import/export endpoints (non-campaign)
- [ ] Customer import/export endpoints
- [ ] Generic `/v1/exports` endpoint for redemptions
- [ ] S3 integration (LocalStack for dev)
- [ ] CSV parser/generator utilities
- [ ] Batch processing with progress updates
- [ ] Error collection and reporting
- [ ] Integration tests with async job polling
- [ ] API documentation (SpringDoc)
