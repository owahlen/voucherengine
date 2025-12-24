package org.wahlen.voucherengine.service.async

import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.VoucherBulkUpdateRequest
import org.wahlen.voucherengine.api.dto.request.VoucherCreateRequest
import org.wahlen.voucherengine.api.dto.request.VoucherImportRequest
import org.wahlen.voucherengine.api.dto.request.VoucherMetadataUpdateRequest
import org.wahlen.voucherengine.service.TenantService
import org.wahlen.voucherengine.service.async.command.BulkUpdateCommand
import org.wahlen.voucherengine.service.async.command.CampaignVoucherGenerationCommand
import org.wahlen.voucherengine.service.async.command.MetadataUpdateCommand
import org.wahlen.voucherengine.service.async.command.VoucherImportCommand
import org.wahlen.voucherengine.service.async.command.VoucherUpdateItem
import java.util.UUID

/**
 * Domain-specific service for publishing voucher-related async jobs.
 * 
 * This service knows about voucher domain concepts and creates appropriate
 * commands, then delegates to the generic AsyncJobPublisher for persistence
 * and SQS publishing.
 */
@Service
class VoucherJobService(
    private val asyncJobPublisher: AsyncJobPublisher,
    private val tenantService: TenantService,
    private val objectMapper: ObjectMapper
) {

    /**
     * Publish a bulk voucher update job
     */
    @Transactional
    fun publishBulkUpdate(
        tenantName: String,
        updates: List<VoucherBulkUpdateRequest>
    ): UUID {
        val tenant = tenantService.requireTenant(tenantName)

        val updateItems = updates.map {
            VoucherUpdateItem(
                code = it.code,
                metadata = it.metadata.orEmpty()
            )
        }

        val command = BulkUpdateCommand(
            tenantName = tenantName,
            updates = updateItems
        )

        return asyncJobPublisher.publish(command, tenant)
    }

    /**
     * Publish a metadata update job
     */
    @Transactional
    fun publishMetadataUpdate(
        tenantName: String,
        request: VoucherMetadataUpdateRequest
    ): UUID {
        val tenant = tenantService.requireTenant(tenantName)

        val command = MetadataUpdateCommand(
            tenantName = tenantName,
            codes = request.codes,
            metadata = request.metadata
        )

        return asyncJobPublisher.publish(command, tenant)
    }

    /**
     * Publish a voucher import job
     */
    @Transactional
    fun publishVoucherImport(
        tenantName: String,
        request: VoucherImportRequest
    ): UUID {
        val tenant = tenantService.requireTenant(tenantName)

        // Serialize vouchers to JSON string for the command
        val vouchersJson = objectMapper.writeValueAsString(request.vouchers)

        val command = VoucherImportCommand(
            tenantName = tenantName,
            vouchers = vouchersJson,
            voucherCount = request.vouchers.size
        )

        return asyncJobPublisher.publish(command, tenant)
    }

    /**
     * Publish a campaign voucher generation job
     */
    @Transactional
    fun publishCampaignVoucherGeneration(
        tenantName: String,
        campaignId: UUID,
        voucherTemplate: VoucherCreateRequest,
        count: Int
    ): UUID {
        val tenant = tenantService.requireTenant(tenantName)

        val command = CampaignVoucherGenerationCommand(
            tenantName = tenantName,
            campaignId = campaignId,
            voucherTemplate = voucherTemplate,
            count = count
        )

        return asyncJobPublisher.publish(command, tenant)
    }
}
