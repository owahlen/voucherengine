package org.wahlen.voucherengine.api.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class QualificationFilterConditions(
    @JsonProperty("\$is")
    var `is`: List<String>? = null,
    @JsonProperty("\$is_not")
    var is_not: List<String>? = null,
    @JsonProperty("\$in")
    var `in`: List<String>? = null,
    @JsonProperty("\$not_in")
    var not_in: List<String>? = null,
    @JsonProperty("\$has_value")
    var has_value: Boolean? = null,
    @JsonProperty("\$is_unknown")
    var is_unknown: Boolean? = null
)
