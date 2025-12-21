package org.wahlen.voucherengine.persistence

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.AmountOffType
import org.wahlen.voucherengine.api.dto.DiscountDto
import org.wahlen.voucherengine.api.dto.DiscountType
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.validation.ValidationRule
import org.wahlen.voucherengine.persistence.model.validation.ValidationRuleContextType
import org.wahlen.voucherengine.persistence.model.validation.ValidationRuleType
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import org.wahlen.voucherengine.persistence.model.voucher.VoucherType
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.ValidationRuleRepository
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoryCrudTests @Autowired constructor(
    private val voucherRepository: VoucherRepository,
    private val customerRepository: CustomerRepository,
    private val validationRuleRepository: ValidationRuleRepository,
) {

    @Test
    fun `voucher CRUD persists discount dto`() {
        val voucher = Voucher(
            code = "TEST-10",
            type = VoucherType.DISCOUNT_VOUCHER,
            discountJson = DiscountDto(
                type = DiscountType.PERCENT,
                percent_off = 10
            ),
            metadata = mapOf("audience" to "vip")
        )

        val saved = voucherRepository.save(voucher)
        val fetched = voucherRepository.findByIdOrNull(saved.id!!)

        assertNotNull(fetched)
        assertEquals(DiscountType.PERCENT, fetched.discountJson?.type)
        assertEquals(10, fetched.discountJson?.percent_off)
        assertEquals("vip", fetched.metadata?.get("audience"))

        fetched.discountJson = DiscountDto(
            type = DiscountType.AMOUNT,
            amount_off = 500,
            amount_off_type = AmountOffType.FIXED
        )
        val updated = voucherRepository.save(fetched)

        assertEquals(DiscountType.AMOUNT, updated.discountJson?.type)
        assertEquals(500, updated.discountJson?.amount_off)

        voucherRepository.delete(updated)
        assertNull(voucherRepository.findByIdOrNull(updated.id!!))
    }

    @Test
    fun `customer CRUD persists metadata`() {
        val customer = Customer(
            sourceId = "customer-123",
            name = "Alice Example",
            metadata = mapOf("tier" to "gold")
        )

        val saved = customerRepository.save(customer)
        val fetched = customerRepository.findByIdOrNull(saved.id!!)

        assertNotNull(fetched)
        assertEquals("Alice Example", fetched.name)
        assertEquals("gold", fetched.metadata?.get("tier"))

        fetched.name = "Alice Updated"
        val updated = customerRepository.save(fetched)

        assertEquals("Alice Updated", updated.name)

        customerRepository.delete(updated)
        assertNull(customerRepository.findByIdOrNull(updated.id!!))
    }

    @Test
    fun `validation rule CRUD persists rules json`() {
        val validationRule = ValidationRule(
            name = "One redemption per customer",
            type = ValidationRuleType.BASIC,
            contextType = ValidationRuleContextType.GLOBAL,
            rules = mapOf(
                "redemptions" to mapOf(
                    "per_customer" to 1,
                    "per_incentive" to true
                )
            ),
            error = mapOf("code" to "limit_exceeded", "message" to "Only once per customer")
        )

        val saved = validationRuleRepository.save(validationRule)
        val fetched = validationRuleRepository.findByIdOrNull(saved.id!!)

        assertNotNull(fetched)
        assertEquals(ValidationRuleType.BASIC, fetched.type)
        assertEquals(ValidationRuleContextType.GLOBAL, fetched.contextType)
        assertEquals(1, (fetched.rules?.get("redemptions") as? Map<*, *>)?.get("per_customer"))

        fetched.name = "Updated redemption limit"
        val updated = validationRuleRepository.save(fetched)

        assertEquals("Updated redemption limit", updated.name)

        validationRuleRepository.delete(updated)
        assertNull(validationRuleRepository.findByIdOrNull(updated.id!!))
    }
}
