package com.retailinventory.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {
  private final ProductRepository repo;

  public ProductController(ProductRepository repo) { this.repo = repo; }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
  public List<Product> all() { return repo.findAll(); }

  @PostMapping
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
  public ResponseEntity<Product> create(@Valid @RequestBody Product input) {
    input.id = null;
    return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(input));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
  public Product get(@PathVariable Long id) { return repo.findById(id).orElseThrow(); }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
  public Product update(@PathVariable Long id, @Valid @RequestBody Product input) {
    Product product = get(id);
    product.name = input.name;
    product.sku = input.sku;
    product.price = input.price;
    product.category = input.category;
    return repo.save(product);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
  public void delete(@PathVariable Long id) { repo.deleteById(id); }

  @Entity
  @Table(name = "products")
  public static class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @NotBlank public String sku;
    @NotBlank public String name;
    @NotNull @PositiveOrZero public BigDecimal price;
    public String category;
  }
}
