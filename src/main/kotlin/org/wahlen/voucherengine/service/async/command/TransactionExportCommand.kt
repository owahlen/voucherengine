package org.wahlen.voucherengine.service.async.command

import org.springframework.context.ApplicationContext
import org.wahlen.voucherengine.persistence.model.async.AsyncJob
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.async.AsyncJobType
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import java.util.UUID

/**
 * Command for transaction export
 */
data class TransactionExportCommand(
    override var jobId: UUID? = null,
    override val tenantName: String,
    val voucherCode: String
) : AsyncJobCommand {
    
    override fun toAsyncJob(tenant: Tenant): AsyncJob {
        return AsyncJob(
            type = AsyncJobType.TRANSACTION_EXPORT,
            status = AsyncJobStatus.PENDING,
            total = 1,
            parameters = mapOf(
                "voucher_code" to voucherCode
            ),
            tenant = tenant
        )
    }
    
    override fun execute(context: ApplicationContext) {
        // TODO: Implement when transaction export handler is added
        throw UnsupportedOperationException("Transaction export not yet implemented")
    }
}
