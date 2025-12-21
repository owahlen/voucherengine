package org.wahlen.voucherengine.persistence.model.validation

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * Persists ValidationRuleType values using Voucherengine string identifiers.
 */
@Converter(autoApply = false)
class ValidationRuleTypeConverter : AttributeConverter<ValidationRuleType?, String?> {
    override fun convertToDatabaseColumn(attribute: ValidationRuleType?): String? = attribute?.value

    override fun convertToEntityAttribute(dbData: String?): ValidationRuleType? =
        dbData?.let { value -> ValidationRuleType.entries.firstOrNull { it.value == value } }
}
