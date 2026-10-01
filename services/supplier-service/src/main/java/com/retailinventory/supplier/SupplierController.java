package com.retailinventory.supplier;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {
  private final SupplierRepository repo;
  private final PurchaseOrderRepository purchaseOrders;
  private final RestClient client = RestClient.builder().build();
  private final String inventoryUrl;

  public SupplierController(SupplierRepository repo, PurchaseOrderRepository purchaseOrders,
      @Value("${services.inventory-url:http://localhost:8083}") String inventoryUrl) {
    this.repo = repo;
    this.purchaseOrders = purchaseOrders;
    this.inventoryUrl = inventoryUrl;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
  public List<Supplier> all() { return repo.findAll(); }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Supplier> create(@Valid @RequestBody Supplier supplier) {
    supplier.id = null;
    return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(supplier));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public Supplier update(@PathVariable Long id, @Valid @RequestBody Supplier input) {
    Supplier supplier = repo.findById(id).orElseThrow();
    supplier.name = input.name;
    supplier.email = input.email;
    supplier.phone = input.phone;
    return repo.save(supplier);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public void delete(@PathVariable Long id) { repo.deleteById(id); }

  @RestController
  @RequestMapping("/purchase-orders")
  class PurchaseOrderController {

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public List<PurchaseOrder> all() { return purchaseOrders.findAll(); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PurchaseOrder> create(@Valid @RequestBody PurchaseOrder input) {
      input.id = null;
      input.status = "OPEN";
      input.createdAt = Instant.now();
      return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrders.save(input));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public PurchaseOrder receive(@PathVariable Long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
      PurchaseOrder order = purchaseOrders.findById(id).orElseThrow();
      if (!"RECEIVED".equals(order.status)) {
        client.post().uri(inventoryUrl + "/inventory/stock-in")
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .body(new StockChange(order.productId, order.warehouseId, order.quantity))
            .retrieve().toBodilessEntity();
        order.status = "RECEIVED";
        return purchaseOrders.save(order);
      }
      return order;
    }
  }

  @Entity
  @Table(name = "suppliers")
  public static class Supplier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @NotBlank public String name;
    @Email public String email;
    public String phone;
  }

  @Entity
  @Table(name = "purchase_orders")
  static class PurchaseOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @NotNull Long supplierId;
    @NotNull Long productId;
    @NotNull Long warehouseId;
    @Min(1) int quantity;
    String status;
    Instant createdAt;
  }

  record StockChange(Long productId, Long warehouseId, int quantity) {}
}

interface SupplierRepository extends JpaRepository<SupplierController.Supplier, Long> {}
interface PurchaseOrderRepository extends JpaRepository<SupplierController.PurchaseOrder, Long> {}
