package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.event.CustomerEvent
import java.time.Instant
import java.util.UUID

/**
 * Repository for customer event queries.
 * 
 * Note: customer_id and campaign_id are stored as strings (not FK),
 * allowing events to survive deletion of referenced entities.
 */
interface CustomerEventRepository : JpaRepository<CustomerEvent, UUID> {
    
    /**
     * Find all events for a customer in a specific tenant.
     */
    fun findAllByCustomerIdAndTenant_Name(
        customerId: String?,
        tenantName: String,
        pageable: Pageable
    ): Page<CustomerEvent>
    
    /**
     * Find events filtered by event types.
     */
    fun findAllByCustomerIdAndTenant_NameAndEventTypeIn(
        customerId: String?,
        tenantName: String,
        eventTypes: List<String>,
        pageable: Pageable
    ): Page<CustomerEvent>
    
    /**
     * Find events filtered by campaign.
     */
    fun findAllByCustomerIdAndTenant_NameAndCampaignId(
        customerId: String?,
        tenantName: String,
        campaignId: String,
        pageable: Pageable
    ): Page<CustomerEvent>
    
    /**
     * Find events within a date range.
     */
    fun findAllByCustomerIdAndTenant_NameAndCreatedAtBetween(
        customerId: String?,
        tenantName: String,
        startDate: Instant,
        endDate: Instant,
        pageable: Pageable
    ): Page<CustomerEvent>
}
