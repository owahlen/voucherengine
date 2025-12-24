package org.wahlen.voucherengine.persistence

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.common.AmountOffType
import org.wahlen.voucherengine.api.dto.common.DiscountDto
import org.wahlen.voucherengine.api.dto.common.DiscountType
import org.wahlen.voucherengine.api.dto.common.RedemptionDto
import org.wahlen.voucherengine.config.IntegrationTest
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import org.wahlen.voucherengine.persistence.model.voucher.VoucherType
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@IntegrationTest
@Transactional
class VoucherRepositoryTest @Autowired constructor(
    private val voucherRepository: VoucherRepository,
    private val tenantRepository: TenantRepository
) {
    private val tenantName = "test-tenant"
    private lateinit var tenant: Tenant

    @BeforeEach
    fun setUp() {
        tenant = tenantRepository.findByName(tenantName) ?: tenantRepository.save(Tenant(name = tenantName))
    }

    @Test
    fun `voucher CRUD persists discount and redemption DTO`() {
        val voucher = Voucher(
            code = "TEST-10",
            type = VoucherType.DISCOUNT_VOUCHER,
            discountJson = DiscountDto(
                type = DiscountType.PERCENT,
                percent_off = 10
            ),
            metadata = mapOf("audience" to "vip"),
            redemptionJson = RedemptionDto(quantity = 2, redeemed_quantity = 0),
            tenant = tenant
        )

        val saved = voucherRepository.save(voucher)
        val fetched = voucherRepository.findByIdOrNull(saved.id!!)

        assertNotNull(fetched)
        assertEquals(DiscountType.PERCENT, fetched.discountJson?.type)
        assertEquals(10, fetched.discountJson?.percent_off)
        assertEquals(2, fetched.redemptionJson?.quantity)
        assertEquals("vip", fetched.metadata?.get("audience"))

        fetched.discountJson = DiscountDto(
            type = DiscountType.AMOUNT,
            amount_off = 500,
            amount_off_type = AmountOffType.FIXED
        )
        fetched.redemptionJson = RedemptionDto(quantity = 5, redeemed_quantity = 1)
        val updated = voucherRepository.save(fetched)

        assertEquals(DiscountType.AMOUNT, updated.discountJson?.type)
        assertEquals(500, updated.discountJson?.amount_off)
        assertEquals(5, updated.redemptionJson?.quantity)
        assertEquals(1, updated.redemptionJson?.redeemed_quantity)

        voucherRepository.delete(updated)
        assertNull(voucherRepository.findByIdOrNull(updated.id!!))
    }
}
