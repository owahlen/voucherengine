package org.wahlen.voucherengine.persistence.model.validation

import jakarta.persistence.*
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import java.util.UUID

/**
 * Represents a Voucherengine validation rule assignment linking a rule to a target object.
 *
 * Validation rules define eligibility criteria such as customer segments, redemption limits,
 * order amount thresholds, or product restrictions. Assignments connect a rule to a voucher,
 * campaign, or promotion so the rule is evaluated during qualification and redemption.
 *
 * Voucherengine API Docs: Validation Rules.
 */
@Entity
@Table(
    indexes = [
        Index(name = "idx_vra_rule_id", columnList = "rule_id"),
        Index(name = "idx_vra_related_object", columnList = "related_object_type,related_object_id")
    ]
)
class ValidationRulesAssignment(

    /**
     * ID of the validation rule being assigned.
     */
    @Column(name = "rule_id")
    var ruleId: UUID? = null,

    /**
     * Reference to the validation rule entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", insertable = false, updatable = false)
    var rule: ValidationRule? = null,

    /**
     * ID of the related object (voucher, campaign, promotion tier).
     */
    @Column(name = "related_object_id")
    var relatedObjectId: String? = null,

    /**
     * Type of the related object (e.g., voucher, campaign).
     */
    @Column(name = "related_object_type")
    var relatedObjectType: String? = null,

    /**
     * Validation status for this assignment.
     */
    @Column(name = "validation_status")
    var validationStatus: String? = null,

    /**
     * Rules omitted from validation for this assignment.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "validation_rules_assignment_omitted_rules",
        joinColumns = [JoinColumn(name = "assignment_id")]
    )
    @Column(name = "omitted_rule")
    var omittedRules: MutableSet<String> = mutableSetOf(),

    /**
     * Tenant that owns this assignment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null

) : AuditablePersistable()
