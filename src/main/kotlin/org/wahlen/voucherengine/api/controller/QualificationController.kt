package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.wahlen.voucherengine.api.dto.request.QualificationRequest
import org.wahlen.voucherengine.api.dto.response.QualificationResponse
import org.wahlen.voucherengine.service.QualificationService

@RestController
@RequestMapping("/v1")
@Validated
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    ]
)
class QualificationController(
    private val qualificationService: QualificationService
) {

    @Operation(
        summary = "Check eligibility",
        operationId = "checkEligibility",
        responses = [
            ApiResponse(responseCode = "200", description = "Qualifications returned"),
            ApiResponse(responseCode = "400", description = "Validation error")
        ]
    )
    @PostMapping("/qualifications")
    fun qualify(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody body: QualificationRequest
    ): ResponseEntity<QualificationResponse> =
        ResponseEntity.ok(qualificationService.qualify(tenant, body))
}
