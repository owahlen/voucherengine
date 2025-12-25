package org.wahlen.voucherengine.service

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.persistence.model.async.AsyncJob
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.order.Order
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.persistence.repository.OrderRepository
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayOutputStream
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Service for exporting orders to CSV or JSON format.
 */
@Service
class OrderExportService(
    private val orderRepository: OrderRepository,
    private val asyncJobRepository: AsyncJobRepository,
    private val s3Service: S3Service,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val BATCH_SIZE = 100
        private val DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT
    }

    /**
     * Execute order export: query orders, generate file, upload to S3.
     */
    @Transactional
    fun executeExport(jobId: UUID, tenantName: String, parameters: Map<String, Any?>) {
        val job = asyncJobRepository.findById(jobId).orElseThrow()
        
        try {
            job.status = AsyncJobStatus.IN_PROGRESS
            asyncJobRepository.save(job)

            val format = parameters["format"] as? String ?: "CSV"
            val filters = parseFilters(parameters)
            
            // Extract field selection (per Voucherify spec)
            @Suppress("UNCHECKED_CAST")
            val requestedFields = parameters["fields"] as? List<String>
            val fields = if (requestedFields.isNullOrEmpty()) {
                // Default fields when empty (per spec: id, source_id, status)
                listOf("id", "source_id", "status")
            } else {
                requestedFields
            }
            
            // Extract sort order (per Voucherify spec)
            val sortOrder = parameters["order"] as? String

            // Count total for progress tracking
            val total = countOrders(tenantName, filters)
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
            val fileName = "orders-${UUID.randomUUID()}.${format.lowercase()}"
            val contentType = if (format.uppercase() == "CSV") "text/csv" else "application/json"
            val url = s3Service.uploadExport(tenantName, fileName, content, contentType)

            // Mark as completed
            job.status = AsyncJobStatus.COMPLETED
            job.progress = total
            job.result = mapOf(
                "url" to url,
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
        filters: OrderFilters,
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
                val orders = fetchOrders(tenantName, filters, sortOrder, pageable)

                if (orders.isEmpty()) break

                orders.forEach { order ->
                    writer.write(toCsvRow(order, fields))
                    writer.newLine()
                }

                processedCount += orders.size
                updateProgress(job, processedCount)

                if (orders.size < BATCH_SIZE) break
                page++
            }
        }

        return output.toByteArray()
    }

    private fun generateJson(
        tenantName: String,
        filters: OrderFilters,
        fields: List<String>,
        sortOrder: String?,
        job: AsyncJob
    ): ByteArray {
        val allOrders = mutableListOf<Map<String, Any?>>()
        var processedCount = 0
        var page = 0

        while (true) {
            val pageable = PageRequest.of(page, BATCH_SIZE)
            val orders = fetchOrders(tenantName, filters, sortOrder, pageable)

            if (orders.isEmpty()) break

            orders.forEach { order ->
                allOrders.add(toJsonObject(order, fields))
            }

            processedCount += orders.size
            updateProgress(job, processedCount)

            if (orders.size < BATCH_SIZE) break
            page++
        }

        return objectMapper.writeValueAsBytes(allOrders)
    }

    private fun toCsvRow(order: Order, fields: List<String>): String {
        return fields.joinToString(",") { field ->
            val value = extractFieldValue(order, field)
            // Escape commas and quotes in CSV
            when (value) {
                null -> ""
                is String -> "\"${value.replace("\"", "\"\"")}\""
                else -> value.toString()
            }
        }
    }

    private fun toJsonObject(order: Order, fields: List<String>): Map<String, Any?> {
        return fields.associateWith { field ->
            extractFieldValue(order, field)
        }
    }
    
    /**
     * Extract field value from order, supporting metadata.X notation (per Voucherify spec).
     */
    private fun extractFieldValue(order: Order, field: String): Any? {
        return when {
            field.startsWith("metadata.") -> {
                // Extract specific metadata field (e.g., "metadata.payment_method")
                val metadataKey = field.substring("metadata.".length)
                order.metadata?.get(metadataKey)
            }
            field == "metadata" -> {
                // Return full metadata object
                order.metadata
            }
            else -> {
                // Standard fields
                when (field) {
                    "id" -> order.id.toString()
                    "source_id" -> order.sourceId
                    "status" -> order.status
                    "amount" -> order.amount
                    "initial_amount" -> order.initialAmount
                    "discount_amount" -> order.discountAmount
                    "items_discount_amount" -> 0L // Not in our model yet
                    "total_discount_amount" -> order.discountAmount ?: 0L
                    "total_amount" -> (order.amount ?: 0L) - (order.discountAmount ?: 0L)
                    "customer_id" -> order.customer?.id?.toString()
                    "referrer_id" -> null // Not in our model yet
                    "created_at" -> order.createdAt?.let { DATE_FORMATTER.format(it.atOffset(ZoneOffset.UTC)) }
                    "updated_at" -> order.updatedAt?.let { DATE_FORMATTER.format(it.atOffset(ZoneOffset.UTC)) }
                    else -> null // Unknown field
                }
            }
        }
    }
    
    private fun fetchOrders(tenantName: String, filters: OrderFilters, sortOrder: String?, pageable: Pageable): List<Order> {
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
                "source_id" -> "sourceId"
                else -> fieldName
            }
            PageRequest.of(pageable.pageNumber, pageable.pageSize, direction, entityField)
        } else {
            pageable
        }
        
        // Fetch orders with filters (currently basic filtering)
        return orderRepository.findAllByTenantName(tenantName, sortedPageable).content
    }

    private fun countOrders(tenantName: String, filters: OrderFilters): Int {
        // For now, count all orders for the tenant
        return orderRepository.countByTenantName(tenantName).toInt()
    }

    private fun parseFilters(parameters: Map<String, Any?>): OrderFilters {
        // TODO: Parse filters from parameters (status, date range, etc.)
        return OrderFilters()
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
            "message" to "No orders found matching the criteria"
        )
        asyncJobRepository.save(job)
    }

    /**
     * Placeholder for future filter support
     */
    data class OrderFilters(
        val status: String? = null,
        val createdAfter: String? = null,
        val createdBefore: String? = null
    )
}
