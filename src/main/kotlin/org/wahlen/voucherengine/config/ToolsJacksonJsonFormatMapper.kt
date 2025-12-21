package org.wahlen.voucherengine.config

import org.hibernate.type.format.AbstractJsonFormatMapper
import tools.jackson.databind.ObjectMapper
import java.lang.reflect.Type

/**
 * Hibernate JSON FormatMapper using Jackson 3 (tools.jackson.*).
 * Keeps JSONB columns working without relying on com.fasterxml packages.
 */
class ToolsJacksonJsonFormatMapper(
    private val objectMapper: ObjectMapper = ObjectMapper()
) : AbstractJsonFormatMapper() {

    override fun <T> fromString(charSequence: CharSequence, type: Type): T {
        val javaType = objectMapper.typeFactory.constructType(type)
        return objectMapper.readValue(charSequence.toString(), javaType)
    }

    override fun <T> toString(value: T, type: Type): String {
        val javaType = objectMapper.typeFactory.constructType(type)
        return objectMapper.writerFor(javaType).writeValueAsString(value)
    }
}
