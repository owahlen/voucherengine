package org.wahlen.voucherengine.service.async.command

data class VoucherUpdateItem(
    val code: String,
    val metadata: Map<String, Any?>
)
