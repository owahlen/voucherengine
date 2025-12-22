package org.wahlen.voucherengine.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.wahlen.voucherengine.api.controller.ErrorDtoFactory
import org.wahlen.voucherengine.service.TenantService

@Component
class TenantHeaderAuthorizationFilter(
    private val errorDtoFactory: ErrorDtoFactory,
    private val tenantService: TenantService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication is JwtAuthenticationToken) {
            val tenantHeader = request.getHeader("tenant")?.trim()
            if (tenantHeader.isNullOrBlank()) {
                errorDtoFactory.write(response, request, HttpStatus.FORBIDDEN, "Tenant header required")
                return
            }
            if (authentication.authorities.any { it.authority == "ROLE_MANAGER" }) {
                filterChain.doFilter(request, response)
                return
            }
            val tenants = authentication.token.getClaimAsStringList("tenants") ?: emptyList()
            if (!tenants.contains(tenantHeader)) {
                errorDtoFactory.write(response, request, HttpStatus.FORBIDDEN, "Tenant not allowed")
                return
            }
            tenantService.ensureTenantExists(tenantHeader)
        }
        filterChain.doFilter(request, response)
    }
}
