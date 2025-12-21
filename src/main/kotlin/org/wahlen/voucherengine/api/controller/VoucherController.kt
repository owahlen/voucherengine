package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.wahlen.voucherengine.api.dto.request.*
import jakarta.validation.Valid

@RestController
@RequestMapping("/v1")
@Validated
class VoucherController {

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
    fun createVoucher(@Valid @RequestBody body: VoucherCreateRequest): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(mapOf("status" to "not_implemented"))

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
    ): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(mapOf("status" to "not_implemented"))

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
    fun redeem(@Valid @RequestBody body: RedemptionRequest): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(mapOf("status" to "not_implemented"))
}
