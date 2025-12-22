package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.wahlen.voucherengine.api.dto.request.CampaignCreateRequest
import org.wahlen.voucherengine.api.dto.request.VoucherCreateRequest
import org.wahlen.voucherengine.api.dto.response.CampaignResponse
import org.wahlen.voucherengine.api.dto.response.VoucherResponse
import org.wahlen.voucherengine.persistence.model.campaign.Campaign
import org.wahlen.voucherengine.persistence.repository.CampaignRepository
import org.wahlen.voucherengine.service.VoucherService
import java.util.UUID

@RestController
@RequestMapping("/v1")
@Validated
class CampaignController(
    private val campaignRepository: CampaignRepository,
    private val voucherService: VoucherService
) {

    @Operation(
        summary = "Create campaign",
        operationId = "createCampaign",
        responses = [
            ApiResponse(responseCode = "201", description = "Campaign created"),
            ApiResponse(responseCode = "400", description = "Validation error")
        ]
    )
    @PostMapping("/campaigns")
    fun createCampaign(@Valid @RequestBody body: CampaignCreateRequest): ResponseEntity<CampaignResponse> {
        val saved = campaignRepository.save(
            Campaign(
                name = body.name,
                type = body.type,
                mode = body.mode,
                description = body.description,
                codePattern = body.code_pattern,
                startDate = body.start_date,
                expirationDate = body.expiration_date,
                metadata = body.metadata,
                active = body.active ?: true
            )
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved))
    }

    @Operation(
        summary = "List campaigns",
        operationId = "listCampaigns",
        responses = [
            ApiResponse(responseCode = "200", description = "List of campaigns")
        ]
    )
    @GetMapping("/campaigns")
    fun listCampaigns(): ResponseEntity<List<CampaignResponse>> =
        ResponseEntity.ok(campaignRepository.findAll().map(::toResponse))

    @Operation(
        summary = "Get campaign",
        operationId = "getCampaign",
        responses = [
            ApiResponse(responseCode = "200", description = "Campaign found"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @GetMapping("/campaigns/{id}")
    fun getCampaign(@PathVariable id: UUID): ResponseEntity<CampaignResponse> {
        val campaign = campaignRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(toResponse(campaign))
    }

    @Operation(
        summary = "Update campaign",
        operationId = "updateCampaign",
        responses = [
            ApiResponse(responseCode = "200", description = "Campaign updated"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @PutMapping("/campaigns/{id}")
    fun updateCampaign(@PathVariable id: UUID, @Valid @RequestBody body: CampaignCreateRequest): ResponseEntity<CampaignResponse> {
        val campaign = campaignRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        campaign.name = body.name ?: campaign.name
        campaign.type = body.type ?: campaign.type
        campaign.mode = body.mode ?: campaign.mode
        campaign.description = body.description ?: campaign.description
        campaign.codePattern = body.code_pattern ?: campaign.codePattern
        campaign.startDate = body.start_date ?: campaign.startDate
        campaign.expirationDate = body.expiration_date ?: campaign.expirationDate
        campaign.metadata = body.metadata ?: campaign.metadata
        campaign.active = body.active ?: campaign.active
        return ResponseEntity.ok(toResponse(campaignRepository.save(campaign)))
    }

    @Operation(
        summary = "Delete campaign",
        operationId = "deleteCampaign",
        responses = [
            ApiResponse(responseCode = "204", description = "Deleted"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @DeleteMapping("/campaigns/{id}")
    fun deleteCampaign(@PathVariable id: UUID): ResponseEntity<Void> {
        val campaign = campaignRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        campaignRepository.delete(campaign)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Create voucher within a campaign",
        operationId = "createCampaignVoucher",
        responses = [
            ApiResponse(responseCode = "201", description = "Voucher created"),
            ApiResponse(responseCode = "404", description = "Campaign not found"),
            ApiResponse(responseCode = "400", description = "Validation error")
        ]
    )
    @PostMapping("/campaigns/{id}/vouchers")
    fun createVoucherInCampaign(
        @PathVariable id: UUID,
        @Valid @RequestBody body: VoucherCreateRequest
    ): ResponseEntity<VoucherResponse> {
        val campaign = campaignRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        val voucher = voucherService.createVoucher(body.copy(campaign_id = campaign.id))
        return ResponseEntity.status(HttpStatus.CREATED).body(voucherService.toVoucherResponse(voucher))
    }

    @Operation(
        summary = "List vouchers in campaign",
        operationId = "listCampaignVouchers",
        responses = [
            ApiResponse(responseCode = "200", description = "List of vouchers for the campaign"),
            ApiResponse(responseCode = "404", description = "Campaign not found")
        ]
    )
    @GetMapping("/campaigns/{id}/vouchers")
    fun listCampaignVouchers(@PathVariable id: UUID): ResponseEntity<List<VoucherResponse>> {
        val campaign = campaignRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        val vouchers = voucherService.listVouchersByCampaign(campaign.id!!)
        return ResponseEntity.ok(vouchers.map { voucherService.toVoucherResponse(it) })
    }

    private fun toResponse(campaign: Campaign) = CampaignResponse(
        id = campaign.id,
        name = campaign.name,
        type = campaign.type,
        mode = campaign.mode,
        code_pattern = campaign.codePattern,
        start_date = campaign.startDate,
        expiration_date = campaign.expirationDate,
        metadata = campaign.metadata,
        active = campaign.active,
        description = campaign.description,
        created_at = campaign.createdAt,
        updated_at = campaign.updatedAt
    )
}
