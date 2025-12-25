package org.wahlen.voucherengine.service.async

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.CustomerReferenceDto
import org.wahlen.voucherengine.api.dto.request.OrderCreateRequest
import org.wahlen.voucherengine.api.dto.request.OrderItemDto
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.service.CustomerService
import org.wahlen.voucherengine.service.OrderService
import java.time.Instant
import java.util.*

@Service
class OrderImportService(
    private val asyncJobRepository: AsyncJobRepository,
    private val orderService: OrderService,
    private val customerService: CustomerService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun processImport(jobId: UUID, tenantName: String, orders: List<Map<String, Any?>>) {
        val job = asyncJobRepository.findById(jobId).orElseThrow {
            IllegalStateException("AsyncJob $jobId not found")
        }

        try {
            job.status = AsyncJobStatus.IN_PROGRESS
            job.startedAt = Instant.now()
            asyncJobRepository.save(job)

            var successCount = 0
            var errorCount = 0
            val errors = mutableListOf<String>()

            orders.forEachIndexed { index, orderData ->
                try {
                    val request = convertToOrderRequest(orderData, tenantName)
                    orderService.create(tenantName, request)
                    successCount++
                } catch (e: Exception) {
                    errorCount++
                    val errorMsg = "Order ${index + 1}: ${e.message}"
                    errors.add(errorMsg)
                    logger.warn("Failed to import order: $errorMsg", e)
                }

                job.progress = index + 1
                if ((index + 1) % 10 == 0) {
                    asyncJobRepository.save(job)
                }
            }

            job.status = AsyncJobStatus.COMPLETED
            job.completedAt = Instant.now()
            job.result = mapOf(
                "imported" to successCount,
                "failed" to errorCount,
                "errors" to errors.take(100)
            )
            asyncJobRepository.save(job)

            logger.info("Order import job $jobId completed: $successCount imported, $errorCount failed")

        } catch (e: Exception) {
            logger.error("Order import job $jobId failed", e)
            job.status = AsyncJobStatus.FAILED
            job.errorMessage = e.message
            job.completedAt = Instant.now()
            asyncJobRepository.save(job)
            throw e
        }
    }

    private fun convertToOrderRequest(data: Map<String, Any?>, tenantName: String): OrderCreateRequest {
        val sourceId = data["source_id"]?.toString()
        val customerId = data["customer_id"]?.toString()
        val customerSourceId = data["customer"]?.toString()

        var customerRef: CustomerReferenceDto? = null
        if (customerId != null) {
            try {
                val uuid = UUID.fromString(customerId)
                val customer = customerService.getByIdOrSource(tenantName, uuid.toString())
                if (customer != null) {
                    customerRef = CustomerReferenceDto(source_id = customer.sourceId)
                }
            } catch (e: IllegalArgumentException) {
                // Invalid UUID
            }
        } else if (customerSourceId != null) {
            customerRef = CustomerReferenceDto(source_id = customerSourceId)
        }

        val items = (data["items"] as? List<*>)?.mapNotNull { itemData ->
            if (itemData is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val item = itemData as Map<String, Any?>
                OrderItemDto(
                    source_id = item["source_id"]?.toString(),
                    product_id = item["product_id"]?.toString(),
                    sku_id = item["sku_id"]?.toString(),
                    quantity = (item["quantity"] as? Number)?.toInt(),
                    amount = (item["amount"] as? Number)?.toLong(),
                    price = (item["price"] as? Number)?.toLong(),
                    metadata = item["metadata"] as? Map<String, Any?>
                )
            } else null
        } ?: emptyList()

        return OrderCreateRequest(
            source_id = sourceId,
            status = data["status"]?.toString(),
            amount = (data["amount"] as? Number)?.toLong(),
            initial_amount = (data["initial_amount"] as? Number)?.toLong(),
            discount_amount = (data["discount_amount"] as? Number)?.toLong(),
            metadata = data["metadata"] as? Map<String, Any?>,
            customer = customerRef,
            items = items
        )
    }
}
