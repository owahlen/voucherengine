package org.wahlen.voucherengine.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class SegmentCreateRequest(
    @field:NotBlank
    var name: String,
    
    var type: String = "static", // auto-update, passive, static
    
    var filter: Map<String, Any?>? = null,
    
    @field:Size(max = 20000, message = "Static segments limited to 20,000 customers")
    var customers: List<String>? = null // Customer IDs or source_ids for static segments
)
