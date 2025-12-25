package org.wahlen.voucherengine.service

import org.springframework.data.domain.PageRequest
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
                "CSV" -> generateCsv(tenantName, filters, job)
                "JSON" -> generateJson(tenantName, filters, job)
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

    private fun generateCsv(tenantName: String, filters: OrderFilters, job: AsyncJob): ByteArray {
        val output = ByteArrayOutputStream()
        output.bufferedWriter().use { writer ->
            // CSV Header
            writer.write("id,source_id,status,amount,initial_amount,discount_amount,customer_id,created_at,updated_at")
            writer.newLine()

            var processedCount = 0
            var page = 0

            while (true) {
                val pageable = PageRequest.of(page, BATCH_SIZE)
                val orders = fetchOrders(tenantName, filters, pageable)

                if (orders.isEmpty()) break

                orders.forEach { order ->
                    writer.write(toCsvRow(order))
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

    private fun generateJson(tenantName: String, filters: OrderFilters, job: AsyncJob): ByteArray {
        val allOrders = mutableListOf<Map<String, Any?>>()
        var processedCount = 0
        var page = 0

        while (true) {
            val pageable = PageRequest.of(page, BATCH_SIZE)
            val orders = fetchOrders(tenantName, filters, pageable)

            if (orders.isEmpty()) break

            orders.forEach { order ->
                allOrders.add(toJsonObject(order))
            }

            processedCount += orders.size
            updateProgress(job, processedCount)

            if (orders.size < BATCH_SIZE) break
            page++
        }

        return objectMapper.writeValueAsBytes(allOrders)
    }

    private fun toCsvRow(order: Order): String {
        return listOf(
            order.id.toString(),
            order.sourceId ?: "",
            order.status ?: "",
            order.amount?.toString() ?: "",
            order.initialAmount?.toString() ?: "",
            order.discountAmount?.toString() ?: "",
            order.customer?.id?.toString() ?: "",
            order.createdAt?.atOffset(ZoneOffset.UTC)?.format(DATE_FORMATTER) ?: "",
            order.updatedAt?.atOffset(ZoneOffset.UTC)?.format(DATE_FORMATTER) ?: ""
        ).joinToString(",") { escapeCsv(it) }
    }

    private fun toJsonObject(order: Order): Map<String, Any?> {
        return mapOf(
            "id" to order.id.toString(),
            "source_id" to order.sourceId,
            "status" to order.status,
            "amount" to order.amount,
            "initial_amount" to order.initialAmount,
            "discount_amount" to order.discountAmount,
            "customer_id" to order.customer?.id?.toString(),
            "created_at" to order.createdAt?.atOffset(ZoneOffset.UTC)?.format(DATE_FORMATTER),
            "updated_at" to order.updatedAt?.atOffset(ZoneOffset.UTC)?.format(DATE_FORMATTER),
            "metadata" to order.metadata
        )
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun fetchOrders(tenantName: String, filters: OrderFilters, pageable: PageRequest): List<Order> {
        // For now, fetch all orders for the tenant. In future, apply filters
        return orderRepository.findAllByTenantName(tenantName, pageable).content
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
