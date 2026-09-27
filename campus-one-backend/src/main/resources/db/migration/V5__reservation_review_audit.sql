-- Every DDL step is guarded because MySQL commits DDL independently. A deployment
-- interrupted between statements can therefore be retried safely by Flyway repair.
SET @has_approved_by = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'resource_reservation'
    AND column_name = 'approved_by'
);
SET @add_approved_by_sql = IF(@has_approved_by = 0,
  'ALTER TABLE resource_reservation ADD COLUMN approved_by BIGINT NULL AFTER approval_instance_id',
  'SELECT 1');
PREPARE add_approved_by_stmt FROM @add_approved_by_sql;
EXECUTE add_approved_by_stmt;
DEALLOCATE PREPARE add_approved_by_stmt;

SET @has_approval_remark = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'resource_reservation'
    AND column_name = 'approval_remark'
);
SET @add_approval_remark_sql = IF(@has_approval_remark = 0,
  'ALTER TABLE resource_reservation ADD COLUMN approval_remark VARCHAR(500) NULL AFTER approved_by',
  'SELECT 1');
PREPARE add_approval_remark_stmt FROM @add_approval_remark_sql;
EXECUTE add_approval_remark_stmt;
DEALLOCATE PREPARE add_approval_remark_stmt;

SET @has_approved_at = (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'resource_reservation'
    AND column_name = 'approved_at'
);
SET @add_approved_at_sql = IF(@has_approved_at = 0,
  'ALTER TABLE resource_reservation ADD COLUMN approved_at DATETIME NULL AFTER approval_remark',
  'SELECT 1');
PREPARE add_approved_at_stmt FROM @add_approved_at_sql;
EXECUTE add_approved_at_stmt;
DEALLOCATE PREPARE add_approved_at_stmt;

SET @has_rsrv_pending_idx = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'resource_reservation'
    AND index_name = 'idx_rsrv_pending'
);
SET @add_rsrv_pending_idx_sql = IF(@has_rsrv_pending_idx = 0,
  'CREATE INDEX idx_rsrv_pending ON resource_reservation(status, created_at)',
  'SELECT 1');
PREPARE add_rsrv_pending_idx_stmt FROM @add_rsrv_pending_idx_sql;
EXECUTE add_rsrv_pending_idx_stmt;
DEALLOCATE PREPARE add_rsrv_pending_idx_stmt;

SET @has_rsrv_approver_fk = (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE() AND table_name = 'resource_reservation'
    AND constraint_name = 'fk_rsrv_approver'
);
SET @add_rsrv_approver_fk_sql = IF(@has_rsrv_approver_fk = 0,
  'ALTER TABLE resource_reservation ADD CONSTRAINT fk_rsrv_approver FOREIGN KEY (approved_by) REFERENCES sys_user(id)',
  'SELECT 1');
PREPARE add_rsrv_approver_fk_stmt FROM @add_rsrv_approver_fk_sql;
EXECUTE add_rsrv_approver_fk_stmt;
DEALLOCATE PREPARE add_rsrv_approver_fk_stmt;
