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
import org.wahlen.voucherengine.api.dto.request.ValidationRuleAssignmentRequest
import org.wahlen.voucherengine.api.dto.request.ValidationRuleCreateRequest
import org.wahlen.voucherengine.service.ValidationRuleService
import java.util.UUID

@RestController
@RequestMapping("/v1")
@Validated
class ValidationRuleController(
    private val validationRuleService: ValidationRuleService
) {

    @Operation(
        summary = "Create a validation rule",
        operationId = "createValidationRule",
        responses = [
            ApiResponse(responseCode = "201", description = "Validation rule created"),
            ApiResponse(responseCode = "400", description = "Validation error")
        ]
    )
    @PostMapping("/validation-rules")
    fun createValidationRule(@Valid @RequestBody body: ValidationRuleCreateRequest): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CREATED).body(validationRuleService.createRule(body))

    @Operation(
        summary = "Assign a validation rule to an object (voucher or campaign)",
        operationId = "assignValidationRule",
        responses = [
            ApiResponse(responseCode = "200", description = "Assignment created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Rule not found")
        ]
    )
    @PostMapping("/validation-rules/{id}/assignments")
    fun assignRule(
        @PathVariable id: String,
        @Valid @RequestBody body: ValidationRuleAssignmentRequest
    ): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.OK)
            .body(validationRuleService.assignRule(UUID.fromString(id), body))

    @Operation(
        summary = "Get validation rule",
        operationId = "getValidationRule",
        responses = [
            ApiResponse(responseCode = "200", description = "Validation rule found"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @GetMapping("/validation-rules/{id}")
    fun getRule(@PathVariable id: String): ResponseEntity<Any> {
        val rule = validationRuleService.getRule(UUID.fromString(id)) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(rule)
    }

    @Operation(
        summary = "List validation rules",
        operationId = "listValidationRules",
        responses = [
            ApiResponse(responseCode = "200", description = "List of rules")
        ]
    )
    @GetMapping("/validation-rules")
    fun listRules(): ResponseEntity<Any> = ResponseEntity.ok(validationRuleService.listRules())

    @Operation(
        summary = "Delete validation rule",
        operationId = "deleteValidationRule",
        responses = [
            ApiResponse(responseCode = "204", description = "Deleted"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @DeleteMapping("/validation-rules/{id}")
    fun deleteRule(@PathVariable id: String): ResponseEntity<Void> {
        val existing = validationRuleService.getRule(UUID.fromString(id)) ?: return ResponseEntity.notFound().build()
        validationRuleService.deleteRule(existing.id!!)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Update validation rule",
        operationId = "updateValidationRule",
        responses = [
            ApiResponse(responseCode = "200", description = "Updated"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @PutMapping("/validation-rules/{id}")
    fun updateRule(
        @PathVariable id: String,
        @Valid @RequestBody body: ValidationRuleCreateRequest
    ): ResponseEntity<Any> {
        val updated = validationRuleService.updateRule(UUID.fromString(id), body) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }
}
