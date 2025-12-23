package org.wahlen.voucherengine.service.async.command

import org.springframework.context.ApplicationContext
import org.wahlen.voucherengine.service.async.VoucherAsyncService
import java.util.UUID

/**
 * Command for voucher import
 */
data class VoucherImportCommand(
    override val jobId: UUID,
    override val tenantName: String,
    val vouchers: String  // JSON string of List<VoucherCreateRequest>
) : AsyncJobCommand {
    override fun execute(context: ApplicationContext) {
        val service = context.getBean(VoucherAsyncService::class.java)
        service.handleVoucherImport(this)
    }
}
