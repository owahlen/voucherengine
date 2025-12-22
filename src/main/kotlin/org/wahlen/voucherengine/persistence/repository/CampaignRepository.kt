package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.campaign.Campaign
import java.util.UUID

interface CampaignRepository : JpaRepository<Campaign, UUID> {
    fun findByIdAndTenantName(id: UUID, tenantName: String): Campaign?
    fun findAllByTenantName(tenantName: String): List<Campaign>
    fun findByNameAndTenantName(name: String, tenantName: String): Campaign?
}
