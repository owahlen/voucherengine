package org.wahlen.voucherengine.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.CustomerCreateRequest
import org.wahlen.voucherengine.api.dto.request.CustomerReferenceDto
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.PublicationRepository
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import java.util.UUID

@Service
class CustomerService(
    private val customerRepository: CustomerRepository,
    private val publicationRepository: PublicationRepository,
    private val voucherRepository: VoucherRepository,
    private val tenantService: TenantService
) {
    @Transactional
    fun upsert(tenantName: String, request: CustomerCreateRequest): Customer {
        val tenant = tenantService.requireTenant(tenantName)
        val existing = request.source_id?.let { customerRepository.findBySourceIdAndTenantName(it, tenantName) }
        val customer = existing ?: Customer(sourceId = request.source_id)
        customer.email = request.email ?: customer.email
        customer.name = request.name ?: customer.name
        customer.phone = request.phone ?: customer.phone
        customer.metadata = request.metadata ?: customer.metadata
        customer.tenant = tenant
        return customerRepository.save(customer)
    }

    @Transactional(readOnly = true)
    fun list(tenantName: String, pageable: Pageable): Page<Customer> =
        customerRepository.findAllByTenantName(tenantName, pageable)

    @Transactional(readOnly = true)
    fun getByIdOrSource(tenantName: String, idOrSource: String): Customer? {
        val fromUuid = runCatching { UUID.fromString(idOrSource) }.getOrNull()?.let { uuid ->
            customerRepository.findByIdAndTenantName(uuid, tenantName)
        }
        if (fromUuid != null) return fromUuid
        return customerRepository.findBySourceIdAndTenantName(idOrSource, tenantName)
    }

    @Transactional
    fun delete(tenantName: String, idOrSource: String) {
        val existing = getByIdOrSource(tenantName, idOrSource) ?: return
        val customerId = existing.id
        if (customerId != null) {
            val publications = publicationRepository.findAllByTenantNameAndCustomerId(tenantName, customerId)
            if (publications.isNotEmpty()) {
                publicationRepository.deleteAll(publications)
            }
            val vouchers = voucherRepository.findAllByTenantNameAndHolderId(tenantName, customerId)
            if (vouchers.isNotEmpty()) {
                vouchers.forEach { it.holder = null }
                voucherRepository.saveAll(vouchers)
            }
        }
        customerRepository.delete(existing)
    }

    @Transactional
    fun ensureCustomer(tenantName: String, ref: CustomerReferenceDto?): Customer? {
        if (ref == null || ref.source_id == null) return null
        val tenant = tenantService.requireTenant(tenantName)
        val sourceId = ref.source_id
        val existing = sourceId?.let { customerRepository.findBySourceIdAndTenantName(it, tenantName) }
        if (existing != null) {
            if (ref.email != null) existing.email = ref.email
            if (ref.name != null) existing.name = ref.name
            if (ref.phone != null) existing.phone = ref.phone
            existing.tenant = tenant
            return customerRepository.save(existing)
        }
        val customer = Customer(
            sourceId = ref.source_id,
            email = ref.email,
            name = ref.name,
            phone = ref.phone
        )
        customer.tenant = tenant
        return customerRepository.save(customer)
    }
}
