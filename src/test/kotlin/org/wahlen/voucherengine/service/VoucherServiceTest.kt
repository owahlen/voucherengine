package org.wahlen.voucherengine.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.common.*
import org.wahlen.voucherengine.api.dto.request.*
import org.wahlen.voucherengine.config.IntegrationTest
import org.wahlen.voucherengine.persistence.model.product.Product
import org.wahlen.voucherengine.persistence.model.product.Sku
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.repository.CategoryRepository
import org.wahlen.voucherengine.persistence.repository.ProductRepository
import org.wahlen.voucherengine.persistence.repository.SkuRepository
import org.wahlen.voucherengine.persistence.repository.TenantRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@IntegrationTest
@Import(VoucherServiceTest.ClockTestConfig::class)
@Transactional
class VoucherServiceTest @Autowired constructor(
    private val voucherService: VoucherService,
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository,
    private val skuRepository: SkuRepository,
    private val validationRuleService: ValidationRuleService,
    private val tenantRepository: TenantRepository,
    private val clock: MutableClock
) {
    private val tenantName = "test-tenant"

    @BeforeEach
    fun setUp() {
        if (tenantRepository.findByName(tenantName) == null) {
            tenantRepository.save(Tenant(name = tenantName))
        }
    }

    @Test
    fun `redeem stops at total quantity limit`() {
        voucherService.createVoucher(
            tenantName,
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

        val first = voucherService.redeem(tenantName, redemptionRequest)
        val second = voucherService.redeem(tenantName, redemptionRequest)
        val third = voucherService.redeem(tenantName, redemptionRequest)

        assertTrue(first.error == null)
        assertTrue(second.error == null)
        assertEquals("redemption_limit_exceeded", third.error?.code)
    }

    @Test
    fun `per customer limit enforced`() {
        voucherService.createVoucher(
            tenantName,
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

        val first = voucherService.redeem(tenantName, redemptionRequest)
        val second = voucherService.redeem(tenantName, redemptionRequest)

        assertTrue(first.error == null)
        assertEquals("redemption_limit_per_customer_exceeded", second.error?.code)
    }

    @Test
    fun `voucher assigned to specific customer`() {
        voucherService.createVoucher(
            tenantName,
            VoucherCreateRequest(
                code = "ALICE-ONLY",
                type = "DISCOUNT_VOUCHER",
                discount = DiscountDto(type = DiscountType.PERCENT, percent_off = 10),
                customer = CustomerReferenceDto(source_id = "alice"),
                redemption = RedemptionDto(quantity = 1)
            )
        )

        val validationBob = voucherService.validateVoucher(
            tenantName,
            "ALICE-ONLY",
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "bob"))
        )

        val validationAlice = voucherService.validateVoucher(
            tenantName,
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
            tenantName,
            VoucherCreateRequest(
                code = "RULE-BLOCKED",
                type = "DISCOUNT_VOUCHER",
                discount = DiscountDto(type = DiscountType.PERCENT, percent_off = 10)
            )
        )

        val rule = validationRuleService.createRule(
            tenantName,
            ValidationRuleCreateRequest(
                name = "Stop redemptions",
                type = "basic",
                context_type = "voucher.discount_voucher",
                rules = mapOf("redemptions" to mapOf("quantity" to 0)),
                error = mapOf("code" to "rule_blocked", "message" to "Rule prevents redemption")
            )
        )
        validationRuleService.assignRule(
            tenantName,
            rule.id!!,
            ValidationRuleAssignmentRequest(
                `object` = "voucher",
                id = voucher.code
            )
        )

        val validation = voucherService.validateVoucher(
            tenantName,
            voucher.code!!,
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "cust"))
        )

        assertFalse(validation.valid)
        assertEquals("rule_blocked", validation.error?.code)
    }

    @Test
    fun `voucher validation respects complex rule logic`() {
        ensureSku("prod-rule-logic", "SKU_A")
        val voucher = voucherService.createVoucher(
            tenantName,
            VoucherCreateRequest(
                code = "RULE-LOGIC",
                type = "DISCOUNT_VOUCHER",
                discount = DiscountDto(type = DiscountType.PERCENT, percent_off = 10)
            )
        )

        val rule = validationRuleService.createRule(
            tenantName,
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
            tenantName,
            rule.id!!,
            ValidationRuleAssignmentRequest(`object` = "voucher", id = voucher.code)
        )

        val invalid = voucherService.validateVoucher(
            tenantName,
            voucher.code!!,
            VoucherValidationRequest(
                customer = CustomerReferenceDto(source_id = "cust"),
                order = org.wahlen.voucherengine.api.dto.request.OrderRequest(
                    id = "order-1",
                    amount = 500,
                    items = listOf(org.wahlen.voucherengine.api.dto.request.OrderItemDto(sku_id = "SKU_A", quantity = 1, price = 500))
                )
            )
        )
        assertFalse(invalid.valid)

        val valid = voucherService.validateVoucher(
            tenantName,
            voucher.code!!,
            VoucherValidationRequest(
                customer = CustomerReferenceDto(source_id = "cust"),
                order = org.wahlen.voucherengine.api.dto.request.OrderRequest(
                    id = "order-2",
                    amount = 1500,
                    items = listOf(org.wahlen.voucherengine.api.dto.request.OrderItemDto(sku_id = "SKU_A", quantity = 1, price = 1500))
                )
            )
        )
        assertTrue(valid.valid)
    }

    @Test
    fun `category mismatch invalidates voucher`() {
        val tenant = tenantRepository.findByName(tenantName)!!
        val category = categoryRepository.save(
            org.wahlen.voucherengine.persistence.model.voucher.Category(name = "electronics").apply { this.tenant = tenant }
        )
        voucherService.createVoucher(
            tenantName,
            VoucherCreateRequest(
                code = "CAT-ONLY",
                type = "DISCOUNT_VOUCHER",
                discount = DiscountDto(type = DiscountType.PERCENT, percent_off = 10),
                redemption = RedemptionDto(quantity = 1),
                category_ids = listOf(category.id!!)
            )
        )

        val invalid = voucherService.validateVoucher(
            tenantName,
            "CAT-ONLY",
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "cust"), categories = listOf(UUID.randomUUID()))
        )
        assertFalse(invalid.valid)
        assertEquals("voucher_category_mismatch", invalid.error?.code)

        val missingCategoryContext = voucherService.validateVoucher(
            tenantName,
            "CAT-ONLY",
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "cust"))
        )
        assertFalse(missingCategoryContext.valid)
        assertEquals("voucher_category_mismatch", missingCategoryContext.error?.code)

        val valid = voucherService.validateVoucher(
            tenantName,
            "CAT-ONLY",
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "cust"), categories = listOf(category.id!!))
        )
        assertTrue(valid.valid)
    }

    @Test
    fun `unit discount applies per item quantity`() {
        ensureSku("prod-unit-1", "sku-1")
        voucherService.createVoucher(
            tenantName,
            VoucherCreateRequest(
                code = "UNIT-10",
                type = "DISCOUNT_VOUCHER",
                discount = DiscountDto(type = DiscountType.UNIT, amount_off = 100),
                redemption = RedemptionDto(quantity = 10)
            )
        )

        val response = voucherService.validateVoucher(
            tenantName,
            "UNIT-10",
            VoucherValidationRequest(
                order = org.wahlen.voucherengine.api.dto.request.OrderRequest(
                    id = "order-1",
                    amount = 1000,
                    items = listOf(
                        org.wahlen.voucherengine.api.dto.request.OrderItemDto(sku_id = "sku-1", quantity = 2, price = 500)
                    )
                )
            )
        )

        assertTrue(response.valid)
        assertEquals(200, response.order?.discount_amount)
        assertEquals(800, response.order?.total_amount)
    }

    @Test
    fun `validity day of week and hours enforced`() {
        clock.instant = Instant.parse("2025-01-05T10:00:00Z") // Sunday
        clock.setZone(ZoneOffset.UTC)

        voucherService.createVoucher(
            tenantName,
            VoucherCreateRequest(
                code = "WEEKDAY-ONLY",
                type = "DISCOUNT_VOUCHER",
                discount = DiscountDto(type = DiscountType.PERCENT, percent_off = 10),
                validity_day_of_week = listOf(1, 2, 3, 4, 5),
                validity_hours = ValidityHoursDto(
                    daily = listOf(
                        ValidityHoursDailyDto(start_time = "09:00", expiration_time = "12:00", days_of_week = listOf(1, 2, 3, 4, 5))
                    )
                )
            )
        )

        val invalidDay = voucherService.validateVoucher(
            tenantName,
            "WEEKDAY-ONLY",
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "cust"))
        )
        assertFalse(invalidDay.valid)

        clock.instant = Instant.parse("2025-01-06T10:00:00Z") // Monday 10:00 UTC
        val valid = voucherService.validateVoucher(
            tenantName,
            "WEEKDAY-ONLY",
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "cust"))
        )
        assertTrue(valid.valid)

        clock.instant = Instant.parse("2025-01-06T13:00:00Z") // outside hours
        val invalidHour = voucherService.validateVoucher(
            tenantName,
            "WEEKDAY-ONLY",
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "cust"))
        )
        assertFalse(invalidHour.valid)
    }

    @Test
    fun `validity timeframe enforced`() {
        val start = Instant.parse("2025-01-01T00:00:00Z")
        clock.instant = start.plusSeconds(1800)
        clock.setZone(ZoneOffset.UTC)

        voucherService.createVoucher(
            tenantName,
            VoucherCreateRequest(
                code = "WINDOWED",
                type = "DISCOUNT_VOUCHER",
                discount = DiscountDto(type = DiscountType.PERCENT, percent_off = 5),
                start_date = start,
                validity_timeframe = ValidityTimeframeDto(duration = "PT1H", interval = "P1D")
            )
        )

        val valid = voucherService.validateVoucher(
            tenantName,
            "WINDOWED",
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "cust"))
        )
        assertTrue(valid.valid)

        clock.instant = start.plusSeconds(7200)
        val invalid = voucherService.validateVoucher(
            tenantName,
            "WINDOWED",
            VoucherValidationRequest(customer = CustomerReferenceDto(source_id = "cust"))
        )
        assertFalse(invalid.valid)
    }

    @TestConfiguration
    class ClockTestConfig {
        @Bean
        @Primary
        fun mutableClock(): MutableClock = MutableClock(Instant.now(), ZoneOffset.UTC)
    }

    class MutableClock(var instant: Instant, private var zoneValue: java.time.ZoneId) : Clock() {
        override fun withZone(zone: java.time.ZoneId): Clock {
            this.zoneValue = zone
            return this
        }
        override fun getZone(): java.time.ZoneId = zoneValue
        override fun instant(): Instant = instant

        fun setZone(zone: java.time.ZoneId) {
            this.zoneValue = zone
        }
    }

    private fun ensureSku(productSourceId: String, skuSourceId: String) {
        val tenant = tenantRepository.findByName(tenantName) ?: tenantRepository.save(Tenant(name = tenantName))
        val product = productRepository.findBySourceIdAndTenantName(productSourceId, tenantName)
            ?: productRepository.save(Product(sourceId = productSourceId, name = "Test").apply { this.tenant = tenant })
        if (skuRepository.findBySourceIdAndTenantName(skuSourceId, tenantName) == null) {
            skuRepository.save(
                Sku(
                    sourceId = skuSourceId,
                    sku = skuSourceId,
                    product = product
                ).apply { this.tenant = tenant }
            )
        }
    }
}
