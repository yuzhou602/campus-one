-- Repair databases created before role-permission relationships were seeded.
-- Resolve natural keys instead of assuming IDs, so customized existing databases
-- receive the same relationships safely.
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_key IN ('SUPER_ADMIN', 'ADMIN')
UNION ALL
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_key = 'TEACHER'
  AND p.perm_key IN ('reservation:view', 'reservation:create', 'repair:create',
                     'application:create', 'activity:create', 'activity:register', 'ai:use')
UNION ALL
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_key = 'COUNSELOR'
  AND p.perm_key IN ('reservation:view', 'reservation:create', 'reservation:approve',
                     'repair:create', 'application:approve', 'activity:create',
                     'activity:approve', 'activity:register', 'notice:create',
                     'notice:publish', 'ai:use')
UNION ALL
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_key = 'STUDENT'
  AND p.perm_key IN ('reservation:view', 'reservation:create', 'repair:create',
                     'application:create', 'activity:register', 'ai:use')
UNION ALL
SELECT r.id, p.id FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_key = 'SERVICE'
  AND p.perm_key IN ('repair:process', 'ai:use');
