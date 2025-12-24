package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
import org.wahlen.voucherengine.api.dto.response.CustomerActivityDto
import org.wahlen.voucherengine.api.dto.response.CustomerActivityResponse
import org.wahlen.voucherengine.api.dto.response.CustomersListResponse
import org.wahlen.voucherengine.persistence.model.event.EventCategory
import org.wahlen.voucherengine.service.CustomerEventService
import org.wahlen.voucherengine.service.CustomerService
import java.time.Instant
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
class CustomerController(
    private val customerService: CustomerService,
    private val customerEventService: CustomerEventService
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

    @Operation(
        summary = "Permanently delete customer",
        operationId = "deleteCustomerPermanently",
        responses = [
            ApiResponse(responseCode = "204", description = "Customer permanently deleted"),
            ApiResponse(responseCode = "404", description = "Customer not found")
        ]
    )
    @PostMapping("/customers/{id}/permanent-deletion")
    fun deleteCustomerPermanently(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: String
    ): ResponseEntity<Void> {
        val existing = customerService.getByIdOrSource(tenant, id) ?: return ResponseEntity.notFound().build()
        customerService.delete(tenant, existing.id?.toString() ?: existing.sourceId!!)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Import customers from CSV",
        operationId = "importCustomersCSV",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented")
        ]
    )
    @PostMapping("/customers/importCSV", consumes = ["text/csv"])
    fun importCustomersCSV(
        @RequestHeader("tenant") tenant: String,
        @RequestBody csvContent: String
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Customer CSV import not yet implemented"))
    }

    @Operation(
        summary = "Update customers in bulk asynchronously",
        operationId = "updateCustomersBulkAsync",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented")
        ]
    )
    @PostMapping("/customers/bulk/async")
    fun updateCustomersBulkAsync(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody updates: List<CustomerCreateRequest>
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Customer bulk update not yet implemented"))
    }

    @Operation(
        summary = "Update customers metadata in bulk asynchronously",
        operationId = "updateCustomersMetadataAsync",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented")
        ]
    )
    @PostMapping("/customers/metadata/async")
    fun updateCustomersMetadataAsync(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody request: Map<String, Any>
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Customer metadata bulk update not yet implemented"))
    }

    @Operation(
        summary = "Get customer activity",
        operationId = "getCustomerActivity",
        description = "Retrieves activity details of a customer including redemptions, validations, publications, and other events",
        responses = [
            ApiResponse(responseCode = "200", description = "Customer activity retrieved"),
            ApiResponse(responseCode = "404", description = "Customer not found")
        ]
    )
    @GetMapping("/customers/{id}/activity")
    fun getCustomerActivity(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: String,
        @Parameter(description = "Max number of items per page", example = "10")
        @RequestParam(required = false, defaultValue = "10") limit: Int,
        @Parameter(description = "1-based page index", example = "1")
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @Parameter(description = "Filter by event type (e.g., customer.redemption.succeeded)")
        @RequestParam(required = false) type: String?,
        @Parameter(description = "Filter by campaign ID")
        @RequestParam(required = false) campaign_id: UUID?,
        @Parameter(description = "Filter by category: ACTION or EFFECT")
        @RequestParam(required = false) category: String?,
        @Parameter(description = "Start date filter (ISO 8601)")
        @RequestParam(required = false) start_date: String?,
        @Parameter(description = "End date filter (ISO 8601)")
        @RequestParam(required = false) end_date: String?
    ): ResponseEntity<CustomerActivityResponse> {
        val customer = customerService.getByIdOrSource(tenant, id) 
            ?: return ResponseEntity.notFound().build()
        
        val cappedLimit = limit.coerceIn(1, 100)
        val pageable = PageRequest.of(
            (page - 1).coerceAtLeast(0),
            cappedLimit,
            Sort.by("createdAt").descending()
        )
        
        val eventTypes = type?.let { listOf(it) }
        val eventCategory = category?.let { EventCategory.valueOf(it) }
        val startInstant = start_date?.let { Instant.parse(it) }
        val endInstant = end_date?.let { Instant.parse(it) }
        
        val events = customerEventService.listCustomerActivity(
            tenantName = tenant,
            customerId = customer.id!!,
            eventTypes = eventTypes,
            campaignId = campaign_id,
            category = eventCategory,
            startDate = startInstant,
            endDate = endInstant,
            pageable = pageable
        )
        
        return ResponseEntity.ok(
            CustomerActivityResponse(
                `object` = "list",
                data_ref = "data",
                data = events.content.map { event ->
                    CustomerActivityDto(
                        id = event.id.toString(),
                        type = event.eventType,
                        data = event.data,
                        created_at = event.createdAt,
                        group_id = event.groupId
                    )
                },
                has_more = events.hasNext(),
                more_starting_after = events.content.lastOrNull()?.id?.toString()
            )
        )
    }

    @Operation(
        summary = "List customer's segments",
        operationId = "getCustomerSegments",
        responses = [
            ApiResponse(responseCode = "200", description = "Customer segments retrieved"),
            ApiResponse(responseCode = "404", description = "Customer not found")
        ]
    )
    @GetMapping("/customers/{id}/segments")
    fun getCustomerSegments(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        val customer = customerService.getByIdOrSource(tenant, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
            mapOf(
                "object" to "list",
                "data_ref" to "segments",
                "segments" to emptyList<Any>(),
                "total" to 0
            )
        )
    }

    @Operation(
        summary = "List customer's redeemables",
        operationId = "getCustomerRedeemables",
        responses = [
            ApiResponse(responseCode = "200", description = "Customer redeemables retrieved"),
            ApiResponse(responseCode = "404", description = "Customer not found")
        ]
    )
    @GetMapping("/customers/{id}/redeemables")
    fun getCustomerRedeemables(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: String
    ): ResponseEntity<Map<String, Any>> {
        val customer = customerService.getByIdOrSource(tenant, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
            mapOf(
                "object" to "list",
                "data_ref" to "redeemables",
                "redeemables" to emptyList<Any>(),
                "total" to 0
            )
        )
    }
}
