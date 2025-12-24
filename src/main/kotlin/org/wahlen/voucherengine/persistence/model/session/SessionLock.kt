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

/**
 * Represents a session lock for vouchers during validation/redemption flows.
 *
 * Prevents concurrent redemption of the same voucher by holding a temporary lock
 * during the validation and redemption window. Locks expire automatically.
 *
 * Voucherengine API Docs: Redemptions → Session Locking.
 */
@Entity
class SessionLock(

    /**
     * Session key identifying the validation/redemption session.
     */
    @Column(name = "session_key", nullable = false)
    var sessionKey: String,

    /**
     * ID of the voucher or redeemable being locked.
     */
    @Column(nullable = false)
    var redeemableId: String,

    /**
     * Type of redeemable object (e.g., voucher, promotion).
     */
    @Column(nullable = false)
    var redeemableObject: String,

    /**
     * Lock expiration timestamp.
     */
    @Column
    var expiresAt: Instant? = null,

    /**
     * Tenant that owns this session lock.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

) : AuditablePersistable()
