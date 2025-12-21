package org.wahlen.voucherengine.persistence.model.validation

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * Persists ValidationRuleContextType values using Voucherengine string identifiers.
 */
@Converter(autoApply = false)
class ValidationRuleContextTypeConverter : AttributeConverter<ValidationRuleContextType?, String?> {
    override fun convertToDatabaseColumn(attribute: ValidationRuleContextType?): String? = attribute?.value

    override fun convertToEntityAttribute(dbData: String?): ValidationRuleContextType? =
        dbData?.let { value -> ValidationRuleContextType.entries.firstOrNull { it.value == value } }
}
