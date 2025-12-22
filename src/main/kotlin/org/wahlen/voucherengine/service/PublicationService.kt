package org.wahlen.voucherengine.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import org.wahlen.voucherengine.api.dto.request.PublicationCreateRequest
import org.wahlen.voucherengine.api.dto.response.PublicationCustomerDto
import org.wahlen.voucherengine.api.dto.response.PublicationResponse
import org.wahlen.voucherengine.api.dto.response.PublicationVoucherDto
import org.wahlen.voucherengine.persistence.model.publication.Publication
import org.wahlen.voucherengine.persistence.model.publication.PublicationResult
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import org.wahlen.voucherengine.persistence.repository.CampaignRepository
import org.wahlen.voucherengine.persistence.repository.PublicationRepository
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import org.springframework.data.domain.PageRequest
import java.time.Instant

@Service
class PublicationService(
    private val publicationRepository: PublicationRepository,
    private val voucherRepository: VoucherRepository,
    private val campaignRepository: CampaignRepository,
    private val customerService: CustomerService,
    private val tenantService: TenantService
) {

    @Transactional
    fun createPublication(tenantName: String, request: PublicationCreateRequest, joinOnce: Boolean?): PublicationResponse {
        val tenant = tenantService.requireTenant(tenantName)
        val customerRef = request.customer ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer is required")
        val customer = customerService.ensureCustomer(tenantName, customerRef)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer is required")

        if (!request.voucher.isNullOrBlank()) {
            return createPublicationForVoucher(tenantName, tenant, request, customer, joinOnce == true)
        }
        if (request.campaign != null) {
            return createPublicationForCampaign(tenantName, tenant, request, customer, joinOnce == true)
        }
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign or voucher is required")
    }

    @Transactional(readOnly = true)
    fun listPublications(
        tenantName: String,
        campaignName: String?,
        customerId: String?,
        voucherCode: String?,
        result: String?,
        sourceId: String?
    ): List<Publication> {
        val normalizedResult = result?.trim()?.uppercase()
        val items = when {
            !campaignName.isNullOrBlank() -> publicationRepository.findAllByTenantNameAndCampaignName(tenantName, campaignName)
            !customerId.isNullOrBlank() -> publicationRepository.findAllByTenantNameAndCustomerId(
                tenantName,
                java.util.UUID.fromString(customerId)
            )
            !voucherCode.isNullOrBlank() -> publicationRepository.findAllByTenantNameAndVoucherCode(tenantName, voucherCode)
            !normalizedResult.isNullOrBlank() -> publicationRepository.findAllByTenantNameAndResult(
                tenantName,
                PublicationResult.valueOf(normalizedResult)
            )
            !sourceId.isNullOrBlank() -> publicationRepository.findAllByTenantNameAndSourceId(tenantName, sourceId)
            else -> publicationRepository.findAllByTenantName(tenantName)
        }
        return items.sortedByDescending { it.createdAt }
    }

    private fun createPublicationForVoucher(
        tenantName: String,
        tenant: org.wahlen.voucherengine.persistence.model.tenant.Tenant,
        request: PublicationCreateRequest,
        customer: org.wahlen.voucherengine.persistence.model.customer.Customer,
        joinOnce: Boolean
    ): PublicationResponse {
        val voucher = voucherRepository.findByCodeAndTenantName(request.voucher!!, tenantName)
            ?: return failurePublication(tenant, customer, request, null, "voucher_not_found", "Voucher not found")

        if (joinOnce && voucher.id != null) {
            publicationRepository.findByTenantNameAndVoucherIdAndCustomerId(tenantName, voucher.id!!, customer.id!!)
                ?.let { return toResponse(it) }
        }
        val now = Instant.now()
        voucherSuitabilityFailure(voucher, now)?.let { (code, message) ->
            return failurePublication(tenant, customer, request, voucher.campaign, code, message)
        }
        if (voucher.holder != null) {
            return failurePublication(tenant, customer, request, voucher.campaign, "voucher_already_published", "Voucher has already been published")
        }

        voucher.holder = customer
        voucherRepository.save(voucher)

        val publication = Publication(
            voucher = voucher,
            customer = customer,
            campaign = voucher.campaign,
            result = PublicationResult.SUCCESS,
            sourceId = request.source_id,
            channel = request.channel ?: "API",
            metadata = request.metadata
        )
        publication.tenant = tenant
        return toResponse(publicationRepository.save(publication))
    }

    private fun createPublicationForCampaign(
        tenantName: String,
        tenant: org.wahlen.voucherengine.persistence.model.tenant.Tenant,
        request: PublicationCreateRequest,
        customer: org.wahlen.voucherengine.persistence.model.customer.Customer,
        joinOnce: Boolean
    ): PublicationResponse {
        val campaignName = request.campaign?.name?.trim().orEmpty()
        if (campaignName.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign name is required")
        }
        val campaign = campaignRepository.findByNameAndTenantName(campaignName, tenantName)
            ?: return failurePublication(tenant, customer, request, null, "campaign_not_found", "Campaign not found")

        if (joinOnce) {
            publicationRepository.findByTenantNameAndCampaignIdAndCustomerId(tenantName, campaign.id!!, customer.id!!)
                ?.let { return toResponse(it) }
            val existing = voucherRepository.findAllByCampaignIdAndTenantNameAndHolderId(campaign.id!!, tenantName, customer.id!!)
            if (existing.isNotEmpty()) {
                val publication = Publication(
                    voucher = existing.first(),
                    customer = customer,
                    campaign = campaign,
                    result = PublicationResult.SUCCESS,
                    sourceId = request.source_id,
                    channel = request.channel ?: "API",
                    metadata = request.metadata
                )
                publication.tenant = tenant
                return toResponse(publicationRepository.save(publication))
            }
        }

        val count = request.campaign?.count ?: 1
        if (count < 1 || count > 20) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "campaign.count must be between 1 and 20")
        }
        val now = Instant.now()
        val vouchers = voucherRepository.findAllSuitableForPublication(
            campaign.id!!,
            tenantName,
            now,
            PageRequest.of(0, count)
        )
        if (vouchers.size < count) {
            return failurePublication(tenant, customer, request, campaign, "voucher_not_found", "No available vouchers for campaign")
        }
        vouchers.forEach {
            it.holder = customer
        }
        voucherRepository.saveAll(vouchers)

        val publication = Publication(
            voucher = if (count == 1) vouchers.first() else null,
            customer = customer,
            campaign = campaign,
            result = PublicationResult.SUCCESS,
            sourceId = request.source_id,
            channel = request.channel ?: "API",
            metadata = request.metadata
        )
        if (count > 1) {
            publication.vouchers.addAll(vouchers)
        }
        publication.tenant = tenant
        return toResponse(publicationRepository.save(publication))
    }

    private fun failurePublication(
        tenant: org.wahlen.voucherengine.persistence.model.tenant.Tenant,
        customer: org.wahlen.voucherengine.persistence.model.customer.Customer,
        request: PublicationCreateRequest,
        campaign: org.wahlen.voucherengine.persistence.model.campaign.Campaign?,
        code: String,
        message: String
    ): PublicationResponse {
        val publication = Publication(
            customer = customer,
            campaign = campaign,
            result = PublicationResult.FAILURE,
            failureCode = code,
            failureMessage = message,
            sourceId = request.source_id,
            channel = request.channel ?: "API",
            metadata = request.metadata
        )
        publication.tenant = tenant
        return toResponse(publicationRepository.save(publication))
    }

    fun toResponse(publication: Publication): PublicationResponse {
        val customer = publication.customer
        val voucher = publication.voucher
        val vouchers = publication.vouchers
        val voucherCodes = vouchers.mapNotNull { it.code }.ifEmpty { voucher?.code?.let { listOf(it) } ?: emptyList() }
        val voucherIds = vouchers.mapNotNull { it.id }.ifEmpty { voucher?.id?.let { listOf(it) } ?: emptyList() }
        return PublicationResponse(
            id = publication.id,
            created_at = publication.createdAt,
            customer_id = customer?.id,
            tracking_id = customer?.sourceId,
            metadata = publication.metadata,
            channel = publication.channel,
            source_id = publication.sourceId,
            result = publication.result?.name,
            failure_code = publication.failureCode,
            failure_message = publication.failureMessage,
            customer = PublicationCustomerDto(
                id = customer?.id,
                name = customer?.name,
                email = customer?.email,
                source_id = customer?.sourceId,
                metadata = customer?.metadata
            ),
            voucher = if (vouchers.isEmpty()) voucher?.let { toVoucherDto(it) } else null,
            vouchers = if (vouchers.isNotEmpty() && publication.result == PublicationResult.SUCCESS) voucherCodes else null,
            vouchers_id = if (voucherIds.isNotEmpty() && publication.result == PublicationResult.SUCCESS) voucherIds else null
        )
    }

    private fun toVoucherDto(voucher: Voucher): PublicationVoucherDto =
        PublicationVoucherDto(
            id = voucher.id,
            code = voucher.code,
            campaign = voucher.campaign?.name,
            campaign_id = voucher.campaign?.id,
            type = voucher.type?.name,
            discount = voucher.discountJson,
            gift = voucher.giftJson,
            loyalty_card = voucher.loyaltyCardJson,
            start_date = voucher.startDate,
            expiration_date = voucher.expirationDate,
            validity_timeframe = voucher.validityTimeframe,
            validity_day_of_week = voucher.validityDayOfWeek,
            validity_hours = voucher.validityHours,
            active = voucher.active,
            additional_info = voucher.additionalInfo,
            metadata = voucher.metadata,
            assets = voucher.assets?.let {
                org.wahlen.voucherengine.api.dto.response.VoucherAssetsDto(
                    qr = org.wahlen.voucherengine.api.dto.response.AssetDto(id = it.qrId, url = it.qrUrl),
                    barcode = org.wahlen.voucherengine.api.dto.response.AssetDto(id = it.barcodeId, url = it.barcodeUrl)
                )
            },
            is_referral_code = false
        )

    private fun voucherSuitabilityFailure(voucher: Voucher, now: Instant): Pair<String, String>? {
        if (voucher.active == false) {
            return "voucher_disabled" to "Voucher is disabled"
        }
        if (voucher.startDate != null && now.isBefore(voucher.startDate)) {
            return "voucher_not_active" to "Voucher is not yet active"
        }
        if (voucher.expirationDate != null && now.isAfter(voucher.expirationDate)) {
            return "voucher_expired" to "Voucher has expired"
        }
        return null
    }

    @Transactional
    fun deletePublication(tenantName: String, id: java.util.UUID): Boolean {
        val publication = publicationRepository.findByIdAndTenantName(id, tenantName) ?: return false
        publicationRepository.delete(publication)
        return true
    }
}
