package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.wahlen.voucherengine.api.dto.request.CampaignCreateRequest
import org.wahlen.voucherengine.api.dto.request.VoucherCreateRequest
import org.wahlen.voucherengine.api.dto.response.CampaignsListResponse
import org.wahlen.voucherengine.api.dto.response.CampaignResponse
import org.wahlen.voucherengine.api.dto.response.VoucherResponse
import org.wahlen.voucherengine.persistence.model.campaign.Campaign
import org.wahlen.voucherengine.service.CampaignService
import org.wahlen.voucherengine.service.VoucherService
import java.util.UUID

@RestController
@RequestMapping("/v1")
@Validated
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    ]
)
class CampaignController(
    private val campaignService: CampaignService,
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
    fun createCampaign(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody body: CampaignCreateRequest
    ): ResponseEntity<CampaignResponse> {
        val saved = campaignService.create(tenant, body)
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
    fun listCampaigns(
        @RequestHeader("tenant") tenant: String,
        @Parameter(description = "Max number of items per page", example = "10")
        @RequestParam(required = false, defaultValue = "10") limit: Int,
        @Parameter(description = "1-based page index", example = "1")
        @RequestParam(required = false, defaultValue = "1") page: Int
    ): ResponseEntity<CampaignsListResponse> {
        val cappedLimit = limit.coerceIn(1, 100)
        val pageable = org.springframework.data.domain.PageRequest.of(
            (page - 1).coerceAtLeast(0),
            cappedLimit,
            org.springframework.data.domain.Sort.by("createdAt").descending()
        )
        val campaigns = campaignService.list(tenant, pageable)
        return ResponseEntity.ok(
            CampaignsListResponse(
                campaigns = campaigns.content.map(::toResponse),
                total = campaigns.totalElements.toInt()
            )
        )
    }

    @Operation(
        summary = "Get campaign",
        operationId = "getCampaign",
        responses = [
            ApiResponse(responseCode = "200", description = "Campaign found"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @GetMapping("/campaigns/{id}")
    fun getCampaign(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID
    ): ResponseEntity<CampaignResponse> {
        val campaign = campaignService.get(tenant, id) ?: return ResponseEntity.notFound().build()
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
    fun updateCampaign(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID,
        @Valid @RequestBody body: CampaignCreateRequest
    ): ResponseEntity<CampaignResponse> {
        val updated = campaignService.update(tenant, id, body) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(toResponse(updated))
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
    fun deleteCampaign(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        return if (campaignService.delete(tenant, id)) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
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
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID,
        @Valid @RequestBody body: VoucherCreateRequest
    ): ResponseEntity<VoucherResponse> {
        val campaign = campaignService.get(tenant, id) ?: return ResponseEntity.notFound().build()
        val voucher = voucherService.createVoucher(tenant, body.copy(campaign_id = campaign.id))
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
    fun listCampaignVouchers(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID
    ): ResponseEntity<List<VoucherResponse>> {
        val campaign = campaignService.get(tenant, id) ?: return ResponseEntity.notFound().build()
        val vouchers = voucherService.listVouchersByCampaign(tenant, campaign.id!!)
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
