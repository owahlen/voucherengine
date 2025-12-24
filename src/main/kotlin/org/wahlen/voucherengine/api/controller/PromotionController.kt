package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1")
@Validated
@Tag(name = "Promotions", description = "Promotion tiers and stacks management API")
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden"),
        ApiResponse(responseCode = "501", description = "Not implemented - promotions not yet supported")
    ]
)
class PromotionController {

    // Promotion Tiers (Global)

    @Operation(summary = "List promotion tiers", operationId = "listPromotionTiers")
    @GetMapping("/promotions/tiers")
    fun listPromotionTiers(@RequestHeader("tenant") tenant: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    @Operation(summary = "Get promotion tier", operationId = "getPromotionTier")
    @GetMapping("/promotions/tiers/{promotionTierId}")
    fun getPromotionTier(@RequestHeader("tenant") tenant: String, @PathVariable promotionTierId: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    @Operation(summary = "Enable promotion tier", operationId = "enablePromotionTier")
    @PostMapping("/promotions/tiers/{promotionTierId}/enable")
    fun enablePromotionTier(@RequestHeader("tenant") tenant: String, @PathVariable promotionTierId: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    @Operation(summary = "Disable promotion tier", operationId = "disablePromotionTier")
    @PostMapping("/promotions/tiers/{promotionTierId}/disable")
    fun disablePromotionTier(@RequestHeader("tenant") tenant: String, @PathVariable promotionTierId: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    // Promotion Stacks (Global)

    @Operation(summary = "List promotion stacks", operationId = "listPromotionStacks")
    @GetMapping("/promotions/stacks")
    fun listPromotionStacks(@RequestHeader("tenant") tenant: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    // Campaign-scoped Promotion Tiers

    @Operation(summary = "List campaign promotion tiers", operationId = "listCampaignPromotionTiers")
    @GetMapping("/promotions/{campaignId}/tiers")
    fun listCampaignPromotionTiers(@RequestHeader("tenant") tenant: String, @PathVariable campaignId: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    // Campaign-scoped Promotion Stacks

    @Operation(summary = "List campaign promotion stacks", operationId = "listCampaignPromotionStacks")
    @GetMapping("/promotions/{campaignId}/stacks")
    fun listCampaignPromotionStacks(@RequestHeader("tenant") tenant: String, @PathVariable campaignId: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    @Operation(summary = "Get campaign promotion stack", operationId = "getCampaignPromotionStack")
    @GetMapping("/promotions/{campaignId}/stacks/{stackId}")
    fun getCampaignPromotionStack(@RequestHeader("tenant") tenant: String, @PathVariable campaignId: String, @PathVariable stackId: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    private fun notImplemented(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Promotions not yet implemented"))
    }
}
