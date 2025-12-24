package org.wahlen.voucherengine.service.async.command

import org.springframework.context.ApplicationContext
import org.wahlen.voucherengine.persistence.model.async.AsyncJob
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.async.AsyncJobType
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.service.async.VoucherCommandService
import java.util.UUID

/**
 * Command for voucher metadata async update (merges metadata)
 */
data class MetadataUpdateCommand(
    override var jobId: UUID? = null,
    override val tenantName: String,
    val codes: List<String>,
    val metadata: Map<String, Any?>
) : AsyncJobCommand {
    
    override fun toAsyncJob(tenant: Tenant): AsyncJob {
        return AsyncJob(
            type = AsyncJobType.VOUCHER_METADATA_UPDATE,
            status = AsyncJobStatus.PENDING,
            total = codes.size,
            parameters = mapOf(
                "codes_count" to codes.size
            )
        ).apply {
            this.tenant = tenant
        }
    }
    
    override fun execute(context: ApplicationContext) {
        val service = context.getBean(VoucherCommandService::class.java)
        service.handleMetadataUpdate(this)
    }
}
