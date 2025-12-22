package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.wahlen.voucherengine.api.dto.request.*
import org.wahlen.voucherengine.api.dto.response.VoucherResponse
import org.wahlen.voucherengine.api.dto.response.VouchersListResponse
import org.wahlen.voucherengine.api.dto.response.VoucherAssetsDto
import org.wahlen.voucherengine.api.dto.response.AssetDto
import org.wahlen.voucherengine.service.VoucherService
import org.wahlen.voucherengine.service.dto.ErrorResponse
import org.wahlen.voucherengine.service.dto.RedemptionResponse
import org.wahlen.voucherengine.service.dto.ValidationResponse
import org.wahlen.voucherengine.service.dto.ValidationStackResponse
import org.wahlen.voucherengine.service.QrCodeService
import org.wahlen.voucherengine.service.BarcodeService

@RestController
@RequestMapping("/v1")
@Validated
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    ]
)
class VoucherController(
    private val voucherService: VoucherService,
    private val qrCodeService: QrCodeService,
    private val barcodeService: BarcodeService
) {

    @Operation(
        summary = "Create a voucher (discount/gift/loyalty)",
        operationId = "createVoucher",
        responses = [
            ApiResponse(responseCode = "201", description = "Voucher created"),
            ApiResponse(responseCode = "400", description = "Validation error")
        ]
    )
    @PostMapping("/vouchers")
    fun createVoucher(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody body: VoucherCreateRequest
    ): ResponseEntity<VoucherResponse> {
        val voucher = voucherService.createVoucher(tenant, body)
        return ResponseEntity.status(HttpStatus.CREATED).body(voucherService.toVoucherResponse(voucher))
    }

    @Operation(
        summary = "Validate a voucher code in a checkout context",
        operationId = "validateVoucher",
        responses = [
            ApiResponse(responseCode = "200", description = "Validation succeeded"),
            ApiResponse(responseCode = "400", description = "Validation failed (e.g., limit exceeded, inactive/expired, not assigned)"),
            ApiResponse(responseCode = "404", description = "Voucher not found")
        ]
    )
    @PostMapping("/vouchers/{code}/validate")
    fun validateVoucher(
        @RequestHeader("tenant") tenant: String,
        @PathVariable code: String,
        @Valid @RequestBody body: VoucherValidationRequest
    ): ResponseEntity<ValidationResponse> {
        val result = voucherService.validateVoucher(tenant, code, body)
        val status = if (result.valid) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(result)
    }

    @Operation(
        summary = "Get voucher by code",
        operationId = "getVoucher",
        responses = [
            ApiResponse(responseCode = "200", description = "Voucher found"),
            ApiResponse(responseCode = "404", description = "Voucher not found")
        ]
    )
    @GetMapping("/vouchers/{code}")
    fun getVoucher(
        @RequestHeader("tenant") tenant: String,
        @PathVariable code: String
    ): ResponseEntity<VoucherResponse> {
        val voucher = voucherService.getByCode(tenant, code) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(voucherService.toVoucherResponse(voucher))
    }

    @Operation(
        summary = "Update voucher",
        operationId = "updateVoucher",
        responses = [
            ApiResponse(responseCode = "200", description = "Voucher updated"),
            ApiResponse(responseCode = "404", description = "Voucher not found")
        ]
    )
    @PutMapping("/vouchers/{code}")
    fun updateVoucher(
        @RequestHeader("tenant") tenant: String,
        @PathVariable code: String,
        @Valid @RequestBody body: VoucherCreateRequest
    ): ResponseEntity<VoucherResponse> {
        val updated = voucherService.updateVoucher(tenant, code, body) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(voucherService.toVoucherResponse(updated))
    }

    @Operation(
        summary = "Delete voucher",
        operationId = "deleteVoucher",
        responses = [
            ApiResponse(responseCode = "204", description = "Voucher deleted"),
            ApiResponse(responseCode = "404", description = "Voucher not found")
        ]
    )
    @DeleteMapping("/vouchers/{code}")
    fun deleteVoucher(
        @RequestHeader("tenant") tenant: String,
        @PathVariable code: String
    ): ResponseEntity<Void> {
        return if (voucherService.deleteVoucher(tenant, code)) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    @Operation(
        summary = "List vouchers",
        operationId = "listVouchers",
        responses = [
            ApiResponse(responseCode = "200", description = "List of vouchers")
        ]
    )
    @GetMapping("/vouchers")
    fun listVouchers(
        @RequestHeader("tenant") tenant: String,
        @Parameter(description = "Max number of items per page", example = "10")
        @RequestParam(required = false, defaultValue = "10") limit: Int,
        @Parameter(description = "1-based page index", example = "1")
        @RequestParam(required = false, defaultValue = "1") page: Int
    ): ResponseEntity<VouchersListResponse> {
        val cappedLimit = limit.coerceIn(1, 100)
        val pageable = org.springframework.data.domain.PageRequest.of(
            (page - 1).coerceAtLeast(0),
            cappedLimit,
            org.springframework.data.domain.Sort.by("createdAt").descending()
        )
        val vouchers = voucherService.listVouchers(tenant, pageable)
        return ResponseEntity.ok(
            VouchersListResponse(
                vouchers = vouchers.content.map { voucherService.toVoucherResponse(it) },
                total = vouchers.totalElements.toInt()
            )
        )
    }

    @Operation(
        summary = "Validate multiple redeemables (stackable discounts)",
        operationId = "validateStack",
        responses = [
            ApiResponse(responseCode = "200", description = "Stack validation result"),
            ApiResponse(responseCode = "400", description = "Validation error (at least one redeemable invalid)")
        ]
    )
    @PostMapping("/validations")
    fun validateStack(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody body: ValidationStackRequest
    ): ResponseEntity<ValidationStackResponse> {
        val responses = body.redeemables.map { redeemable ->
            if (redeemable.`object` != "voucher") {
                ValidationResponse(
                    valid = false,
                    error = ErrorResponse("unsupported_redeemable", "Only vouchers are supported in the stack validation")
                )
            } else {
                voucherService.validateVoucher(
                    tenant,
                    redeemable.id,
                    VoucherValidationRequest(customer = body.customer, order = body.order)
                )
            }
        }
        val status = if (responses.all { it.valid }) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(ValidationStackResponse(responses))
    }

    @Operation(
        summary = "Redeem voucher(s) with order context",
        operationId = "redeem",
        responses = [
            ApiResponse(responseCode = "200", description = "Redemption result"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Voucher not found")
        ]
    )
    @PostMapping("/redemptions")
    fun redeem(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody body: RedemptionRequest
    ): ResponseEntity<RedemptionResponse> {
        val result = voucherService.redeem(tenant, body)
        val status = if (result.error == null) HttpStatus.OK else HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(result)
    }

    @Operation(
        summary = "Get voucher QR code",
        operationId = "getVoucherQr",
        responses = [
            ApiResponse(responseCode = "200", description = "QR code PNG for voucher"),
            ApiResponse(responseCode = "404", description = "Voucher not found")
        ]
    )
    @GetMapping("/vouchers/{code}/qr", produces = [MediaType.IMAGE_PNG_VALUE])
    fun getVoucherQr(
        @RequestHeader("tenant") tenant: String,
        @PathVariable code: String
    ): ResponseEntity<ByteArray> {
        val voucher = voucherService.getByCode(tenant, code) ?: return ResponseEntity.notFound().build()
        val png = qrCodeService.generatePng(voucher.code ?: code)
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png)
    }

    @Operation(
        summary = "Get voucher barcode (Code 128)",
        operationId = "getVoucherBarcode",
        responses = [
            ApiResponse(responseCode = "200", description = "Barcode PNG for voucher"),
            ApiResponse(responseCode = "404", description = "Voucher not found")
        ]
    )
    @GetMapping("/vouchers/{code}/barcode", produces = [MediaType.IMAGE_PNG_VALUE])
    fun getVoucherBarcode(
        @RequestHeader("tenant") tenant: String,
        @PathVariable code: String
    ): ResponseEntity<ByteArray> {
        val voucher = voucherService.getByCode(tenant, code) ?: return ResponseEntity.notFound().build()
        val png = barcodeService.generateCode128Png(voucher.code ?: code)
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png)
    }
}
