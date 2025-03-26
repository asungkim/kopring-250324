package com.example.upload.global.security

import com.example.upload.global.app.AppConfig
import com.example.upload.global.dto.RsData
import com.example.upload.standard.util.Ut
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customAuthenticationFilter: CustomAuthenticationFilter,
    private val customAuthorizationRequestResolver: CustomAuthorizationRequestResolver,
    private val customAuthenticationSuccessHandler: CustomAuthenticationSuccessHandler
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .headers { headers ->
                headers.frameOptions { frameOptions ->
                    frameOptions.sameOrigin()
                }
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests {
                it
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/*/posts/{id:\\d+}",
                        "/api/*/posts",
                        "/api/*/posts/{postId:\\d+}/comments",
                        "/api/*/posts/{postId:\\d+}/genFiles"
                    ).permitAll()
                    .requestMatchers(
                        "/api/*/members/login",
                        "/api/*/members/join",
                        "/api/*/members/logout"
                    ).permitAll()
                    .requestMatchers("/api/v1/posts/statistics").hasRole("ADMIN")
                    .requestMatchers("/api/*/**").authenticated()
                    .anyRequest().permitAll()
            }
            .oauth2Login { oauth2 ->
                oauth2
                    .authorizationEndpoint {
                        it.authorizationRequestResolver(customAuthorizationRequestResolver)
                    }
                    .successHandler(customAuthenticationSuccessHandler)
            }
            .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling {
                it
                    .authenticationEntryPoint { request, response, _ ->
                        response.contentType = "application/json;charset=UTF-8"
                        response.status = HttpServletResponse.SC_UNAUTHORIZED
                        response.writer.write(
                            Ut.json.toString(RsData("401-1", "잘못된 인증키입니다.") {})
                        )
                    }
                    .accessDeniedHandler { request, response, _ ->
                        response.contentType = "application/json;charset=UTF-8"
                        response.status = HttpServletResponse.SC_FORBIDDEN
                        response.writer.write(
                            Ut.json.toString(RsData("403-1", "접근 권한이 없습니다.") {})
                        )
                    }
            }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("https://cdpn.io", AppConfig.getSiteFrontUrl())
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE")
            allowCredentials = true
            allowedHeaders = listOf("*")
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", configuration)
        }
    }
}