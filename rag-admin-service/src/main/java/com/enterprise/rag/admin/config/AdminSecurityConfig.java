package com.enterprise.rag.admin.config;

import com.enterprise.rag.admin.security.AdminJwtAuthenticationFilter;
import com.enterprise.rag.admin.security.AdminAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("!test")
public class AdminSecurityConfig {

    @Value("${admin.security.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Value("${admin.security.cors.allowed-methods}")
    private List<String> allowedMethods;

    @Bean
    public SecurityFilterChain adminFilterChain(HttpSecurity http,
                                               AdminJwtAuthenticationFilter jwtAuthFilter,
                                               AdminAuthenticationEntryPoint authEntryPoint) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/admin/api/auth/login", "/admin/api/auth/refresh").permitAll()
                        .requestMatchers("/admin/api/health", "/admin/api/info").permitAll()
                        .requestMatchers("/admin/api/v3/api-docs/**", "/admin/api/swagger-ui/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        
                        // Admin-only endpoints
                        .requestMatchers("/admin/api/tenants/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/admin/api/analytics/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers("/admin/api/monitoring/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers("/admin/api/users/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(allowedMethods);
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}