package org.wahlen.voucherengine.service.async.command

import org.springframework.context.ApplicationContext
import org.wahlen.voucherengine.service.async.VoucherAsyncService
import java.util.UUID

/**
 * Command for bulk voucher metadata update
 */
data class BulkUpdateCommand(
    override val jobId: UUID,
    override val tenantName: String,
    val updates: List<VoucherUpdateItem>
) : AsyncJobCommand {
    override fun execute(context: ApplicationContext) {
        val service = context.getBean(VoucherAsyncService::class.java)
        service.handleBulkUpdate(this)
    }
}
