# Customer Activity Tracking - Implementation Plan

## Overview

Implement a comprehensive event logging infrastructure to track all customer interactions with the voucher system. This enables activity history, audit trails, analytics, and compliance requirements.

## Goals

1. ✅ Track all customer-related events (redemptions, validations, publications, etc.)
2. ✅ Provide queryable activity history via `GET /v1/customers/{customerId}/activity`
3. ✅ Support filtering by date, campaign, event type, and category
4. ✅ Enable future analytics and reporting
5. ✅ Maintain performance with async event logging
6. ✅ Keep audit trail for compliance

---

## Architecture Approach

### **Option 1: Dedicated Event Store (Recommended)**

Create a separate `CustomerEvent` entity to store all activity with flexible JSON data.

**Pros:**
- Clean separation from business entities
- Scalable - events can be archived/purged independently
- Flexible schema via JSONB for event-specific data
- Easy to add new event types without schema changes
- Can support CDC (Change Data Capture) patterns later

**Cons:**
- Additional storage (mitigated by archival strategy)
- Slightly more complex queries for audit

**Storage Estimate:** ~500 bytes per event, 1M events = ~500MB

### **Option 2: Unified Event Log**

Single `Event` table for all system events (customer, order, voucher, etc.).

**Pros:**
- Single source of truth for all events
- Easier cross-entity analytics
- Simpler codebase

**Cons:**
- Larger table, more complex indexing
- Harder to partition/archive
- Risk of coupling customer events with system events

### **Option 3: Derive from Existing Data**

Query redemptions, publications, etc. on-the-fly without dedicated event store.

**Pros:**
- No additional storage
- No synchronization issues

**Cons:**
- ❌ Performance degrades with data growth
- ❌ Missing many event types (segment changes, tier changes, etc.)
- ❌ Can't track deleted records
- ❌ Limited to what's in existing tables

---

## ✅ Recommended Solution: Option 1 - Dedicated Event Store

---

## Data Model

### Entity: `CustomerEvent`

```kotlin
@Entity
@Table(
    name = "customer_event",
    indexes = [
        Index(name = "idx_customer_event_customer_created", columnList = "customer_id, created_at"),
        Index(name = "idx_customer_event_type", columnList = "event_type"),
        Index(name = "idx_customer_event_campaign", columnList = "campaign_id"),
        Index(name = "idx_customer_event_group", columnList = "group_id"),
        Index(name = "idx_customer_event_created", columnList = "created_at")
    ]
)
class CustomerEvent(
    
    @Column(nullable = false, length = 100)
    var eventType: String, // e.g., "customer.redemption.succeeded"
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: Customer,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var category: EventCategory? = null, // ACTION or EFFECT
    
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var data: Map<String, Any?> = emptyMap(), // Event-specific data
    
    @Column(length = 50)
    var groupId: String? = null, // Group related events (e.g., single API request)
    
    @Column(length = 50)
    var eventSource: String? = null, // "API", "Dashboard", "Webhook", etc.
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant

) : AuditablePersistable()
```

### Enum: `EventCategory`

```kotlin
enum class EventCategory {
    ACTION,  // Customer initiated (redemption, validation)
    EFFECT   // System response (points added, tier changed)
}
```

### Event Types (Phase 1 - Core Events)

```kotlin
object CustomerEventType {
    // Customer lifecycle
    const val CUSTOMER_CREATED = "customer.created"
    const val CUSTOMER_UPDATED = "customer.updated"
    const val CUSTOMER_DELETED = "customer.deleted"
    
    // Voucher operations
    const val VALIDATION_SUCCEEDED = "customer.validation.succeeded"
    const val VALIDATION_FAILED = "customer.validation.failed"
    const val REDEMPTION_SUCCEEDED = "customer.redemption.succeeded"
    const val REDEMPTION_FAILED = "customer.redemption.failed"
    const val REDEMPTION_ROLLBACK_SUCCEEDED = "customer.redemption.rollback.succeeded"
    const val REDEMPTION_ROLLBACK_FAILED = "customer.redemption.rollback.failed"
    
    // Publications
    const val PUBLICATION_SUCCEEDED = "customer.publication.succeeded"
    const val PUBLICATION_FAILED = "customer.publication.failed"
    
    // Order events
    const val ORDER_CREATED = "customer.order.created"
    const val ORDER_UPDATED = "customer.order.updated"
    const val ORDER_CANCELED = "customer.order.canceled"
    
    // Rewards (future)
    const val REWARDED = "customer.rewarded"
    const val REWARDED_LOYALTY_POINTS = "customer.rewarded.loyalty_points"
    
    // Segments (future)
    const val SEGMENT_ENTERED = "customer.segment.entered"
    const val SEGMENT_LEFT = "customer.segment.left"
}
```

