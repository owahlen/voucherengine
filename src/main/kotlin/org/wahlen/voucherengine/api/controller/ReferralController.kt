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
@Tag(name = "Referrals", description = "Referral program management API")
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden"),
        ApiResponse(responseCode = "501", description = "Not implemented - referral programs not yet supported")
    ]
)
class ReferralController {

    // Campaign-scoped Referral Members

    @Operation(summary = "List referral member holders", operationId = "listReferralMemberHolders")
    @GetMapping("/referrals/{campaignId}/members/{memberId}/holders")
    fun listReferralMemberHolders(@RequestHeader("tenant") tenant: String, @PathVariable campaignId: String, @PathVariable memberId: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    @Operation(summary = "Get referral member holder", operationId = "getReferralMemberHolder")
    @GetMapping("/referrals/{campaignId}/members/{memberId}/holders/{holderId}")
    fun getReferralMemberHolder(@RequestHeader("tenant") tenant: String, @PathVariable campaignId: String, @PathVariable memberId: String, @PathVariable holderId: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    // Global Referral Members

    @Operation(summary = "List referral member holders (global)", operationId = "listReferralMemberHoldersGlobal")
    @GetMapping("/referrals/members/{memberId}/holders")
    fun listReferralMemberHoldersGlobal(@RequestHeader("tenant") tenant: String, @PathVariable memberId: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    @Operation(summary = "Get referral member holder (global)", operationId = "getReferralMemberHolderGlobal")
    @GetMapping("/referrals/members/{memberId}/holders/{holderId}")
    fun getReferralMemberHolderGlobal(@RequestHeader("tenant") tenant: String, @PathVariable memberId: String, @PathVariable holderId: String): ResponseEntity<Map<String, String>> {
        return notImplemented()
    }

    private fun notImplemented(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Referral programs not yet implemented"))
    }
}
