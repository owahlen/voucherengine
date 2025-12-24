package org.wahlen.voucherengine.service

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.persistence.model.campaign.Campaign
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.event.CustomerEvent
import org.wahlen.voucherengine.persistence.model.event.EventCategory
import org.wahlen.voucherengine.persistence.repository.CustomerEventRepository
import java.time.Instant
import java.util.UUID

/**
 * Service for managing customer activity events.
 * 
 * Events are DENORMALIZED snapshots - they store IDs and selected attributes
 * in JSON, not foreign keys. This allows events to survive entity deletions.
 * 
 * Event logging is synchronous and participates in the caller's transaction.
 * This ensures atomic behavior: events are saved with successful operations,
 * not saved if the operation fails/rolls back.
 */
@Service
class CustomerEventService(
    private val customerEventRepository: CustomerEventRepository,
    private val tenantRepository: org.wahlen.voucherengine.persistence.repository.TenantRepository
) {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * Log a customer event with denormalized snapshot data.
     * 
     * Runs in the caller's transaction (@Transactional with REQUIRED propagation).
     * Event is saved atomically with the business operation.
     * 
     * Event types cover both success and failure:
     * - "customer.redemption.succeeded" - logged when redemption commits
     * - "customer.redemption.failed" - logged by caller before rolling back
     * 
     * The try-catch ensures event logging bugs don't break business logic.
     *
     * Creates an immutable event record with:
     * - Customer/campaign IDs for indexing (NOT foreign keys)  
     * - Denormalized snapshot in data field (customer, campaign, voucher details)
     * - Event-specific fields (amount, tracking_id, etc.)
     *
     * Events survive deletion of referenced entities.
     */
    @Transactional
    fun logEvent(
        tenantName: String,
        customer: Customer?,
        eventType: String,
        category: EventCategory? = null,
        campaign: Campaign? = null,
        data: Map<String, Any?> = emptyMap(),
        groupId: String? = null,
        eventSource: String = "API"
    ) {
        if (customer == null) return
        
        try {
            // Create denormalized snapshot
            val enrichedData = data.toMutableMap()
            
            // Add customer snapshot
            enrichedData["customer"] = mapOf(
                "id" to customer.id?.toString(),
                "email" to customer.email,
                "source_id" to customer.sourceId,
                "name" to customer.name
            )
            
            // Add campaign snapshot if present
            campaign?.let {
                enrichedData["campaign"] = mapOf(
                    "id" to it.id?.toString(),
                    "name" to it.name,
                    "type" to it.type?.name
                )
            }
            
            val event = CustomerEvent(
                eventType = eventType,
                customerId = customer.id?.toString(),
                campaignId = campaign?.id?.toString(),
                category = category,
                data = enrichedData,
                groupId = groupId,
                eventSource = eventSource,
                tenant = customer.tenant!! // Tenant relationship must be managed
            )
            customerEventRepository.save(event)
            logger.debug("Logged customer event: {} for customer {}", eventType, customer.id)
        } catch (e: Exception) {
            logger.error("Failed to log customer event: {} for customer {}", eventType, customer.id, e)
            // Don't rethrow - event logging should never break business logic
        }
    }
    
    /**
     * Log customer.segment.entered event when a customer joins a segment.
     */
    @Transactional
    fun logSegmentEntered(
        tenantName: String,
        customerId: UUID,
        segmentId: UUID,
        segmentName: String
    ) {
        try {
            val tenant = tenantRepository.findByName(tenantName)
                ?: throw IllegalArgumentException("Tenant not found: $tenantName")
            
            val event = CustomerEvent(
                eventType = "customer.segment.entered",
                customerId = customerId.toString(),
                campaignId = null,
                category = EventCategory.EFFECT,
                data = mapOf(
                    "segment" to mapOf(
                        "id" to segmentId.toString(),
                        "name" to segmentName
                    )
                ),
                groupId = null,
                eventSource = "API",
                tenant = tenant
            )
            customerEventRepository.save(event)
            customerEventRepository.flush()
            logger.debug("Logged customer.segment.entered for customer: {}, segment: {}", customerId, segmentId)
        } catch (e: Exception) {
            logger.error("Failed to log customer.segment.entered event", e)
        }
    }
    
    /**
     * Log customer.segment.left event when a customer leaves a segment.
     */
    @Transactional
    fun logSegmentLeft(
        tenantName: String,
        customerId: UUID,
        segmentId: UUID,
        segmentName: String
    ) {
        try {
            val tenant = tenantRepository.findByName(tenantName)
                ?: throw IllegalArgumentException("Tenant not found: $tenantName")
            
            val event = CustomerEvent(
                eventType = "customer.segment.left",
                customerId = customerId.toString(),
                campaignId = null,
                category = EventCategory.EFFECT,
                data = mapOf(
                    "segment" to mapOf(
                        "id" to segmentId.toString(),
                        "name" to segmentName
                    )
                ),
                groupId = null,
                eventSource = "API",
                tenant = tenant
            )
            customerEventRepository.save(event)
            logger.debug("Logged customer.segment.left for customer: {}, segment: {}", customerId, segmentId)
        } catch (e: Exception) {
            logger.error("Failed to log customer.segment.left event", e)
        }
    }
    
    @Transactional(readOnly = true)
    fun listCustomerActivity(
        tenantName: String,
        customerId: UUID,
        eventTypes: List<String>? = null,
        campaignId: UUID? = null,
        category: EventCategory? = null,
        startDate: Instant? = null,
        endDate: Instant? = null,
        pageable: Pageable
    ): Page<CustomerEvent> {
        val customerIdStr = customerId.toString()
        val campaignIdStr = campaignId?.toString()
        
        return when {
            eventTypes != null && eventTypes.isNotEmpty() -> 
                customerEventRepository.findAllByCustomerIdAndTenant_NameAndEventTypeIn(
                    customerIdStr, tenantName, eventTypes, pageable
                )
            
            campaignIdStr != null -> 
                customerEventRepository.findAllByCustomerIdAndTenant_NameAndCampaignId(
                    customerIdStr, tenantName, campaignIdStr, pageable
                )
            
            startDate != null && endDate != null -> 
                customerEventRepository.findAllByCustomerIdAndTenant_NameAndCreatedAtBetween(
                    customerIdStr, tenantName, startDate, endDate, pageable
                )
            
            else -> 
                customerEventRepository.findAllByCustomerIdAndTenant_Name(
                    customerIdStr, tenantName, pageable
                )
        }
    }
}
