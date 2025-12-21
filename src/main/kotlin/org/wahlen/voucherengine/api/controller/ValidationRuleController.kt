package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.wahlen.voucherengine.api.dto.request.ValidationRuleAssignmentRequest
import org.wahlen.voucherengine.api.dto.request.ValidationRuleCreateRequest

@RestController
@RequestMapping("/v1")
@Validated
class ValidationRuleController {

    @Operation(
        summary = "Create a validation rule",
        operationId = "createValidationRule",
        responses = [
            ApiResponse(responseCode = "201", description = "Validation rule created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "501", description = "Not implemented")
        ]
    )
    @PostMapping("/validation-rules")
    fun createValidationRule(@Valid @RequestBody body: ValidationRuleCreateRequest): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(mapOf("status" to "not_implemented"))

    @Operation(
        summary = "Assign a validation rule to an object (voucher or campaign)",
        operationId = "assignValidationRule",
        responses = [
            ApiResponse(responseCode = "200", description = "Assignment created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Rule not found"),
            ApiResponse(responseCode = "501", description = "Not implemented")
        ]
    )
    @PostMapping("/validation-rules/{id}/assignments")
    fun assignRule(
        @PathVariable id: String,
        @Valid @RequestBody body: ValidationRuleAssignmentRequest
    ): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(mapOf("status" to "not_implemented"))
}
