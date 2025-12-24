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
@Tag(name = "Segments", description = "Customer segmentation API")
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    ]
)
class SegmentController {

    @Operation(
        summary = "List segments",
        operationId = "listSegments",
        description = "List all customer segments",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - customer segmentation not yet supported")
        ]
    )
    @GetMapping("/segments")
    fun listSegments(
        @RequestHeader("tenant") tenant: String
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Customer segmentation not yet implemented"))
    }

    @Operation(
        summary = "Get segment",
        operationId = "getSegment",
        description = "Get a specific segment by ID",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - customer segmentation not yet supported")
        ]
    )
    @GetMapping("/segments/{segmentId}")
    fun getSegment(
        @RequestHeader("tenant") tenant: String,
        @PathVariable segmentId: String
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Customer segmentation not yet implemented"))
    }
}
