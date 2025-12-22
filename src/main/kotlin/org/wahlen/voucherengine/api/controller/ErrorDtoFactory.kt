package org.wahlen.voucherengine.api.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.wahlen.voucherengine.api.dto.ErrorDto
import tools.jackson.databind.ObjectMapper

@Component
class ErrorDtoFactory(
    private val objectMapper: ObjectMapper
) {

    fun build(
        status: HttpStatus,
        message: String,
        errors: List<String>?,
        request: HttpServletRequest
    ): ErrorDto = ErrorDto(
        timestamp = System.currentTimeMillis(),
        status = status.value(),
        error = status.reasonPhrase,
        message = message,
        errors = errors,
        path = request.requestURI
    )

    fun write(
        response: HttpServletResponse,
        request: HttpServletRequest,
        status: HttpStatus,
        message: String,
        errors: List<String>? = null
    ) {
        if (response.isCommitted) return
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        objectMapper.writeValue(response.outputStream, build(status, message, errors, request))
    }
}
