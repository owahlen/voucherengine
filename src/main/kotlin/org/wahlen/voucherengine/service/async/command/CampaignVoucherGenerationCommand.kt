package org.wahlen.voucherengine.service.async.command

import org.springframework.context.ApplicationContext
import org.wahlen.voucherengine.api.dto.request.VoucherCreateRequest
import org.wahlen.voucherengine.persistence.model.async.AsyncJob
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.async.AsyncJobType
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.service.async.VoucherCommandService
import java.util.UUID

data class CampaignVoucherGenerationCommand(
    override var jobId: UUID? = null,
    override val tenantName: String,
    val campaignId: UUID,
    val voucherTemplate: VoucherCreateRequest,
    val count: Int
) : AsyncJobCommand {
    
    override fun toAsyncJob(tenant: Tenant): AsyncJob {
        return AsyncJob(
            type = AsyncJobType.CAMPAIGN_VOUCHER_GENERATION,
            status = AsyncJobStatus.PENDING,
            total = count,
            parameters = mapOf(
                "campaignId" to campaignId.toString(),
                "count" to count
            ),
            tenant = tenant
        )
    }
    
    override fun execute(context: ApplicationContext) {
        val service = context.getBean(VoucherCommandService::class.java)
        service.handleCampaignVoucherGeneration(this)
    }
}
