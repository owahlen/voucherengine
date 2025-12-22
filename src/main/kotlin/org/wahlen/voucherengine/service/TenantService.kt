package org.wahlen.voucherengine.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import org.wahlen.voucherengine.api.dto.request.TenantCreateRequest
import org.wahlen.voucherengine.api.dto.response.TenantResponse
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import java.util.UUID

@Service
class TenantService(
    private val tenantRepository: TenantRepository
) {

    @Transactional
    fun create(requesterTenant: String, request: TenantCreateRequest): TenantResponse {
        val tenant = Tenant(name = request.name)
        return toResponse(tenantRepository.save(tenant))
    }

    @Transactional
    fun update(requesterTenant: String, id: UUID, request: TenantCreateRequest): TenantResponse? {
        val existing = tenantRepository.findById(id).orElse(null) ?: return null
        existing.name = request.name ?: existing.name
        return toResponse(tenantRepository.save(existing))
    }

    @Transactional(readOnly = true)
    fun get(requesterTenant: String, id: UUID): TenantResponse? =
        tenantRepository.findById(id).orElse(null)?.let(::toResponse)

    @Transactional(readOnly = true)
    fun list(requesterTenant: String): List<TenantResponse> =
        tenantRepository.findAll().map(::toResponse)

    @Transactional
    fun delete(requesterTenant: String, id: UUID): Boolean {
        val existing = tenantRepository.findById(id).orElse(null) ?: return false
        tenantRepository.delete(existing)
        return true
    }

    @Transactional(readOnly = true)
    fun getByName(tenantName: String): Tenant? = tenantRepository.findByName(tenantName)

    @Transactional
    fun ensureTenantExists(tenantName: String): Tenant =
        tenantRepository.findByName(tenantName) ?: tenantRepository.save(Tenant(name = tenantName))

    @Transactional(readOnly = true)
    fun requireTenant(tenantName: String): Tenant =
        tenantRepository.findByName(tenantName)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found")

    private fun toResponse(tenant: Tenant): TenantResponse =
        TenantResponse(
            id = tenant.id,
            name = tenant.name,
            created_at = tenant.createdAt,
            updated_at = tenant.updatedAt
        )
}
