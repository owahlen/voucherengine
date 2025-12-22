package org.wahlen.voucherengine.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "voucherengine.stacking-rules")
data class StackingRulesProperties(
    val redeemablesLimit: Int = 30,
    val applicableRedeemablesLimit: Int = 5,
    val applicableRedeemablesPerCategoryLimit: Int = 1,
    val applicableRedeemablesCategoryLimits: Map<String, Int> = emptyMap(),
    val applicableExclusiveRedeemablesLimit: Int = 1,
    val applicableExclusiveRedeemablesPerCategoryLimit: Int = 1,
    val exclusiveCategories: List<String> = emptyList(),
    val jointCategories: List<String> = emptyList(),
    val redeemablesApplicationMode: String = "ALL",
    val redeemablesSortingRule: String = "REQUESTED_ORDER",
    val redeemablesProductsApplicationMode: String? = null,
    val redeemablesNoEffectRule: String? = null,
    val noEffectSkipCategories: List<String> = emptyList(),
    val noEffectRedeemAnywayCategories: List<String> = emptyList(),
    val redeemablesRollbackOrderMode: String? = "WITH_ORDER"
)
