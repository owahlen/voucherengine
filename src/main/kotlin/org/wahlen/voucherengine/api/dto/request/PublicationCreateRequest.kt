package org.wahlen.voucherengine.api.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class PublicationCreateRequest(
    @field:Schema(description = "Optional publication source id", example = "publication_source_ID_10")
    var source_id: String? = null,
    @field:Valid
    @field:NotNull
    @field:Schema(description = "Customer data for the publication")
    var customer: CustomerReferenceDto? = null,
    @field:Schema(description = "Publication metadata")
    var metadata: Map<String, Any?>? = null,
    @field:Schema(description = "Publication channel", example = "api")
    var channel: String? = null,
    @field:Schema(description = "Voucher code to publish", example = "WELCOME-1234")
    var voucher: String? = null,
    @field:Valid
    @field:Schema(description = "Campaign details for publication")
    var campaign: PublicationCampaignRequest? = null
)
