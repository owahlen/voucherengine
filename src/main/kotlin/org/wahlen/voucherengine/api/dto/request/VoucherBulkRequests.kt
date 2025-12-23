package org.wahlen.voucherengine.api.dto.request

data class VoucherBulkUpdateRequest(
    val code: String,
    val metadata: Map<String, Any?>
)

data class VoucherMetadataUpdateRequest(
    val codes: List<String>,
    val metadata: Map<String, Any?>
)

data class VoucherImportRequest(
    val vouchers: List<VoucherCreateRequest>
)
