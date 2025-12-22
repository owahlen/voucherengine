package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.wahlen.voucherengine.api.dto.request.OrderCreateRequest
import org.wahlen.voucherengine.api.dto.response.OrderResponse
import org.wahlen.voucherengine.service.OrderService

@RestController
@RequestMapping("/v1")
@Validated
class OrderController(
    private val orderService: OrderService
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
    fun createOrder(@Valid @RequestBody body: OrderCreateRequest): ResponseEntity<OrderResponse> {
        val created = orderService.create(body)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @Operation(
        summary = "List orders",
        operationId = "listOrders",
        responses = [
            ApiResponse(responseCode = "200", description = "List of orders")
        ]
    )
    @GetMapping("/orders")
    fun listOrders(): ResponseEntity<List<OrderResponse>> = ResponseEntity.ok(orderService.list())

    @Operation(
        summary = "Get order by id or source id",
        operationId = "getOrder",
        responses = [
            ApiResponse(responseCode = "200", description = "Order found"),
            ApiResponse(responseCode = "404", description = "Order not found")
        ]
    )
    @GetMapping("/orders/{orderId}")
    fun getOrder(@PathVariable orderId: String): ResponseEntity<OrderResponse> {
        val order = orderService.getByIdOrSource(orderId) ?: return ResponseEntity.notFound().build()
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
        @PathVariable orderId: String,
        @Valid @RequestBody body: OrderCreateRequest
    ): ResponseEntity<OrderResponse> {
        val updated = orderService.update(orderId, body) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @Operation(
        summary = "Delete order",
        operationId = "deleteOrder",
        responses = [
            ApiResponse(responseCode = "204", description = "Order deleted"),
            ApiResponse(responseCode = "404", description = "Order not found")
        ]
    )
    @DeleteMapping("/orders/{orderId}")
    fun deleteOrder(@PathVariable orderId: String): ResponseEntity<Void> {
        val deleted = orderService.delete(orderId)
        return if (deleted) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }
}
