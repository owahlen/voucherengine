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
import org.wahlen.voucherengine.api.dto.response.ValidationsValidateResponse
import org.wahlen.voucherengine.api.dto.response.RedemptionsRedeemResponse
import org.wahlen.voucherengine.api.dto.response.AsyncActionResponse
import org.wahlen.voucherengine.api.dto.response.AsyncJobStatusResponse
import org.wahlen.voucherengine.service.VoucherService
import org.wahlen.voucherengine.api.dto.response.ValidationResponse
import org.wahlen.voucherengine.service.ValidationStackService
import org.wahlen.voucherengine.service.RedemptionStackService
import org.wahlen.voucherengine.service.QrCodeService
import org.wahlen.voucherengine.service.BarcodeService
import org.wahlen.voucherengine.service.async.AsyncJobPublisher
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
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
class VoucherController(
    private val voucherService: VoucherService,
    private val qrCodeService: QrCodeService,
    private val barcodeService: BarcodeService,
    private val validationStackService: ValidationStackService,
    private val redemptionStackService: RedemptionStackService,
    private val sessionLockRepository: org.wahlen.voucherengine.persistence.repository.SessionLockRepository,
    private val asyncJobPublisher: AsyncJobPublisher,
    private val asyncJobRepository: AsyncJobRepository
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
        summary = "Enable voucher",
        operationId = "enableVoucher",
        responses = [
            ApiResponse(responseCode = "200", description = "Voucher enabled"),
            ApiResponse(responseCode = "404", description = "Voucher not found")
        ]
    )
    @PostMapping("/vouchers/{code}/enable")
    fun enableVoucher(
        @RequestHeader("tenant") tenant: String,
        @PathVariable code: String
    ): ResponseEntity<VoucherResponse> {
        val updated = voucherService.setVoucherActive(tenant, code, true) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(voucherService.toVoucherResponse(updated))
    }

    @Operation(
        summary = "Disable voucher",
        operationId = "disableVoucher",
        responses = [
            ApiResponse(responseCode = "200", description = "Voucher disabled"),
            ApiResponse(responseCode = "404", description = "Voucher not found")
        ]
    )
    @PostMapping("/vouchers/{code}/disable")
    fun disableVoucher(
        @RequestHeader("tenant") tenant: String,
        @PathVariable code: String
    ): ResponseEntity<VoucherResponse> {
        val updated = voucherService.setVoucherActive(tenant, code, false) ?: return ResponseEntity.notFound().build()
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
    ): ResponseEntity<ValidationsValidateResponse> {
        val response = validationStackService.validate(tenant, body)
        return ResponseEntity.ok(response)
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
    ): ResponseEntity<RedemptionsRedeemResponse> {
        val response = redemptionStackService.redeem(tenant, body)
        return ResponseEntity.ok(response)
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

    @Operation(
        summary = "Adjust voucher balance (gift or loyalty cards)",
        operationId = "adjustVoucherBalance",
        responses = [
            ApiResponse(responseCode = "200", description = "Balance updated"),
            ApiResponse(responseCode = "400", description = "Invalid request or insufficient balance"),
            ApiResponse(responseCode = "404", description = "Voucher not found")
        ]
    )
    @PostMapping("/vouchers/{code}/balance")
    fun adjustVoucherBalance(
        @RequestHeader("tenant") tenant: String,
        @PathVariable code: String,
        @Valid @RequestBody body: VoucherBalanceUpdateRequest
    ): ResponseEntity<org.wahlen.voucherengine.api.dto.response.VoucherBalanceUpdateResponse> {
        val result = voucherService.adjustVoucherBalance(tenant, code, body)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }

    @Operation(
        summary = "List voucher transactions",
        operationId = "listVoucherTransactions",
        responses = [
            ApiResponse(responseCode = "200", description = "List of transactions"),
            ApiResponse(responseCode = "404", description = "Voucher not found")
        ]
    )
    @GetMapping("/vouchers/{code}/transactions")
    fun listVoucherTransactions(
        @RequestHeader("tenant") tenant: String,
        @PathVariable code: String,
        @Parameter(description = "Max number of items per page", example = "10")
        @RequestParam(required = false, defaultValue = "10") limit: Int,
        @Parameter(description = "1-based page index", example = "1")
        @RequestParam(required = false, defaultValue = "1") page: Int
    ): ResponseEntity<org.wahlen.voucherengine.api.dto.response.VoucherTransactionsListResponse> {
        val cappedLimit = limit.coerceIn(1, 100)
        val pageable = org.springframework.data.domain.PageRequest.of(
            (page - 1).coerceAtLeast(0),
            cappedLimit,
            org.springframework.data.domain.Sort.by("createdAt").descending()
        )
        val transactions = voucherService.listVoucherTransactions(tenant, code, pageable)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
            org.wahlen.voucherengine.api.dto.response.VoucherTransactionsListResponse(
                data = transactions.content.map { voucherService.toTransactionResponse(it) },
                has_more = transactions.hasNext(),
                total = transactions.totalElements.toInt()
            )
        )
    }

    @Operation(
        summary = "Get voucher's redemptions",
        operationId = "getVoucherRedemptions",
        responses = [
            ApiResponse(responseCode = "200", description = "List of redemptions for this voucher"),
            ApiResponse(responseCode = "404", description = "Voucher not found")
        ]
    )
    @GetMapping("/vouchers/{code}/redemption")
    fun getVoucherRedemptions(
        @RequestHeader("tenant") tenant: String,
        @PathVariable code: String
    ): ResponseEntity<org.wahlen.voucherengine.api.dto.response.VoucherRedemptionsResponse> {
        val response = voucherService.listVoucherRedemptions(tenant, code)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(response)
    }

    @Operation(
        summary = "Update vouchers in bulk",
        operationId = "updateVouchersInBulk",
        responses = [
            ApiResponse(responseCode = "202", description = "Job accepted for processing")
        ]
    )
    @PostMapping("/vouchers/bulk/async")
    fun updateVouchersInBulk(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody updates: List<org.wahlen.voucherengine.api.dto.request.VoucherBulkUpdateRequest>
    ): ResponseEntity<AsyncActionResponse> {
        val jobId = asyncJobPublisher.publishBulkUpdate(tenant, updates)
        
        return ResponseEntity.accepted().body(
            AsyncActionResponse(
                async_action_id = jobId.toString(),
                status = "ACCEPTED"
            )
        )
    }

    @Operation(
        summary = "Update voucher metadata asynchronously",
        operationId = "updateVoucherMetadataAsync",
        responses = [
            ApiResponse(responseCode = "202", description = "Job accepted for processing")
        ]
    )
    @PostMapping("/vouchers/metadata/async")
    fun updateVoucherMetadataAsync(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody request: org.wahlen.voucherengine.api.dto.request.VoucherMetadataUpdateRequest
    ): ResponseEntity<AsyncActionResponse> {
        val jobId = asyncJobPublisher.publishMetadataUpdate(tenant, request)
        
        return ResponseEntity.accepted().body(
            AsyncActionResponse(
                async_action_id = jobId.toString(),
                status = "ACCEPTED"
            )
        )
    }

    @Operation(
        summary = "Import vouchers",
        operationId = "importVouchers",
        responses = [
            ApiResponse(responseCode = "202", description = "Import job accepted")
        ]
    )
    @PostMapping("/vouchers/import")
    fun importVouchers(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody request: org.wahlen.voucherengine.api.dto.request.VoucherImportRequest
    ): ResponseEntity<AsyncActionResponse> {
        val jobId = asyncJobPublisher.publishVoucherImport(tenant, request)
        
        return ResponseEntity.accepted().body(
            AsyncActionResponse(
                async_action_id = jobId.toString(),
                status = "ACCEPTED"
            )
        )
    }

    @Operation(
        summary = "Import vouchers from CSV",
        operationId = "importVouchersCSV",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - use JSON import")
        ]
    )
    @PostMapping("/vouchers/importCSV")
    fun importVouchersCSV(
        @RequestHeader("tenant") tenant: String
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "CSV import not implemented. Use /vouchers/import with JSON format."))
    }

    @Operation(
        summary = "Export voucher transactions",
        operationId = "exportVoucherTransactions",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - use GET /vouchers/{code}/transactions")
        ]
    )
    @PostMapping("/vouchers/{code}/transactions/export")
    fun exportVoucherTransactions(
        @RequestHeader("tenant") tenant: String,
        @PathVariable code: String
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Transaction export not implemented. Use GET /vouchers/{code}/transactions with pagination."))
    }

    @Operation(
        summary = "Release validation session lock",
        operationId = "releaseValidationSession",
        responses = [
            ApiResponse(responseCode = "204", description = "Session released"),
            ApiResponse(responseCode = "404", description = "Voucher or session not found")
        ]
    )
    @DeleteMapping("/vouchers/{code}/sessions/{sessionKey}")
    fun releaseValidationSession(
        @RequestHeader("tenant") tenant: String,
        @PathVariable code: String,
        @PathVariable sessionKey: String
    ): ResponseEntity<Void> {
        val voucher = voucherService.getByCode(tenant, code) ?: return ResponseEntity.notFound().build()
        sessionLockRepository.deleteByTenantNameAndSessionKey(tenant, sessionKey)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Get async job status",
        operationId = "getAsyncJobStatus",
        responses = [
            ApiResponse(responseCode = "200", description = "Job status retrieved"),
            ApiResponse(responseCode = "404", description = "Job not found")
        ]
    )
    @GetMapping("/async-actions/{id}")
    fun getAsyncJobStatus(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID
    ): ResponseEntity<AsyncJobStatusResponse> {
        val job = asyncJobRepository.findByIdAndTenant_Name(id, tenant)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(
            AsyncJobStatusResponse(
                id = job.id!!,
                type = job.type.name,
                status = job.status.name,
                progress = job.progress,
                total = job.total,
                result = job.result,
                error_message = job.errorMessage,
                created_at = job.createdAt,
                started_at = job.startedAt,
                completed_at = job.completedAt
            )
        )
    }
}
