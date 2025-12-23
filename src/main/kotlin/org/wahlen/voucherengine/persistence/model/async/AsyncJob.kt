package org.wahlen.voucherengine.persistence.model.async

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import java.time.Instant

/**
 * Represents an asynchronous job that is processed via AWS SQS.
 * Tracks the status and results of long-running operations.
 */
@Entity
class AsyncJob(
    /**
     * Type of async job (bulk update, import, export, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: AsyncJobType,

    /**
     * Current status of the job
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AsyncJobStatus = AsyncJobStatus.PENDING,

    /**
     * Current progress count (e.g., items processed)
     */
    @Column
    var progress: Int = 0,

    /**
     * Total count of items to process
     */
    @Column
    var total: Int = 0,

    /**
     * Input parameters for the job (stored as JSON)
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var parameters: Map<String, Any?>? = null,

    /**
     * Result data from job execution (stored as JSON)
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var result: Map<String, Any?>? = null,

    /**
     * Error message if job failed
     */
    @Column(columnDefinition = "TEXT")
    var errorMessage: String? = null,

    /**
     * When the job started processing
     */
    @Column
    var startedAt: Instant? = null,

    /**
     * When the job completed (success or failure)
     */
    @Column
    var completedAt: Instant? = null

) : AuditablePersistable() {

    /**
     * Tenant that owns this job
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null
}
