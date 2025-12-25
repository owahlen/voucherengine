package org.wahlen.voucherengine.service.async.command

import org.springframework.context.ApplicationContext
import org.wahlen.voucherengine.persistence.model.async.AsyncJob
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.async.AsyncJobType
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.service.async.OrderImportService
import java.util.*

/**
 * Command for importing orders asynchronously via SQS.
 */
data class OrderImportCommand(
    override var jobId: UUID? = null,
    override val tenantName: String,
    val orders: List<Map<String, Any?>>
) : AsyncJobCommand {

    override fun toAsyncJob(tenant: Tenant): AsyncJob {
        return AsyncJob(
            type = AsyncJobType.ORDER_IMPORT,
            status = AsyncJobStatus.PENDING,
            total = orders.size,
            parameters = mapOf("orderCount" to orders.size),
            tenant = tenant
        )
    }

    override fun execute(context: ApplicationContext) {
        val service = context.getBean(OrderImportService::class.java)
        service.processImport(jobId!!, tenantName, orders)
    }
}
