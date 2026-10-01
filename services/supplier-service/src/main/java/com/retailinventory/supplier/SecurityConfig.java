package com.retailinventory.supplier;

import com.retailinventory.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, @Value("${security.jwt-secret}") String secret) throws Exception {
    return http.csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(e -> e.authenticationEntryPoint((request, response, error) -> response.sendError(
          request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_JWT_ATTRIBUTE) == null
            ? HttpStatus.UNAUTHORIZED.value() : HttpStatus.FORBIDDEN.value()))
            .accessDeniedHandler((request, response, error) -> response.sendError(HttpStatus.FORBIDDEN.value())))
        .authorizeHttpRequests(a -> a.requestMatchers("/actuator/health").permitAll().anyRequest().authenticated())
        .addFilterBefore(new JwtAuthenticationFilter(secret), UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