---

## Implementation Steps

### Phase 1: Infrastructure (Week 1)

#### 1. Create Entity & Repository

**File:** `src/main/kotlin/org/wahlen/voucherengine/persistence/model/event/CustomerEvent.kt`

**File:** `src/main/kotlin/org/wahlen/voucherengine/persistence/repository/CustomerEventRepository.kt`

```kotlin
interface CustomerEventRepository : JpaRepository<CustomerEvent, UUID> {
    
    fun findAllByCustomer_IdAndTenant_Name(
        customerId: UUID,
        tenantName: String,
        pageable: Pageable
    ): Page<CustomerEvent>
    
    fun findAllByCustomer_IdAndTenant_NameAndEventTypeIn(
        customerId: UUID,
        tenantName: String,
        eventTypes: List<String>,
        pageable: Pageable
    ): Page<CustomerEvent>
    
    fun findAllByCustomer_IdAndTenant_NameAndCampaign_Id(
        customerId: UUID,
        tenantName: String,
        campaignId: UUID,
        pageable: Pageable
    ): Page<CustomerEvent>
    
    fun findAllByCustomer_IdAndTenant_NameAndCreatedAtBetween(
        customerId: UUID,
        tenantName: String,
        startDate: Instant,
        endDate: Instant,
        pageable: Pageable
    ): Page<CustomerEvent>
    
    // Complex queries with Specifications for filtering
}
```

#### 2. Liquibase Migration

**File:** `src/main/resources/db/changelog/migrations/20241224_create_customer_event.yaml`

```yaml
databaseChangeLog:
  - changeSet:
      id: 20241224-create-customer-event
      author: system
      changes:
        - createTable:
            tableName: customer_event
            columns:
              - column: { name: id, type: uuid, constraints: { primaryKey: true } }
              - column: { name: event_type, type: varchar(100), constraints: { nullable: false } }
              - column: { name: customer_id, type: uuid, constraints: { nullable: false } }
              - column: { name: campaign_id, type: uuid }
              - column: { name: category, type: varchar(20) }
              - column: { name: data, type: jsonb }
              - column: { name: group_id, type: varchar(50) }
              - column: { name: event_source, type: varchar(50) }
              - column: { name: tenant_id, type: uuid, constraints: { nullable: false } }
              - column: { name: created_at, type: timestamp }
              - column: { name: updated_at, type: timestamp }
              
        - createIndex:
            tableName: customer_event
            indexName: idx_customer_event_customer_created
            columns:
              - column: { name: customer_id }
              - column: { name: created_at }
              
        - createIndex:
            tableName: customer_event
            indexName: idx_customer_event_type
            columns:
              - column: { name: event_type }
              
        - createIndex:
            tableName: customer_event
            indexName: idx_customer_event_campaign
            columns:
              - column: { name: campaign_id }
              
        - createIndex:
            tableName: customer_event
            indexName: idx_customer_event_group
            columns:
              - column: { name: group_id }
              
        - createIndex:
            tableName: customer_event
            indexName: idx_customer_event_created
            columns:
              - column: { name: created_at }
        
        - addForeignKeyConstraint:
            baseTableName: customer_event
            baseColumnNames: customer_id
            referencedTableName: customer
            referencedColumnNames: id
            constraintName: fk_customer_event_customer
            
        - addForeignKeyConstraint:
            baseTableName: customer_event
            baseColumnNames: campaign_id
            referencedTableName: campaign
            referencedColumnNames: id
            constraintName: fk_customer_event_campaign
            
        - addForeignKeyConstraint:
            baseTableName: customer_event
            baseColumnNames: tenant_id
            referencedTableName: tenant
            referencedColumnNames: id
            constraintName: fk_customer_event_tenant
```

