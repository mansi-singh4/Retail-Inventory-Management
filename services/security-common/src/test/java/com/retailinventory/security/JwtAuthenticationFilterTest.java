package com.retailinventory.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {
  private static final String SECRET = "test-secret-that-is-long-enough-for-hs256";

  @AfterEach
  void clearContext() { SecurityContextHolder.clearContext(); }

  @Test
  void authenticatesValidRoleClaim() throws Exception {
    String token = Jwts.builder().subject("user@example.com").claim("role", "STAFF")
        .expiration(new Date(System.currentTimeMillis() + 60_000))
        .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    new JwtAuthenticationFilter(SECRET).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertEquals("user@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
    assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals("ROLE_STAFF")));
  }

  @Test
  void ignoresTamperedToken() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer not-a-valid-token");

    new JwtAuthenticationFilter(SECRET).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void authenticatesAllValidRolesClaimValues() throws Exception {
    String token = Jwts.builder().subject("admin@example.com").claim("roles", List.of("ADMIN", "MANAGER"))
        .expiration(new Date(System.currentTimeMillis() + 60_000))
        .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    new JwtAuthenticationFilter(SECRET).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .map(authority -> authority.getAuthority()).toList();
    assertTrue(authorities.contains("ROLE_ADMIN"));
    assertTrue(authorities.contains("ROLE_MANAGER"));
  }

  @Test
  void rejectsExpiredToken() throws Exception {
    String token = Jwts.builder().subject("user@example.com").claim("roles", List.of("STAFF"))
        .expiration(new Date(System.currentTimeMillis() - 1_000))
        .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    new JwtAuthenticationFilter(SECRET).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void rejectsDoublePrefixedRole() throws Exception {
    String token = Jwts.builder().subject("admin@example.com").claim("roles", List.of("ROLE_ADMIN"))
        .expiration(new Date(System.currentTimeMillis() + 60_000))
        .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);

    new JwtAuthenticationFilter(SECRET).doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
