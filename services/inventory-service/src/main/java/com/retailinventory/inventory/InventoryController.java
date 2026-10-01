package com.retailinventory.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
  private final StockRepository stocks;
  private final WarehouseRepository warehouses;

  public InventoryController(StockRepository stocks, WarehouseRepository warehouses) {
    this.stocks = stocks;
    this.warehouses = warehouses;
  }

  @GetMapping("/warehouses")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
  public List<Warehouse> warehouses() { return warehouses.findAll(); }

  @PostMapping("/warehouses")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public ResponseEntity<Warehouse> addWarehouse(@Valid @RequestBody Warehouse warehouse) {
    warehouse.id = null;
    return ResponseEntity.status(HttpStatus.CREATED).body(warehouses.save(warehouse));
  }

  @GetMapping("/stock")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
  public List<Stock> stock() { return stocks.findAll(); }

  @PostMapping("/stock-in")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public Stock stockIn(@Valid @RequestBody StockChange change) {
    Stock stock = stocks.findByProductIdAndWarehouseId(change.productId, change.warehouseId)
        .orElseGet(() -> new Stock(change.productId, change.warehouseId));
    stock.available += change.quantity;
    return stocks.save(stock);
  }

  @PostMapping("/adjust")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public Stock adjust(@Valid @RequestBody StockChange change) {
    Stock stock = stocks.findByProductIdAndWarehouseId(change.productId, change.warehouseId)
        .orElseGet(() -> new Stock(change.productId, change.warehouseId));
    stock.available = change.quantity;
    return stocks.save(stock);
  }

  @PostMapping("/transfer")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public Map<String, Object> transfer(@Valid @RequestBody Transfer transfer) {
    Stock from = stocks.findByProductIdAndWarehouseId(transfer.productId, transfer.fromWarehouseId).orElseThrow();
    if (from.available < transfer.quantity) throw new IllegalStateException("Insufficient available stock");
    from.available -= transfer.quantity;
    Stock to = stocks.findByProductIdAndWarehouseId(transfer.productId, transfer.toWarehouseId)
        .orElseGet(() -> new Stock(transfer.productId, transfer.toWarehouseId));
    to.available += transfer.quantity;
    stocks.save(from);
    stocks.save(to);
    return Map.of("status", "transferred");
  }

  @PostMapping("/reserve")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public Map<String, Object> reserve(@Valid @RequestBody StockChange change) {
    Stock stock = stocks.findByProductIdAndWarehouseId(change.productId, change.warehouseId).orElseThrow();
    if (stock.available < change.quantity) throw new IllegalStateException("Insufficient available stock");
    stock.available -= change.quantity;
    stock.reserved += change.quantity;
    stocks.save(stock);
    return Map.of("reserved", change.quantity);
  }

  @PostMapping("/release")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public Map<String, Object> release(@Valid @RequestBody StockChange change) {
    Stock stock = stocks.findByProductIdAndWarehouseId(change.productId, change.warehouseId).orElseThrow();
    if (stock.reserved < change.quantity) throw new IllegalStateException("Insufficient reserved stock");
    stock.reserved -= change.quantity;
    stock.available += change.quantity;
    stocks.save(stock);
    return Map.of("released", change.quantity);
  }

  @PostMapping("/consume")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public Map<String, Object> consume(@Valid @RequestBody StockChange change) {
    Stock stock = stocks.findByProductIdAndWarehouseId(change.productId, change.warehouseId).orElseThrow();
    if (stock.reserved < change.quantity) throw new IllegalStateException("Insufficient reserved stock");
    stock.reserved -= change.quantity;
    stocks.save(stock);
    return Map.of("consumed", change.quantity);
  }

  @Entity
  @Table(name = "warehouses")
  public static class Warehouse {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @NotBlank public String name;
    @NotBlank public String location;
  }

  @Entity
  @Table(name = "stock")
  public static class Stock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "product_id") public Long productId;
    @Column(name = "warehouse_id") public Long warehouseId;
    @Min(0) public int available;
    @Min(0) public int reserved;
    public Stock() {}
    Stock(Long productId, Long warehouseId) { this.productId = productId; this.warehouseId = warehouseId; }
  }

  public record StockChange(@NotNull Long productId, @NotNull Long warehouseId, @Min(1) int quantity) {}
  public record Transfer(@NotNull Long productId, @NotNull Long fromWarehouseId, @NotNull Long toWarehouseId, @Min(1) int quantity) {}
}
