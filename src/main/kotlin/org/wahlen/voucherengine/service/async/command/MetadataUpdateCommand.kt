package org.wahlen.voucherengine.service.async.command

import org.springframework.context.ApplicationContext
import org.wahlen.voucherengine.service.async.VoucherAsyncService
import java.util.UUID

/**
 * Command for voucher metadata async update (merges metadata)
 */
data class MetadataUpdateCommand(
    override val jobId: UUID,
    override val tenantName: String,
    val codes: List<String>,
    val metadata: Map<String, Any?>
) : AsyncJobCommand {
    override fun execute(context: ApplicationContext) {
        val service = context.getBean(VoucherAsyncService::class.java)
        service.handleMetadataUpdate(this)
    }
}
