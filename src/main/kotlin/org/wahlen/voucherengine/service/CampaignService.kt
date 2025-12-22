package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.CampaignCreateRequest
import org.wahlen.voucherengine.persistence.model.campaign.Campaign
import org.wahlen.voucherengine.persistence.repository.CampaignRepository
import java.util.UUID

@Service
class CampaignService(
    private val campaignRepository: CampaignRepository,
    private val tenantService: TenantService
) {

    @Transactional
    fun create(tenantName: String, request: CampaignCreateRequest): Campaign {
        val tenant = tenantService.requireTenant(tenantName)
        val campaign = Campaign(
            name = request.name,
            type = request.type,
            mode = request.mode,
            description = request.description,
            codePattern = request.code_pattern,
            startDate = request.start_date,
            expirationDate = request.expiration_date,
            metadata = request.metadata,
            active = request.active ?: true
        )
        campaign.tenant = tenant
        return campaignRepository.save(campaign)
    }

    @Transactional(readOnly = true)
    fun list(tenantName: String): List<Campaign> = campaignRepository.findAllByTenantName(tenantName)

    @Transactional(readOnly = true)
    fun get(tenantName: String, id: UUID): Campaign? = campaignRepository.findByIdAndTenantName(id, tenantName)

    @Transactional
    fun update(tenantName: String, id: UUID, request: CampaignCreateRequest): Campaign? {
        val existing = campaignRepository.findByIdAndTenantName(id, tenantName) ?: return null
        existing.name = request.name ?: existing.name
        existing.type = request.type ?: existing.type
        existing.mode = request.mode ?: existing.mode
        existing.description = request.description ?: existing.description
        existing.codePattern = request.code_pattern ?: existing.codePattern
        existing.startDate = request.start_date ?: existing.startDate
        existing.expirationDate = request.expiration_date ?: existing.expirationDate
        existing.metadata = request.metadata ?: existing.metadata
        existing.active = request.active ?: existing.active
        return campaignRepository.save(existing)
    }

    @Transactional
    fun delete(tenantName: String, id: UUID): Boolean {
        val existing = campaignRepository.findByIdAndTenantName(id, tenantName) ?: return false
        campaignRepository.delete(existing)
        return true
    }
}
