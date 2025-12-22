package org.wahlen.voucherengine.api.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class QualificationBundle(
    @field:Schema(description = "Bundle quantity")
    val quantity: Int? = null,
    @field:Schema(description = "Identified bundle items")
    val identified: List<QualificationBundleItem>? = null,
    @field:Schema(description = "Missing bundle items")
    val missing: List<QualificationBundleItem>? = null
)
