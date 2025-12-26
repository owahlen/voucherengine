package org.wahlen.voucherengine.service

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.persistence.model.async.AsyncJob
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayOutputStream
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Service for exporting vouchers to CSV or JSON format.
 */
@Service
class VoucherExportService(
    private val voucherRepository: VoucherRepository,
    private val asyncJobRepository: AsyncJobRepository,
    private val exportRepository: org.wahlen.voucherengine.persistence.repository.ExportRepository,
    private val s3Service: S3Service,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val BATCH_SIZE = 100
        private val DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT
    }

    /**
     * Execute voucher export: query vouchers, generate file, upload to S3, update Export entity.
     */
    @Transactional
    fun executeExport(jobId: UUID, tenantName: String, parameters: Map<String, Any?>) {
        val job = asyncJobRepository.findById(jobId).orElseThrow()
        val exportId = (parameters["exportId"] as? String)?.let { UUID.fromString(it) }
        
        try {
            job.status = AsyncJobStatus.IN_PROGRESS
            asyncJobRepository.save(job)

            val format = parameters["format"] as? String ?: "CSV"
            val filters = parseFilters(parameters)
            
            // Extract field selection (per Voucherify spec)
            @Suppress("UNCHECKED_CAST")
            val requestedFields = parameters["fields"] as? List<String>
            val fields = if (requestedFields.isNullOrEmpty()) {
                // Default fields when empty (common voucher fields per spec)
                listOf("code", "voucher_type", "campaign", "active", "created_at")
            } else {
                requestedFields
            }
            
            // Extract sort order (per Voucherify spec)
            val sortOrder = parameters["order"] as? String

            // Count total for progress tracking
            val total = countVouchers(tenantName, filters)
            job.total = total
            asyncJobRepository.save(job)

            if (total == 0) {
                completeWithEmptyResult(job, format)
                return
            }

            // Generate export file
            val content = when (format.uppercase()) {
                "CSV" -> generateCsv(tenantName, filters, fields, sortOrder, job)
                "JSON" -> generateJson(tenantName, filters, fields, sortOrder, job)
                else -> throw IllegalArgumentException("Unsupported format: $format")
            }

            // Upload to S3
            val fileName = "vouchers-${UUID.randomUUID()}.${format.lowercase()}"
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
            job.result = mapOf("error" to (e.message ?: "Unknown error"))
            asyncJobRepository.save(job)
            throw e
        }
    }

    private fun generateCsv(
        tenantName: String,
        filters: VoucherFilters,
        fields: List<String>,
        sortOrder: String?,
        job: AsyncJob
    ): ByteArray {
        val output = ByteArrayOutputStream()
        output.bufferedWriter().use { writer ->
            // CSV Header (use requested fields)
            writer.write(fields.joinToString(","))
            writer.newLine()

            var processedCount = 0
            var page = 0

            while (true) {
                val pageable = PageRequest.of(page, BATCH_SIZE)
                val vouchers = fetchVouchers(tenantName, filters, sortOrder, pageable)

                if (vouchers.isEmpty()) break

                vouchers.forEach { voucher ->
                    writer.write(toCsvRow(voucher, fields))
                    writer.newLine()
                }

                processedCount += vouchers.size
                updateProgress(job, processedCount)

                if (vouchers.size < BATCH_SIZE) break
                page++
            }
        }

        return output.toByteArray()
    }

    private fun generateJson(
        tenantName: String,
        filters: VoucherFilters,
        fields: List<String>,
        sortOrder: String?,
        job: AsyncJob
    ): ByteArray {
        val allVouchers = mutableListOf<Map<String, Any?>>()
        var processedCount = 0
        var page = 0

        while (true) {
            val pageable = PageRequest.of(page, BATCH_SIZE)
            val vouchers = fetchVouchers(tenantName, filters, sortOrder, pageable)

            if (vouchers.isEmpty()) break

            vouchers.forEach { voucher ->
                allVouchers.add(toJsonObject(voucher, fields))
            }

            processedCount += vouchers.size
            updateProgress(job, processedCount)

            if (vouchers.size < BATCH_SIZE) break
            page++
        }

        return objectMapper.writeValueAsBytes(allVouchers)
    }

    private fun toCsvRow(voucher: Voucher, fields: List<String>): String {
        return fields.joinToString(",") { field ->
            val value = extractFieldValue(voucher, field)
            // Escape commas and quotes in CSV
            when (value) {
                null -> ""
                is String -> "\"${value.replace("\"", "\"\"")}\""
                else -> value.toString()
            }
        }
    }

    private fun toJsonObject(voucher: Voucher, fields: List<String>): Map<String, Any?> {
        return fields.associateWith { field ->
            extractFieldValue(voucher, field)
        }
    }
    
    /**
     * Extract field value from voucher, supporting metadata.X notation (per Voucherify spec).
     */
    private fun extractFieldValue(voucher: Voucher, field: String): Any? {
        return when {
            field.startsWith("metadata.") -> {
                // Extract specific metadata field (e.g., "metadata.source")
                val metadataKey = field.substring("metadata.".length)
                voucher.metadata?.get(metadataKey)
            }
            field == "metadata" -> {
                // Return full metadata object
                voucher.metadata
            }
            else -> {
                // Standard fields per Voucherify spec
                when (field) {
                    "id" -> voucher.id.toString()
                    "code" -> voucher.code
                    "voucher_type" -> voucher.type?.name
                    "value" -> voucher.discountJson?.percent_off ?: voucher.discountJson?.amount_off
                    "discount_type" -> voucher.discountJson?.type?.name
                    "campaign" -> voucher.campaign?.name
                    "campaign_id" -> voucher.campaign?.id?.toString()
                    "category" -> voucher.categories.firstOrNull()?.name
                    "category_id" -> voucher.categories.firstOrNull()?.id?.toString()
                    "start_date" -> voucher.startDate?.let { DATE_FORMATTER.format(it.atOffset(ZoneOffset.UTC)) }
                    "expiration_date" -> voucher.expirationDate?.let { DATE_FORMATTER.format(it.atOffset(ZoneOffset.UTC)) }
                    "gift_balance" -> voucher.giftJson?.balance
                    "loyalty_balance" -> voucher.loyaltyCardJson?.balance
                    "redemption_quantity" -> voucher.redemptionJson?.quantity
                    "redemption_count" -> voucher.redemptions.size
                    "active" -> voucher.active
                    "qr_code" -> voucher.assets?.qrId
                    "bar_code" -> voucher.assets?.barcodeId
                    "is_referral_code" -> false // TODO: need referral support
                    "created_at" -> voucher.createdAt?.let { DATE_FORMATTER.format(it.atOffset(ZoneOffset.UTC)) }
                    "updated_at" -> voucher.updatedAt?.let { DATE_FORMATTER.format(it.atOffset(ZoneOffset.UTC)) }
                    "validity_timeframe_interval" -> voucher.validityTimeframe?.interval
                    "validity_timeframe_duration" -> voucher.validityTimeframe?.duration
                    "validity_day_of_week" -> voucher.validityDayOfWeek?.joinToString(",")
                    "discount_amount_limit" -> null // Not in our DiscountDto yet
                    "additional_info" -> voucher.additionalInfo
                    "customer_id" -> voucher.holder?.id?.toString()
                    "discount_unit_type" -> null // Not in our DiscountDto yet
                    "discount_unit_effect" -> null // Not in our DiscountDto yet
                    else -> null // Unknown field
                }
            }
        }
    }
    
    private fun fetchVouchers(tenantName: String, filters: VoucherFilters, sortOrder: String?, pageable: Pageable): List<Voucher> {
        // Apply sorting if specified (per Voucherify spec: "-field" for descending, "field" for ascending)
        val sortedPageable = if (sortOrder != null) {
            val direction = if (sortOrder.startsWith("-")) {
                org.springframework.data.domain.Sort.Direction.DESC
            } else {
                org.springframework.data.domain.Sort.Direction.ASC
            }
            val fieldName = sortOrder.removePrefix("-")
            // Map spec field names to entity field names
            val entityField = when (fieldName) {
                "created_at" -> "createdAt"
                "updated_at" -> "updatedAt"
                "expiration_date" -> "expirationDate"
                "start_date" -> "startDate"
                "campaign_id" -> "campaign.id"
                else -> fieldName
            }
            PageRequest.of(pageable.pageNumber, pageable.pageSize, direction, entityField)
        } else {
            pageable
        }
        
        // Apply campaign filter if specified
        return if (filters.campaignIds.isNotEmpty()) {
            voucherRepository.findAllByCampaignIdInAndTenantName(filters.campaignIds, tenantName, sortedPageable).content
        } else {
            voucherRepository.findAllByTenantName(tenantName, sortedPageable).content
        }
    }

    private fun countVouchers(tenantName: String, filters: VoucherFilters): Int {
        return if (filters.campaignIds.isNotEmpty()) {
            voucherRepository.countByCampaignIdInAndTenantName(filters.campaignIds, tenantName).toInt()
        } else {
            voucherRepository.countByTenantName(tenantName).toInt()
        }
    }

    private fun parseFilters(parameters: Map<String, Any?>): VoucherFilters {
        @Suppress("UNCHECKED_CAST")
        val filters = parameters["filters"] as? Map<String, Any?>
        
        // Extract campaign_ids filter (per Voucherify spec)
        val campaignIds = if (filters != null) {
            @Suppress("UNCHECKED_CAST")
            val campaignIdsFilter = filters["campaign_ids"] as? Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val conditions = campaignIdsFilter?.get("conditions") as? Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val inList = conditions?.get("\$in") as? List<String>
            inList?.mapNotNull { 
                try {
                    UUID.fromString(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            } ?: emptyList()
        } else {
            emptyList()
        }
        
        return VoucherFilters(campaignIds = campaignIds)
    }

    private fun updateProgress(job: AsyncJob, processedCount: Int) {
        job.progress = processedCount
        asyncJobRepository.save(job)
    }

    private fun completeWithEmptyResult(job: AsyncJob, format: String) {
        job.status = AsyncJobStatus.COMPLETED
        job.progress = 0
        job.total = 0
        job.result = mapOf(
            "recordCount" to 0,
            "format" to format,
            "message" to "No vouchers found matching the criteria"
        )
        asyncJobRepository.save(job)
    }

    /**
     * Filters for voucher export
     */
    data class VoucherFilters(
        val campaignIds: List<UUID> = emptyList()
    )
}
