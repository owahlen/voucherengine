package org.wahlen.voucherengine.api.controller

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.tenantJwt
import org.wahlen.voucherengine.config.IntegrationTest
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.*
import tools.jackson.databind.ObjectMapper
import java.util.*

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class CascadeDeleteIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val tenantRepository: TenantRepository,
    private val customerRepository: CustomerRepository,
    private val voucherRepository: VoucherRepository,
    private val publicationRepository: PublicationRepository,
    private val redemptionRepository: RedemptionRepository,
    private val validationRulesAssignmentRepository: ValidationRulesAssignmentRepository,
    private val campaignRepository: CampaignRepository
) {
    private val objectMapper = ObjectMapper()
    private val tenantName = "test-tenant"

    @BeforeEach
    fun setUp() {
        if (tenantRepository.findByName(tenantName) == null) {
            tenantRepository.save(Tenant(name = tenantName))
        }
    }

    @Test
    fun `deleting voucher cascades to publications redemptions and validation assignments`() {
        val customerSource = "customer-${UUID.randomUUID().toString().take(6)}"
        createCustomer(customerSource)

        val voucherCode = "V-${UUID.randomUUID().toString().take(6)}"
        val voucherId = createVoucher(voucherCode)

        createPublicationForVoucher(voucherCode, customerSource)
        createRedemption(voucherCode, customerSource)
        createValidationRuleAssignment("voucher", voucherCode)

        mockMvc.perform(
            delete("/v1/vouchers/$voucherCode")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNoContent)

        assertNull(voucherRepository.findByCodeAndTenantName(voucherCode, tenantName))
        assertTrue(publicationRepository.findAllByTenantNameAndVoucherCode(tenantName, voucherCode).isEmpty())
        assertTrue(redemptionRepository.findAllByTenantNameAndVoucherId(tenantName, voucherId).isEmpty())
        assertTrue(
            validationRulesAssignmentRepository
                .findAllByTenantNameAndRelatedObjectTypeAndRelatedObjectIdIn(tenantName, "voucher", listOf(voucherCode))
                .isEmpty()
        )
        assertNotNull(customerRepository.findBySourceIdAndTenantName(customerSource, tenantName))
    }

    @Test
    fun `deleting campaign cascades to vouchers publications redemptions and validation assignments`() {
        val customerSource = "customer-${UUID.randomUUID().toString().take(6)}"
        createCustomer(customerSource)

        val campaignName = "Campaign-${UUID.randomUUID().toString().take(6)}"
        val campaignId = createCampaign(campaignName)
        val voucherCode = "C-${UUID.randomUUID().toString().take(6)}"
        val voucherId = createCampaignVoucher(campaignId, voucherCode)

        createPublicationForCampaign(campaignName, customerSource)
        createRedemption(voucherCode, customerSource)
        createValidationRuleAssignment("campaign", campaignId.toString())

        mockMvc.perform(
            delete("/v1/campaigns/$campaignId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNoContent)

        assertNull(campaignRepository.findByIdAndTenantName(campaignId, tenantName))
        assertNull(voucherRepository.findByCodeAndTenantName(voucherCode, tenantName))
        assertTrue(publicationRepository.findAllByTenantNameAndCampaignId(tenantName, campaignId).isEmpty())
        assertTrue(redemptionRepository.findAllByTenantNameAndVoucherId(tenantName, voucherId).isEmpty())
        assertTrue(
            validationRulesAssignmentRepository
                .findAllByTenantNameAndRelatedObjectTypeAndRelatedObjectIdIn(tenantName, "campaign", listOf(campaignId.toString()))
                .isEmpty()
        )
        assertNotNull(customerRepository.findBySourceIdAndTenantName(customerSource, tenantName))
    }

    @Test
    fun `deleting customer cascades to publications and unassigns vouchers`() {
        val customerSource = "customer-${UUID.randomUUID().toString().take(6)}"
        val customerId = createCustomer(customerSource)

        val voucherCode = "H-${UUID.randomUUID().toString().take(6)}"
        createVoucher(voucherCode)
        createPublicationForVoucher(voucherCode, customerSource)

        mockMvc.perform(
            delete("/v1/customers/$customerSource")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNoContent)

        assertTrue(publicationRepository.findAllByTenantNameAndCustomerId(tenantName, customerId).isEmpty())
        val voucher = voucherRepository.findByCodeAndTenantName(voucherCode, tenantName)
        assertNotNull(voucher)
        assertNull(voucher!!.holder)
    }

    @Test
    fun `deleting publication does not delete voucher or customer`() {
        val customerSource = "customer-${UUID.randomUUID().toString().take(6)}"
        createCustomer(customerSource)

        val voucherCode = "P-${UUID.randomUUID().toString().take(6)}"
        createVoucher(voucherCode)
        val publicationId = createPublicationForVoucher(voucherCode, customerSource)

        mockMvc.perform(
            delete("/v1/publications/$publicationId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNoContent)

        assertNotNull(voucherRepository.findByCodeAndTenantName(voucherCode, tenantName))
        assertNotNull(customerRepository.findBySourceIdAndTenantName(customerSource, tenantName))
        assertNull(publicationRepository.findByIdAndTenantName(publicationId, tenantName))
    }

    private fun createCustomer(sourceId: String): UUID {
        val body = """
            { "source_id": "$sourceId", "name": "Test User", "email": "$sourceId@example.com" }
        """.trimIndent()
        val response = mockMvc.perform(
            post("/v1/customers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)
            .andReturn()
        return UUID.fromString(objectMapper.readTree(response.response.contentAsString).get("id").asString()).also {
            assertEquals(it, customerRepository.findBySourceIdAndTenantName(sourceId, tenantName)?.id)
        }
    }

    private fun createVoucher(code: String): UUID {
        val body = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 }, "redemption": { "quantity": 1 } }
        """.trimIndent()
        val response = mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)
            .andReturn()
        return UUID.fromString(objectMapper.readTree(response.response.contentAsString).get("id").asString())
    }

    private fun createCampaign(name: String): UUID {
        val body = """
            { "name": "$name", "type": "DISCOUNT_COUPONS", "mode": "STATIC", "code_pattern": "CASCADE-####" }
        """.trimIndent()
        val response = mockMvc.perform(
            post("/v1/campaigns")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)
            .andReturn()
        return UUID.fromString(objectMapper.readTree(response.response.contentAsString).get("id").asString())
    }

    private fun createCampaignVoucher(campaignId: UUID, code: String): UUID {
        val body = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 15 }, "redemption": { "quantity": 1 } }
        """.trimIndent()
        val response = mockMvc.perform(
            post("/v1/campaigns/$campaignId/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)
            .andReturn()
        return UUID.fromString(objectMapper.readTree(response.response.contentAsString).get("id").asString())
    }

    private fun createPublicationForVoucher(code: String, customerSource: String): UUID {
        val body = """
            { "voucher": "$code", "customer": { "source_id": "$customerSource", "email": "$customerSource@example.com" }, "channel": "api" }
        """.trimIndent()
        val response = mockMvc.perform(
            post("/v1/publications")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)
            .andReturn()
        return UUID.fromString(objectMapper.readTree(response.response.contentAsString).get("id").asString())
    }

    private fun createPublicationForCampaign(campaignName: String, customerSource: String): UUID {
        val body = """
            { "campaign": { "name": "$campaignName" }, "customer": { "source_id": "$customerSource", "email": "$customerSource@example.com" }, "channel": "api" }
        """.trimIndent()
        val response = mockMvc.perform(
            post("/v1/publications")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)
            .andReturn()
        return UUID.fromString(objectMapper.readTree(response.response.contentAsString).get("id").asString())
    }

    private fun createRedemption(code: String, customerSource: String) {
        val body = """
            { "redeemables": [ { "object": "voucher", "id": "$code" } ], "customer": { "source_id": "$customerSource" } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/redemptions")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isOk)
    }

    private fun createValidationRuleAssignment(objectType: String, objectId: String) {
        val ruleBody = """
            { "name": "Min amount", "type": "redemptions", "conditions": { "redemptions": { "per_customer": 1 } } }
        """.trimIndent()
        val ruleResponse = mockMvc.perform(
            post("/v1/validation-rules")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ruleBody)
        ).andExpect(status().isOk)
            .andReturn()
        val ruleId = objectMapper.readTree(ruleResponse.response.contentAsString).get("id").asString()

        val assignmentBody = """
            { "object": "$objectType", "id": "$objectId" }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/validation-rules/$ruleId/assignments")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignmentBody)
        ).andExpect(status().isOk)
    }
}
