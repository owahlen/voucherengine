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

@Entity
@Table(name = "export")
class Export(
    @Column(name = "exported_object", nullable = false)
    var exportedObject: String,
    @Column(nullable = false)
    var status: String,
    @Column
    var channel: String? = null,
    @Column(name = "result_url")
    var resultUrl: String? = null,
    @Column(name = "result_token")
    var resultToken: String? = null,
    @Column(name = "user_id")
    var userId: String? = null,
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var parameters: Map<String, Any?>? = null
) : AuditablePersistable() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null
}
