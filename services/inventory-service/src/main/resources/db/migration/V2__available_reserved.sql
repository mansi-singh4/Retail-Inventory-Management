ALTER TABLE inventory_service.stock ADD COLUMN IF NOT EXISTS available INTEGER NOT NULL DEFAULT 0;
ALTER TABLE inventory_service.stock ADD COLUMN IF NOT EXISTS reserved INTEGER NOT NULL DEFAULT 0;
UPDATE inventory_service.stock SET available = quantity WHERE available = 0 AND quantity > 0;
ALTER TABLE inventory_service.stock DROP COLUMN IF EXISTS quantity;