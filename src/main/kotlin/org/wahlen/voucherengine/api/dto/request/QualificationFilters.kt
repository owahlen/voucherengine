package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema

data class QualificationFilters(
    @field:Schema(description = "Logical junction", example = "and")
    var junction: String? = null,
    var category_id: QualificationFieldConditions? = null,
    var campaign_id: QualificationFieldConditions? = null,
    var campaign_type: QualificationFieldConditions? = null,
    var resource_id: QualificationFieldConditions? = null,
    var resource_type: QualificationFieldConditions? = null,
    var voucher_type: QualificationFieldConditions? = null,
    var code: QualificationFieldConditions? = null,
    var holder_role: QualificationFieldConditions? = null
)
