package org.wahlen.voucherengine.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.tenantJwt
import org.wahlen.voucherengine.config.IntegrationTest
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import java.util.UUID

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class ExportControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val tenantRepository: TenantRepository
) {
    private val tenantName = "test-tenant"
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        if (tenantRepository.findByName(tenantName) == null) {
            tenantRepository.save(Tenant(name = tenantName))
        }
    }

    @Test
    fun `create list get download and delete exports`() {
        val code = "EXP-${UUID.randomUUID().toString().take(6)}"
        val voucherBody = """
            { "code": "$code", "type": "DISCOUNT_VOUCHER", "discount": { "type": "PERCENT", "percent_off": 10 } }
        """.trimIndent()
        mockMvc.perform(
            post("/v1/vouchers")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(voucherBody)
        ).andExpect(status().isCreated)

        val exportBody = """
            { "exported_object": "voucher" }
        """.trimIndent()
        val exportResult = mockMvc.perform(
            post("/v1/exports")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(exportBody)
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.exported_object").value("voucher"))
            .andReturn()

        val payload = objectMapper.readValue(exportResult.response.contentAsString, Map::class.java)
        val exportId = payload["id"] as? String ?: error("Missing export id")
        val resultUrl = ((payload["result"] as? Map<*, *>)?.get("url") as? String) ?: error("Missing export url")
        val token = resultUrl.substringAfter("token=", "")
        if (token.isBlank()) {
            error("Missing export token")
        }

        mockMvc.perform(
            get("/v1/exports")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.object").value("list"))
            .andExpect(jsonPath("$.data_ref").value("exports"))

        mockMvc.perform(
            get("/v1/exports/$exportId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(exportId))

        val downloadResult = mockMvc.perform(
            get("/v1/exports/$exportId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .param("token", token)
        ).andExpect(status().isOk)
            .andReturn()
        val csv = downloadResult.response.contentAsString
        if (!csv.contains("code,voucher_type,value,discount_type")) {
            error("Missing csv headers")
        }
        if (!csv.contains(code)) {
            error("Missing voucher code in csv")
        }

        mockMvc.perform(
            delete("/v1/exports/$exportId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            get("/v1/exports/$exportId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `points expiration and voucher transactions exports download headers`() {
        val pointsBody = """{ "exported_object": "points_expiration" }"""
        val pointsResult = mockMvc.perform(
            post("/v1/exports")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(pointsBody)
        ).andExpect(status().isOk)
            .andReturn()
        val pointsPayload = objectMapper.readValue(pointsResult.response.contentAsString, Map::class.java)
        val pointsId = pointsPayload["id"] as? String ?: error("Missing export id")
        val pointsUrl = ((pointsPayload["result"] as? Map<*, *>)?.get("url") as? String) ?: error("Missing export url")
        val pointsToken = pointsUrl.substringAfter("token=", "")
        val pointsCsv = mockMvc.perform(
            get("/v1/exports/$pointsId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .param("token", pointsToken)
        ).andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
        if (!pointsCsv.contains("id,campaign_id,voucher_id,status,expires_at,points")) {
            error("Missing points expiration csv headers")
        }

        val txBody = """{ "exported_object": "voucher_transactions" }"""
        val txResult = mockMvc.perform(
            post("/v1/exports")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .contentType(MediaType.APPLICATION_JSON)
                .content(txBody)
        ).andExpect(status().isOk)
            .andReturn()
        val txPayload = objectMapper.readValue(txResult.response.contentAsString, Map::class.java)
        val txId = txPayload["id"] as? String ?: error("Missing export id")
        val txUrl = ((txPayload["result"] as? Map<*, *>)?.get("url") as? String) ?: error("Missing export url")
        val txToken = txUrl.substringAfter("token=", "")
        val txCsv = mockMvc.perform(
            get("/v1/exports/$txId")
                .header("tenant", tenantName)
                .with(tenantJwt(tenantName))
                .param("token", txToken)
        ).andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
        if (!txCsv.contains("id,type,source_id,status,reason,source,balance,amount,created_at")) {
            error("Missing voucher transactions csv headers")
        }
    }
}
