-- Roles
INSERT IGNORE INTO sys_role (id, role_name, role_key) VALUES
(1, '超级管理员', 'SUPER_ADMIN'),
(2, '管理员', 'ADMIN'),
(3, '教师', 'TEACHER'),
(4, '辅导员', 'COUNSELOR'),
(5, '学生', 'STUDENT'),
(6, '服务人员', 'SERVICE');

-- Permissions
INSERT IGNORE INTO sys_permission (id, perm_name, perm_key, type) VALUES
(1, '场地预约查看', 'reservation:view', 'button'),
(2, '场地预约创建', 'reservation:create', 'button'),
(3, '场地预约审批', 'reservation:approve', 'button'),
(4, '报修创建', 'repair:create', 'button'),
(5, '报修分配', 'repair:assign', 'button'),
(6, '报修处理', 'repair:process', 'button'),
(7, '申请创建', 'application:create', 'button'),
(8, '申请审批', 'application:approve', 'button'),
(9, '活动创建', 'activity:create', 'button'),
(10, '活动审批', 'activity:approve', 'button'),
(11, '活动报名', 'activity:register', 'button'),
(12, '通知创建', 'notice:create', 'button'),
(13, '通知发布', 'notice:publish', 'button'),
(14, 'AI使用', 'ai:use', 'button');

-- Role permissions. SUPER_ADMIN is represented by a wildcard in the API;
-- the rows below keep database permission checks consistent for every role.
INSERT IGNORE INTO sys_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7),
(1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14),
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7),
(2, 8), (2, 9), (2, 10), (2, 11), (2, 12), (2, 13), (2, 14),
(3, 1), (3, 2), (3, 4), (3, 7), (3, 9), (3, 11), (3, 14),
(4, 1), (4, 2), (4, 3), (4, 4), (4, 8), (4, 9), (4, 10),
(4, 11), (4, 12), (4, 13), (4, 14),
(5, 1), (5, 2), (5, 4), (5, 7), (5, 11), (5, 14),
(6, 6), (6, 14);

-- Admin user (password: admin123)
INSERT IGNORE INTO sys_user (id, username, password, real_name, role, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'SUPER_ADMIN', 1),
(2, 'teacher01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王教授', 'TEACHER', 1),
(3, 'counselor01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李辅导员', 'COUNSELOR', 1),
(4, 'student01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张同学', 'STUDENT', 1),
(5, 'student02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李同学', 'STUDENT', 1),
(6, 'service01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李师傅', 'SERVICE', 1);

-- Colleges
INSERT IGNORE INTO campus_college (id, name, code) VALUES
(1, '计算机科学与技术学院', 'CS'),
(2, '电子信息工程学院', 'EIE'),
(3, '经济管理学院', 'EM');

-- Buildings
INSERT IGNORE INTO campus_building (id, name, campus) VALUES
(1, '信息楼', '主校区'),
(2, '教学楼A', '主校区'),
(3, '教学楼B', '主校区'),
(4, '实验楼', '主校区'),
(5, '行政楼', '主校区');

-- Resources
INSERT IGNORE INTO campus_resource (id, resource_code, resource_name, resource_type, building_id, room_number, capacity, equipment_json, need_approval) VALUES
(1, 'RES001', '软件实验室 305', 'lab', 1, '3F-305', 45, '电脑×45,投影仪,空调', 0),
(2, 'RES002', 'AI 实验室 402', 'lab', 1, '4F-402', 30, '电脑×30,投影仪', 0),
(3, 'RES003', '多媒体教室 A201', 'classroom', 2, '2F-A201', 120, '投影仪,音响系统', 1),
(4, 'RES004', '自习室 B102', 'study', 3, '1F-B102', 60, '空调,照明', 0),
(5, 'RES005', '会议室 301', 'meeting', 5, '3F-301', 20, '投影仪,视频会议系统', 1);

-- Services
INSERT IGNORE INTO campus_service (id, name, description, category, target_roles, duration, approval_flow) VALUES
(1, '请假申请', '因病、因事需要请假的学生可在此提交申请', 'leave', 'STUDENT', '1个工作日', '班主任→辅导员'),
(2, '学生证明申请', '在读证明、成绩证明、学籍证明等', 'certificate', 'STUDENT', '2个工作日', '辅导员审批'),
(3, '场地特殊使用申请', '教室、实验室等场地的特殊使用申请', 'venue', 'STUDENT,TEACHER', '3个工作日', '管理员审批');

-- Activities
INSERT IGNORE INTO campus_activity (id, activity_code, title, description, category, location, start_time, end_time, registration_deadline, capacity, registered_count, organizer, creator_id) VALUES
(1, 'AC202609050001', 'AI与未来软件开发讲座', '邀请业界知名AI专家分享前沿技术', 'lecture', '大学生活活动中心', '2026-09-12 14:00:00', '2026-09-12 16:00:00', '2026-09-11 23:59:59', 200, 186, '计算机学院', 2),
(2, 'AC202609050002', '新生编程马拉松', '面向新生的编程竞赛活动', 'competition', '创新实验室', '2026-09-15 09:00:00', '2026-09-15 18:00:00', '2026-09-14 23:59:59', 60, 42, '计算机学院', 2);
