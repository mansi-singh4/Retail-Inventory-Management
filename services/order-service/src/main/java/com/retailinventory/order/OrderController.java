package com.retailinventory.order;

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
@RequestMapping("/orders")
public class OrderController {
  private final OrderRepository repo;
  private final RestClient client = RestClient.builder().build();
  private final String inventoryUrl;

  public OrderController(OrderRepository repo, @Value("${services.inventory-url:http://localhost:8083}") String inventoryUrl) {
    this.repo = repo;
    this.inventoryUrl = inventoryUrl;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public List<CustomerOrder> all() { return repo.findAll(); }

  @PostMapping("/customer")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public ResponseEntity<CustomerOrder> create(@Valid @RequestBody OrderRequest request,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    postInventory("/inventory/reserve", new StockChange(request.productId, request.warehouseId, request.quantity), authorization);
    CustomerOrder order = new CustomerOrder();
    order.customerEmail = request.customerEmail;
    order.productId = request.productId;
    order.warehouseId = request.warehouseId;
    order.quantity = request.quantity;
    order.status = "RESERVED";
    order.createdAt = Instant.now();
    return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(order));
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public CustomerOrder cancel(@PathVariable Long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    CustomerOrder order = repo.findById(id).orElseThrow();
    if ("RESERVED".equals(order.status)) {
      postInventory("/inventory/release", new StockChange(order.productId, order.warehouseId, order.quantity), authorization);
      order.status = "CANCELLED";
      return repo.save(order);
    }
    return order;
  }

  @PostMapping("/{id}/fulfill")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public CustomerOrder fulfill(@PathVariable Long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    CustomerOrder order = repo.findById(id).orElseThrow();
    if ("RESERVED".equals(order.status)) {
      postInventory("/inventory/consume", new StockChange(order.productId, order.warehouseId, order.quantity), authorization);
      order.status = "FULFILLED";
      return repo.save(order);
    }
    return order;
  }

  @PostMapping("/purchase")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public ResponseEntity<CustomerOrder> purchase(@Valid @RequestBody OrderRequest request) {
    CustomerOrder order = new CustomerOrder();
    order.customerEmail = request.customerEmail;
    order.productId = request.productId;
    order.warehouseId = request.warehouseId;
    order.quantity = request.quantity;
    order.status = "PURCHASED";
    order.createdAt = Instant.now();
    return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(order));
  }

  private void postInventory(String path, StockChange body, String authorization) {
    client.post().uri(inventoryUrl + path)
        .header(HttpHeaders.AUTHORIZATION, authorization)
        .body(body)
        .retrieve().toBodilessEntity();
  }

  @Entity
  @Table(name = "orders")
  public static class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String customerEmail;
    public Long productId;
    public Long warehouseId;
    public int quantity;
    public String status;
    public Instant createdAt;
  }

  public record OrderRequest(@NotBlank String customerEmail, @NotNull Long productId,
      @NotNull Long warehouseId, @Min(1) int quantity) {}
  record StockChange(Long productId, Long warehouseId, int quantity) {}
}

interface OrderRepository extends JpaRepository<OrderController.CustomerOrder, Long> {}
