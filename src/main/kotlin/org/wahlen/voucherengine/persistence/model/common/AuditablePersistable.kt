package org.wahlen.voucherengine.persistence.model.common

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant

/**
 * Base entity with UUID identifier and audit timestamps managed by Hibernate.
 */
@MappedSuperclass
abstract class AuditablePersistable : AbstractPersistable() {
    @CreationTimestamp
    @Column
    var createdAt: Instant? = null

    @UpdateTimestamp
    @Column
    var updatedAt: Instant? = null
}
