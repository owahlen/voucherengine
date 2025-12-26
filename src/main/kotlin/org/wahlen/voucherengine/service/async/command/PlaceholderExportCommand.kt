package org.wahlen.voucherengine.service.async.command

import org.springframework.context.ApplicationContext
import org.wahlen.voucherengine.persistence.model.async.AsyncJob
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.async.AsyncJobType
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.persistence.repository.ExportRepository
import java.util.*

/**
 * Placeholder command for export types that aren't yet implemented with async processing.
 * Immediately marks the export as DONE with empty result (only headers in CSV).
 */
data class PlaceholderExportCommand(
    override var jobId: UUID? = null,
    override val tenantName: String,
    val exportId: UUID,
    val exportedObject: String,
    val parameters: Map<String, Any?> = emptyMap()
) : AsyncJobCommand {

    override fun toAsyncJob(tenant: Tenant): AsyncJob {
        return AsyncJob(
            type = AsyncJobType.VOUCHER_EXPORT, // Reuse type for all exports
            status = AsyncJobStatus.PENDING,
            parameters = parameters,
            tenant = tenant
        )
    }

    override fun execute(context: ApplicationContext) {
        val asyncJobRepository = context.getBean(AsyncJobRepository::class.java)
        val exportRepository = context.getBean(ExportRepository::class.java)
        
        val currentJobId = jobId ?: throw IllegalStateException("Job ID must be set before execution")
        val job = asyncJobRepository.findById(currentJobId).orElseThrow()
        
        try {
            job.status = AsyncJobStatus.IN_PROGRESS
            asyncJobRepository.save(job)
            
            // Generate empty CSV with just headers
            val fields = getDefaultFields(exportedObject)
            val csvContent = fields.joinToString(",")
            
            // Update export entity
            val export = exportRepository.findById(exportId).orElseThrow()
            val token = UUID.randomUUID().toString().replace("-", "")
            export.status = "DONE"
            export.resultToken = token
            // For placeholder exports, we generate inline download URL
            export.resultUrl = "/v1/exports/${export.id}/download?token=$token"
            exportRepository.save(export)
            
            // Mark job as completed
            job.status = AsyncJobStatus.COMPLETED
            job.progress = 0
            job.total = 0
            job.result = mapOf(
                "recordCount" to 0,
                "message" to "Export type '$exportedObject' has no data (placeholder implementation)"
            )
            asyncJobRepository.save(job)
            
        } catch (e: Exception) {
            job.status = AsyncJobStatus.FAILED
            job.result = mapOf("error" to (e.message ?: "Unknown error"))
            asyncJobRepository.save(job)
            throw e
        }
    }
    
    private fun getDefaultFields(exportedObject: String): List<String> {
        return when (exportedObject) {
            "points_expiration" -> listOf("id", "campaign_id", "voucher_id", "status", "expires_at", "points")
            "voucher_transactions" -> listOf("id", "type", "source_id", "status", "reason", "source", "balance", "amount", "created_at")
            "redemption" -> listOf("id", "object", "voucher_code", "customer_id", "date", "result")
            "publication" -> listOf("code", "customer_id", "date", "channel")
            "customer" -> listOf("name", "source_id")
            "product" -> listOf("id", "name", "price", "image_url", "source_id", "attributes", "created_at")
            "sku" -> listOf("id", "sku", "product_id", "currency", "price", "image_url", "source_id", "attributes", "created_at")
            else -> listOf("id")
        }
    }
}