#### 3. Event Service

**File:** `src/main/kotlin/org/wahlen/voucherengine/service/CustomerEventService.kt`

```kotlin
@Service
class CustomerEventService(
    private val customerEventRepository: CustomerEventRepository,
    private val tenantService: TenantService
) {
    
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun logEventAsync(
        tenantName: String,
        customer: Customer,
        eventType: String,
        category: EventCategory? = null,
        campaign: Campaign? = null,
        data: Map<String, Any?> = emptyMap(),
        groupId: String? = null,
        eventSource: String = "API"
    ) {
        val tenant = tenantService.requireTenant(tenantName)
        val event = CustomerEvent(
            eventType = eventType,
            customer = customer,
            campaign = campaign,
            category = category,
            data = data,
            groupId = groupId,
            eventSource = eventSource,
            tenant = tenant
        )
        customerEventRepository.save(event)
    }
    
    @Transactional(readOnly = true)
    fun listCustomerActivity(
        tenantName: String,
        customerId: UUID,
        eventTypes: List<String>? = null,
        campaignId: UUID? = null,
        category: EventCategory? = null,
        startDate: Instant? = null,
        endDate: Instant? = null,
        pageable: Pageable
    ): Page<CustomerEvent> {
        // Use Spring Data Specifications for complex filtering
        return when {
            eventTypes != null -> customerEventRepository
                .findAllByCustomer_IdAndTenant_NameAndEventTypeIn(
                    customerId, tenantName, eventTypes, pageable
                )
            campaignId != null -> customerEventRepository
                .findAllByCustomer_IdAndTenant_NameAndCampaign_Id(
                    customerId, tenantName, campaignId, pageable
                )
            startDate != null && endDate != null -> customerEventRepository
                .findAllByCustomer_IdAndTenant_NameAndCreatedAtBetween(
                    customerId, tenantName, startDate, endDate, pageable
                )
            else -> customerEventRepository
                .findAllByCustomer_IdAndTenant_Name(
                    customerId, tenantName, pageable
                )
        }
    }
}
```

### Phase 2: Integration Points (Week 2)

#### 4. Hook into Existing Operations

**VoucherService - Track Redemptions:**

```kotlin
@Transactional
fun redeemVoucher(...): RedemptionResult {
    val result = performRedemption(...)
    
    // Log event
    if (result.success) {
        customerEventService.logEventAsync(
            tenantName = tenant,
            customer = customer,
            eventType = CustomerEventType.REDEMPTION_SUCCEEDED,
            category = EventCategory.ACTION,
            campaign = voucher.campaign,
            data = mapOf(
                "voucher_code" to voucher.code,
                "amount" to result.amount,
                "order_id" to order?.id,
                "redemption_id" to result.redemptionId
            ),
            groupId = requestId
        )
    } else {
        customerEventService.logEventAsync(
            tenantName = tenant,
            customer = customer,
            eventType = CustomerEventType.REDEMPTION_FAILED,
            category = EventCategory.ACTION,
            campaign = voucher.campaign,
            data = mapOf(
                "voucher_code" to voucher.code,
                "failure_reason" to result.failureReason
            ),
            groupId = requestId
        )
    }
    
    return result
}
```

**PublicationService - Track Publications:**

```kotlin
@Transactional
fun publishVoucher(...): Publication {
    val publication = createPublication(...)
    
    customerEventService.logEventAsync(
        tenantName = tenant,
        customer = customer,
        eventType = CustomerEventType.PUBLICATION_SUCCEEDED,
        category = EventCategory.EFFECT,
        campaign = voucher.campaign,
        data = mapOf(
            "voucher_code" to voucher.code,
            "publication_id" to publication.id,
            "channel" to publication.channel
        )
    )
    
    return publication
}
```

**OrderService - Track Orders:**

