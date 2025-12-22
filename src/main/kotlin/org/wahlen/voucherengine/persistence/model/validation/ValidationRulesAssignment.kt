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
    @Column(name = "rule_id")
    var ruleId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", insertable = false, updatable = false)
    var rule: ValidationRule? = null,

    @Column(name = "related_object_id")
    var relatedObjectId: String? = null,

    @Column(name = "related_object_type")
    var relatedObjectType: String? = null,

    @Column(name = "validation_status")
    var validationStatus: String? = null,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "validation_rules_assignment_omitted_rules",
        joinColumns = [JoinColumn(name = "assignment_id")]
    )
    @Column(name = "omitted_rule")
    var omittedRules: MutableSet<String> = mutableSetOf(),

) : AuditablePersistable() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null
}
