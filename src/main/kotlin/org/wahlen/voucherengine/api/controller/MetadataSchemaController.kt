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
@Tag(name = "Metadata Schemas", description = "Metadata schema management API")
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden"),
        ApiResponse(responseCode = "501", description = "Not implemented - metadata schemas not yet supported")
    ]
)
class MetadataSchemaController {

    @Operation(
        summary = "List metadata schemas",
        operationId = "listMetadataSchemas",
        description = "List all metadata schema definitions"
    )
    @GetMapping("/metadata-schemas")
    fun listMetadataSchemas(@RequestHeader("tenant") tenant: String): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Metadata schemas not yet implemented"))
    }

    @Operation(
        summary = "Get metadata schema for resource",
        operationId = "getMetadataSchema",
        description = "Get metadata schema definition for a specific resource type (voucher, customer, etc.)"
    )
    @GetMapping("/metadata-schemas/{resource}")
    fun getMetadataSchema(@RequestHeader("tenant") tenant: String, @PathVariable resource: String): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Metadata schemas not yet implemented"))
    }
}
