-- Remove duplicate revoked-token rows before enforcing one-time refresh-token use.
DELETE newer
FROM token_blacklist newer
JOIN token_blacklist older
  ON newer.token = older.token
 AND newer.id > older.id;

-- Replace the legacy prefix-only index with a full-token unique index.
SET @has_uk_token = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'token_blacklist'
    AND index_name = 'uk_token'
);
SET @drop_uk_token_sql = IF(
  @has_uk_token > 0,
  'ALTER TABLE token_blacklist DROP INDEX uk_token',
  'SELECT 1'
);
PREPARE drop_uk_token_stmt FROM @drop_uk_token_sql;
EXECUTE drop_uk_token_stmt;
DEALLOCATE PREPARE drop_uk_token_stmt;

ALTER TABLE token_blacklist ADD UNIQUE KEY uk_token (token);

-- The old non-unique prefix index is redundant once the full unique index exists.
SET @has_idx_token = (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'token_blacklist'
    AND index_name = 'idx_token'
);
SET @drop_idx_token_sql = IF(
  @has_idx_token > 0,
  'ALTER TABLE token_blacklist DROP INDEX idx_token',
  'SELECT 1'
);
PREPARE drop_idx_token_stmt FROM @drop_idx_token_sql;
EXECUTE drop_idx_token_stmt;
DEALLOCATE PREPARE drop_idx_token_stmt;