```kotlin
@Transactional
fun createOrder(...): Order {
    val order = saveOrder(...)
    
    order.customer?.let { customer ->
        customerEventService.logEventAsync(
            tenantName = tenant,
            customer = customer,
            eventType = CustomerEventType.ORDER_CREATED,
            category = EventCategory.ACTION,
            data = mapOf(
                "order_id" to order.id,
                "order_source_id" to order.sourceId,
                "amount" to order.amount
            )
        )
    }
    
    return order
}
```

#### 5. Update CustomerController

**File:** `src/main/kotlin/org/wahlen/voucherengine/api/controller/CustomerController.kt`

Replace the stub implementation:

```kotlin
@Operation(
    summary = "Get customer activity",
    operationId = "getCustomerActivity",
    description = "Retrieves activity details of a customer including redemptions, validations, publications, and other events",
    responses = [
        ApiResponse(responseCode = "200", description = "Customer activity retrieved"),
        ApiResponse(responseCode = "404", description = "Customer not found")
    ]
)
@GetMapping("/customers/{id}/activity")
fun getCustomerActivity(
    @RequestHeader("tenant") tenant: String,
    @PathVariable id: String,
    @Parameter(description = "Max number of items per page", example = "10")
    @RequestParam(required = false, defaultValue = "10") limit: Int,
    @Parameter(description = "1-based page index", example = "1")
    @RequestParam(required = false, defaultValue = "1") page: Int,
    @Parameter(description = "Filter by event type (e.g., customer.redemption.succeeded)")
    @RequestParam(required = false) type: String?,
    @Parameter(description = "Filter by campaign ID")
    @RequestParam(required = false) campaign_id: UUID?,
    @Parameter(description = "Filter by category: ACTION or EFFECT")
    @RequestParam(required = false) category: String?,
    @Parameter(description = "Start date filter (ISO 8601)")
    @RequestParam(required = false) start_date: String?,
    @Parameter(description = "End date filter (ISO 8601)")
    @RequestParam(required = false) end_date: String?
): ResponseEntity<CustomerActivityResponse> {
    val customer = customerService.getByIdOrSource(tenant, id) 
        ?: return ResponseEntity.notFound().build()
    
    val cappedLimit = limit.coerceIn(1, 100)
    val pageable = PageRequest.of(
        (page - 1).coerceAtLeast(0),
        cappedLimit,
        Sort.by("createdAt").descending()
    )
    
    val eventTypes = type?.let { listOf(it) }
    val eventCategory = category?.let { EventCategory.valueOf(it) }
    val startInstant = start_date?.let { Instant.parse(it) }
    val endInstant = end_date?.let { Instant.parse(it) }
    
    val events = customerEventService.listCustomerActivity(
        tenantName = tenant,
        customerId = customer.id!!,
        eventTypes = eventTypes,
        campaignId = campaign_id,
        category = eventCategory,
        startDate = startInstant,
        endDate = endInstant,
        pageable = pageable
    )
    
    return ResponseEntity.ok(
        CustomerActivityResponse(
            `object` = "list",
            data_ref = "data",
            data = events.content.map { event ->
                CustomerActivityDto(
                    id = event.id.toString(),
                    type = event.eventType,
                    data = event.data,
                    created_at = event.createdAt,
                    group_id = event.groupId
                )
            },
            has_more = events.hasNext(),
            more_starting_after = events.content.lastOrNull()?.id?.toString()
        )
    )
}
```

### Phase 3: Testing & Optimization (Week 3)

#### 6. Unit Tests

```kotlin
@SpringBootTest
class CustomerEventServiceTest {
    
    @Test
    fun `should log redemption event`() {
        // Given
        val customer = createTestCustomer()
        val campaign = createTestCampaign()
        
        // When
        customerEventService.logEventAsync(
            tenantName = "test-tenant",
            customer = customer,
            eventType = CustomerEventType.REDEMPTION_SUCCEEDED,
            category = EventCategory.ACTION,
            campaign = campaign,
            data = mapOf("voucher_code" to "TEST123")
        )
        
        // Then
        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            val events = customerEventRepository
                .findAllByCustomer_IdAndTenant_Name(customer.id!!, "test-tenant", PageRequest.of(0, 10))
            assertThat(events.content).hasSize(1)
            assertThat(events.content[0].eventType).isEqualTo(CustomerEventType.REDEMPTION_SUCCEEDED)
        }
    }
}
```

