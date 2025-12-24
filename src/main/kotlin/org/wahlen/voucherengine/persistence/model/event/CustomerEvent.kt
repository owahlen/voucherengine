package org.wahlen.voucherengine.persistence.model.event

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.tenant.Tenant

/**
 * Customer event entity for tracking all customer-related activities.
 * 
 * Events are IMMUTABLE, DENORMALIZED snapshots that preserve historical data
 * even when referenced entities (customer, campaign, voucher) are deleted.
 * 
 * Events do NOT use foreign keys to customers/campaigns/vouchers - they store
 * IDs and relevant attributes as JSON snapshots in the data field.
 * 
 * Only tenant uses FK because tenant deletion should cascade to events.
 */
@Entity
@Table(
    name = "customer_event",
    indexes = [
        // Primary query pattern: customer + tenant + created_at (for pagination)
        Index(name = "idx_customer_event_customer_tenant_created", columnList = "customer_id, tenant_id, created_at"),
        
        // Filter by event type
        Index(name = "idx_customer_event_customer_tenant_type", columnList = "customer_id, tenant_id, event_type, created_at"),
        
        // Filter by campaign
        Index(name = "idx_customer_event_customer_tenant_campaign", columnList = "customer_id, tenant_id, campaign_id, created_at"),
        
        // Group ID for correlating related events (less common, but useful)
        Index(name = "idx_customer_event_group", columnList = "group_id"),
        
        // Tenant-level queries (admin dashboards, analytics)
        Index(name = "idx_customer_event_tenant_created", columnList = "tenant_id, created_at")
    ]
)
class CustomerEvent(
    
    /**
     * Event type using Voucherify naming conventions.
     * Examples: "customer.redemption.succeeded", "customer.validation.failed"
     * See CustomerEventType for standard constants.
     */
    @Column(nullable = false, length = 100)
    var eventType: String,
    
    /**
     * Customer ID for indexing/filtering. NOT a foreign key.
     * Allows events to survive customer deletion.
     */
    @Column(name = "customer_id", length = 36)
    var customerId: String? = null,
    
    /**
     * Campaign ID for indexing/filtering. NOT a foreign key.
     * Allows events to survive campaign deletion.
     */
    @Column(name = "campaign_id", length = 36)
    var campaignId: String? = null,
    
    /**
     * Event category - ACTION (customer-initiated) or EFFECT (system response).
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var category: EventCategory? = null,
    
    /**
     * Event-specific data stored as JSON - DENORMALIZED SNAPSHOT.
     * 
     * Contains snapshots of customer, campaign, voucher data at event time:
     * - customer: { id, email, source_id, name }
     * - campaign: { id, name, type }
     * - voucher: { id, code, discount: {...} }
     * - Plus event-specific fields (amount, tracking_id, failure_reason, etc.)
     * 
     * Schema is flexible to accommodate different event types.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var data: Map<String, Any?> = emptyMap(),
    
    /**
     * Groups related events from a single API request.
     * Allows correlation of multiple events triggered by one action.
     */
    @Column(length = 50)
    var groupId: String? = null,
    
    /**
     * Source of the event (e.g., "API", "Dashboard", "Webhook", "System").
     */
    @Column(length = 50)
    var eventSource: String? = "API",
    
    /**
     * Tenant that owns this event.
     * This is the ONLY foreign key - tenant deletion should cascade to events.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant

) : AuditablePersistable()
