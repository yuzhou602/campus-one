-- Preserve legacy orphan rows for audit/recovery before enforcing relationships.
CREATE TABLE IF NOT EXISTS migration_v4_user_notification_orphan (
  original_id BIGINT PRIMARY KEY,
  notification_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  is_read TINYINT(1),
  read_at DATETIME,
  created_at DATETIME,
  orphan_reason VARCHAR(50) NOT NULL,
  archived_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Rows archived by Flyway V4 before notification FK enforcement';

INSERT IGNORE INTO migration_v4_user_notification_orphan
  (original_id, notification_id, user_id, is_read, read_at, created_at, orphan_reason)
SELECT un.id, un.notification_id, un.user_id, un.is_read, un.read_at, un.created_at,
       CASE
         WHEN n.id IS NULL AND u.id IS NULL THEN 'MISSING_NOTIFICATION_AND_USER'
         WHEN n.id IS NULL THEN 'MISSING_NOTIFICATION'
         ELSE 'MISSING_USER'
       END
FROM user_notification un
LEFT JOIN sys_notification n ON n.id = un.notification_id
LEFT JOIN sys_user u ON u.id = un.user_id
WHERE n.id IS NULL OR u.id IS NULL;

DELETE un
FROM user_notification un
LEFT JOIN sys_notification n ON n.id = un.notification_id
LEFT JOIN sys_user u ON u.id = un.user_id
WHERE n.id IS NULL OR u.id IS NULL;

-- The canonical schema already contains these constraints for fresh databases.
-- Existing installations receive them here without failing if they are present.
SET @has_notification_fk = (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = 'user_notification'
    AND constraint_name = 'fk_user_notif_notification'
);
SET @add_notification_fk_sql = IF(
  @has_notification_fk = 0,
  'ALTER TABLE user_notification ADD CONSTRAINT fk_user_notif_notification FOREIGN KEY (notification_id) REFERENCES sys_notification(id) ON DELETE CASCADE',
  'SELECT 1'
);
PREPARE add_notification_fk_stmt FROM @add_notification_fk_sql;
EXECUTE add_notification_fk_stmt;
DEALLOCATE PREPARE add_notification_fk_stmt;

SET @has_notification_user_fk = (
  SELECT COUNT(*) FROM information_schema.table_constraints
  WHERE constraint_schema = DATABASE()
    AND table_name = 'user_notification'
    AND constraint_name = 'fk_user_notif_user'
);
SET @add_notification_user_fk_sql = IF(
  @has_notification_user_fk = 0,
  'ALTER TABLE user_notification ADD CONSTRAINT fk_user_notif_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE',
  'SELECT 1'
);
PREPARE add_notification_user_fk_stmt FROM @add_notification_user_fk_sql;
EXECUTE add_notification_user_fk_stmt;
DEALLOCATE PREPARE add_notification_user_fk_stmt;
