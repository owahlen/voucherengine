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
@Tag(name = "Locations", description = "Store/location management API")
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    ]
)
class LocationController {

    @Operation(
        summary = "List locations",
        operationId = "listLocations",
        description = "List all store/redemption locations",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - location management not yet supported")
        ]
    )
    @GetMapping("/locations")
    fun listLocations(
        @RequestHeader("tenant") tenant: String
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Location management not yet implemented"))
    }

    @Operation(
        summary = "Get location",
        operationId = "getLocation",
        description = "Get a specific location by ID",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - location management not yet supported")
        ]
    )
    @GetMapping("/locations/{locationId}")
    fun getLocation(
        @RequestHeader("tenant") tenant: String,
        @PathVariable locationId: String
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Location management not yet implemented"))
    }
}
