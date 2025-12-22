package org.wahlen.voucherengine.persistence.model.validation

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.tenant.Tenant

/**
 * Represents a Voucherengine validation rule definition.
 *
 * Validation rules describe eligibility logic (basic, advanced, or expression-based) and
 * how a voucher, campaign, or promotion tier should qualify against order, customer, or
 * product context. They can also define inclusion/exclusion of items for discounts.
 *
 * Voucherengine API Docs: Validation Rules.
 */
@Entity
@Table
class ValidationRule(
    @Column
    var name: String? = null,

    @Column
    @Convert(converter = ValidationRuleTypeConverter::class)
    var type: ValidationRuleType? = null,

    @Column
    @Convert(converter = ValidationRuleContextTypeConverter::class)
    var contextType: ValidationRuleContextType? = null,

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var rules: Map<String, Any?>? = null,

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var bundleRules: Map<String, Any?>? = null,

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var applicableTo: Map<String, Any?>? = null,

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var error: Map<String, Any?>? = null,

    @Column
    var assignmentsCount: Int? = null,

    @Column
    var objectType: String? = null,
) : AuditablePersistable() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

    @OneToMany(mappedBy = "rule", fetch = FetchType.LAZY)
    var assignments: MutableList<ValidationRulesAssignment> = mutableListOf()
}
