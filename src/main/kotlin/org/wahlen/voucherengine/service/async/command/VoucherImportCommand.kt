package org.wahlen.voucherengine.service.async.command

import org.springframework.context.ApplicationContext
import org.wahlen.voucherengine.persistence.model.async.AsyncJob
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.async.AsyncJobType
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.service.async.VoucherCommandService
import java.util.UUID

/**
 * Command for voucher import
 */
data class VoucherImportCommand(
    override var jobId: UUID? = null,
    override val tenantName: String,
    val vouchers: String,  // JSON string of List<VoucherCreateRequest>
    val voucherCount: Int
) : AsyncJobCommand {
    
    override fun toAsyncJob(tenant: Tenant): AsyncJob {
        return AsyncJob(
            type = AsyncJobType.VOUCHER_IMPORT,
            status = AsyncJobStatus.PENDING,
            total = voucherCount,
            parameters = mapOf(
                "voucher_count" to voucherCount
            ),
            tenant = tenant
        )
    }
    
    override fun execute(context: ApplicationContext) {
        val service = context.getBean(VoucherCommandService::class.java)
        service.handleVoucherImport(this)
    }
}
