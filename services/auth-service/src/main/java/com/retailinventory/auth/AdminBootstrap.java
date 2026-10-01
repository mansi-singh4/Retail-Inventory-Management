package com.retailinventory.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {
  private final UserRepository users;
  private final String email;

  public AdminBootstrap(UserRepository users, @Value("${security.initial-admin-email:}") String email) {
    this.users = users;
    this.email = email;
  }

  @Override
  public void run(String... args) {
    if (email.isBlank() || users.countByRole(Role.ADMIN) > 0) return;
    users.findByEmail(email.trim()).ifPresent(user -> {
      user.role = Role.ADMIN;
      users.save(user);
    });
  }
}
