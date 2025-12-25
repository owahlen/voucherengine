package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.RestController
import org.wahlen.voucherengine.api.dto.request.ValidationRuleAssignmentRequest
import org.wahlen.voucherengine.api.dto.request.ValidationRuleCreateRequest
import org.wahlen.voucherengine.api.dto.response.ValidationRuleAssignmentResponse
import org.wahlen.voucherengine.api.dto.response.ValidationRuleResponse
import org.wahlen.voucherengine.service.ValidationRuleService
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
    fun createValidationRule(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody body: ValidationRuleCreateRequest
    ): ResponseEntity<ValidationRuleResponse> =
        ResponseEntity.status(HttpStatus.OK).body(validationRuleService.createRule(tenant, body))

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
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: String,
        @Valid @RequestBody body: ValidationRuleAssignmentRequest
    ): ResponseEntity<ValidationRuleAssignmentResponse> =
        ResponseEntity.status(HttpStatus.OK)
            .body(validationRuleService.assignRule(tenant, UUID.fromString(id), body))

    @Operation(
        summary = "Get validation rule",
        operationId = "getValidationRule",
        responses = [
            ApiResponse(responseCode = "200", description = "Validation rule found"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @GetMapping("/validation-rules/{id}")
    fun getRule(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: String
    ): ResponseEntity<ValidationRuleResponse> {
        val rule = validationRuleService.getRule(tenant, UUID.fromString(id)) ?: return ResponseEntity.notFound().build()
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
    fun listRules(@RequestHeader("tenant") tenant: String): ResponseEntity<List<ValidationRuleResponse>> =
        ResponseEntity.ok(validationRuleService.listRules(tenant))

    @Operation(
        summary = "List validation rule assignments",
        operationId = "listValidationRuleAssignments",
        responses = [
            ApiResponse(responseCode = "200", description = "List of assignments")
        ]
    )
    @GetMapping("/validation-rules-assignments")
    fun listAssignments(@RequestHeader("tenant") tenant: String): ResponseEntity<List<ValidationRuleAssignmentResponse>> =
        ResponseEntity.ok(validationRuleService.listAssignments(tenant))

    @Operation(
        summary = "List assignments for a rule",
        operationId = "listValidationRuleAssignmentsByRule",
        responses = [
            ApiResponse(responseCode = "200", description = "List of assignments"),
            ApiResponse(responseCode = "404", description = "Rule not found")
        ]
    )
    @GetMapping("/validation-rules/{id}/assignments")
    fun listAssignmentsForRule(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: String
    ): ResponseEntity<List<ValidationRuleAssignmentResponse>> {
        if (validationRuleService.getRule(tenant, UUID.fromString(id)) == null) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(validationRuleService.listAssignmentsForRule(tenant, UUID.fromString(id)))
    }

    @Operation(
        summary = "Delete validation rule assignment",
        operationId = "deleteValidationRuleAssignment",
        responses = [
            ApiResponse(responseCode = "204", description = "Deleted"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @DeleteMapping("/validation-rules-assignments/{assignmentId}")
    fun deleteAssignment(
        @RequestHeader("tenant") tenant: String,
        @PathVariable assignmentId: String
    ): ResponseEntity<Void> {
        val deleted = validationRuleService.deleteAssignment(tenant, UUID.fromString(assignmentId))
        return if (deleted) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    @Operation(
        summary = "Delete validation rule",
        operationId = "deleteValidationRule",
        responses = [
            ApiResponse(responseCode = "204", description = "Deleted"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @DeleteMapping("/validation-rules/{id}")
    fun deleteRule(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: String
    ): ResponseEntity<Void> {
        val existing = validationRuleService.getRule(tenant, UUID.fromString(id)) ?: return ResponseEntity.notFound().build()
        validationRuleService.deleteRule(tenant, existing.id!!)
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
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: String,
        @Valid @RequestBody body: ValidationRuleCreateRequest
    ): ResponseEntity<Any> {
        val updated = validationRuleService.updateRule(tenant, UUID.fromString(id), body) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }
}
