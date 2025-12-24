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

    /** Timestamp when the entity was created. Automatically set by Hibernate. */
    @CreationTimestamp
    @Column
    var createdAt: Instant? = null

    /** Timestamp when the entity was last updated. Automatically set by Hibernate. */
    @UpdateTimestamp
    @Column
    var updatedAt: Instant? = null
}
