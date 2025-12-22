package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.validation.ValidationRulesAssignment
import java.util.UUID

interface ValidationRulesAssignmentRepository : JpaRepository<ValidationRulesAssignment, UUID>
{
    fun findByRelatedObjectIdAndRelatedObjectType(relatedObjectId: String, relatedObjectType: String): List<ValidationRulesAssignment>
}
