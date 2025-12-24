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
class ValidationRule(

    /**
     * Name of the validation rule.
     */
    @Column
    var name: String? = null,

    /**
     * Type of validation logic (basic, advanced, expression).
     */
    @Column
    @Convert(converter = ValidationRuleTypeConverter::class)
    var type: ValidationRuleType? = null,

    /**
     * Context where this rule applies (order, customer, product, etc.).
     */
    @Column
    @Convert(converter = ValidationRuleContextTypeConverter::class)
    var contextType: ValidationRuleContextType? = null,

    /**
     * Rule conditions as JSON.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var rules: Map<String, Any?>? = null,

    /**
     * Bundle-specific rule conditions.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var bundleRules: Map<String, Any?>? = null,

    /**
     * Defines which items this rule applies to (inclusion/exclusion logic).
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var applicableTo: Map<String, Any?>? = null,

    /**
     * Custom error response if validation fails.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var error: Map<String, Any?>? = null,

    /**
     * Number of campaigns/vouchers assigned to this rule.
     */
    @Column
    var assignmentsCount: Int? = null,

    /**
     * Object type this rule applies to (e.g., voucher, campaign).
     */
    @Column
    var objectType: String? = null,

    /**
     * Tenant that owns this validation rule.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null,

    /**
     * Assignments linking this rule to campaigns or vouchers.
     */
    @OneToMany(mappedBy = "rule", fetch = FetchType.LAZY)
    var assignments: MutableList<ValidationRulesAssignment> = mutableListOf()

) : AuditablePersistable()
