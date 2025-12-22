package org.wahlen.voucherengine.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.ValidationRuleAssignmentRequest
import org.wahlen.voucherengine.api.dto.request.ValidationRuleCreateRequest
import org.wahlen.voucherengine.persistence.repository.ValidationRuleRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ValidationRuleServiceTest @Autowired constructor(
    private val validationRuleService: ValidationRuleService,
    private val validationRuleRepository: ValidationRuleRepository
) {

    @Test
    fun `create rule persists`() {
        val created = validationRuleService.createRule(
            ValidationRuleCreateRequest(
                name = "One per customer",
                type = "redemptions",
                conditions = mapOf("redemptions" to mapOf("per_customer" to 1))
            )
        )
        val found = validationRuleRepository.findById(created.id!!).orElse(null)
        assertNotNull(found)
        assertEquals("One per customer", found.name)
    }

    @Test
    fun `assign rule links target`() {
        val rule = validationRuleService.createRule(
            ValidationRuleCreateRequest(
                name = "Global",
                type = "global",
                conditions = emptyMap()
            )
        )
        val assignment = validationRuleService.assignRule(
            rule.id!!,
            ValidationRuleAssignmentRequest(`object` = "voucher", id = "TEST")
        )
        assertNotNull(assignment.id)
        assertEquals("voucher", assignment.related_object_type)
        assertEquals("TEST", assignment.related_object_id)
    }
}
