package org.wahlen.voucherengine.persistence.model.session

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import java.time.Instant

@Entity
class SessionLock(
    @Column(name = "session_key", nullable = false)
    var sessionKey: String,
    @Column(nullable = false)
    var redeemableId: String,
    @Column(nullable = false)
    var redeemableObject: String,
    @Column
    var expiresAt: Instant? = null
) : AuditablePersistable() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null
}
