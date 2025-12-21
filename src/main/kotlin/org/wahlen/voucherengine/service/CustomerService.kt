package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.CustomerCreateRequest
import org.wahlen.voucherengine.api.dto.request.CustomerReferenceDto
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.repository.CustomerRepository

@Service
class CustomerService(
    private val customerRepository: CustomerRepository
) {
    @Transactional
    fun upsert(request: CustomerCreateRequest): Customer {
        val existing = request.source_id?.let { customerRepository.findBySourceId(it) }
        val customer = existing ?: Customer(sourceId = request.source_id)
        customer.email = request.email ?: customer.email
        customer.name = request.name ?: customer.name
        customer.phone = request.phone ?: customer.phone
        customer.metadata = request.metadata ?: customer.metadata
        return customerRepository.save(customer)
    }

    @Transactional
    fun ensureCustomer(ref: CustomerReferenceDto?): Customer? {
        if (ref == null || ref.source_id == null) return null
        val existing = customerRepository.findBySourceId(ref.source_id)
        if (existing != null) {
            if (ref.email != null) existing.email = ref.email
            if (ref.name != null) existing.name = ref.name
            if (ref.phone != null) existing.phone = ref.phone
            return customerRepository.save(existing)
        }
        return customerRepository.save(
            Customer(
                sourceId = ref.source_id,
                email = ref.email,
                name = ref.name,
                phone = ref.phone
            )
        )
    }
}
