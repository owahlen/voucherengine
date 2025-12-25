package org.wahlen.voucherengine.service.async.command

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.context.ApplicationContext
import org.wahlen.voucherengine.persistence.model.async.AsyncJob
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import java.util.UUID

/**
 * Base command interface for async job execution via SQS.
 * 
 * Each command implementation knows how to:
 * 1. Create its own AsyncJob entity (toAsyncJob)
 * 2. Execute the actual work (execute)
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "jobType"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = BulkUpdateCommand::class, name = "BULK_VOUCHER_UPDATE"),
    JsonSubTypes.Type(value = MetadataUpdateCommand::class, name = "VOUCHER_METADATA_UPDATE"),
    JsonSubTypes.Type(value = VoucherImportCommand::class, name = "VOUCHER_IMPORT"),
    JsonSubTypes.Type(value = TransactionExportCommand::class, name = "TRANSACTION_EXPORT"),
    JsonSubTypes.Type(value = CampaignVoucherGenerationCommand::class, name = "CAMPAIGN_VOUCHER_GENERATION"),
    JsonSubTypes.Type(value = OrderImportCommand::class, name = "ORDER_IMPORT"),
    JsonSubTypes.Type(value = OrderExportCommand::class, name = "ORDER_EXPORT")
)
sealed interface AsyncJobCommand {
    var jobId: UUID?
    val tenantName: String
    
    /**
     * Create the AsyncJob entity for this command.
     * Called by AsyncJobPublisher before persisting the job.
     */
    fun toAsyncJob(tenant: Tenant): AsyncJob
    
    /**
     * Execute this async job command using the provided application context
     */
    fun execute(context: ApplicationContext)
}
