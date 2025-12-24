package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1")
@Validated
@Tag(name = "Events", description = "Custom event tracking API")
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    ]
)
class EventController {

    @Operation(
        summary = "Track custom event",
        operationId = "trackCustomEvent",
        description = "Track a custom event for a customer",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - custom event tracking not yet supported")
        ]
    )
    @PostMapping("/events")
    fun trackCustomEvent(
        @RequestHeader("tenant") tenant: String,
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Custom event tracking not yet implemented"))
    }
}
