package org.wahlen.voucherengine.api.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import org.wahlen.voucherengine.api.dto.ErrorDto

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorDto> {
        val errors = ex.bindingResult.fieldErrors.map { "'${it.field}' ${it.defaultMessage}" } +
            ex.bindingResult.globalErrors.map { it.defaultMessage ?: "Invalid request" }
        val body = errorDto(HttpStatus.BAD_REQUEST, "Parameter validation failed", errors, request)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorDto> {
        val errors = ex.constraintViolations.map { it.propertyPath.toString() + " " + it.message }
        val body = errorDto(HttpStatus.BAD_REQUEST, "Parameter validation failed", errors, request)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedJson(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorDto> {
        val body = errorDto(HttpStatus.BAD_REQUEST, "Malformed JSON request", null, request)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(
        ex: ResponseStatusException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorDto> {
        val status = org.springframework.http.HttpStatus.resolve(ex.statusCode.value())
            ?: HttpStatus.INTERNAL_SERVER_ERROR
        val body = errorDto(status, ex.reason ?: status.reasonPhrase, null, request)
        return ResponseEntity.status(status).body(body)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorDto> {
        log.error("Unhandled exception", ex)
        val body = errorDto(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", null, request)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body)
    }

    private fun errorDto(
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
}
