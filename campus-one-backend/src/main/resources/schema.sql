-- System tables
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(200) NOT NULL,
  real_name VARCHAR(50),
  avatar VARCHAR(500),
  email VARCHAR(100),
  phone VARCHAR(20),
  role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
  data_scope VARCHAR(20) DEFAULT 'SELF',
  college_id BIGINT,
  major_id BIGINT,
  class_id BIGINT,
  school_id BIGINT,
  status TINYINT DEFAULT 1,
  deleted TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(50) NOT NULL,
  role_key VARCHAR(50) NOT NULL UNIQUE,
  status TINYINT DEFAULT 1,
  deleted TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_permission (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  perm_name VARCHAR(100) NOT NULL,
  perm_key VARCHAR(100) NOT NULL UNIQUE,
  type VARCHAR(20) DEFAULT 'button',
  parent_id BIGINT DEFAULT 0,
  sort_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS sys_operation_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  module VARCHAR(50),
  action VARCHAR(50),
  method VARCHAR(10),
  url VARCHAR(200),
  ip VARCHAR(50),
  params TEXT,
  result TEXT,
  status INT,
  duration BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (user_id),
  INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

CREATE TABLE IF NOT EXISTS sys_notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  content TEXT,
  type VARCHAR(30),
  target_type VARCHAR(20) DEFAULT 'ALL',
  target_id BIGINT,
  sender_id BIGINT,
  is_read TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Per-user notification tracking (replaces single-row is_read)
CREATE TABLE IF NOT EXISTS user_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    is_read TINYINT(1) DEFAULT 0,
    read_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_notif_user (notification_id, user_id),
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_notification (notification_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户通知阅读记录';

-- Campus organization
CREATE TABLE IF NOT EXISTS campus_college (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(50) UNIQUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS campus_major (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(50),
  college_id BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS campus_class (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  major_id BIGINT,
  grade_year INT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS campus_building (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  campus VARCHAR(50),
  description VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Academic
CREATE TABLE IF NOT EXISTS academic_course (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(50),
  teacher_id BIGINT,
  college_id BIGINT,
  credit DECIMAL(3,1),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS academic_course_schedule (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  class_id BIGINT,
  day_of_week INT,
  start_time VARCHAR(10),
  end_time VARCHAR(10),
  room VARCHAR(100),
  week_range VARCHAR(50)
);

-- Service & Application
CREATE TABLE IF NOT EXISTS campus_service (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  category VARCHAR(50),
  target_roles VARCHAR(200),
  duration VARCHAR(50),
  approval_flow VARCHAR(200),
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS service_application (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  application_no VARCHAR(30) NOT NULL UNIQUE,
  service_id BIGINT NOT NULL,
  applicant_id BIGINT NOT NULL,
  form_data_json TEXT,
  status VARCHAR(20) DEFAULT 'DRAFT',
  process_instance_id VARCHAR(100),
  current_node VARCHAR(50),
  urge_count INT DEFAULT 0,
  last_urged_at DATETIME,
  submitted_at DATETIME,
  completed_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS application_approval_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  application_id BIGINT NOT NULL,
  task_id VARCHAR(100),
  node_name VARCHAR(50),
  assignee_id BIGINT,
  assignee_name VARCHAR(50),
  action VARCHAR(20),
  comment VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Reservation
CREATE TABLE IF NOT EXISTS campus_resource (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  resource_code VARCHAR(30) NOT NULL UNIQUE,
  resource_name VARCHAR(100) NOT NULL,
  resource_type VARCHAR(30),
  building_id BIGINT,
  room_number VARCHAR(50),
  capacity INT DEFAULT 0,
  description VARCHAR(500),
  equipment_json VARCHAR(500),
  status VARCHAR(20) DEFAULT 'AVAILABLE',
  need_approval TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resource_reservation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  reservation_no VARCHAR(30) NOT NULL UNIQUE,
  resource_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  reservation_date DATE NOT NULL,
  start_time VARCHAR(10) NOT NULL,
  end_time VARCHAR(10) NOT NULL,
  purpose VARCHAR(500),
  participant_count INT DEFAULT 1,
  status VARCHAR(20) DEFAULT 'PENDING',
  approval_instance_id VARCHAR(100),
  version INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Repair
CREATE TABLE IF NOT EXISTS repair_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  repair_no VARCHAR(30) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  location VARCHAR(200),
  category VARCHAR(50),
  description TEXT,
  priority VARCHAR(20) DEFAULT 'MEDIUM',
  status VARCHAR(20) DEFAULT 'SUBMITTED',
  assigned_user_id BIGINT,
  contact VARCHAR(50),
  available_time VARCHAR(100),
  ai_category VARCHAR(50),
  ai_priority VARCHAR(20),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  accepted_at DATETIME,
  resolved_at DATETIME,
  closed_at DATETIME
);

CREATE TABLE IF NOT EXISTS repair_image (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  repair_id BIGINT NOT NULL,
  image_url VARCHAR(500),
  image_key VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS repair_evaluation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  repair_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  rating INT,
  comment VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Activity
CREATE TABLE IF NOT EXISTS campus_activity (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  activity_code VARCHAR(30) NOT NULL UNIQUE,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  category VARCHAR(30),
  cover_image VARCHAR(500),
  location VARCHAR(200),
  start_time DATETIME,
  end_time DATETIME,
  registration_deadline DATETIME,
  capacity INT DEFAULT 0,
  registered_count INT DEFAULT 0,
  organizer VARCHAR(100),
  creator_id BIGINT,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS activity_registration (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  activity_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  registered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  checked_in TINYINT DEFAULT 0,
  checked_in_at DATETIME,
  UNIQUE KEY uk_activity_user (activity_id, user_id)
);

-- Knowledge
CREATE TABLE IF NOT EXISTS knowledge_document (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(200) NOT NULL,
  file_key VARCHAR(200),
  file_url VARCHAR(500),
  file_type VARCHAR(20),
  category VARCHAR(50),
  uploader_id BIGINT,
  status VARCHAR(20) DEFAULT 'PROCESSED',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS knowledge_chunk (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  content TEXT,
  chunk_index INT,
  embedding BLOB
);

-- AI
CREATE TABLE IF NOT EXISTS ai_conversation (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  title VARCHAR(200),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  content TEXT,
  tool_calls_json TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Token blacklist for revoked tokens
CREATE TABLE IF NOT EXISTS token_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(500) NOT NULL,
    user_id BIGINT,
    expires_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_token (token),
    INDEX idx_user_id (user_id),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Token黑名单';

-- ============================================================
-- Foreign Key Constraints (all validated against actual schema)
-- ============================================================

-- sys_user -> campus hierarchy
ALTER TABLE sys_user
  ADD CONSTRAINT fk_user_college  FOREIGN KEY (college_id) REFERENCES campus_college(id),
  ADD CONSTRAINT fk_user_major    FOREIGN KEY (major_id)   REFERENCES campus_major(id),
  ADD CONSTRAINT fk_user_class    FOREIGN KEY (class_id)   REFERENCES campus_class(id);

-- junction tables
ALTER TABLE sys_user_role
  ADD CONSTRAINT fk_ur_user  FOREIGN KEY (user_id) REFERENCES sys_user(id),
  ADD CONSTRAINT fk_ur_role  FOREIGN KEY (role_id) REFERENCES sys_role(id);

ALTER TABLE sys_role_permission
  ADD CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES sys_role(id),
  ADD CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id);

-- logs / notifications
ALTER TABLE sys_operation_log
  ADD CONSTRAINT fk_oplog_user FOREIGN KEY (user_id) REFERENCES sys_user(id);

ALTER TABLE sys_notification
  ADD CONSTRAINT fk_notif_sender FOREIGN KEY (sender_id) REFERENCES sys_user(id);

-- campus hierarchy
ALTER TABLE campus_major
  ADD CONSTRAINT fk_major_college FOREIGN KEY (college_id) REFERENCES campus_college(id);

ALTER TABLE campus_class
  ADD CONSTRAINT fk_class_major FOREIGN KEY (major_id) REFERENCES campus_major(id);

-- academic
ALTER TABLE academic_course
  ADD CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id),
  ADD CONSTRAINT fk_course_college FOREIGN KEY (college_id) REFERENCES campus_college(id);

ALTER TABLE academic_course_schedule
  ADD CONSTRAINT fk_sched_course FOREIGN KEY (course_id) REFERENCES academic_course(id),
  ADD CONSTRAINT fk_sched_class   FOREIGN KEY (class_id) REFERENCES campus_class(id);

-- service application
ALTER TABLE service_application
  ADD CONSTRAINT fk_app_service   FOREIGN KEY (service_id)   REFERENCES campus_service(id),
  ADD CONSTRAINT fk_app_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user(id);

ALTER TABLE application_approval_record
  ADD CONSTRAINT fk_appr_app     FOREIGN KEY (application_id) REFERENCES service_application(id),
  ADD CONSTRAINT fk_appr_assignee FOREIGN KEY (assignee_id)   REFERENCES sys_user(id);

-- reservation
ALTER TABLE campus_resource
  ADD CONSTRAINT fk_res_building FOREIGN KEY (building_id) REFERENCES campus_building(id);

ALTER TABLE resource_reservation
  ADD CONSTRAINT fk_rsrv_resource FOREIGN KEY (resource_id) REFERENCES campus_resource(id),
  ADD CONSTRAINT fk_rsrv_user     FOREIGN KEY (user_id)     REFERENCES sys_user(id);

-- repair
ALTER TABLE repair_order
  ADD CONSTRAINT fk_repair_user        FOREIGN KEY (user_id)         REFERENCES sys_user(id),
  ADD CONSTRAINT fk_repair_assigned    FOREIGN KEY (assigned_user_id) REFERENCES sys_user(id);

ALTER TABLE repair_image
  ADD CONSTRAINT fk_repairimg_repair FOREIGN KEY (repair_id) REFERENCES repair_order(id);

ALTER TABLE repair_evaluation
  ADD CONSTRAINT fk_repeval_repair FOREIGN KEY (repair_id) REFERENCES repair_order(id),
  ADD CONSTRAINT fk_repeval_user   FOREIGN KEY (user_id)   REFERENCES sys_user(id);

-- activity
ALTER TABLE campus_activity
  ADD CONSTRAINT fk_activity_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id);

ALTER TABLE activity_registration
  ADD CONSTRAINT fk_actreg_activity FOREIGN KEY (activity_id) REFERENCES campus_activity(id),
  ADD CONSTRAINT fk_actreg_user     FOREIGN KEY (user_id)     REFERENCES sys_user(id);

-- knowledge
ALTER TABLE knowledge_document
  ADD CONSTRAINT fk_kdoc_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user(id);

ALTER TABLE knowledge_chunk
  ADD CONSTRAINT fk_kchunk_doc FOREIGN KEY (document_id) REFERENCES knowledge_document(id);

-- AI
ALTER TABLE ai_conversation
  ADD CONSTRAINT fk_aignv_user FOREIGN KEY (user_id) REFERENCES sys_user(id);

ALTER TABLE ai_message
  ADD CONSTRAINT fk_aimsg_conv FOREIGN KEY (conversation_id) REFERENCES ai_conversation(id);

-- token blacklist
ALTER TABLE token_blacklist
  ADD CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES sys_user(id);

-- ============================================================
-- Indexes for common query patterns
-- ============================================================

-- sys_user: lookup by role, status, org membership
CREATE INDEX idx_user_role        ON sys_user(role);
CREATE INDEX idx_user_status      ON sys_user(status, deleted);
CREATE INDEX idx_user_college     ON sys_user(college_id);
CREATE INDEX idx_user_major       ON sys_user(major_id);
CREATE INDEX idx_user_class       ON sys_user(class_id);

-- sys_operation_log: query by user and time range
-- indexes defined inline in CREATE TABLE

-- sys_notification: query by sender, target, read status
CREATE INDEX idx_notif_sender     ON sys_notification(sender_id);
CREATE INDEX idx_notif_target     ON sys_notification(target_type, target_id);
CREATE INDEX idx_notif_read       ON sys_notification(is_read, created_at);

-- academic_course: lookup by teacher / college
CREATE INDEX idx_course_teacher   ON academic_course(teacher_id);
CREATE INDEX idx_course_college   ON academic_course(college_id);

-- academic_course_schedule: query by course and class
CREATE INDEX idx_sched_course     ON academic_course_schedule(course_id);
CREATE INDEX idx_sched_class      ON academic_course_schedule(class_id);

-- service_application: query by applicant, service, status
CREATE INDEX idx_app_applicant    ON service_application(applicant_id);
CREATE INDEX idx_app_service      ON service_application(service_id);
CREATE INDEX idx_app_status       ON service_application(status, created_at);

-- application_approval_record: query by application
CREATE INDEX idx_appr_app         ON application_approval_record(application_id);

-- campus_resource: lookup by building, type, status
CREATE INDEX idx_res_building     ON campus_resource(building_id);
CREATE INDEX idx_res_type         ON campus_resource(resource_type, status);

-- resource_reservation: query by user, resource, date range
CREATE INDEX idx_rsrv_user        ON resource_reservation(user_id);
CREATE INDEX idx_rsrv_resource    ON resource_reservation(resource_id);
CREATE INDEX idx_rsrv_date        ON resource_reservation(reservation_date, start_time);

-- repair_order: query by user, status, assigned person
CREATE INDEX idx_repair_user      ON repair_order(user_id);
CREATE INDEX idx_repair_status    ON repair_order(status, created_at);
CREATE INDEX idx_repair_assigned  ON repair_order(assigned_user_id);

-- repair_image / repair_evaluation
CREATE INDEX idx_repairimg_repair ON repair_image(repair_id);
CREATE INDEX idx_repeval_repair   ON repair_evaluation(repair_id);

-- campus_activity: query by status, category, creator
CREATE INDEX idx_activity_status  ON campus_activity(status);
CREATE INDEX idx_activity_creator ON campus_activity(creator_id);
CREATE INDEX idx_activity_cat     ON campus_activity(category, status);

-- activity_registration: query by activity, user
CREATE INDEX idx_actreg_activity  ON activity_registration(activity_id);
CREATE INDEX idx_actreg_user      ON activity_registration(user_id);

-- knowledge: query by category, uploader, document
CREATE INDEX idx_kdoc_category    ON knowledge_document(category, status);
CREATE INDEX idx_kdoc_uploader    ON knowledge_document(uploader_id);
CREATE INDEX idx_kchunk_doc       ON knowledge_chunk(document_id);

-- AI: query by user, conversation
CREATE INDEX idx_aignv_user       ON ai_conversation(user_id);
CREATE INDEX idx_aimsg_conv       ON ai_message(conversation_id);
