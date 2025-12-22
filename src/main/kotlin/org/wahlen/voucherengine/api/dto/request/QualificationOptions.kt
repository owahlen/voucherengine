package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class QualificationOptions(
    @field:Schema(description = "Max number of redeemables to return", example = "5")
    var limit: Int? = null,
    @field:Schema(description = "Cursor for pagination", example = "2023-10-31T12:13:16.374Z")
    var starting_after: Instant? = null,
    @field:Schema(description = "Qualification filters")
    var filters: QualificationFilters? = null,
    @field:Schema(description = "Expand options", example = """["redeemable","category"]""")
    var expand: List<String>? = null,
    @field:Schema(description = "Sorting rule", example = "DEFAULT")
    var sorting_rule: String? = null
)
