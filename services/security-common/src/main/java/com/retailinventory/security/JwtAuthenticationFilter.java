package com.retailinventory.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  public static final String INVALID_JWT_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".invalid";
  public static final String AUTHENTICATED_JWT_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".authenticated";
  private final byte[] secret;
  private static final Pattern ROLE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");

  public JwtAuthenticationFilter(String secret) {
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secret))
            .build()
            .parseSignedClaims(header.substring(7))
            .getPayload();
        String subject = claims.getSubject();
        List<String> roles = extractRoles(claims);
        if (subject != null && !roles.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
          var authorities = roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
          SecurityContextHolder.getContext().setAuthentication(
              new UsernamePasswordAuthenticationToken(subject, null, authorities));
          request.setAttribute(AUTHENTICATED_JWT_ATTRIBUTE, Boolean.TRUE);
            log.info("JWT authentication established method={} path={} subject={} roles={} authorities={}",
              request.getMethod(), request.getRequestURI(), subject, roles, authorities);
          log.debug("JWT verified method={} path={} subject={} roles={} authorities={} expiresAt={}",
              request.getMethod(), request.getRequestURI(), subject, roles, authorities, claims.getExpiration());
        } else {
          log.warn("JWT verified but no authentication created method={} path={} subject={} roles={} existingAuthentication={}",
              request.getMethod(), request.getRequestURI(), subject, roles,
              SecurityContextHolder.getContext().getAuthentication() != null);
        }
      } catch (RuntimeException exception) {
        log.warn("JWT rejected method={} path={} exceptionType={} message={}",
            request.getMethod(), request.getRequestURI(), exception.getClass().getSimpleName(), exception.getMessage());
        request.setAttribute(INVALID_JWT_ATTRIBUTE, Boolean.TRUE);
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }

  private static List<String> extractRoles(Claims claims) {
    Object rolesClaim = claims.get("roles");
    List<String> values = new ArrayList<>();
    if (rolesClaim instanceof List<?> list) {
      for (Object value : list) {
        if (!(value instanceof String role) || !isValidRole(role)) return List.of();
        values.add(role);
      }
    } else {
      String legacyRole = claims.get("role", String.class);
      if (legacyRole != null && isValidRole(legacyRole)) values.add(legacyRole);
    }
    return values.stream().distinct().toList();
  }

  private static boolean isValidRole(String role) {
    return ROLE_PATTERN.matcher(role).matches() && !role.startsWith("ROLE_");
  }
}
