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

    companion object {
        fun fromString(value: String): ValidationRuleType? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) || it.name.equals(value, ignoreCase = true) }
    }
}
