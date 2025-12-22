package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class ValidationRuleCreateRequest(
    @field:NotBlank
    @field:Schema(description = "Human-readable rule name", example = "Order total at least 10")
    var name: String? = null,
    @field:NotBlank
    @field:Schema(description = "Rule type", example = "order")
    var type: String? = null,
    @field:Schema(description = "Rule conditions payload", example = """{"lte":{"order.amount":1000}}""")
    var conditions: Map<String, Any?>? = null
)
