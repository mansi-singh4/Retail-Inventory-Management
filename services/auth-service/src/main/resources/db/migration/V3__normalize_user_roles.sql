ALTER TABLE auth_service.users
  DROP CONSTRAINT users_role_check;

UPDATE auth_service.users
SET role = CASE role
  WHEN 'INVENTORY_MANAGER' THEN 'MANAGER'
  WHEN 'SALES' THEN 'STAFF'
  WHEN 'VIEWER' THEN 'STAFF'
  ELSE role
END;

ALTER TABLE auth_service.users
  ALTER COLUMN role SET DEFAULT 'STAFF';

ALTER TABLE auth_service.users
  ADD CONSTRAINT users_role_check
  CHECK (role IN ('ADMIN', 'MANAGER', 'STAFF'));