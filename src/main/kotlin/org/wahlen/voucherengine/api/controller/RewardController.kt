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
@Tag(name = "Rewards", description = "Rewards management API")
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    ]
)
class RewardController {

    @Operation(
        summary = "List rewards",
        operationId = "listRewards",
        description = "List all rewards",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - rewards not yet supported")
        ]
    )
    @GetMapping("/rewards")
    fun listRewards(
        @RequestHeader("tenant") tenant: String
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Rewards management not yet implemented"))
    }

    @Operation(
        summary = "Get reward",
        operationId = "getReward",
        description = "Get a specific reward by ID",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - rewards not yet supported")
        ]
    )
    @GetMapping("/rewards/{rewardId}")
    fun getReward(
        @RequestHeader("tenant") tenant: String,
        @PathVariable rewardId: String
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Rewards management not yet implemented"))
    }

    @Operation(
        summary = "List reward assignments",
        operationId = "listRewardAssignments",
        description = "List assignments for a reward",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - rewards not yet supported")
        ]
    )
    @GetMapping("/rewards/{rewardId}/assignments")
    fun listRewardAssignments(
        @RequestHeader("tenant") tenant: String,
        @PathVariable rewardId: String
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Rewards management not yet implemented"))
    }

    @Operation(
        summary = "Get reward assignment",
        operationId = "getRewardAssignment",
        description = "Get a specific reward assignment",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - rewards not yet supported")
        ]
    )
    @GetMapping("/rewards/{rewardId}/assignments/{assignmentId}")
    fun getRewardAssignment(
        @RequestHeader("tenant") tenant: String,
        @PathVariable rewardId: String,
        @PathVariable assignmentId: String
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Rewards management not yet implemented"))
    }
}
