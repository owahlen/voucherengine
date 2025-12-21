package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.wahlen.voucherengine.api.dto.request.CustomerCreateRequest

@RestController
@RequestMapping("/v1")
@Validated
class CustomerController {

    @Operation(
        summary = "Create or update a customer",
        operationId = "createOrUpdateCustomer",
        responses = [
            ApiResponse(responseCode = "200", description = "Customer created or updated"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "501", description = "Not implemented")
        ]
    )
    @PostMapping("/customers")
    fun createCustomer(@Valid @RequestBody body: CustomerCreateRequest): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(mapOf("status" to "not_implemented"))
}
