package com.retailinventory.inventory;
import java.util.Optional; import org.springframework.data.jpa.repository.JpaRepository;
interface StockRepository extends JpaRepository<InventoryController.Stock,Long>{Optional<InventoryController.Stock> findByProductIdAndWarehouseId(Long productId,Long warehouseId);}
interface WarehouseRepository extends JpaRepository<InventoryController.Warehouse,Long>{}
