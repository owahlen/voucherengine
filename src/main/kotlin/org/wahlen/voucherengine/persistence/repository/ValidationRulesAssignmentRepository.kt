package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.validation.ValidationRulesAssignment
import java.util.UUID

interface ValidationRulesAssignmentRepository : JpaRepository<ValidationRulesAssignment, UUID> {
    fun findByRelatedObjectIdAndRelatedObjectTypeAndTenantName(
        relatedObjectId: String,
        relatedObjectType: String,
        tenantName: String
    ): List<ValidationRulesAssignment>

    fun findAllByTenantName(tenantName: String): List<ValidationRulesAssignment>
    fun findAllByRuleIdAndTenantName(ruleId: UUID, tenantName: String): List<ValidationRulesAssignment>
    fun findByIdAndTenantName(id: UUID, tenantName: String): ValidationRulesAssignment?
    fun findAllByTenantNameAndRelatedObjectTypeAndRelatedObjectIdIn(
        tenantName: String,
        relatedObjectType: String,
        relatedObjectIds: List<String>
    ): List<ValidationRulesAssignment>
}
