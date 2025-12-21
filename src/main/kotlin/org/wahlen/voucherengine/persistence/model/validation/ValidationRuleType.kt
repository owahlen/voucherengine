package org.wahlen.voucherengine.persistence.model.validation

/**
 * Enumerates the validation rule types supported by Voucherengine.
 *
 * Voucherengine API Docs: Validation Rules.
 */
enum class ValidationRuleType(val value: String) {
    EXPRESSION("expression"),
    BASIC("basic"),
    ADVANCED("advanced"),
    COMPLEX("complex");
}
