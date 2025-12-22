package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.validation.ValidationRule
import java.util.UUID

interface ValidationRuleRepository : JpaRepository<ValidationRule, UUID> {
    fun findAllByTenantName(tenantName: String): List<ValidationRule>
    fun findByIdAndTenantName(id: UUID, tenantName: String): ValidationRule?
}
