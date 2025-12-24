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
@Tag(name = "Templates", description = "Campaign template management API")
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden"),
        ApiResponse(responseCode = "501", description = "Not implemented - campaign templates not yet supported")
    ]
)
class TemplateController {

    @Operation(
        summary = "List campaign templates",
        operationId = "listCampaignTemplates",
        description = "List all available campaign templates"
    )
    @GetMapping("/templates/campaigns")
    fun listCampaignTemplates(@RequestHeader("tenant") tenant: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    @Operation(
        summary = "Get campaign template",
        operationId = "getCampaignTemplate",
        description = "Get a specific campaign template by ID"
    )
    @GetMapping("/templates/campaigns/{campaignTemplateId}")
    fun getCampaignTemplate(@RequestHeader("tenant") tenant: String, @PathVariable campaignTemplateId: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    @Operation(
        summary = "Get campaign setup from template",
        operationId = "getCampaignSetupFromTemplate",
        description = "Get campaign setup configuration from a template"
    )
    @GetMapping("/templates/campaigns/{campaignTemplateId}/campaign-setup")
    fun getCampaignSetupFromTemplate(@RequestHeader("tenant") tenant: String, @PathVariable campaignTemplateId: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    @Operation(
        summary = "Get tier setup from template",
        operationId = "getTierSetupFromTemplate",
        description = "Get tier setup configuration from a template"
    )
    @GetMapping("/templates/campaigns/{campaignTemplateId}/tier-setup")
    fun getTierSetupFromTemplate(@RequestHeader("tenant") tenant: String, @PathVariable campaignTemplateId: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    private fun notImplemented(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Campaign templates not yet implemented"))
    }
}
