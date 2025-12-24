package org.wahlen.voucherengine.persistence.model.export

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.tenant.Tenant

/**
 * Represents a voucher or transaction export job.
 *
 * Exports generate CSV or JSON data files for external systems.
 * Tracks status, parameters, and download URLs for completed exports.
 *
 * Voucherengine API Docs: Exports.
 */
@Entity
@Table(name = "export")
class Export(

    /**
     * Type of object being exported (e.g., voucher, order, redemption).
     */
    @Column(name = "exported_object", nullable = false)
    var exportedObject: String,

    /**
     * Export status (SCHEDULED, IN_PROGRESS, DONE, ERROR).
     */
    @Column(nullable = false)
    var status: String,

    /**
     * Distribution channel for the export (e.g., API, S3).
     */
    @Column
    var channel: String? = null,

    /**
     * URL where the export file can be downloaded.
     */
    @Column(name = "result_url")
    var resultUrl: String? = null,

    /**
     * Access token for downloading the export file.
     */
    @Column(name = "result_token")
    var resultToken: String? = null,

    /**
     * User who requested the export.
     */
    @Column(name = "user_id")
    var userId: String? = null,

    /**
     * Export filter parameters (e.g., date range, filters) as JSON.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var parameters: Map<String, Any?>? = null,

    /**
     * Tenant that owns this export.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

) : AuditablePersistable()
