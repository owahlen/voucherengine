package org.wahlen.voucherengine.api.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ErrorDto(
    @field:Schema(
        description = "Unix time of the error",
        example = "1737568182590"
    )
    val timestamp: Long,

    @field:Schema(
        description = "http status of the response",
        example = "400"
    )
    val status: Int,

    @field:Schema(
        description = "name of the error",
        example = "Bad Request"
    )
    val error: String,

    @field:Schema(
        description = "description of the error",
        example = "Parameter validation failed"
    )
    val message: String,

    @field:Schema(
        description = "list of parameter validation errors if applicable",
        example = """["'email' must be a well-formed email address"]""",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    val errors: List<String>?,

    @field:Schema(
        description = "request path",
        example = "/users/signup"
    )
    val path: String
)
