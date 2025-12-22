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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.wahlen.voucherengine.api.dto.request.RollbackRequest
import org.wahlen.voucherengine.api.dto.response.RedemptionDetailResponse
import org.wahlen.voucherengine.api.dto.response.RedemptionRollbackResponse
import org.wahlen.voucherengine.persistence.repository.RedemptionRepository
import org.wahlen.voucherengine.persistence.repository.RedemptionRollbackRepository
import org.wahlen.voucherengine.service.VoucherService
import java.util.UUID

@RestController
@RequestMapping("/v1")
@Validated
class RedemptionController(
    private val redemptionRepository: RedemptionRepository,
    private val redemptionRollbackRepository: RedemptionRollbackRepository,
    private val voucherService: VoucherService
) {

    @Operation(
        summary = "List redemptions",
        operationId = "listRedemptions",
        responses = [ApiResponse(responseCode = "200", description = "List of redemptions")]
    )
    @GetMapping("/redemptions")
    fun listRedemptions(): ResponseEntity<List<RedemptionDetailResponse>> =
        ResponseEntity.ok(
            redemptionRepository.findAll().map {
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
    fun getRedemption(@PathVariable id: UUID): ResponseEntity<RedemptionDetailResponse> {
        val redemption = redemptionRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
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
        @PathVariable id: UUID,
        @Valid @RequestBody body: RollbackRequest
    ): ResponseEntity<RedemptionRollbackResponse> {
        val rollback = voucherService.rollbackRedemption(id, body) ?: return ResponseEntity.notFound().build()
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
