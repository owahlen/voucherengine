package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.wahlen.voucherengine.api.dto.request.*
import org.wahlen.voucherengine.api.dto.response.VoucherResponse
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
    fun createVoucher(@Valid @RequestBody body: VoucherCreateRequest): ResponseEntity<VoucherResponse> {
        val voucher = voucherService.createVoucher(body)
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
        @PathVariable code: String,
        @Valid @RequestBody body: VoucherValidationRequest
    ): ResponseEntity<ValidationResponse> {
        val result = voucherService.validateVoucher(code, body)
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
    fun getVoucher(@PathVariable code: String): ResponseEntity<VoucherResponse> {
        val voucher = voucherService.getByCode(code) ?: return ResponseEntity.notFound().build()
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
        @PathVariable code: String,
        @Valid @RequestBody body: VoucherCreateRequest
    ): ResponseEntity<VoucherResponse> {
        val updated = voucherService.updateVoucher(code, body) ?: return ResponseEntity.notFound().build()
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
    fun deleteVoucher(@PathVariable code: String): ResponseEntity<Void> {
        return if (voucherService.deleteVoucher(code)) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    @Operation(
        summary = "List vouchers",
        operationId = "listVouchers",
        responses = [
            ApiResponse(responseCode = "200", description = "List of vouchers")
        ]
    )
    @GetMapping("/vouchers")
    fun listVouchers(): ResponseEntity<List<VoucherResponse>> =
        ResponseEntity.ok(voucherService.listVouchers().map { voucherService.toVoucherResponse(it) })

    @Operation(
        summary = "Validate multiple redeemables (stackable discounts)",
        operationId = "validateStack",
        responses = [
            ApiResponse(responseCode = "200", description = "Stack validation result"),
            ApiResponse(responseCode = "400", description = "Validation error (at least one redeemable invalid)")
        ]
    )
    @PostMapping("/validations")
    fun validateStack(@Valid @RequestBody body: ValidationStackRequest): ResponseEntity<ValidationStackResponse> {
        val responses = body.redeemables.map { redeemable ->
            if (redeemable.`object` != "voucher") {
                ValidationResponse(
                    valid = false,
                    error = ErrorResponse("unsupported_redeemable", "Only vouchers are supported in the stack validation")
                )
            } else {
                voucherService.validateVoucher(
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
    fun redeem(@Valid @RequestBody body: RedemptionRequest): ResponseEntity<RedemptionResponse> {
        val result = voucherService.redeem(body)
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
    fun getVoucherQr(@PathVariable code: String): ResponseEntity<ByteArray> {
        val voucher = voucherService.getByCode(code) ?: return ResponseEntity.notFound().build()
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
    fun getVoucherBarcode(@PathVariable code: String): ResponseEntity<ByteArray> {
        val voucher = voucherService.getByCode(code) ?: return ResponseEntity.notFound().build()
        val png = barcodeService.generateCode128Png(voucher.code ?: code)
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png)
    }
}
