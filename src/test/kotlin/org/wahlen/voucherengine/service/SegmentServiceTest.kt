package org.wahlen.voucherengine.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.SegmentCreateRequest
import org.wahlen.voucherengine.config.IntegrationTest
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.segment.SegmentType
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.CustomerEventRepository
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.SegmentRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import java.util.UUID

@IntegrationTest
@Transactional
class SegmentServiceTest @Autowired constructor(
    private val segmentService: SegmentService,
    private val tenantRepository: TenantRepository,
    private val customerRepository: CustomerRepository,
    private val segmentRepository: SegmentRepository,
    private val customerEventRepository: CustomerEventRepository
) {
    private val tenantName = "test-tenant"
    private lateinit var tenant: Tenant

    @BeforeEach
    fun setUp() {
        tenant = tenantRepository.findByName(tenantName) ?: tenantRepository.save(Tenant(name = tenantName))
    }

    @Test
    fun `create static segment with customer UUIDs`() {
        val customer = customerRepository.save(Customer(
            sourceId = "cust-1",
            email = "test@example.com",
            tenant = tenant
        ))

        val request = SegmentCreateRequest(
            name = "VIP Customers",
            type = "static",
            customers = listOf(customer.id.toString())
        )

        val segmentResponse = segmentService.create(tenantName, request)
        
        assertThat(segmentResponse.name).isEqualTo("VIP Customers")
        assertThat(segmentResponse.type).isEqualTo("static")

        val segment = segmentRepository.findById(UUID.fromString(segmentResponse.id)).orElseThrow()
        assertThat(segment.customerIds).contains(customer.id)

        val events = customerEventRepository.findAll()
        assertThat(events).anyMatch { event ->
            event.eventType == "customer.segment.entered" && event.customerId.toString() == customer.id.toString()
        }
    }

    @Test
    fun `create static segment with customer source_ids`() {
        val customer = customerRepository.save(Customer(
            sourceId = "source-123",
            email = "test@example.com",
            tenant = tenant
        ))

        val request = SegmentCreateRequest(
            name = "Segment by SourceId",
            type = "static",
            customers = listOf("source-123")
        )

        val segmentResponse = segmentService.create(tenantName, request)
        
        val segment = segmentRepository.findById(UUID.fromString(segmentResponse.id)).orElseThrow()
        assertThat(segment.customerIds).contains(customer.id)
    }

    @Test
    fun `create auto-update segment`() {
        val request = SegmentCreateRequest(
            name = "Auto Segment",
            type = "auto-update"
        )

        val segmentResponse = segmentService.create(tenantName, request)
        
        assertThat(segmentResponse.type).isEqualTo("auto-update")
        
        val segment = segmentRepository.findById(UUID.fromString(segmentResponse.id)).orElseThrow()
        assertThat(segment.customerIds).isEmpty()
    }

    @Test
    fun `get customer segments`() {
        val customer = customerRepository.save(Customer(
            sourceId = "cust-1",
            email = "test@example.com",
            tenant = tenant
        ))

        val segment1 = segmentRepository.save(org.wahlen.voucherengine.persistence.model.segment.Segment(
            name = "Segment 1",
            type = SegmentType.STATIC,
            customerIds = mutableSetOf(customer.id!!),
            tenant = tenant
        ))

        val segment2 = segmentRepository.save(org.wahlen.voucherengine.persistence.model.segment.Segment(
            name = "Segment 2",
            type = SegmentType.STATIC,
            customerIds = mutableSetOf(customer.id!!),
            tenant = tenant
        ))

        val segments = segmentService.getCustomerSegments(tenantName, customer.id!!)
        
        assertThat(segments).hasSize(2)
        assertThat(segments.map { it.name }).containsExactlyInAnyOrder("Segment 1", "Segment 2")
    }

    @Test
    fun `delete segment emits customer segment left events`() {
        val customer = customerRepository.save(Customer(
            sourceId = "cust-1",
            email = "test@example.com",
            tenant = tenant
        ))

        val segment = segmentRepository.save(org.wahlen.voucherengine.persistence.model.segment.Segment(
            name = "To Delete",
            type = SegmentType.STATIC,
            customerIds = mutableSetOf(customer.id!!),
            tenant = tenant
        ))

        segmentService.delete(tenantName, segment.id!!)

        val events = customerEventRepository.findAll()
        assertThat(events).anyMatch { event ->
            event.eventType == "customer.segment.left" && event.customerId.toString() == customer.id.toString()
        }
    }

    @Test
    fun `list all segments for tenant`() {
        segmentRepository.save(org.wahlen.voucherengine.persistence.model.segment.Segment(
            name = "Segment 1",
            type = SegmentType.STATIC,
            tenant = tenant
        ))
        segmentRepository.save(org.wahlen.voucherengine.persistence.model.segment.Segment(
            name = "Segment 2",
            type = SegmentType.AUTO_UPDATE,
            tenant = tenant
        ))

        val segments = segmentService.list(tenantName)
        
        assertThat(segments).hasSize(2)
        assertThat(segments.map { it.name }).containsExactlyInAnyOrder("Segment 1", "Segment 2")
    }
}
