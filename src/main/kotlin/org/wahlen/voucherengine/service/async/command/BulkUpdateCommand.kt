package org.wahlen.voucherengine.service.async.command

import org.springframework.context.ApplicationContext
import org.wahlen.voucherengine.persistence.model.async.AsyncJob
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.async.AsyncJobType
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.service.async.VoucherCommandService
import java.util.UUID

/**
 * Command for bulk voucher metadata update
 */
data class BulkUpdateCommand(
    override var jobId: UUID? = null,
    override val tenantName: String,
    val updates: List<VoucherUpdateItem>
) : AsyncJobCommand {
    
    override fun toAsyncJob(tenant: Tenant): AsyncJob {
        return AsyncJob(
            type = AsyncJobType.BULK_VOUCHER_UPDATE,
            status = AsyncJobStatus.PENDING,
            total = updates.size,
            parameters = mapOf(
                "update_count" to updates.size
            ),
            tenant = tenant
        )
    }
    
    override fun execute(context: ApplicationContext) {
        val service = context.getBean(VoucherCommandService::class.java)
        service.handleBulkUpdate(this)
    }
}
