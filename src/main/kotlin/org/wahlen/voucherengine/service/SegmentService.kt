package org.wahlen.voucherengine.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.SegmentCreateRequest
import org.wahlen.voucherengine.api.dto.response.CustomerSegmentResponse
import org.wahlen.voucherengine.api.dto.response.SegmentResponse
import org.wahlen.voucherengine.persistence.model.segment.Segment
import org.wahlen.voucherengine.persistence.model.segment.SegmentType
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.SegmentRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import java.util.UUID

@Service
@Transactional
class SegmentService(
    private val segmentRepository: SegmentRepository,
    private val tenantRepository: TenantRepository,
    private val customerRepository: CustomerRepository,
    private val customerEventService: CustomerEventService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    fun create(tenantName: String, request: SegmentCreateRequest): SegmentResponse {
        val tenant = tenantRepository.findByName(tenantName)
            ?: throw IllegalArgumentException("Tenant not found: $tenantName")
        
        val type = when (request.type.lowercase().replace("-", "_")) {
            "auto_update", "autoupdate" -> SegmentType.AUTO_UPDATE
            "passive" -> SegmentType.PASSIVE
            "static" -> SegmentType.STATIC
            else -> throw IllegalArgumentException("Invalid segment type: ${request.type}")
        }
        
        val segment = Segment(
            name = request.name,
            type = type,
            filter = request.filter,
            tenant = tenant
        )
        
        // For static segments, resolve customer IDs
        if (type == SegmentType.STATIC) {
            val customerList = request.customers
            if (customerList != null && customerList.isNotEmpty()) {
                segment.customerIds = customerList.mapNotNull { customerIdOrSource ->
                    // Try as UUID first, then as source_id
                    try {
                        UUID.fromString(customerIdOrSource)
                    } catch (e: IllegalArgumentException) {
                        customerRepository.findBySourceIdAndTenantName(customerIdOrSource, tenantName)?.id
                    }
                }.toMutableSet()
            }
        }
        
        val saved = segmentRepository.save(segment)
        logger.info("Created segment: {} for tenant: {}", saved.id, tenantName)
        
        // Emit customer.segment.entered events for each customer
        saved.id?.let { segmentId ->
            saved.customerIds.forEach { customerId ->
                customerEventService.logSegmentEntered(
                    tenantName = tenantName,
                    customerId = customerId,
                    segmentId = segmentId,
                    segmentName = saved.name
                )
            }
        }
        
        return toResponse(saved)
    }
    
    @Transactional(readOnly = true)
    fun list(tenantName: String): List<SegmentResponse> {
        return segmentRepository.findAllByTenant_Name(tenantName).map { toResponse(it) }
    }
    
    @Transactional(readOnly = true)
    fun get(tenantName: String, id: UUID): SegmentResponse? {
        return segmentRepository.findByIdAndTenant_Name(id, tenantName)?.let { toResponse(it) }
    }
    
    fun delete(tenantName: String, id: UUID): Boolean {
        val segment = segmentRepository.findByIdAndTenant_Name(id, tenantName) ?: return false
        
        // Emit customer.segment.left events for each customer
        segment.id?.let { segmentId ->
            segment.customerIds.forEach { customerId ->
                customerEventService.logSegmentLeft(
                    tenantName = tenantName,
                    customerId = customerId,
                    segmentId = segmentId,
                    segmentName = segment.name
                )
            }
        }
        
        segmentRepository.delete(segment)
        logger.info("Deleted segment: {} for tenant: {}", id, tenantName)
        return true
    }
    
    @Transactional(readOnly = true)
    fun getCustomerSegments(tenantName: String, customerId: UUID): List<CustomerSegmentResponse> {
        val segments = segmentRepository.findAllByTenant_NameAndCustomerIdsContaining(tenantName, customerId)
        return segments.map { CustomerSegmentResponse(it.id.toString(), it.name) }
    }
    
    private fun toResponse(segment: Segment): SegmentResponse {
        return SegmentResponse(
            id = segment.id.toString(),
            name = segment.name,
            created_at = segment.createdAt,
            type = when (segment.type) {
                SegmentType.AUTO_UPDATE -> "auto-update"
                SegmentType.PASSIVE -> "passive"
                SegmentType.STATIC -> "static"
            },
            filter = segment.filter
        )
    }
}
