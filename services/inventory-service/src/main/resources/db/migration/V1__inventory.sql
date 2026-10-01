CREATE SCHEMA IF NOT EXISTS inventory_service;
CREATE TABLE IF NOT EXISTS inventory_service.warehouses (id BIGSERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL, location VARCHAR(255) NOT NULL);
CREATE TABLE IF NOT EXISTS inventory_service.stock (id BIGSERIAL PRIMARY KEY, product_id BIGINT NOT NULL, warehouse_id BIGINT NOT NULL, quantity INTEGER NOT NULL DEFAULT 0, UNIQUE(product_id, warehouse_id));
