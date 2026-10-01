package com.retailinventory.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity @Table(name="users")
public class User {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(nullable=false, unique=true) public String email;
  @JsonIgnore @Column(nullable=false) public String passwordHash;
  @Enumerated(EnumType.STRING) @Column(nullable=false, length=32) public Role role;
  public User() {}
  public User(String email, String passwordHash) { this(email, passwordHash, Role.STAFF); }
  public User(String email, String passwordHash, Role role) { this.email=email; this.passwordHash=passwordHash; this.role=role; }
}
