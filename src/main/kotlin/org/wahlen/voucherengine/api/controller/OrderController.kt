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
import org.wahlen.voucherengine.api.dto.request.OrderCreateRequest
import org.wahlen.voucherengine.api.dto.response.OrdersListResponse
import org.wahlen.voucherengine.api.dto.response.OrderResponse
import org.wahlen.voucherengine.service.OrderService
import org.wahlen.voucherengine.service.TenantService
import org.wahlen.voucherengine.service.async.AsyncJobPublisher
import org.wahlen.voucherengine.service.async.command.OrderImportCommand

@RestController
@RequestMapping("/v1")
@Validated
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    ]
)
class OrderController(
    private val orderService: OrderService,
    private val asyncJobPublisher: AsyncJobPublisher,
    private val tenantService: TenantService
) {

    @Operation(
        summary = "Create an order",
        operationId = "createOrder",
        responses = [
            ApiResponse(responseCode = "201", description = "Order created"),
            ApiResponse(responseCode = "400", description = "Validation error")
        ]
    )
    @PostMapping("/orders")
    fun createOrder(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody body: OrderCreateRequest
    ): ResponseEntity<OrderResponse> {
        val created = orderService.create(tenant, body)
        return ResponseEntity.status(HttpStatus.OK).body(created)
    }

    @Operation(
        summary = "List orders",
        operationId = "listOrders",
        responses = [
            ApiResponse(responseCode = "200", description = "List of orders")
        ]
    )
    @GetMapping("/orders")
    fun listOrders(
        @RequestHeader("tenant") tenant: String,
        @Parameter(description = "Max number of items per page", example = "10")
        @RequestParam(required = false, defaultValue = "10") limit: Int,
        @Parameter(description = "1-based page index", example = "1")
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @Parameter(description = "Sort field, prefix with '-' for descending", example = "-created_at")
        @RequestParam(required = false, defaultValue = "created_at") order: String
    ): ResponseEntity<OrdersListResponse> {
        val sort = parseSort(order, mapOf("created_at" to "createdAt", "updated_at" to "updatedAt", "amount" to "amount"), "created_at")
        val cappedLimit = limit.coerceIn(1, 100)
        val pageable = org.springframework.data.domain.PageRequest.of((page - 1).coerceAtLeast(0), cappedLimit, sort)
        val ordersPage = orderService.list(tenant, pageable)
        return ResponseEntity.ok(
            OrdersListResponse(
                orders = ordersPage.content,
                total = ordersPage.totalElements.toInt()
            )
        )
    }

    @Operation(
        summary = "Get order by id or source id",
        operationId = "getOrder",
        responses = [
            ApiResponse(responseCode = "200", description = "Order found"),
            ApiResponse(responseCode = "404", description = "Order not found")
        ]
    )
    @GetMapping("/orders/{orderId}")
    fun getOrder(
        @RequestHeader("tenant") tenant: String,
        @PathVariable orderId: String
    ): ResponseEntity<OrderResponse> {
        val order = orderService.getByIdOrSource(tenant, orderId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(order)
    }

    @Operation(
        summary = "Update order",
        operationId = "updateOrder",
        responses = [
            ApiResponse(responseCode = "200", description = "Order updated"),
            ApiResponse(responseCode = "404", description = "Order not found"),
            ApiResponse(responseCode = "400", description = "Validation error")
        ]
    )
    @PutMapping("/orders/{orderId}")
    fun updateOrder(
        @RequestHeader("tenant") tenant: String,
        @PathVariable orderId: String,
        @Valid @RequestBody body: OrderCreateRequest
    ): ResponseEntity<OrderResponse> {
        val updated = orderService.update(tenant, orderId, body) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @Operation(
        summary = "Export orders",
        operationId = "exportOrders",
        description = "Asynchronously export orders to CSV or JSON format. Returns async job ID for tracking.",
        responses = [
            ApiResponse(responseCode = "200", description = "Export job created"),
            ApiResponse(responseCode = "400", description = "Invalid request")
        ]
    )
    @PostMapping("/orders/export")
    fun exportOrders(
        @RequestHeader("tenant") tenantName: String,
        @Valid @RequestBody request: org.wahlen.voucherengine.api.dto.request.OrderExportRequest
    ): ResponseEntity<Map<String, String>> {
        val tenant = tenantService.getByName(tenantName)
            ?: throw IllegalArgumentException("Tenant not found: $tenantName")

        // Extract parameters from the request
        val parameters = mutableMapOf<String, Any?>()
        
        // Default format to CSV
        var format = "CSV"
        
        request.parameters?.let { params ->
            params.fields?.let { parameters["fields"] = it }
            params.order?.let { parameters["order"] = it }
            params.format?.let { format = it }
            params.filters?.let { filters ->
                filters.status?.let { parameters["status"] = it }
                filters.created_at?.after?.let { parameters["created_after"] = it }
                filters.created_at?.before?.let { parameters["created_before"] = it }
                filters.updated_at?.after?.let { parameters["updated_after"] = it }
                filters.updated_at?.before?.let { parameters["updated_before"] = it }
            }
        }

        val command = org.wahlen.voucherengine.service.async.command.OrderExportCommand(
            tenantName = tenantName,
            parameters = parameters + ("format" to format)
        )

        val jobId = asyncJobPublisher.publish(command, tenant)
        return ResponseEntity.ok(
            mapOf(
                "async_action_id" to jobId.toString(),
                "message" to "Export job created"
            )
        )
    }

    @Operation(
        summary = "Import orders",
        operationId = "importOrders",
        description = "Asynchronously import multiple orders. Request body should be an array of order objects. Returns async job ID for tracking.",
        responses = [
            ApiResponse(responseCode = "200", description = "Import job created"),
            ApiResponse(responseCode = "400", description = "Invalid request body")
        ]
    )
    @PostMapping("/orders/import")
    fun importOrders(
        @RequestHeader("tenant") tenantName: String,
        @RequestBody orders: List<Map<String, Any?>>
    ): ResponseEntity<Map<String, String>> {
        if (orders.isEmpty()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Orders array cannot be empty"))
        }

        val tenant = tenantService.getByName(tenantName) 
            ?: throw IllegalArgumentException("Tenant not found: $tenantName")

        val command = OrderImportCommand(
            tenantName = tenantName,
            orders = orders
        )

        val jobId = asyncJobPublisher.publish(command, tenant)
        return ResponseEntity.ok(mapOf(
            "async_action_id" to jobId.toString(),
            "message" to "Import job created with ${orders.size} orders"
        ))
    }
}
