package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.wahlen.voucherengine.api.dto.request.*
import org.wahlen.voucherengine.api.dto.VoucherResponse
import org.wahlen.voucherengine.service.VoucherService
import org.wahlen.voucherengine.service.dto.RedemptionResponse
import org.wahlen.voucherengine.service.dto.ValidationResponse

@RestController
@RequestMapping("/v1")
@Validated
class VoucherController(
    private val voucherService: VoucherService
) {

    @Operation(
        summary = "Create a voucher (discount/gift/loyalty)",
        operationId = "createVoucher",
        responses = [
            ApiResponse(responseCode = "201", description = "Voucher created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "501", description = "Not implemented")
        ]
    )
    @PostMapping("/vouchers")
    fun createVoucher(@Valid @RequestBody body: VoucherCreateRequest): ResponseEntity<VoucherResponse> {
        val voucher = voucherService.createVoucher(body)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            VoucherResponse(
                id = voucher.id,
                code = voucher.code,
                type = voucher.type,
                redemption = voucher.redemptionJson
            )
        )
    }

    @Operation(
        summary = "Validate a voucher code in a checkout context",
        operationId = "validateVoucher",
        responses = [
            ApiResponse(responseCode = "200", description = "Validation result"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Voucher not found"),
            ApiResponse(responseCode = "501", description = "Not implemented")
        ]
    )
    @PostMapping("/vouchers/{code}/validate")
    fun validateVoucher(
        @PathVariable code: String,
        @Valid @RequestBody body: VoucherValidationRequest
    ): ResponseEntity<ValidationResponse> =
        ResponseEntity.ok(voucherService.validateVoucher(code, body))

    @Operation(
        summary = "Validate multiple redeemables (stackable discounts)",
        operationId = "validateStack",
        responses = [
            ApiResponse(responseCode = "200", description = "Stack validation result"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "501", description = "Not implemented")
        ]
    )
    @PostMapping("/validations")
    fun validateStack(@Valid @RequestBody body: ValidationStackRequest): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(mapOf("status" to "not_implemented"))

    @Operation(
        summary = "Redeem voucher(s) with order context",
        operationId = "redeem",
        responses = [
            ApiResponse(responseCode = "200", description = "Redemption result"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Voucher not found"),
            ApiResponse(responseCode = "501", description = "Not implemented")
        ]
    )
    @PostMapping("/redemptions")
    fun redeem(@Valid @RequestBody body: RedemptionRequest): ResponseEntity<RedemptionResponse> {
        val result = voucherService.redeem(body)
        val status = if (result.error == null) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(result)
    }
}
