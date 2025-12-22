package org.wahlen.voucherengine.api.dto.request

data class QualificationFilterConditions(
    var `is`: List<String>? = null,
    var is_not: List<String>? = null,
    var `in`: List<String>? = null,
    var not_in: List<String>? = null
)
