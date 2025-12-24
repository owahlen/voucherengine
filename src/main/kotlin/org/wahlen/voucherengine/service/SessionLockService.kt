package org.wahlen.voucherengine.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.wahlen.voucherengine.api.dto.common.SessionDto
import org.wahlen.voucherengine.api.dto.response.ValidationRedeemableResponse
import org.wahlen.voucherengine.persistence.model.session.SessionLock
import org.wahlen.voucherengine.persistence.repository.SessionLockRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class SessionLockService(
    private val sessionLockRepository: SessionLockRepository,
    private val tenantRepository: TenantRepository,
    private val clock: Clock
) {
    fun createLocks(
        tenantName: String,
        session: SessionDto?,
        redeemables: List<ValidationRedeemableResponse>
    ): SessionDto? {
        if (session?.type?.uppercase() != "LOCK") return session
        val key = session.key ?: "sess_${UUID.randomUUID()}"
        sessionLockRepository.deleteByTenantNameAndSessionKey(tenantName, key)
        val tenant = tenantRepository.findByName(tenantName)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found")
        val expiresAt = resolveExpiresAt(session)
        val locks = redeemables.filter { it.status == "APPLICABLE" }.mapNotNull { redeemable ->
            val redeemableId = redeemable.id ?: return@mapNotNull null
            val redeemableObject = redeemable.`object` ?: "voucher"
            SessionLock(
                sessionKey = key,
                redeemableId = redeemableId,
                redeemableObject = redeemableObject,
                expiresAt = expiresAt,
                tenant = tenant
            )
        }
        if (locks.isNotEmpty()) {
            sessionLockRepository.saveAll(locks)
        }
        return session.copy(key = key)
    }

    fun clearLocks(tenantName: String, key: String) {
        sessionLockRepository.deleteByTenantNameAndSessionKey(tenantName, key)
    }

    private fun resolveExpiresAt(session: SessionDto): Instant? {
        val ttl = session.ttl ?: return null
        val unitValue = session.ttl_unit?.uppercase()
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "session.ttl_unit is required when ttl is set")
        val unit = when (unitValue) {
            "DAYS" -> ChronoUnit.DAYS
            "HOURS" -> ChronoUnit.HOURS
            "MICROSECONDS" -> ChronoUnit.MICROS
            "MILLISECONDS" -> ChronoUnit.MILLIS
            "MINUTES" -> ChronoUnit.MINUTES
            "NANOSECONDS" -> ChronoUnit.NANOS
            "SECONDS" -> ChronoUnit.SECONDS
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported session.ttl_unit")
        }
        return Instant.now(clock).plus(ttl, unit)
    }
}
