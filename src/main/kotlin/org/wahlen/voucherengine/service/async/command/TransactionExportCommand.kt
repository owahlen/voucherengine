package org.wahlen.voucherengine.service.async.command

import org.springframework.context.ApplicationContext
import java.util.UUID

/**
 * Command for transaction export
 */
data class TransactionExportCommand(
    override val jobId: UUID,
    override val tenantName: String,
    val voucherCode: String
) : AsyncJobCommand {
    override fun execute(context: ApplicationContext) {
        // TODO: Implement when transaction export handler is added
        throw UnsupportedOperationException("Transaction export not yet implemented")
    }
}
