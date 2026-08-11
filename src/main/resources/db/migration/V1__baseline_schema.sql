-- Baseline schema: what Hibernate had generated, plus the unique keys on
-- users.user_name and users.email that were added by hand afterwards.
--
-- An existing database is marked as already at this version by
-- spring.flyway.baseline-on-migrate, so this file only ever runs against an empty
-- one. Every change from here on gets its own V2, V3, ... file, which is the whole
-- point: the schema now lives in the repository instead of in whichever database
-- happens to remember the statement that created it.
--
-- Foreign keys are disabled for the duration because the tables are emitted in
-- alphabetical order, so several reference tables that do not exist yet.

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `applications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `applied_at` datetime(6) DEFAULT NULL,
  `cover_note` text,
  `status` enum('APPLIED','REJECTED','SHORTLISTED') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `developer_id` bigint NOT NULL,
  `job_id` bigint NOT NULL,
  `mail_sent` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_application_developer_job` (`developer_id`,`job_id`),
  KEY `idx_application_status_mail_sent` (`status`,`mail_sent`),
  KEY `idx_application_job` (`job_id`),
  CONSTRAINT `FKq5tbm3pwte35xt2h333lgqtmh` FOREIGN KEY (`developer_id`) REFERENCES `developer_profiles` (`id`),
  CONSTRAINT `FKskjjnjxbf1ir89jh5ts9mh0p4` FOREIGN KEY (`job_id`) REFERENCES `job_postings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `developer_profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bio` text,
  `full_name` varchar(255) DEFAULT NULL,
  `linkedin_url` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `resume_url` varchar(255) DEFAULT NULL,
  `years_exp` int DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKo37urhm8hqo8je4ehmeyopwc6` (`user_id`),
  CONSTRAINT `FK84acli7jcbsxp7eu3lsms7xvn` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `developer_skills` (
  `developer_id` bigint NOT NULL,
  `skill_id` bigint NOT NULL,
  PRIMARY KEY (`developer_id`,`skill_id`),
  KEY `FKdiks1a1r12e9p48dju8aq3ywb` (`skill_id`),
  CONSTRAINT `FK4f2ofcf6yjiydhvo2dj98nnj4` FOREIGN KEY (`developer_id`) REFERENCES `developer_profiles` (`id`),
  CONSTRAINT `FKdiks1a1r12e9p48dju8aq3ywb` FOREIGN KEY (`skill_id`) REFERENCES `skills` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `job_postings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `description` text,
  `experience_required` int DEFAULT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `job_type` enum('HYBRID','ONSITE','REMOTE') DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `status` enum('ACTIVE','CLOSED') DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `recruiter_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_job_status` (`status`),
  KEY `idx_job_status_expires_at` (`status`,`expires_at`),
  KEY `idx_job_recruiter` (`recruiter_id`),
  CONSTRAINT `FK5r85dxu1luqfnk15vps88mv3o` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter_profiles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `job_required_skills` (
  `job_id` bigint NOT NULL,
  `skill_id` bigint NOT NULL,
  PRIMARY KEY (`job_id`,`skill_id`),
  KEY `FKr2qb5pubg2bivgp65p9wdyy6e` (`skill_id`),
  CONSTRAINT `FKbq8fe72fxjwq3gyrsqkmaicp4` FOREIGN KEY (`job_id`) REFERENCES `job_postings` (`id`),
  CONSTRAINT `FKr2qb5pubg2bivgp65p9wdyy6e` FOREIGN KEY (`skill_id`) REFERENCES `skills` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `recruiter_profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_desc` text,
  `company_name` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKgju0uv9tit5jywakidv5cgunk` (`user_id`),
  CONSTRAINT `FKptvsvv96hvn5sjrauymiew6gy` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `skills` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK85woe63nu9klkk9fa73vf0jd0` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `role` json DEFAULT NULL,
  `user_name` varchar(255) DEFAULT NULL,
  `email_preference` enum('PRIMARY','SECONDARY') DEFAULT NULL,
  `secondary_email` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_user_name` (`user_name`),
  UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
