package org.wahlen.voucherengine.service.async

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.config.IntegrationTest
import org.wahlen.voucherengine.persistence.model.async.AsyncJobStatus
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.AsyncJobRepository
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.OrderRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import org.wahlen.voucherengine.service.async.command.OrderImportCommand
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@IntegrationTest
@Transactional
class OrderImportServiceTest @Autowired constructor(
    private val orderImportService: OrderImportService,
    private val asyncJobRepository: AsyncJobRepository,
    private val tenantRepository: TenantRepository,
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository
) {

    private val tenantName = "test-tenant"
    private lateinit var tenant: Tenant

    @BeforeEach
    fun setUp() {
        tenant = tenantRepository.findByName(tenantName) ?: tenantRepository.save(Tenant(name = tenantName))
    }

    @Test
    fun `should import orders successfully`() {
        val customer = customerRepository.save(Customer(sourceId = "cust-123", tenant = tenant))

        val orders = listOf(
            mapOf(
                "source_id" to "order-1",
                "status" to "PAID",
                "amount" to 1000,
                "customer_id" to customer.id.toString(),
                "items" to listOf(
                    mapOf(
                        "source_id" to "item-1",
                        "product_id" to "prod-1",
                        "quantity" to 2,
                        "amount" to 1000
                    )
                )
            ),
            mapOf(
                "source_id" to "order-2",
                "status" to "CREATED",
                "amount" to 2000,
                "metadata" to mapOf("custom" to "value")
            )
        )

        val command = OrderImportCommand(tenantName = tenantName, orders = orders)
        val job = command.toAsyncJob(tenant)
        asyncJobRepository.save(job)

        orderImportService.processImport(job.id!!, tenantName, orders)

        val updatedJob = asyncJobRepository.findById(job.id!!).get()
        assertEquals(AsyncJobStatus.COMPLETED, updatedJob.status)
        assertEquals(2, updatedJob.progress)
        assertEquals(2, updatedJob.total)
        assertNotNull(updatedJob.result)
        assertEquals(2, updatedJob.result!!["imported"])
        assertEquals(0, updatedJob.result!!["failed"])

        val importedOrders = orderRepository.findAll().filter { it.tenant?.name == tenantName }
        assertEquals(2, importedOrders.size)
        assertEquals("order-1", importedOrders.find { it.sourceId == "order-1" }?.sourceId)
        assertEquals("order-2", importedOrders.find { it.sourceId == "order-2" }?.sourceId)
    }

    @Test
    fun `should resolve customer by source_id`() {
        val customer = customerRepository.save(Customer(sourceId = "cust-ext-123", tenant = tenant))

        val orders = listOf(
            mapOf(
                "source_id" to "order-1",
                "customer" to customer.sourceId,
                "amount" to 1000
            )
        )

        val command = OrderImportCommand(tenantName = tenantName, orders = orders)
        val job = command.toAsyncJob(tenant)
        asyncJobRepository.save(job)

        orderImportService.processImport(job.id!!, tenantName, orders)

        val importedOrder = orderRepository.findAll().find { it.sourceId == "order-1" && it.tenant?.name == tenantName }
        assertNotNull(importedOrder)
        assertEquals(customer.id, importedOrder.customer?.id)
    }

    @Test
    fun `should fail job if exception occurs`() {
        val command = OrderImportCommand(tenantName = tenantName, orders = emptyList())
        val job = command.toAsyncJob(tenant)
        job.id = UUID.randomUUID()
        
        assertThrows<IllegalStateException> {
            orderImportService.processImport(job.id!!, tenantName, emptyList())
        }
    }
}
