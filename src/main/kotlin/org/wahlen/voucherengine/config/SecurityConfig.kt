package org.wahlen.voucherengine.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.config.http.SessionCreationPolicy
import org.wahlen.voucherengine.api.controller.ErrorDtoFactory

@Configuration
class SecurityConfig(
    private val tenantHeaderAuthorizationFilter: TenantHeaderAuthorizationFilter,
    private val errorDtoFactory: ErrorDtoFactory
) {

    @Bean
    fun roleHierarchy(): RoleHierarchy =
        RoleHierarchyImpl.fromHierarchy(
            """
            ROLE_MANAGER > ROLE_TENANT
            """.trimIndent()
        )

    @Bean
    fun jwtAuthenticationConverter(roleHierarchy: RoleHierarchy): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt ->
            val roles = mutableSetOf<String>()
            val realmAccess = jwt.getClaimAsMap("realm_access")?.get("roles")
            if (realmAccess is Collection<*>) {
                roles.addAll(realmAccess.filterNotNull().map { it.toString() })
            }
            jwt.getClaimAsStringList("roles")?.let { roles.addAll(it) }
            val authorities = roles
                .map { role ->
                    val normalized = role.uppercase()
                    val authority = if (normalized.startsWith("ROLE_")) normalized else "ROLE_$normalized"
                    SimpleGrantedAuthority(authority)
                }
                .toMutableSet()
            roleHierarchy.getReachableGrantedAuthorities(authorities)
        }
        return converter
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter
    ): SecurityFilterChain {
        http.csrf { it.disable() }
        http.authorizeHttpRequests { auth ->
            auth.requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html"
            ).permitAll()
                .requestMatchers("/v1/tenants/**").hasRole("MANAGER")
                .anyRequest().hasRole("TENANT")
        }
        http.oauth2ResourceServer { oauth ->
            oauth.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter) }
        }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        http.exceptionHandling { handlers ->
            handlers.authenticationEntryPoint { request, response, _ ->
                errorDtoFactory.write(response, request, HttpStatus.UNAUTHORIZED, "Unauthorized")
            }
            handlers.accessDeniedHandler { request, response, _ ->
                errorDtoFactory.write(response, request, HttpStatus.FORBIDDEN, "Forbidden")
            }
        }
        http.addFilterAfter(tenantHeaderAuthorizationFilter, BearerTokenAuthenticationFilter::class.java)
        return http.build()
    }
}
