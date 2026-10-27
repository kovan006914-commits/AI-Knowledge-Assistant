-- Select/create an isolated database first. Never drops existing data.
-- For existing deployments, retain registration migrations and check user.password >=60,
-- username unique, nullable email VARCHAR(254) unique before running.
CREATE TABLE IF NOT EXISTS user (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 username VARCHAR(50) NOT NULL,
 password VARCHAR(255) NOT NULL,
 email VARCHAR(254) DEFAULT NULL,
 create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_user_username (username),
 UNIQUE KEY uk_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS knowledge_item (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 owner_id BIGINT NOT NULL,
 title VARCHAR(120) NOT NULL,
 content TEXT NOT NULL,
 tags VARCHAR(200) NOT NULL DEFAULT '',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_knowledge_owner_updated (owner_id, updated_at),
 CONSTRAINT fk_knowledge_owner FOREIGN KEY (owner_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
