package org.wahlen.voucherengine.service.async.command

import org.springframework.context.ApplicationContext
import org.wahlen.voucherengine.persistence.model.async.AsyncJob
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.async.AsyncJobType
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.service.PublicationExportService
import java.util.*

/**
 * Command for exporting publications asynchronously via SQS.
 */
data class PublicationExportCommand(
    override var jobId: UUID? = null,
    override val tenantName: String,
    val parameters: Map<String, Any?> = emptyMap()
) : AsyncJobCommand {

    override fun toAsyncJob(tenant: Tenant): AsyncJob {
        return AsyncJob(
            type = AsyncJobType.PUBLICATION_EXPORT,
            status = AsyncJobStatus.PENDING,
            parameters = parameters,
            tenant = tenant
        )
    }

    override fun execute(context: ApplicationContext) {
        val exportService = context.getBean(PublicationExportService::class.java)
        val currentJobId = jobId ?: throw IllegalStateException("Job ID must be set before execution")
        exportService.executeExport(currentJobId, tenantName, parameters)
    }
}
