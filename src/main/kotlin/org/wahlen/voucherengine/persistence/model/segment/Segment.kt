package org.wahlen.voucherengine.persistence.model.segment

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import java.util.*

/**
 * Customer segment entity.
 * 
 * Segments group customers based on:
 * - Static: manually selected customer IDs
 * - Auto-update: dynamic filter criteria with events
 * - Passive: dynamic filter criteria without events
 */
@Entity
@Table(
    name = "segment",
    indexes = [
        Index(name = "idx_segment_tenant", columnList = "tenant_id"),
        Index(name = "idx_segment_type", columnList = "type")
    ]
)
class Segment(
    
    @Column(nullable = false)
    var name: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: SegmentType = SegmentType.STATIC,
    
    /**
     * Filter criteria for auto-update and passive segments.
     * Stored as JSONB for flexibility.
     * Example: {"junction": "and", "created_at": {"conditions": {"$after": ["2021-12-01"]}}}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var filter: Map<String, Any?>? = null,
    
    /**
     * Customer IDs for static segments.
     * For auto-update/passive, this is computed dynamically.
     */
    @ElementCollection
    @CollectionTable(
        name = "segment_customer",
        joinColumns = [JoinColumn(name = "segment_id")],
        indexes = [Index(name = "idx_segment_customer_lookup", columnList = "segment_id, customer_id")]
    )
    @Column(name = "customer_id")
    var customerIds: MutableSet<UUID> = mutableSetOf(),
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant
    
) : AuditablePersistable()

