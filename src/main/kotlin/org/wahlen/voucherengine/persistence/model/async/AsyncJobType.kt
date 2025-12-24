package org.wahlen.voucherengine.persistence.model.async

enum class AsyncJobType {
    BULK_VOUCHER_UPDATE,
    VOUCHER_METADATA_UPDATE,
    VOUCHER_IMPORT,
    TRANSACTION_EXPORT,
    CAMPAIGN_VOUCHER_GENERATION
}
