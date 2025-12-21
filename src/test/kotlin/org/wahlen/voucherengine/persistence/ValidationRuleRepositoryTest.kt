package org.wahlen.voucherengine.persistence

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.persistence.model.validation.ValidationRule
import org.wahlen.voucherengine.persistence.model.validation.ValidationRuleContextType
import org.wahlen.voucherengine.persistence.model.validation.ValidationRuleType
import org.wahlen.voucherengine.persistence.repository.ValidationRuleRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ValidationRuleRepositoryTest @Autowired constructor(
    private val validationRuleRepository: ValidationRuleRepository
) {

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
