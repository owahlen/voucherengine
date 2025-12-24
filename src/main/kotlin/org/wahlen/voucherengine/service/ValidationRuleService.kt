package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.ValidationRuleAssignmentRequest
import org.wahlen.voucherengine.api.dto.request.ValidationRuleCreateRequest
import org.wahlen.voucherengine.api.dto.response.ValidationRuleAssignmentResponse
import org.wahlen.voucherengine.api.dto.response.ValidationRuleResponse
import org.wahlen.voucherengine.persistence.model.validation.ValidationRule
import org.wahlen.voucherengine.persistence.model.validation.ValidationRulesAssignment
import org.wahlen.voucherengine.persistence.repository.ValidationRuleRepository
import org.wahlen.voucherengine.persistence.repository.ValidationRulesAssignmentRepository
import java.util.UUID

@Service
class ValidationRuleService(
    private val validationRuleRepository: ValidationRuleRepository,
    private val validationRulesAssignmentRepository: ValidationRulesAssignmentRepository,
    private val tenantService: TenantService
) {

    @Transactional
    fun createRule(tenantName: String, request: ValidationRuleCreateRequest): ValidationRuleResponse {
        val tenant = tenantService.requireTenant(tenantName)
        val rulesPayload = assembleRulesPayload(request)
        val rule = ValidationRule(
            name = request.name,
            type = request.type?.let { org.wahlen.voucherengine.persistence.model.validation.ValidationRuleType.fromString(it) },
            contextType = request.context_type?.let { org.wahlen.voucherengine.persistence.model.validation.ValidationRuleContextType.fromString(it) },
            rules = rulesPayload,
            bundleRules = request.bundle_rules,
            applicableTo = request.applicable_to,
            error = request.error,
            tenant = tenant
        )
        return toResponse(validationRuleRepository.save(rule))
    }

    @Transactional
    fun assignRule(tenantName: String, ruleId: UUID, request: ValidationRuleAssignmentRequest): ValidationRuleAssignmentResponse {
        val tenant = tenantService.requireTenant(tenantName)
        val assignment = ValidationRulesAssignment(
            ruleId = ruleId,
            relatedObjectId = request.id,
            relatedObjectType = request.`object`,
            tenant = tenant
        )
        return toAssignmentResponse(validationRulesAssignmentRepository.save(assignment))
    }

    @Transactional(readOnly = true)
    fun getRule(tenantName: String, id: UUID): ValidationRuleResponse? =
        validationRuleRepository.findByIdAndTenantName(id, tenantName)?.let(::toResponse)

    @Transactional(readOnly = true)
    fun listRules(tenantName: String): List<ValidationRuleResponse> =
        validationRuleRepository.findAllByTenantName(tenantName).map(::toResponse)

    @Transactional(readOnly = true)
    fun listAssignments(tenantName: String): List<ValidationRuleAssignmentResponse> =
        validationRulesAssignmentRepository.findAllByTenantName(tenantName).map(::toAssignmentResponse)

    @Transactional(readOnly = true)
    fun listAssignmentsForRule(tenantName: String, ruleId: UUID): List<ValidationRuleAssignmentResponse> =
        validationRulesAssignmentRepository.findAllByRuleIdAndTenantName(ruleId, tenantName)
            .map(::toAssignmentResponse)

    @Transactional
    fun deleteRule(tenantName: String, id: UUID) {
        val existing = validationRuleRepository.findByIdAndTenantName(id, tenantName) ?: return
        validationRuleRepository.delete(existing)
    }

    @Transactional
    fun deleteAssignment(tenantName: String, id: UUID): Boolean {
        val existing = validationRulesAssignmentRepository.findByIdAndTenantName(id, tenantName) ?: return false
        if (existing.id != null) {
            validationRulesAssignmentRepository.delete(existing)
            return true
        }
        return false
    }

    @Transactional
    fun updateRule(tenantName: String, id: UUID, request: ValidationRuleCreateRequest): ValidationRuleResponse? {
        val existing = validationRuleRepository.findByIdAndTenantName(id, tenantName) ?: return null
        val rulesPayload = assembleRulesPayload(request)
        existing.name = request.name ?: existing.name
        existing.type = request.type?.let { org.wahlen.voucherengine.persistence.model.validation.ValidationRuleType.fromString(it) } ?: existing.type
        existing.contextType = request.context_type?.let { org.wahlen.voucherengine.persistence.model.validation.ValidationRuleContextType.fromString(it) } ?: existing.contextType
        existing.rules = rulesPayload ?: existing.rules
        existing.bundleRules = request.bundle_rules ?: existing.bundleRules
        existing.applicableTo = request.applicable_to ?: existing.applicableTo
        existing.error = request.error ?: existing.error
        return toResponse(validationRuleRepository.save(existing))
    }

    private fun toResponse(rule: ValidationRule): ValidationRuleResponse = ValidationRuleResponse(
        id = rule.id,
        name = rule.name,
        type = rule.type?.name?.lowercase(),
        context_type = rule.contextType?.value,
        conditions = rule.rules,
        logic = (rule.rules?.get("logic") as? String),
        bundle_rules = rule.bundleRules,
        applicable_to = rule.applicableTo,
        error = rule.error,
        created_at = rule.createdAt
    )
    private fun toAssignmentResponse(assignment: ValidationRulesAssignment): ValidationRuleAssignmentResponse =
        ValidationRuleAssignmentResponse(
            id = assignment.id,
            rule_id = assignment.ruleId,
            related_object_id = assignment.relatedObjectId,
            related_object_type = assignment.relatedObjectType,
            validation_status = assignment.validationStatus,
            validation_omitted_rules = assignment.omittedRules,
            created_at = assignment.createdAt,
            updated_at = assignment.updatedAt
        )

    private fun assembleRulesPayload(request: ValidationRuleCreateRequest): Map<String, Any?>? {
        val base = request.rules ?: request.conditions ?: return null
        val mutable = LinkedHashMap<String, Any?>()
        mutable.putAll(base)
        if (!mutable.containsKey("logic") && request.logic != null) {
            mutable["logic"] = request.logic
        }
        return mutable
    }
}
