package org.wahlen.voucherengine.api.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import org.wahlen.voucherengine.api.dto.ErrorDto

@RestControllerAdvice
class GlobalExceptionHandler(
    private val errorDtoFactory: ErrorDtoFactory
) {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorDto> {
        val errors = ex.bindingResult.fieldErrors.map { "'${it.field}' ${it.defaultMessage}" } +
            ex.bindingResult.globalErrors.map { it.defaultMessage ?: "Invalid request" }
        val body = errorDtoFactory.build(HttpStatus.BAD_REQUEST, "Parameter validation failed", errors, request)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorDto> {
        val errors = ex.constraintViolations.map { it.propertyPath.toString() + " " + it.message }
        val body = errorDtoFactory.build(HttpStatus.BAD_REQUEST, "Parameter validation failed", errors, request)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedJson(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorDto> {
        val body = errorDtoFactory.build(HttpStatus.BAD_REQUEST, "Malformed JSON request", null, request)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(
        ex: ResponseStatusException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorDto> {
        val status = HttpStatus.resolve(ex.statusCode.value())
            ?: HttpStatus.INTERNAL_SERVER_ERROR
        val body = errorDtoFactory.build(status, ex.reason ?: status.reasonPhrase, null, request)
        return ResponseEntity.status(status).body(body)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(
        ex: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorDto> {
        val body = errorDtoFactory.build(HttpStatus.UNAUTHORIZED, "Unauthorized", null, request)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(
        ex: AccessDeniedException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorDto> {
        val body = errorDtoFactory.build(HttpStatus.FORBIDDEN, "Forbidden", null, request)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorDto> {
        log.error("Unhandled exception", ex)
        val body = errorDtoFactory.build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", null, request)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body)
    }
}
