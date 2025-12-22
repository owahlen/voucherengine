package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class StackingRulesResponse(
    @field:Schema(description = "Max redeemables per request")
    val redeemables_limit: Int? = null,
    @field:Schema(description = "Max applicable redeemables per request")
    val applicable_redeemables_limit: Int? = null,
    @field:Schema(description = "Max applicable redeemables per category")
    val applicable_redeemables_per_category_limit: Int? = null,
    @field:Schema(description = "Category limits")
    val applicable_redeemables_category_limits: Map<String, Int>? = null,
    @field:Schema(description = "Exclusive redeemables limit")
    val applicable_exclusive_redeemables_limit: Int? = null,
    @field:Schema(description = "Exclusive per category limit")
    val applicable_exclusive_redeemables_per_category_limit: Int? = null,
    @field:Schema(description = "Exclusive category ids")
    val exclusive_categories: List<String>? = null,
    @field:Schema(description = "Joint category ids")
    val joint_categories: List<String>? = null,
    @field:Schema(description = "Redeemables application mode")
    val redeemables_application_mode: String? = null,
    @field:Schema(description = "Redeemables sorting rule")
    val redeemables_sorting_rule: String? = null,
    @field:Schema(description = "Products application mode")
    val redeemables_products_application_mode: String? = null,
    @field:Schema(description = "No effect rule")
    val redeemables_no_effect_rule: String? = null,
    @field:Schema(description = "No effect skip categories")
    val no_effect_skip_categories: List<String>? = null,
    @field:Schema(description = "No effect redeem anyway categories")
    val no_effect_redeem_anyway_categories: List<String>? = null,
    @field:Schema(description = "Redeemables rollback order mode")
    val redeemables_rollback_order_mode: String? = null
)
