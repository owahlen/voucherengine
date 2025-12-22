package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.wahlen.voucherengine.api.dto.request.RollbackRequest
import org.wahlen.voucherengine.api.dto.response.RedemptionDetailResponse
import org.wahlen.voucherengine.api.dto.response.RedemptionRollbackResponse
import org.wahlen.voucherengine.service.RedemptionService
import org.wahlen.voucherengine.service.VoucherService
import java.util.UUID

@RestController
@RequestMapping("/v1")
@Validated
class RedemptionController(
    private val redemptionService: RedemptionService,
    private val voucherService: VoucherService
) {

    @Operation(
        summary = "List redemptions",
        operationId = "listRedemptions",
        responses = [ApiResponse(responseCode = "200", description = "List of redemptions")]
    )
    @GetMapping("/redemptions")
    fun listRedemptions(@RequestHeader("tenant") tenant: String): ResponseEntity<List<RedemptionDetailResponse>> =
        ResponseEntity.ok(
            redemptionService.list(tenant).map {
                RedemptionDetailResponse(
                    id = it.id,
                    voucher_code = it.voucher?.code,
                    customer_id = it.customer?.id,
                    amount = it.amount,
                    status = it.status?.name,
                    created_at = it.createdAt
                )
            }
        )

    @Operation(
        summary = "Get redemption by id",
        operationId = "getRedemption",
        responses = [
            ApiResponse(responseCode = "200", description = "Redemption found"),
            ApiResponse(responseCode = "404", description = "Redemption not found")
        ]
    )
    @GetMapping("/redemptions/{id}")
    fun getRedemption(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID
    ): ResponseEntity<RedemptionDetailResponse> {
        val redemption = redemptionService.get(tenant, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
            RedemptionDetailResponse(
                id = redemption.id,
                voucher_code = redemption.voucher?.code,
                customer_id = redemption.customer?.id,
                amount = redemption.amount,
                status = redemption.status?.name,
                created_at = redemption.createdAt
            )
        )
    }

    @Operation(
        summary = "Rollback a redemption",
        operationId = "rollbackRedemption",
        responses = [
            ApiResponse(responseCode = "201", description = "Rollback created"),
            ApiResponse(responseCode = "404", description = "Redemption not found")
        ]
    )
    @PostMapping("/redemptions/{id}/rollback")
    fun rollback(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID,
        @Valid @RequestBody body: RollbackRequest
    ): ResponseEntity<RedemptionRollbackResponse> {
        val rollback = voucherService.rollbackRedemption(tenant, id, body) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.status(HttpStatus.CREATED).body(
            RedemptionRollbackResponse(
                id = rollback.id,
                redemption_id = rollback.redemptionId,
                reason = rollback.reason,
                amount = rollback.amount,
                created_at = rollback.date
            )
        )
    }
}
