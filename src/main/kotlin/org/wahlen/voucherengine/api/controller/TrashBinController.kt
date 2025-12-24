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
@Tag(name = "Trash Bin", description = "Trash bin / recycle bin API for soft-deleted resources")
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden"),
        ApiResponse(responseCode = "501", description = "Not implemented - trash bin not yet supported")
    ]
)
class TrashBinController {

    @Operation(
        summary = "List trash bin entries",
        operationId = "listTrashBinEntries",
        description = "List all soft-deleted resources in the trash bin"
    )
    @GetMapping("/trash-bin")
    fun listTrashBinEntries(@RequestHeader("tenant") tenant: String): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Trash bin not yet implemented"))
    }

    @Operation(
        summary = "Get trash bin entry",
        operationId = "getTrashBinEntry",
        description = "Get a specific soft-deleted resource from trash bin"
    )
    @GetMapping("/trash-bin/{binEntryId}")
    fun getTrashBinEntry(@RequestHeader("tenant") tenant: String, @PathVariable binEntryId: String): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Trash bin not yet implemented"))
    }
}
