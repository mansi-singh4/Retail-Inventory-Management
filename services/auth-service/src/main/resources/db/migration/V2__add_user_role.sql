ALTER TABLE auth_service.users
  ADD COLUMN role VARCHAR(32) NOT NULL DEFAULT 'VIEWER';

ALTER TABLE auth_service.users
  ADD CONSTRAINT users_role_check
  CHECK (role IN ('ADMIN', 'INVENTORY_MANAGER', 'SALES', 'VIEWER'));
