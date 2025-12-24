package org.wahlen.voucherengine.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.CustomerCreateRequest
import org.wahlen.voucherengine.api.dto.request.CustomerReferenceDto
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.event.CustomerEventType
import org.wahlen.voucherengine.persistence.model.event.EventCategory
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.PublicationRepository
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import java.util.UUID

@Service
class CustomerService(
    private val customerRepository: CustomerRepository,
    private val publicationRepository: PublicationRepository,
    private val voucherRepository: VoucherRepository,
    private val tenantService: TenantService,
    private val customerEventService: CustomerEventService
) {
    @Transactional
    fun upsert(tenantName: String, request: CustomerCreateRequest): Customer {
        val tenant = tenantService.requireTenant(tenantName)
        val existing = request.source_id?.let { customerRepository.findBySourceIdAndTenantName(it, tenantName) }
        val customer = existing ?: Customer(sourceId = request.source_id, tenant = tenant)
        val isNew = customer.id == null
        
        customer.email = request.email ?: customer.email
        customer.name = request.name ?: customer.name
        customer.phone = request.phone ?: customer.phone
        customer.metadata = request.metadata ?: customer.metadata
        
        val saved = customerRepository.save(customer)
        
        // Log event
        customerEventService.logEvent(
            tenantName = tenantName,
            customer = saved,
            eventType = if (isNew) CustomerEventType.CUSTOMER_CREATED else CustomerEventType.CUSTOMER_UPDATED,
            category = EventCategory.EFFECT,
            data = mapOf(
                "source_id" to saved.sourceId,
                "email" to saved.email,
                "name" to saved.name
            )
        )
        
        return saved
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
        
        // Log deletion event before actual delete
        customerEventService.logEvent(
            tenantName = tenantName,
            customer = existing,
            eventType = CustomerEventType.CUSTOMER_DELETED,
            category = EventCategory.EFFECT,
            data = mapOf(
                "source_id" to existing.sourceId,
                "email" to existing.email
            )
        )
        
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
            return customerRepository.save(existing)
        }
        val customer = Customer(
            sourceId = ref.source_id,
            email = ref.email,
            name = ref.name,
            phone = ref.phone,
            tenant = tenant
        )
        return customerRepository.save(customer)
    }
}
