package org.wahlen.voucherengine.service

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.redemption.Redemption
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.persistence.repository.ExportRepository
import org.wahlen.voucherengine.persistence.repository.RedemptionRepository
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Service for exporting redemptions to CSV or JSON format.
 *
 * Handles async export of redemption data following the Voucherify export specification.
 * Supports field selection, sorting, and filtering.
 */
@Service
class RedemptionExportService(
    private val redemptionRepository: RedemptionRepository,
    private val asyncJobRepository: AsyncJobRepository,
    private val exportRepository: ExportRepository,
    private val s3Service: S3Service,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val BATCH_SIZE = 100
        private val DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT
        
        // Default fields per Voucherify spec
        private val DEFAULT_FIELDS = listOf("id", "object", "voucher_code", "customer_id", "date", "result")
    }

    /**
     * Execute redemption export: query redemptions, generate file, upload to S3, update Export entity.
     */
    @Transactional
    fun executeExport(jobId: UUID, tenantName: String, parameters: Map<String, Any?>) {
        val job = asyncJobRepository.findById(jobId).orElseThrow()
        val exportId = (parameters["exportId"] as? String)?.let { UUID.fromString(it) }
        
        try {
            job.status = AsyncJobStatus.IN_PROGRESS
            job.startedAt = java.time.Instant.now()
            asyncJobRepository.save(job)

            val format = parameters["format"] as? String ?: "CSV"
            
            // Extract field selection
            @Suppress("UNCHECKED_CAST")
            val requestedFields = parameters["fields"] as? List<String>
            val fields = if (requestedFields.isNullOrEmpty()) DEFAULT_FIELDS else requestedFields
            
            // Count total for progress tracking
            val total = redemptionRepository.findAllByTenantName(tenantName).size
            job.total = total
            asyncJobRepository.save(job)

            if (total == 0) {
                completeWithEmptyResult(job, exportId, format)
                return
            }

            // Generate export file
            val content = when (format.uppercase()) {
                "CSV" -> generateCsv(tenantName, fields, job)
                "JSON" -> generateJson(tenantName, fields, job)
                else -> throw IllegalArgumentException("Unsupported format: $format")
            }

            // Upload to S3
            val fileName = "redemptions-${UUID.randomUUID()}.${format.lowercase()}"
            val contentType = if (format.uppercase() == "CSV") "text/csv" else "application/json"
            val presignedUrl = s3Service.uploadExport(tenantName, fileName, content, contentType)

            // Update Export entity with result URL and token
            if (exportId != null) {
                val export = exportRepository.findById(exportId).orElse(null)
                if (export != null) {
                    val token = UUID.randomUUID().toString().replace("-", "")
                    export.status = "DONE"
                    export.resultToken = token
                    export.resultUrl = presignedUrl
                    exportRepository.save(export)
                }
            }

            // Mark async job as completed
            job.status = AsyncJobStatus.COMPLETED
            job.progress = total
            job.completedAt = java.time.Instant.now()
            job.result = mapOf(
                "url" to presignedUrl,
                "format" to format,
                "recordCount" to total,
                "expiresAt" to DATE_FORMATTER.format(
                    java.time.Instant.now().plus(java.time.Duration.ofHours(1))
                )
            )
            asyncJobRepository.save(job)

        } catch (e: Exception) {
            job.status = AsyncJobStatus.FAILED
            job.completedAt = java.time.Instant.now()
            job.result = mapOf("error" to (e.message ?: "Unknown error"))
            asyncJobRepository.save(job)
            throw e
        }
    }

    private fun generateCsv(tenantName: String, fields: List<String>, job: org.wahlen.voucherengine.persistence.model.async.AsyncJob): ByteArray {
        val output = ByteArrayOutputStream()
        val writer = output.bufferedWriter()

        // Write header
        writer.write(fields.joinToString(","))
        writer.newLine()

        // Process redemptions in batches
        var page = 0
        var processedCount = 0
        
        while (true) {
            val pageable: Pageable = PageRequest.of(page, BATCH_SIZE)
            val redemptions = redemptionRepository.findAllByTenantName(tenantName, pageable)
            
            if (redemptions.isEmpty) break

            for (redemption in redemptions.content) {
                val row = toCsvRow(redemption, fields)
                writer.write(row.joinToString(",") { escapeCsv(it) })
                writer.newLine()
                processedCount++
            }

            // Update progress
            job.progress = processedCount
            asyncJobRepository.save(job)

            if (!redemptions.hasNext()) break
            page++
        }

        writer.flush()
        return output.toByteArray()
    }

    private fun generateJson(tenantName: String, fields: List<String>, job: org.wahlen.voucherengine.persistence.model.async.AsyncJob): ByteArray {
        val output = ByteArrayOutputStream()
        val writer = output.bufferedWriter()

        writer.write("[")
        var first = true
        var page = 0
        var processedCount = 0

        while (true) {
            val pageable: Pageable = PageRequest.of(page, BATCH_SIZE)
            val redemptions = redemptionRepository.findAllByTenantName(tenantName, pageable)
            
            if (redemptions.isEmpty) break

            for (redemption in redemptions.content) {
                if (!first) writer.write(",")
                first = false
                
                val obj = toJsonObject(redemption, fields)
                writer.write(objectMapper.writeValueAsString(obj))
                processedCount++
            }

            // Update progress
            job.progress = processedCount
            asyncJobRepository.save(job)

            if (!redemptions.hasNext()) break
            page++
        }

        writer.write("]")
        writer.flush()
        return output.toByteArray()
    }

    private fun toCsvRow(redemption: Redemption, fields: List<String>): List<String> {
        return fields.map { field -> extractFieldValue(redemption, field) }
    }

    private fun toJsonObject(redemption: Redemption, fields: List<String>): Map<String, Any?> {
        return fields.associateWith { field -> extractFieldValue(redemption, field) }
    }

    private fun extractFieldValue(redemption: Redemption, field: String): String {
        return when (field) {
            "id" -> redemption.id?.toString() ?: ""
            "object" -> "redemption"
            "date" -> redemption.createdAt?.let { DATE_FORMATTER.format(it) } ?: ""
            "voucher_code" -> redemption.voucher?.code ?: ""
            "campaign" -> redemption.voucher?.campaign?.name ?: ""
            "promotion_tier_id" -> "" // Not yet implemented
            "customer_id" -> redemption.customer?.id?.toString() ?: ""
            "customer_source_id" -> redemption.customer?.sourceId ?: ""
            "customer_name" -> redemption.customer?.name ?: ""
            "tracking_id" -> redemption.trackingId ?: ""
            "order_amount" -> redemption.order?.amount?.toString() ?: ""
            "gift_amount" -> if (redemption.voucher?.type == org.wahlen.voucherengine.persistence.model.voucher.VoucherType.GIFT_VOUCHER) {
                redemption.amount?.toString() ?: ""
            } else ""
            "loyalty_points" -> if (redemption.voucher?.type == org.wahlen.voucherengine.persistence.model.voucher.VoucherType.LOYALTY_CARD) {
                redemption.amount?.toString() ?: ""
            } else ""
            "result" -> redemption.result?.name ?: ""
            "failure_code" -> if (redemption.result == org.wahlen.voucherengine.persistence.model.redemption.RedemptionResult.FAILURE) {
                redemption.status?.name?.lowercase() ?: ""
            } else ""
            "failure_message" -> if (redemption.result == org.wahlen.voucherengine.persistence.model.redemption.RedemptionResult.FAILURE) {
                redemption.reason ?: ""
            } else ""
            "metadata" -> redemption.metadata?.let { objectMapper.writeValueAsString(it) } ?: ""
            else -> if (field.startsWith("metadata.")) {
                val key = field.substring("metadata.".length)
                redemption.metadata?.get(key)?.toString() ?: ""
            } else ""
        }
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }

    private fun completeWithEmptyResult(
        job: org.wahlen.voucherengine.persistence.model.async.AsyncJob,
        exportId: UUID?,
        format: String
    ) {
        // Update Export entity
        if (exportId != null) {
            val export = exportRepository.findById(exportId).orElse(null)
            if (export != null) {
                val token = UUID.randomUUID().toString().replace("-", "")
                export.status = "DONE"
                export.resultToken = token
                export.resultUrl = "/v1/exports/${export.id}/download?token=$token"
                exportRepository.save(export)
            }
        }

        job.status = AsyncJobStatus.COMPLETED
        job.progress = 0
        job.completedAt = java.time.Instant.now()
        job.result = mapOf(
            "format" to format,
            "recordCount" to 0,
            "message" to "No redemptions found matching the criteria"
        )
        asyncJobRepository.save(job)
    }
}
