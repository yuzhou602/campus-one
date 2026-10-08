-- Align the persisted catalog with the service definitions exposed by the API.
-- IDs are stable because service_application.service_id references them.
INSERT INTO campus_service
  (id, name, description, category, target_roles, duration, approval_flow, status)
VALUES
  (1, '请假申请', '课程、实习与日常请假在线登记', 'leave', 'STUDENT', '2个工作日', '教师→管理员', 1),
  (2, '学生证明申请', '在读证明、成绩证明等材料申请', 'certificate', 'STUDENT', '1个工作日', '职工→管理员', 1),
  (3, '场地特殊使用', '常规预约时段以外的场地使用申请', 'venue', 'STUDENT,TEACHER', '3个工作日', '职工→管理员', 1),
  (4, '活动场地申请', '社团和班级活动场地备案', 'activity_venue', 'STUDENT,TEACHER', '3个工作日', '职工→管理员', 1),
  (5, '物品借用申请', '公共器材和活动物资借用', 'asset', 'STUDENT,TEACHER', '1个工作日', '职工→管理员', 1),
  (6, '宿舍事务申请', '调宿、晚归等宿舍事务登记', 'dormitory', 'STUDENT', '2个工作日', '职工→管理员', 1)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  category = VALUES(category),
  target_roles = VALUES(target_roles),
  duration = VALUES(duration),
  approval_flow = VALUES(approval_flow),
  status = VALUES(status);
