package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.wahlen.voucherengine.api.dto.request.ExportCreateRequest
import org.wahlen.voucherengine.api.dto.response.ExportResponse
import org.wahlen.voucherengine.api.dto.response.ExportsListResponse
import org.wahlen.voucherengine.service.ExportService
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
class ExportController(
    private val exportService: ExportService
) {

    @Operation(
        summary = "Create export",
        operationId = "createExport",
        responses = [
            ApiResponse(responseCode = "200", description = "Export created"),
            ApiResponse(responseCode = "400", description = "Validation error")
        ]
    )
    @PostMapping("/exports")
    fun createExport(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody body: ExportCreateRequest
    ): ResponseEntity<ExportResponse> =
        ResponseEntity.ok(exportService.createExport(tenant, body))

    @Operation(
        summary = "List exports",
        operationId = "listExports",
        responses = [ApiResponse(responseCode = "200", description = "List of exports")]
    )
    @GetMapping("/exports")
    fun listExports(
        @RequestHeader("tenant") tenant: String,
        @RequestParam(name = "limit", required = false, defaultValue = "10") limit: Int,
        @RequestParam(name = "page", required = false, defaultValue = "1") page: Int,
        @RequestParam(name = "order", required = false) order: String?
    ): ResponseEntity<ExportsListResponse> =
        ResponseEntity.ok(exportService.listExports(tenant, page, limit, order))

    @Operation(
        summary = "Get export",
        operationId = "getExport",
        responses = [
            ApiResponse(responseCode = "200", description = "Export found"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @GetMapping("/exports/{id}")
    fun getExport(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID
    ): ResponseEntity<ExportResponse> {
        val export = exportService.getExport(tenant, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(export)
    }

    @Operation(
        summary = "Download export",
        operationId = "downloadExport",
        responses = [
            ApiResponse(responseCode = "200", description = "Export CSV content"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @GetMapping("/exports/{id}", params = ["token"])
    fun downloadExport(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID,
        @RequestParam("token") token: String
    ): ResponseEntity<String> {
        val csv = exportService.downloadExport(tenant, id, token)
        return ResponseEntity.ok()
            .contentType(MediaType.valueOf("text/csv"))
            .body(csv)
    }

    @Operation(
        summary = "Delete export",
        operationId = "deleteExport",
        responses = [
            ApiResponse(responseCode = "204", description = "Deleted"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @DeleteMapping("/exports/{id}")
    fun deleteExport(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        return if (exportService.deleteExport(tenant, id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
    }
}
