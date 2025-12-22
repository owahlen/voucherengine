package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.session.SessionLock
import java.util.UUID

interface SessionLockRepository : JpaRepository<SessionLock, UUID> {
    fun findAllByTenantNameAndSessionKey(tenantName: String, sessionKey: String): List<SessionLock>
    fun deleteByTenantNameAndSessionKey(tenantName: String, sessionKey: String)
}
