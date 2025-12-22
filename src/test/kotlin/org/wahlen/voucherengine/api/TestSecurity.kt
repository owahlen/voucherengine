package org.wahlen.voucherengine.api

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.security.core.authority.SimpleGrantedAuthority

private fun roleAuthority(role: String): SimpleGrantedAuthority {
    val normalized = role.uppercase()
    val authority = if (normalized.startsWith("ROLE_")) normalized else "ROLE_$normalized"
    return SimpleGrantedAuthority(authority)
}

fun tenantJwt(tenant: String, role: String = "tenant"): RequestPostProcessor =
    jwt()
        .jwt { token ->
            token.claim("tenants", listOf(tenant))
            token.claim("realm_access", mapOf("roles" to listOf(role)))
        }
        .authorities(roleAuthority(role))

fun roleJwt(role: String): RequestPostProcessor =
    jwt()
        .jwt { token ->
            token.claim("realm_access", mapOf("roles" to listOf(role)))
        }
        .authorities(roleAuthority(role))
