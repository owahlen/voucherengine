package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.wahlen.voucherengine.api.dto.request.CustomerCreateRequest
import org.wahlen.voucherengine.api.dto.response.CustomersListResponse
import org.wahlen.voucherengine.service.CustomerService

@RestController
@RequestMapping("/v1")
@Validated
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    ]
)
class CustomerController(
    private val customerService: CustomerService
) {

    @Operation(
        summary = "Create a customer",
        operationId = "createCustomer",
        responses = [
            ApiResponse(responseCode = "200", description = "Customer created"),
            ApiResponse(responseCode = "400", description = "Validation error")
        ]
    )
    @PostMapping("/customers")
    fun createCustomer(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody body: CustomerCreateRequest
    ): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.OK).body(customerService.upsert(tenant, body))

    @Operation(
        summary = "List customers",
        operationId = "listCustomers",
        responses = [
            ApiResponse(responseCode = "200", description = "List of customers")
        ]
    )
    @GetMapping("/customers")
    fun listCustomers(
        @RequestHeader("tenant") tenant: String,
        @Parameter(description = "Max number of items per page", example = "10")
        @RequestParam(required = false, defaultValue = "10") limit: Int,
        @Parameter(description = "1-based page index", example = "1")
        @RequestParam(required = false, defaultValue = "1") page: Int
    ): ResponseEntity<CustomersListResponse> {
        val cappedLimit = limit.coerceIn(1, 100)
        val pageable = org.springframework.data.domain.PageRequest.of(
            (page - 1).coerceAtLeast(0),
            cappedLimit,
            org.springframework.data.domain.Sort.by("createdAt").descending()
        )
        val pageResult = customerService.list(tenant, pageable)
        return ResponseEntity.ok(
            CustomersListResponse(
                customers = pageResult.content,
                total = pageResult.totalElements.toInt()
            )
        )
    }

    @Operation(
        summary = "Get customer",
        operationId = "getCustomer",
        responses = [
            ApiResponse(responseCode = "200", description = "Customer found"),
            ApiResponse(responseCode = "404", description = "Customer not found")
        ]
    )
    @GetMapping("/customers/{id}")
    fun getCustomer(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: String
    ): ResponseEntity<Any> {
        val customer = customerService.getByIdOrSource(tenant, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(customer)
    }

    @Operation(
        summary = "Update customer",
        operationId = "updateCustomer",
        responses = [
            ApiResponse(responseCode = "200", description = "Customer updated"),
            ApiResponse(responseCode = "404", description = "Customer not found")
        ]
    )
    @PutMapping("/customers/{id}")
    fun updateCustomer(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: String,
        @Valid @RequestBody body: CustomerCreateRequest
    ): ResponseEntity<Any> {
        val existing = customerService.getByIdOrSource(tenant, id) ?: return ResponseEntity.notFound().build()
        val merged = body.copy(source_id = existing.sourceId ?: body.source_id)
        return ResponseEntity.ok(customerService.upsert(tenant, merged))
    }

    @Operation(
        summary = "Delete customer",
        operationId = "deleteCustomer",
        responses = [
            ApiResponse(responseCode = "204", description = "Customer deleted"),
            ApiResponse(responseCode = "404", description = "Customer not found")
        ]
    )
    @DeleteMapping("/customers/{id}")
    fun deleteCustomer(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: String
    ): ResponseEntity<Void> {
        val existing = customerService.getByIdOrSource(tenant, id) ?: return ResponseEntity.notFound().build()
        customerService.delete(tenant, existing.id?.toString() ?: existing.sourceId!!)
        return ResponseEntity.noContent().build()
    }
}
