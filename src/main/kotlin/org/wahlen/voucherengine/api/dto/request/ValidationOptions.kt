package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class ValidationOptions(
    @field:Schema(description = "Expand options", example = """["order","redeemable","validation_rules"]""")
    val expand: List<String>? = null
)
