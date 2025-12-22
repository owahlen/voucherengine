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
    @field:Schema(description = "Rule context type", example = "voucher.discount_voucher")
    var context_type: String? = null,
    @field:Schema(
        description = "Full rule set payload with numbered rules and optional logic. See docs/voucherify-rule-semantics.md.",
        example = """{"rules":{"1":{"name":"order.amount","conditions":{"${'$'}gte":1000}}},"logic":"1"}"""
    )
    var rules: Map<String, Any?>? = null,
    @field:Schema(description = "Logic expression combining rules (optional if embedded in rules payload)", example = "1 and 2")
    var logic: String? = null,
    @field:Schema(
        description = "Legacy conditions payload (kept for backward compatibility); prefer `rules` + `logic`.",
        example = """{"rules":{"1":{"name":"order.amount","conditions":{"${'$'}gte":1000}}},"logic":"1"}"""
    )
    var conditions: Map<String, Any?>? = null,
    @field:Schema(description = "Error payload with code/message", example = """{"code":"rule_failed","message":"Not allowed"}""")
    var error: Map<String, Any?>? = null,
    @field:Schema(description = "Bundle rules payload")
    var bundle_rules: Map<String, Any?>? = null,
    @field:Schema(description = "Applicable to payload")
    var applicable_to: Map<String, Any?>? = null
)
