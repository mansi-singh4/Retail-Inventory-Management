package com.retailinventory.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/categories")
public class CategoryController {
  private final CategoryRepository repo;

  public CategoryController(CategoryRepository repo) { this.repo = repo; }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
  public List<Category> all() { return repo.findAll(); }

  @PostMapping
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
  public ResponseEntity<Category> create(@Valid @RequestBody Category input) {
    input.id = null;
    return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(input));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
  public Category update(@PathVariable Long id, @Valid @RequestBody Category input) {
    Category category = repo.findById(id).orElseThrow();
    category.name = input.name;
    category.description = input.description;
    return repo.save(category);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
  public void delete(@PathVariable Long id) { repo.deleteById(id); }

  @Entity
  @Table(name = "categories")
  public static class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @NotBlank public String name;
    public String description;
  }
}

interface CategoryRepository extends JpaRepository<CategoryController.Category, Long> {}