#### 7. Integration Tests

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class CustomerActivityControllerTest {
    
    @Test
    fun `should return customer activity with filters`() {
        // Setup test data
        createRedemptionForCustomer(customerId)
        createPublicationForCustomer(customerId)
        
        // When
        mockMvc.perform(
            get("/v1/customers/$customerId/activity")
                .header("tenant", "test-tenant")
                .param("type", "customer.redemption.succeeded")
                .param("limit", "10")
        )
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.data").isArray)
        .andExpect(jsonPath("$.data[0].type").value("customer.redemption.succeeded"))
    }
}
```

#### 8. Performance Optimization

- **Partitioning:** Consider table partitioning by `created_at` for large datasets
- **Archival:** Implement archival strategy (move events >1 year to archive table)
- **Caching:** Cache recent activity for frequently queried customers
- **Indexing:** Monitor query patterns and add composite indexes as needed

---

## Future Enhancements (Phase 4+)

### Week 4: Advanced Features

1. **Event Streaming**
   - Publish events to Kafka/SQS for real-time processing
   - Enable webhooks for customer activity

2. **Segment Events**
   - Implement segment membership tracking
   - Auto-log when customers enter/leave segments

3. **Loyalty Events**
   - Points earned/redeemed
   - Tier upgrades/downgrades
   - Point expiration

4. **Analytics Dashboard**
   - Activity heatmaps
   - Customer journey visualization
   - Funnel analysis

5. **Event Replay**
   - Reconstruct customer state from events (Event Sourcing lite)
   - Support for GDPR data export

---

## Migration Strategy

### Backfill Historical Data (Optional)

```kotlin
@Service
class EventBackfillService(
    private val redemptionRepository: RedemptionRepository,
    private val publicationRepository: PublicationRepository,
    private val customerEventService: CustomerEventService
) {
    
    fun backfillRedemptionEvents(tenantName: String, batchSize: Int = 1000) {
        var page = 0
        do {
            val redemptions = redemptionRepository.findAllByTenant_Name(
                tenantName,
                PageRequest.of(page++, batchSize, Sort.by("createdAt"))
            )
            
            redemptions.content.forEach { redemption ->
                redemption.customer?.let { customer ->
                    customerEventService.logEventAsync(
                        tenantName = tenantName,
                        customer = customer,
                        eventType = if (redemption.result == RedemptionResult.SUCCESS)
                            CustomerEventType.REDEMPTION_SUCCEEDED
                        else CustomerEventType.REDEMPTION_FAILED,
                        category = EventCategory.ACTION,
                        campaign = redemption.voucher?.campaign,
                        data = mapOf(
                            "voucher_code" to redemption.voucher?.code,
                            "redemption_id" to redemption.id,
                            "backfilled" to true
                        )
                    )
                }
            }
        } while (redemptions.hasNext())
    }
}
```

---

## Success Metrics

- ✅ Events logged within 100ms (async)
- ✅ Activity queries < 500ms for 95th percentile
- ✅ 100% coverage of core events (redemption, validation, publication)
- ✅ Zero data loss
- ✅ Support 10K+ events/minute

---

## Rollout Plan

1. **Week 1:** Infrastructure + migrations (no logging yet)
2. **Week 2:** Enable logging for redemptions only (canary)
3. **Week 3:** Enable all event types, monitor performance
4. **Week 4:** Backfill historical data, enable analytics

---

## Summary

This approach provides:
- ✅ **Clean architecture** - dedicated event store
- ✅ **Scalability** - async logging, indexed queries
- ✅ **Flexibility** - JSONB for event-specific data
- ✅ **Compliance** - complete audit trail
- ✅ **Future-proof** - ready for event streaming, analytics

**Estimated Effort:** 2-3 weeks for full implementation with 80%+ event coverage.
