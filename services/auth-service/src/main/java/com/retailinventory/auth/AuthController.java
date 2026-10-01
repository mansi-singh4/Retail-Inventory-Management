package com.retailinventory.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final byte[] secret;

  public AuthController(UserRepository users, PasswordEncoder encoder,
      @Value("${security.jwt-secret}") String secret) {
    this.users = users;
    this.encoder = encoder;
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest input) {
    if (input.role != null) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "Public registration cannot choose a role"));
    }
    if (users.findByEmail(input.email).isPresent()) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body(java.util.Map.of("error", "Email already registered"));
    }
    User user = users.save(new User(input.email, encoder.encode(input.password), Role.STAFF));
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody Credentials input) {
    User user = users.findByEmail(input.email).orElse(null);
    if (user == null || !encoder.matches(input.password, user.passwordHash)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error", "Invalid credentials"));
    }
    String token = Jwts.builder()
        .subject(user.email)
        .claim("roles", List.of(user.role.name()))
        .issuedAt(new Date())
        .expiration(Date.from(Instant.now().plusSeconds(3600)))
        .signWith(Keys.hmacShaKeyFor(secret))
        .compact();
    return ResponseEntity.ok(java.util.Map.of("token", token, "email", user.email, "role", user.role.name(), "id", user.id));
  }

  @GetMapping("/users")
  @PreAuthorize("hasRole('ADMIN')")
  public List<UserResponse> users() {
    return users.findAll().stream().map(AuthController::toResponse).toList();
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> me(Authentication authentication) {
    return users.findByEmail(authentication.getName())
        .map(user -> ResponseEntity.ok(toResponse(user)))
        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  @GetMapping("/users/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse user(@PathVariable Long id) {
    return toResponse(users.findById(id).orElseThrow());
  }

  @PutMapping("/users/{id}/role")
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse changeRole(@PathVariable Long id, @Valid @RequestBody RoleRequest input, Authentication authentication) {
    User target = users.findById(id).orElseThrow(() ->
      new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    if (target.email.equals(authentication.getName()) && input.role != Role.ADMIN) {
      throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "An administrator cannot demote themselves");
    }
    if (target.role == Role.ADMIN && input.role != Role.ADMIN && users.countByRole(Role.ADMIN) <= 1) {
      throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "The last administrator cannot be demoted");
    }
    target.role = input.role;
    return toResponse(users.save(target));
  }

  private static UserResponse toResponse(User user) {
    return new UserResponse(user.id, user.email, user.role);
  }

  public record Credentials(@Email @NotBlank String email, @Size(min = 8) String password) {}
  public record RegistrationRequest(@Email @NotBlank String email, @Size(min = 8) String password, Role role) {}
  public record RoleRequest(@NotNull Role role) {}
  public record UserResponse(Long id, String email, Role role) {}
}
