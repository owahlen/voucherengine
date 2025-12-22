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
import org.wahlen.voucherengine.api.dto.request.ValidationRuleAssignmentRequest
import org.wahlen.voucherengine.api.dto.request.ValidationRuleCreateRequest
import org.wahlen.voucherengine.persistence.repository.CategoryRepository
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
    private val categoryRepository: CategoryRepository,
    private val validationRuleService: ValidationRuleService
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

    @Test
    fun `validation rule assignments enforce redemption limits`() {
        val voucher = voucherService.createVoucher(
            VoucherCreateRequest(
                code = "RULE-BLOCKED",
                type = "DISCOUNT_VOUCHER",
                discount = DiscountDto(type = DiscountType.PERCENT, percent_off = 10)
            )
        )

        val rule = validationRuleService.createRule(
            ValidationRuleCreateRequest(
                name = "Stop redemptions",
                type = "basic",
                context_type = "voucher.discount_voucher",
                rules = mapOf("redemptions" to mapOf("quantity" to 0)),
                error = mapOf("code" to "rule_blocked", "message" to "Rule prevents redemption")
            )
        )
        validationRuleService.assignRule(
            rule.id!!,
            ValidationRuleAssignmentRequest(
                `object` = "voucher",
                id = voucher.code
            )
        )

        val validation = voucherService.validateVoucher(
            voucher.code!!,
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "cust"))
        )

        assertFalse(validation.valid)
        assertEquals("rule_blocked", validation.error?.code)
    }

    @Test
    fun `voucher validation respects complex rule logic`() {
        val voucher = voucherService.createVoucher(
            VoucherCreateRequest(
                code = "RULE-LOGIC",
                type = "DISCOUNT_VOUCHER",
                discount = DiscountDto(type = DiscountType.PERCENT, percent_off = 10)
            )
        )

        val rule = validationRuleService.createRule(
            ValidationRuleCreateRequest(
                name = "amount and sku",
                type = "expression",
                rules = mapOf(
                    "rules" to mapOf(
                        "1" to mapOf(
                            "name" to "order.amount",
                            "conditions" to mapOf("\$gte" to 1000)
                        ),
                        "2" to mapOf(
                            "name" to "order.items.sku",
                            "conditions" to mapOf("\$contains_any" to listOf("SKU_A"))
                        )
                    ),
                    "logic" to "1 and 2"
                )
            )
        )
        validationRuleService.assignRule(
            rule.id!!,
            ValidationRuleAssignmentRequest(`object` = "voucher", id = voucher.code)
        )

        val invalid = voucherService.validateVoucher(
            voucher.code!!,
            VoucherValidationRequest(
                customer = CustomerReferenceDto(source_id = "cust"),
                order = org.wahlen.voucherengine.api.dto.request.OrderRequest(
                    id = "order-1",
                    amount = 500,
                    items = listOf(org.wahlen.voucherengine.api.dto.request.OrderItemDto(product_id = "SKU_A", quantity = 1, price = 500))
                )
            )
        )
        assertFalse(invalid.valid)

        val valid = voucherService.validateVoucher(
            voucher.code!!,
            VoucherValidationRequest(
                customer = CustomerReferenceDto(source_id = "cust"),
                order = org.wahlen.voucherengine.api.dto.request.OrderRequest(
                    id = "order-2",
                    amount = 1500,
                    items = listOf(org.wahlen.voucherengine.api.dto.request.OrderItemDto(product_id = "SKU_A", quantity = 1, price = 1500))
                )
            )
        )
        assertTrue(valid.valid)
    }

    @Test
    fun `category mismatch invalidates voucher`() {
        val category = categoryRepository.save(org.wahlen.voucherengine.persistence.model.voucher.Category(name = "electronics"))
        voucherService.createVoucher(
            VoucherCreateRequest(
                code = "CAT-ONLY",
                type = "DISCOUNT_VOUCHER",
                discount = DiscountDto(type = DiscountType.PERCENT, percent_off = 10),
                redemption = RedemptionDto(quantity = 1),
                category_ids = listOf(category.id!!)
            )
        )

        val invalid = voucherService.validateVoucher(
            "CAT-ONLY",
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "cust"), categories = listOf(UUID.randomUUID()))
        )
        assertFalse(invalid.valid)
        assertEquals("voucher_category_mismatch", invalid.error?.code)

        val missingCategoryContext = voucherService.validateVoucher(
            "CAT-ONLY",
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "cust"))
        )
        assertFalse(missingCategoryContext.valid)
        assertEquals("voucher_category_mismatch", missingCategoryContext.error?.code)

        val valid = voucherService.validateVoucher(
            "CAT-ONLY",
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "cust"), categories = listOf(category.id!!))
        )
        assertTrue(valid.valid)
    }

    @Test
    fun `unit discount applies per item quantity`() {
        voucherService.createVoucher(
            VoucherCreateRequest(
                code = "UNIT-10",
                type = "DISCOUNT_VOUCHER",
                discount = DiscountDto(type = DiscountType.UNIT, amount_off = 100),
                redemption = RedemptionDto(quantity = 10)
            )
        )

        val response = voucherService.validateVoucher(
            "UNIT-10",
            VoucherValidationRequest(
                order = org.wahlen.voucherengine.api.dto.request.OrderRequest(
                    id = "order-1",
                    amount = 1000,
                    items = listOf(
                        org.wahlen.voucherengine.api.dto.request.OrderItemDto(product_id = "sku-1", quantity = 2, price = 500)
                    )
                )
            )
        )

        assertTrue(response.valid)
        assertEquals(200, response.order?.discount_amount)
        assertEquals(800, response.order?.total_amount)
    }
}
