-- Support the canonical per-user notification list without filesort growth.
SET @has_user_notification_created_idx = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'user_notification'
    AND index_name = 'idx_user_created'
);
SET @add_user_notification_created_idx_sql = IF(
  @has_user_notification_created_idx = 0,
  'CREATE INDEX idx_user_created ON user_notification(user_id, created_at, id)',
  'SELECT 1'
);
PREPARE add_user_notification_created_idx_stmt FROM @add_user_notification_created_idx_sql;
EXECUTE add_user_notification_created_idx_stmt;
DEALLOCATE PREPARE add_user_notification_created_idx_stmt;
