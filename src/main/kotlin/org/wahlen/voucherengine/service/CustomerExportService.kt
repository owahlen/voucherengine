package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.redemption.RedemptionResult
import org.wahlen.voucherengine.persistence.model.redemption.RedemptionStatus
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.ExportRepository
import org.wahlen.voucherengine.persistence.repository.OrderRepository
import org.wahlen.voucherengine.persistence.repository.RedemptionRepository
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Service for exporting customers to CSV or JSON format with aggregated statistics.
 *
 * Handles async export of customer data following the Voucherify export specification.
 * Includes computed aggregates for redemptions and orders per customer.
 */
@Service
class CustomerExportService(
    private val customerRepository: CustomerRepository,
    private val redemptionRepository: RedemptionRepository,
    private val orderRepository: OrderRepository,
    private val asyncJobRepository: AsyncJobRepository,
    private val exportRepository: ExportRepository,
    private val s3Service: S3Service,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val BATCH_SIZE = 100
        private val DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT
        
        // Default fields per Voucherify spec
        private val DEFAULT_FIELDS = listOf("name", "source_id")
    }

    /**
     * Execute customer export: query customers, compute aggregates, generate file, upload to S3.
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
            val total = customerRepository.findAllByTenantName(tenantName).size
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
            val fileName = "customers-${UUID.randomUUID()}.${format.lowercase()}"
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

        // Load all redemptions and orders once for efficiency
        val allRedemptions = redemptionRepository.findAllByTenantName(tenantName)
        val allOrders = orderRepository.findAllByTenantName(tenantName)

        // Process customers
        val customers = customerRepository.findAllByTenantName(tenantName)
        var processedCount = 0

        for (customer in customers) {
            val row = toCsvRow(customer, tenantName, fields, allRedemptions, allOrders)
            writer.write(row.joinToString(",") { escapeCsv(it) })
            writer.newLine()
            processedCount++
            
            // Update progress periodically
            if (processedCount % BATCH_SIZE == 0) {
                job.progress = processedCount
                asyncJobRepository.save(job)
            }
        }

        writer.flush()
        return output.toByteArray()
    }

    private fun generateJson(tenantName: String, fields: List<String>, job: org.wahlen.voucherengine.persistence.model.async.AsyncJob): ByteArray {
        val output = ByteArrayOutputStream()
        val writer = output.bufferedWriter()

        writer.write("[")
        var first = true
        
        // Load all redemptions and orders once for efficiency
        val allRedemptions = redemptionRepository.findAllByTenantName(tenantName)
        val allOrders = orderRepository.findAllByTenantName(tenantName)
        
        val customers = customerRepository.findAllByTenantName(tenantName)
        var processedCount = 0

        for (customer in customers) {
            if (!first) writer.write(",")
            first = false
            
            val obj = toJsonObject(customer, tenantName, fields, allRedemptions, allOrders)
            writer.write(objectMapper.writeValueAsString(obj))
            processedCount++
            
            // Update progress periodically
            if (processedCount % BATCH_SIZE == 0) {
                job.progress = processedCount
                asyncJobRepository.save(job)
            }
        }

        writer.write("]")
        writer.flush()
        return output.toByteArray()
    }

    private fun toCsvRow(customer: Customer, tenantName: String, fields: List<String>, 
                         redemptions: List<org.wahlen.voucherengine.persistence.model.redemption.Redemption>,
                         orders: List<org.wahlen.voucherengine.persistence.model.order.Order>): List<String> {
        return fields.map { field -> extractFieldValue(customer, tenantName, field, redemptions, orders) }
    }

    private fun toJsonObject(customer: Customer, tenantName: String, fields: List<String>,
                             redemptions: List<org.wahlen.voucherengine.persistence.model.redemption.Redemption>,
                             orders: List<org.wahlen.voucherengine.persistence.model.order.Order>): Map<String, Any?> {
        return fields.associateWith { field -> extractFieldValue(customer, tenantName, field, redemptions, orders) }
    }

    private fun extractFieldValue(customer: Customer, tenantName: String, field: String,
                                  redemptions: List<org.wahlen.voucherengine.persistence.model.redemption.Redemption>,
                                  orders: List<org.wahlen.voucherengine.persistence.model.order.Order>): String {
        return when (field) {
            // Basic fields
            "id" -> customer.id?.toString() ?: ""
            "source_id" -> customer.sourceId ?: ""
            "name" -> customer.name ?: ""
            "description" -> customer.description ?: ""
            "email" -> customer.email ?: ""
            "phone" -> customer.phone ?: ""
            "created_at" -> customer.createdAt?.let { DATE_FORMATTER.format(it) } ?: ""
            "updated_at" -> customer.updatedAt?.let { DATE_FORMATTER.format(it) } ?: ""
            "birthdate" -> customer.birthdate?.toString() ?: ""
            "birthday" -> customer.birthdate?.toString() ?: "" // Deprecated, same as birthdate
            
            // Address fields
            "address_city" -> customer.address?.city ?: ""
            "address_state" -> customer.address?.state ?: ""
            "address_line_1" -> customer.address?.line_1 ?: ""
            "address_line_2" -> customer.address?.line_2 ?: ""
            "address_country" -> customer.address?.country ?: ""
            "address_postal_code" -> customer.address?.postalCode ?: ""
            
            // Redemption aggregates
            "redemptions_total_redeemed" -> calculateRedemptionsTotalRedeemed(customer, redemptions).toString()
            "redemptions_total_failed" -> calculateRedemptionsTotalFailed(customer, redemptions).toString()
            "redemptions_total_succeeded" -> calculateRedemptionsTotalSucceeded(customer, redemptions).toString()
            "redemptions_total_rolled_back" -> "0" // Rollback tracking not yet fully implemented
            "redemptions_total_rollback_failed" -> "0" // Rollback tracking not yet fully implemented
            "redemptions_total_rollback_succeeded" -> "0" // Rollback tracking not yet fully implemented
            
            // Order aggregates
            "orders_total_count" -> calculateOrdersTotalCount(customer, orders).toString()
            "orders_total_amount" -> calculateOrdersTotalAmount(customer, orders).toString()
            "orders_average_amount" -> calculateOrdersAverageAmount(customer, orders).toString()
            "orders_last_order_amount" -> calculateOrdersLastOrderAmount(customer, orders).toString()
            "orders_last_order_date" -> calculateOrdersLastOrderDate(customer, orders)
            
            // Loyalty aggregates (not implemented - omitted per requirements)
            "loyalty_points" -> "0"
            "loyalty_referred_customers" -> "0"
            
            // Metadata
            "metadata" -> customer.metadata?.let { objectMapper.writeValueAsString(it) } ?: ""
            
            else -> if (field.startsWith("metadata.")) {
                val key = field.substring("metadata.".length)
                customer.metadata?.get(key)?.toString() ?: ""
            } else ""
        }
    }

    // Redemption aggregate calculations
    private fun calculateRedemptionsTotalRedeemed(customer: Customer, redemptions: List<org.wahlen.voucherengine.persistence.model.redemption.Redemption>): Int {
        return redemptions.count { it.customer?.id == customer.id }
    }

    private fun calculateRedemptionsTotalFailed(customer: Customer, redemptions: List<org.wahlen.voucherengine.persistence.model.redemption.Redemption>): Int {
        return redemptions.filter { it.customer?.id == customer.id }
            .count { it.result == RedemptionResult.FAILURE }
    }

    private fun calculateRedemptionsTotalSucceeded(customer: Customer, redemptions: List<org.wahlen.voucherengine.persistence.model.redemption.Redemption>): Int {
        return redemptions.filter { it.customer?.id == customer.id }
            .count { it.result == RedemptionResult.SUCCESS }
    }

    // Order aggregate calculations
    private fun calculateOrdersTotalCount(customer: Customer, orders: List<org.wahlen.voucherengine.persistence.model.order.Order>): Int {
        return orders.count { it.customer?.id == customer.id }
    }

    private fun calculateOrdersTotalAmount(customer: Customer, orders: List<org.wahlen.voucherengine.persistence.model.order.Order>): Long {
        return orders.filter { it.customer?.id == customer.id }
            .sumOf { it.amount ?: 0L }
    }

    private fun calculateOrdersAverageAmount(customer: Customer, orders: List<org.wahlen.voucherengine.persistence.model.order.Order>): Long {
        val customerOrders = orders.filter { it.customer?.id == customer.id }
        return if (customerOrders.isEmpty()) 0L else customerOrders.sumOf { it.amount ?: 0L } / customerOrders.size
    }

    private fun calculateOrdersLastOrderAmount(customer: Customer, orders: List<org.wahlen.voucherengine.persistence.model.order.Order>): Long {
        return orders.filter { it.customer?.id == customer.id }
            .maxByOrNull { it.createdAt ?: java.time.Instant.MIN }
            ?.amount ?: 0L
    }

    private fun calculateOrdersLastOrderDate(customer: Customer, orders: List<org.wahlen.voucherengine.persistence.model.order.Order>): String {
        return orders.filter { it.customer?.id == customer.id }
            .maxByOrNull { it.createdAt ?: java.time.Instant.MIN }
            ?.createdAt
            ?.let { DATE_FORMATTER.format(it) }
            ?: ""
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
            "message" to "No customers found matching the criteria"
        )
        asyncJobRepository.save(job)
    }
}
