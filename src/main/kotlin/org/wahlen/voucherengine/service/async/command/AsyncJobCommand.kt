package org.wahlen.voucherengine.service.async.command

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.springframework.context.ApplicationContext
import java.util.UUID

/**
 * Base command interface for async job execution via SQS
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
    JsonSubTypes.Type(value = TransactionExportCommand::class, name = "TRANSACTION_EXPORT")
)
sealed interface AsyncJobCommand {
    val jobId: UUID
    val tenantName: String
    
    /**
     * Execute this async job command using the provided application context
     */
    fun execute(context: ApplicationContext)
}
