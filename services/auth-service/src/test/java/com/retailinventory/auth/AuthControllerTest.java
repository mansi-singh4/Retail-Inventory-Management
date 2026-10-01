package com.retailinventory.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class AuthControllerTest {
  private static final String SECRET = "test-secret-that-is-long-enough-for-hs256";

  private User savedUser;
  private UserRepository users;
  private final PasswordEncoder encoder = new BCryptPasswordEncoder();
  private AuthController controller;

  @BeforeEach
  void setUp() {
    users = repository(Optional.empty(), Optional.empty());
    controller = new AuthController(users, encoder, SECRET);
  }

  @Test
  void publicRegistrationAlwaysCreatesStaff() {
    ResponseEntity<?> response = controller.register(
        new AuthController.RegistrationRequest("user@example.com", "password123", null));

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(Role.STAFF, savedUser.role);
    assertEquals(7L, savedUser.id);
    assertTrue(encoder.matches("password123", savedUser.passwordHash));
  }

  @Test
  void publicRegistrationRejectsRequestedRole() {
    ResponseEntity<?> response = controller.register(
        new AuthController.RegistrationRequest("attacker@example.com", "password123", Role.ADMIN));

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(null, savedUser);
  }

  @Test
  void loginTokenContainsOnlyServerControlledRoleClaim() {
    User user = new User("user@example.com", encoder.encode("password123"), Role.STAFF);
    user.id = 9L;
    users = repository(Optional.of(user), Optional.empty());
    controller = new AuthController(users, encoder, SECRET);

    ResponseEntity<?> response = controller.login(
        new AuthController.Credentials(user.email, "password123"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    var body = (java.util.Map<String, Object>) response.getBody();
    var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
        .build().parseSignedClaims((String) body.get("token")).getPayload();
    assertEquals(user.email, claims.getSubject());
    assertEquals(List.of("STAFF"), claims.get("roles"));
    assertFalse(claims.containsKey("email"));
  }

  @Test
  void missingRoleTargetReturnsNotFound() {
    users = repository(Optional.empty(), Optional.empty());
    controller = new AuthController(users, encoder, SECRET);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
        () -> controller.changeRole(44L, new AuthController.RoleRequest(Role.ADMIN),
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "admin@example.com", null, List.of())));

    assertEquals(HttpStatus.NOT_FOUND.value(), exception.getStatusCode().value());
  }

  private UserRepository repository(Optional<User> emailUser, Optional<User> idUser) {
    return (UserRepository) Proxy.newProxyInstance(UserRepository.class.getClassLoader(),
        new Class<?>[] {UserRepository.class}, (proxy, method, args) -> switch (method.getName()) {
          case "findByEmail" -> emailUser;
          case "findById" -> idUser;
          case "save" -> {
            savedUser = (User) args[0];
            savedUser.id = savedUser.id == null ? 7L : savedUser.id;
            yield savedUser;
          }
          case "countByRole" -> 2L;
          default -> null;
        });
  }
}
