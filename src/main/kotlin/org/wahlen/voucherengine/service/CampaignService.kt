package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.CampaignCreateRequest
import org.wahlen.voucherengine.persistence.model.campaign.Campaign
import org.wahlen.voucherengine.persistence.repository.CampaignRepository
import org.wahlen.voucherengine.persistence.repository.PublicationRepository
import org.wahlen.voucherengine.persistence.repository.RedemptionRepository
import org.wahlen.voucherengine.persistence.repository.ValidationRulesAssignmentRepository
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import java.util.UUID

@Service
class CampaignService(
    private val campaignRepository: CampaignRepository,
    private val voucherRepository: VoucherRepository,
    private val publicationRepository: PublicationRepository,
    private val redemptionRepository: RedemptionRepository,
    private val validationRulesAssignmentRepository: ValidationRulesAssignmentRepository,
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
        val vouchers = voucherRepository.findAllByCampaignIdAndTenantName(id, tenantName)
        val voucherIds = vouchers.mapNotNull { it.id }
        val voucherCodes = vouchers.mapNotNull { it.code }
        val campaignIdentifiers = listOfNotNull(existing.id?.toString(), existing.name).distinct()

        if (campaignIdentifiers.isNotEmpty()) {
            val campaignAssignments = validationRulesAssignmentRepository
                .findAllByTenantNameAndRelatedObjectTypeAndRelatedObjectIdIn(tenantName, "campaign", campaignIdentifiers)
            if (campaignAssignments.isNotEmpty()) {
                validationRulesAssignmentRepository.deleteAll(campaignAssignments)
            }
        }

        if (voucherIds.isNotEmpty()) {
            val voucherIdAssignments = validationRulesAssignmentRepository
                .findAllByTenantNameAndRelatedObjectTypeAndRelatedObjectIdIn(
                    tenantName,
                    "voucher",
                    voucherIds.map(UUID::toString)
                )
            if (voucherIdAssignments.isNotEmpty()) {
                validationRulesAssignmentRepository.deleteAll(voucherIdAssignments)
            }
        }
        if (voucherCodes.isNotEmpty()) {
            val voucherCodeAssignments = validationRulesAssignmentRepository
                .findAllByTenantNameAndRelatedObjectTypeAndRelatedObjectIdIn(tenantName, "voucher", voucherCodes)
            if (voucherCodeAssignments.isNotEmpty()) {
                validationRulesAssignmentRepository.deleteAll(voucherCodeAssignments)
            }
        }

        val publications = publicationRepository.findAllByTenantNameAndCampaignId(tenantName, id)
        if (publications.isNotEmpty()) {
            publicationRepository.deleteAll(publications)
        }

        if (voucherIds.isNotEmpty()) {
            val redemptions = redemptionRepository.findAllByTenantNameAndVoucherIdIn(tenantName, voucherIds)
            if (redemptions.isNotEmpty()) {
                redemptionRepository.deleteAll(redemptions)
            }
        }

        if (vouchers.isNotEmpty()) {
            voucherRepository.deleteAll(vouchers)
        }
        campaignRepository.delete(existing)
        return true
    }
}
