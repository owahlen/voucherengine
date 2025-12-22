package org.wahlen.voucherengine.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.common.DiscountDto
import org.wahlen.voucherengine.api.dto.common.DiscountType
import org.wahlen.voucherengine.api.dto.common.RedemptionDto
import org.wahlen.voucherengine.api.dto.request.CustomerReferenceDto
import org.wahlen.voucherengine.api.dto.request.RedeemableDto
import org.wahlen.voucherengine.api.dto.request.RedemptionRequest
import org.wahlen.voucherengine.api.dto.request.VoucherCreateRequest
import org.wahlen.voucherengine.api.dto.request.VoucherValidationRequest
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VoucherServiceTest @Autowired constructor(
    private val voucherService: VoucherService,
    private val voucherRepository: VoucherRepository
) {

    @Test
    fun `redeem stops at total quantity limit`() {
        voucherService.createVoucher(
            VoucherCreateRequest(
                code = "SUMMER2026",
                type = "DISCOUNT_VOUCHER",
                discount = DiscountDto(type = DiscountType.PERCENT, percent_off = 10),
                redemption = RedemptionDto(quantity = 2)
            )
        )

        val redemptionRequest = RedemptionRequest(
            redeemables = listOf(RedeemableDto("voucher", "SUMMER2026")),
            customer = CustomerReferenceDto(source_id = "customer-1")
        )

        val first = voucherService.redeem(redemptionRequest)
        val second = voucherService.redeem(redemptionRequest)
        val third = voucherService.redeem(redemptionRequest)

        assertTrue(first.error == null)
        assertTrue(second.error == null)
        assertEquals("redemption_limit_exceeded", third.error?.code)
    }

    @Test
    fun `per customer limit enforced`() {
        voucherService.createVoucher(
            VoucherCreateRequest(
                code = "ONE-PER-CUSTOMER",
                type = "DISCOUNT_VOUCHER",
                discount = DiscountDto(type = DiscountType.PERCENT, percent_off = 5),
                redemption = RedemptionDto(quantity = 5, per_customer = 1)
            )
        )

        val redemptionRequest = RedemptionRequest(
            redeemables = listOf(RedeemableDto("voucher", "ONE-PER-CUSTOMER")),
            customer = CustomerReferenceDto(source_id = "cust-1")
        )

        val first = voucherService.redeem(redemptionRequest)
        val second = voucherService.redeem(redemptionRequest)

        assertTrue(first.error == null)
        assertEquals("redemption_limit_per_customer_exceeded", second.error?.code)
    }

    @Test
    fun `voucher assigned to specific customer`() {
        voucherService.createVoucher(
            VoucherCreateRequest(
                code = "ALICE-ONLY",
                type = "DISCOUNT_VOUCHER",
                discount = DiscountDto(type = DiscountType.PERCENT, percent_off = 10),
                customer = CustomerReferenceDto(source_id = "alice"),
                redemption = RedemptionDto(quantity = 1)
            )
        )

        val validationBob = voucherService.validateVoucher(
            "ALICE-ONLY",
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "bob"))
        )

        val validationAlice = voucherService.validateVoucher(
            "ALICE-ONLY",
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "alice"))
        )

        assertFalse(validationBob.valid)
        assertEquals("voucher_not_assigned", validationBob.error?.code)
        assertTrue(validationAlice.valid)
    }
}
