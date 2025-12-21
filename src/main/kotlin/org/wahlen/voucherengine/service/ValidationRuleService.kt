package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.ValidationRuleAssignmentRequest
import org.wahlen.voucherengine.api.dto.request.ValidationRuleCreateRequest
import org.wahlen.voucherengine.persistence.model.validation.ValidationRule
import org.wahlen.voucherengine.persistence.model.validation.ValidationRulesAssignment
import org.wahlen.voucherengine.persistence.repository.ValidationRuleRepository
import org.wahlen.voucherengine.persistence.repository.ValidationRulesAssignmentRepository
import java.util.UUID

@Service
class ValidationRuleService(
    private val validationRuleRepository: ValidationRuleRepository,
    private val validationRulesAssignmentRepository: ValidationRulesAssignmentRepository
) {

    @Transactional
    fun createRule(request: ValidationRuleCreateRequest): ValidationRule {
        val rule = ValidationRule(
            name = request.name,
            type = null, // left null; mapping to enum not specified in request
            contextType = null,
            rules = request.conditions
        )
        return validationRuleRepository.save(rule)
    }

    @Transactional
    fun assignRule(ruleId: UUID, request: ValidationRuleAssignmentRequest): ValidationRulesAssignment {
        val assignment = ValidationRulesAssignment(
            ruleId = ruleId,
            relatedObjectId = request.id,
            relatedObjectType = request.`object`
        )
        return validationRulesAssignmentRepository.save(assignment)
    }

    @Transactional(readOnly = true)
    fun getRule(id: UUID): ValidationRule? = validationRuleRepository.findById(id).orElse(null)

    @Transactional(readOnly = true)
    fun listRules(): List<ValidationRule> = validationRuleRepository.findAll()

    @Transactional
    fun deleteRule(id: UUID) {
        validationRuleRepository.findById(id).ifPresent { validationRuleRepository.delete(it) }
    }

    @Transactional
    fun updateRule(id: UUID, request: ValidationRuleCreateRequest): ValidationRule? {
        val existing = validationRuleRepository.findById(id).orElse(null) ?: return null
        existing.name = request.name ?: existing.name
        existing.type = existing.type
        existing.rules = request.conditions ?: existing.rules
        return validationRuleRepository.save(existing)
    }
}
