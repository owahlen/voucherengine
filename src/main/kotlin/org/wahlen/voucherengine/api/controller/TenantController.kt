package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.wahlen.voucherengine.api.dto.request.TenantCreateRequest
import org.wahlen.voucherengine.api.dto.response.TenantResponse
import org.wahlen.voucherengine.service.TenantService
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
class TenantController(
    private val tenantService: TenantService
) {

    @Operation(
        summary = "Create tenant",
        operationId = "createTenant",
        responses = [
            ApiResponse(responseCode = "201", description = "Tenant created"),
            ApiResponse(responseCode = "400", description = "Validation error")
        ]
    )
    @PostMapping("/tenants")
    fun createTenant(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody body: TenantCreateRequest
    ): ResponseEntity<TenantResponse> {
        val created = tenantService.create(tenant, body)
        return ResponseEntity.status(HttpStatus.OK).body(created)
    }

    @Operation(
        summary = "List tenants",
        operationId = "listTenants",
        responses = [ApiResponse(responseCode = "200", description = "List of tenants")]
    )
    @GetMapping("/tenants")
    fun listTenants(@RequestHeader("tenant") tenant: String): ResponseEntity<List<TenantResponse>> =
        ResponseEntity.ok(tenantService.list(tenant))

    @Operation(
        summary = "Get tenant",
        operationId = "getTenant",
        responses = [
            ApiResponse(responseCode = "200", description = "Tenant found"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @GetMapping("/tenants/{id}")
    fun getTenant(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID
    ): ResponseEntity<TenantResponse> {
        val found = tenantService.get(tenant, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(found)
    }

    @Operation(
        summary = "Update tenant",
        operationId = "updateTenant",
        responses = [
            ApiResponse(responseCode = "200", description = "Tenant updated"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @PutMapping("/tenants/{id}")
    fun updateTenant(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID,
        @Valid @RequestBody body: TenantCreateRequest
    ): ResponseEntity<TenantResponse> {
        val updated = tenantService.update(tenant, id, body) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @Operation(
        summary = "Delete tenant",
        operationId = "deleteTenant",
        responses = [
            ApiResponse(responseCode = "204", description = "Deleted"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @DeleteMapping("/tenants/{id}")
    fun deleteTenant(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: UUID
    ): ResponseEntity<Void> {
        return if (tenantService.delete(tenant, id)) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }
}
