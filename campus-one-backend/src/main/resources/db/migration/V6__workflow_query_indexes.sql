-- Guard each index so an interrupted MySQL DDL migration can be retried safely.
SET @has_user_approval_scope_idx = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'sys_user'
    AND index_name = 'idx_user_approval_scope'
);
SET @add_user_approval_scope_idx_sql = IF(@has_user_approval_scope_idx = 0,
  'CREATE INDEX idx_user_approval_scope ON sys_user(role, status, data_scope, college_id, class_id, id)',
  'SELECT 1');
PREPARE add_user_approval_scope_idx_stmt FROM @add_user_approval_scope_idx_sql;
EXECUTE add_user_approval_scope_idx_stmt;
DEALLOCATE PREPARE add_user_approval_scope_idx_stmt;

SET @has_repair_assigned_created_idx = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'repair_order'
    AND index_name = 'idx_repair_assigned_created'
);
SET @add_repair_assigned_created_idx_sql = IF(@has_repair_assigned_created_idx = 0,
  'CREATE INDEX idx_repair_assigned_created ON repair_order(assigned_user_id, created_at, id)',
  'SELECT 1');
PREPARE add_repair_assigned_created_idx_stmt FROM @add_repair_assigned_created_idx_sql;
EXECUTE add_repair_assigned_created_idx_stmt;
DEALLOCATE PREPARE add_repair_assigned_created_idx_stmt;
