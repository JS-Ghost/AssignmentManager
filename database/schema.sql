-- =====================================================================
-- Database Schema for Assignment Manager
-- Compatible with MySQL 5.7+, MySQL 8.0+, MariaDB 10+
-- =====================================================================

CREATE DATABASE IF NOT EXISTS assignment_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE assignment_db;

-- -----------------------------------------------------
-- Table: assignments
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS assignments (
    id INT PRIMARY KEY,
    course_code VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Pending',
    due_date VARCHAR(50) DEFAULT '',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Initial Seed Data
-- -----------------------------------------------------
INSERT INTO assignments (id, course_code, title, status, due_date) VALUES
(1, 'CSE110', 'Programming Language I - Lab Report 01', 'Completed', '2026-12-01'),
(2, 'CSE220', 'Data Structures - Linked List & Stack Implementation', 'Pending', '2026-12-05'),
(3, 'MAT120', 'Calculus and Linear Algebra - Problem Set 3', 'Pending', '2026-12-10'),
(4, 'ENG101', 'English Composition - Term Paper Outline', 'Completed', '2026-12-15'),
(5, 'PHY111', 'Principles of Physics - Harmonic Motion Experiment', 'Pending', '2026-12-20'),
(6, 'CSE321', 'Operating Systems - Process Scheduling Simulation', 'Pending', '2026-12-28')
ON DUPLICATE KEY UPDATE 
    course_code = VALUES(course_code),
    title = VALUES(title),
    status = VALUES(status),
    due_date = VALUES(due_date);
