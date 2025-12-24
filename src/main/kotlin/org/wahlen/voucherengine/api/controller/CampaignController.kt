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
import org.wahlen.voucherengine.service.async.VoucherJobService
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
    private val voucherService: VoucherService,
    private val voucherJobService: VoucherJobService
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
        @Valid @RequestBody body: VoucherCreateRequest,
        @Parameter(description = "Number of vouchers to create for bulk generation")
        @RequestParam(required = false) vouchers_count: Int?
    ): ResponseEntity<out Any> {
        val campaign = campaignService.get(tenant, id) ?: return ResponseEntity.notFound().build()
        
        // If vouchers_count > 1, start async bulk generation
        if (vouchers_count != null && vouchers_count > 1) {
            val jobId = voucherJobService.publishCampaignVoucherGeneration(
                tenant,
                campaign.id!!,
                body,
                vouchers_count
            )
            
            return ResponseEntity.accepted().body(
                org.wahlen.voucherengine.api.dto.response.AsyncActionResponse(
                    async_action_id = jobId.toString(),
                    status = "ACCEPTED"
                )
            )
        }
        
        val voucher = voucherService.createVoucher(tenant, body.copy(campaign_id = campaign.id))
        return ResponseEntity.status(HttpStatus.CREATED).body(voucherService.toVoucherResponse(voucher))
    }

    @Operation(
        summary = "Add voucher with specific code to campaign",
        operationId = "addVoucherWithCodeToCampaign",
        responses = [
            ApiResponse(responseCode = "200", description = "Voucher created with specified code"),
            ApiResponse(responseCode = "404", description = "Campaign not found"),
            ApiResponse(responseCode = "400", description = "Validation error or code already exists")
        ]
    )
    @PostMapping("/campaigns/{id}/vouchers/{code}")
    fun addVoucherWithCodeToCampaign(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID,
        @PathVariable code: String,
        @Valid @RequestBody body: VoucherCreateRequest
    ): ResponseEntity<VoucherResponse> {
        val campaign = campaignService.get(tenant, id) ?: return ResponseEntity.notFound().build()
        
        // Force the code and campaign from path parameters
        val voucher = voucherService.createVoucher(tenant, body.copy(code = code, campaign_id = campaign.id))
        return ResponseEntity.ok(voucherService.toVoucherResponse(voucher))
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

    @Operation(
        summary = "Import vouchers to campaign",
        operationId = "importVouchersToCampaign",
        responses = [
            ApiResponse(responseCode = "202", description = "Import job accepted"),
            ApiResponse(responseCode = "404", description = "Campaign not found")
        ]
    )
    @PostMapping("/campaigns/{id}/import")
    fun importVouchersToCampaign(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID,
        @Valid @RequestBody request: org.wahlen.voucherengine.api.dto.request.VoucherImportRequest
    ): ResponseEntity<org.wahlen.voucherengine.api.dto.response.AsyncActionResponse> {
        val campaign = campaignService.get(tenant, id) ?: return ResponseEntity.notFound().build()
        
        // Add campaign_id to all vouchers in the import
        val vouchersWithCampaign = request.vouchers.map { it.copy(campaign_id = campaign.id) }
        val modifiedRequest = request.copy(vouchers = vouchersWithCampaign)
        
        val jobId = voucherJobService.publishVoucherImport(tenant, modifiedRequest)
        
        return ResponseEntity.accepted().body(
            org.wahlen.voucherengine.api.dto.response.AsyncActionResponse(
                async_action_id = jobId.toString(),
                status = "ACCEPTED"
            )
        )
    }

    @Operation(
        summary = "Import vouchers to campaign from CSV",
        operationId = "importVouchersToCampaignCSV",
        responses = [
            ApiResponse(responseCode = "202", description = "Import job accepted"),
            ApiResponse(responseCode = "404", description = "Campaign not found"),
            ApiResponse(responseCode = "400", description = "Invalid CSV format")
        ]
    )
    @PostMapping("/campaigns/{id}/importCSV", consumes = ["text/csv"])
    fun importVouchersToCampaignCSV(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID,
        @RequestBody csvContent: String
    ): ResponseEntity<org.wahlen.voucherengine.api.dto.response.AsyncActionResponse> {
        val campaign = campaignService.get(tenant, id) ?: return ResponseEntity.notFound().build()
        
        val vouchers = org.wahlen.voucherengine.util.CsvVoucherParser.parseCsv(csvContent)
        val vouchersWithCampaign = vouchers.map { it.copy(campaign_id = campaign.id) }
        val request = org.wahlen.voucherengine.api.dto.request.VoucherImportRequest(vouchers = vouchersWithCampaign)
        val jobId = voucherJobService.publishVoucherImport(tenant, request)
        
        return ResponseEntity.accepted().body(
            org.wahlen.voucherengine.api.dto.response.AsyncActionResponse(
                async_action_id = jobId.toString(),
                status = "ACCEPTED"
            )
        )
    }

    @Operation(
        summary = "Enable campaign",
        operationId = "enableCampaign",
        responses = [
            ApiResponse(responseCode = "200", description = "Campaign enabled"),
            ApiResponse(responseCode = "404", description = "Campaign not found")
        ]
    )
    @PostMapping("/campaigns/{id}/enable")
    fun enableCampaign(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID
    ): ResponseEntity<CampaignResponse> {
        val updated = campaignService.setActive(tenant, id, true) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(toResponse(updated))
    }

    @Operation(
        summary = "Disable campaign",
        operationId = "disableCampaign",
        responses = [
            ApiResponse(responseCode = "200", description = "Campaign disabled"),
            ApiResponse(responseCode = "404", description = "Campaign not found")
        ]
    )
    @PostMapping("/campaigns/{id}/disable")
    fun disableCampaign(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID
    ): ResponseEntity<CampaignResponse> {
        val updated = campaignService.setActive(tenant, id, false) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(toResponse(updated))
    }

    @Operation(
        summary = "Get campaign summary",
        operationId = "getCampaignSummary",
        responses = [
            ApiResponse(responseCode = "200", description = "Campaign summary retrieved"),
            ApiResponse(responseCode = "404", description = "Campaign not found")
        ]
    )
    @GetMapping("/campaigns/{id}/summary")
    fun getCampaignSummary(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID
    ): ResponseEntity<Map<String, Any>> {
        val summary = campaignService.getSummary(tenant, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(summary)
    }

    @Operation(
        summary = "List campaign transactions",
        operationId = "listCampaignTransactions",
        responses = [
            ApiResponse(responseCode = "200", description = "Campaign transactions retrieved"),
            ApiResponse(responseCode = "404", description = "Campaign not found")
        ]
    )
    @GetMapping("/campaigns/{id}/transactions")
    fun listCampaignTransactions(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID,
        @Parameter(description = "Max number of items per page", example = "10")
        @RequestParam(required = false, defaultValue = "10") limit: Int,
        @Parameter(description = "1-based page index", example = "1")
        @RequestParam(required = false, defaultValue = "1") page: Int
    ): ResponseEntity<Map<String, Any>> {
        val campaign = campaignService.get(tenant, id) ?: return ResponseEntity.notFound().build()
        
        val cappedLimit = limit.coerceIn(1, 100)
        val pageable = org.springframework.data.domain.PageRequest.of(
            (page - 1).coerceAtLeast(0),
            cappedLimit,
            org.springframework.data.domain.Sort.by("createdAt").descending()
        )
        
        val vouchers = voucherService.listVouchersByCampaign(tenant, campaign.id!!)
        val voucherIds = vouchers.mapNotNull { it.id }
        
        val transactions = if (voucherIds.isNotEmpty()) {
            voucherService.listCampaignTransactions(tenant, voucherIds, pageable)
        } else {
            org.springframework.data.domain.PageImpl(
                emptyList<org.wahlen.voucherengine.persistence.model.voucher.VoucherTransaction>(),
                pageable,
                0
            )
        }
        
        return ResponseEntity.ok(
            mapOf(
                "data" to transactions.content.map { voucherService.toTransactionResponse(it) },
                "has_more" to transactions.hasNext(),
                "total" to transactions.totalElements.toInt()
            )
        )
    }

    @Operation(
        summary = "Export campaign transactions",
        operationId = "exportCampaignTransactions",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - use GET /campaigns/{id}/transactions")
        ]
    )
    @PostMapping("/campaigns/{id}/transactions/export")
    fun exportCampaignTransactions(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Transaction export not implemented. Use GET /campaigns/{id}/transactions with pagination."))
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
