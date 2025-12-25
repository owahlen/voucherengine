package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.wahlen.voucherengine.api.dto.request.SegmentCreateRequest
import org.wahlen.voucherengine.api.dto.response.SegmentListResponse
import org.wahlen.voucherengine.api.dto.response.SegmentResponse
import org.wahlen.voucherengine.service.SegmentService
import java.util.UUID

@RestController
@RequestMapping("/v1/segments")
@Validated
@Tag(name = "Segments", description = "Customer segmentation API")
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    ]
)
class SegmentController(
    private val segmentService: SegmentService
) {

    @Operation(
        summary = "Create segment",
        operationId = "createSegment",
        description = """
            Create a customer segment.
            
            **Segment Types:**
            - `static`: Manually selected customers (max 20,000)
            - `auto-update`: Customers enter/leave based on filter with events
            - `passive`: Customers enter/leave based on filter without events
            
            **Note:** Dynamic filters (auto-update/passive) are not yet fully implemented.
        """,
        responses = [
            ApiResponse(responseCode = "201", description = "Segment created"),
            ApiResponse(responseCode = "400", description = "Invalid request")
        ]
    )
    @PostMapping
    fun createSegment(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody request: SegmentCreateRequest
    ): ResponseEntity<SegmentResponse> {
        val segment = segmentService.create(tenant, request)
        return ResponseEntity.status(HttpStatus.OK).body(segment)
    }

    @Operation(
        summary = "Get segment",
        operationId = "getSegment",
        description = "Get a specific segment by ID",
        responses = [
            ApiResponse(responseCode = "200", description = "Segment found"),
            ApiResponse(responseCode = "404", description = "Segment not found")
        ]
    )
    @GetMapping("/{segmentId}")
    fun getSegment(
        @RequestHeader("tenant") tenant: String,
        @Parameter(description = "Segment ID") @PathVariable segmentId: UUID
    ): ResponseEntity<SegmentResponse> {
        val segment = segmentService.get(tenant, segmentId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(segment)
    }

    @Operation(
        summary = "List segments",
        operationId = "listSegments",
        description = "List all segments for the tenant",
        responses = [
            ApiResponse(responseCode = "200", description = "List of segments")
        ]
    )
    @GetMapping
    fun listSegments(
        @RequestHeader("tenant") tenant: String
    ): ResponseEntity<SegmentListResponse> {
        val segments = segmentService.list(tenant)
        return ResponseEntity.ok(SegmentListResponse(segments = segments))
    }

    @Operation(
        summary = "Delete segment",
        operationId = "deleteSegment",
        description = "Delete a segment",
        responses = [
            ApiResponse(responseCode = "204", description = "Segment deleted"),
            ApiResponse(responseCode = "404", description = "Segment not found")
        ]
    )
    @DeleteMapping("/{segmentId}")
    fun deleteSegment(
        @RequestHeader("tenant") tenant: String,
        @Parameter(description = "Segment ID") @PathVariable segmentId: UUID
    ): ResponseEntity<Void> {
        val deleted = segmentService.delete(tenant, segmentId)
        return if (deleted) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
    }
}
