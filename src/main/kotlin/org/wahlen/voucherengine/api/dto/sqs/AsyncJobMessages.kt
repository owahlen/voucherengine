package org.wahlen.voucherengine.api.dto.sqs

import java.util.UUID

/**
 * Base message interface for all SQS messages
 */
sealed interface AsyncJobMessage {
    val jobId: UUID
    val tenantName: String
}

/**
 * Message for bulk voucher metadata update
 */
data class BulkUpdateMessage(
    override val jobId: UUID,
    override val tenantName: String,
    val updates: List<VoucherUpdateItem>
) : AsyncJobMessage

data class VoucherUpdateItem(
    val code: String,
    val metadata: Map<String, Any?>
)

/**
 * Message for voucher metadata async update (merges metadata)
 */
data class MetadataUpdateMessage(
    override val jobId: UUID,
    override val tenantName: String,
    val codes: List<String>,
    val metadata: Map<String, Any?>
) : AsyncJobMessage

/**
 * Message for voucher import
 */
data class VoucherImportMessage(
    override val jobId: UUID,
    override val tenantName: String,
    val vouchers: String  // JSON string of List<VoucherCreateRequest>
) : AsyncJobMessage

/**
 * Message for transaction export
 */
data class TransactionExportMessage(
    override val jobId: UUID,
    override val tenantName: String,
    val voucherCode: String
) : AsyncJobMessage
