-- ============================================================
-- SEED DATA - HR / Time Tracking System
-- Password hash for all users: $2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W
-- Plaintext for that hash (dev/test only): password
-- Dates are relative to ~April 2026 for realistic usage
-- Company 1 slug=acme (existing org). Company 2 slug=globex (second tenant).
-- Row identities are not shared: globex depts 9–11, users/memberships 32–35,
-- leave types 8–14, projects 11–12, time entries 40–41, leave request 20.
-- ============================================================

-- ============================================================
-- 0. COMPANIES
-- ============================================================
INSERT INTO companies (id, name, slug, status) VALUES
    (1, 'Acme',   'acme',   'ACTIVE'),
    (2, 'Globex', 'globex', 'ACTIVE');

SELECT setval('companies_id_seq', 2);


-- ============================================================
-- 1. DEPARTMENTS
-- ============================================================
INSERT INTO departments (id, company_id, department_name, department_code, is_active) VALUES
                                                                              (1, 1, 'Engineering',        'ENG',  TRUE),
                                                                              (2, 1, 'Human Resources',    'HR',   TRUE),
                                                                              (3, 1, 'Marketing',          'MKT',  TRUE),
                                                                              (4, 1, 'Finance',            'FIN',  TRUE),
                                                                              (5, 1, 'Sales',              'SAL',  TRUE),
                                                                              (6, 1, 'Operations',         'OPS',  TRUE),
                                                                              (7, 1, 'Legal',              'LEG',  TRUE),
                                                                              (8, 1, 'Product',            'PRD',  TRUE),
                                                                              (9,  2, 'Engineering',        'ENG',  TRUE),
                                                                              (10, 2, 'Human Resources',    'HR',   TRUE),
                                                                              (11, 2, 'Sales',              'SAL',  TRUE);

-- Reset sequence
SELECT setval('departments_id_seq', 11);


-- ============================================================
-- 2. USERS (identity only; role/dept/manager live on company_memberships)
-- All passwords hash to the same bcrypt value provided.
-- Hierarchy is seeded in company_memberships (membership id matches user id).
-- ============================================================
INSERT INTO users (id, username, email, password_hash, first_name, last_name, is_active) VALUES

-- HR Admins
(1,  'alice.morgan',    'alice.morgan@company.com',    '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Alice',   'Morgan',     TRUE),
(2,  'brian.cole',      'brian.cole@company.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Brian',   'Cole',       TRUE),

-- Managers (one per dept; manager_id points to HR Admin 1)
(3,  'carol.knight',    'carol.knight@company.com',    '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Carol',   'Knight',     TRUE),
(4,  'david.shaw',      'david.shaw@company.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'David',   'Shaw',       TRUE),
(5,  'eva.stone',       'eva.stone@company.com',       '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Eva',     'Stone',      TRUE),
(6,  'frank.hayes',     'frank.hayes@company.com',     '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Frank',   'Hayes',      TRUE),
(7,  'grace.bell',      'grace.bell@company.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Grace',   'Bell',       TRUE),
(8,  'henry.ford',      'henry.ford@company.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Henry',   'Ford',       TRUE),

-- Engineering employees (manager = carol.knight, id=3)
(9,  'ivan.petrov',     'ivan.petrov@company.com',     '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Ivan',    'Petrov',     TRUE),
(10, 'julia.ross',      'julia.ross@company.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Julia',   'Ross',       TRUE),
(11, 'kevin.chang',     'kevin.chang@company.com',     '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Kevin',   'Chang',      TRUE),
(12, 'laura.white',     'laura.white@company.com',     '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Laura',   'White',      TRUE),

-- Marketing employees (manager = david.shaw, id=4)
(13, 'mark.jensen',     'mark.jensen@company.com',     '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Mark',    'Jensen',     TRUE),
(14, 'nina.brown',      'nina.brown@company.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Nina',    'Brown',      TRUE),
(15, 'oscar.diaz',      'oscar.diaz@company.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Oscar',   'Diaz',       TRUE),

-- Finance employees (manager = eva.stone, id=5)
(16, 'paula.king',      'paula.king@company.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Paula',   'King',       TRUE),
(17, 'quinn.lee',       'quinn.lee@company.com',       '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Quinn',   'Lee',        TRUE),
(18, 'rachel.scott',    'rachel.scott@company.com',    '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Rachel',  'Scott',      TRUE),

-- Sales employees (manager = frank.hayes, id=6)
(19, 'sam.turner',      'sam.turner@company.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Sam',     'Turner',     TRUE),
(20, 'tina.clark',      'tina.clark@company.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Tina',    'Clark',      TRUE),
(21, 'uma.patel',       'uma.patel@company.com',       '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Uma',     'Patel',      TRUE),
(22, 'victor.nguyen',   'victor.nguyen@company.com',   '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Victor',  'Nguyen',     TRUE),

-- Ops employees (manager = grace.bell, id=7)
(23, 'wendy.hall',      'wendy.hall@company.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Wendy',   'Hall',       TRUE),
(24, 'xander.adams',    'xander.adams@company.com',    '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Xander',  'Adams',      TRUE),
(25, 'yara.james',      'yara.james@company.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Yara',    'James',      TRUE),

-- Product employees (manager = henry.ford, id=8)
(26, 'zoe.wright',      'zoe.wright@company.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Zoe',     'Wright',     TRUE),
(27, 'adam.lewis',      'adam.lewis@company.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Adam',    'Lewis',      TRUE),
(28, 'bella.garcia',    'bella.garcia@company.com',    '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Bella',   'Garcia',     TRUE),

-- HR employees (managed by Alice, id=1)
(29, 'carl.robinson',   'carl.robinson@company.com',   '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Carl',    'Robinson',   TRUE),
(30, 'diana.miller',    'diana.miller@company.com',    '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Diana',   'Miller',     TRUE),

-- Inactive user (for testing is_active=false flows)
(31, 'ex.employee',     'ex.employee@company.com',     '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Ex',      'Employee',   FALSE),

-- Globex (company 2) — emails/usernames globally unique
(32, 'emma.frost',      'emma.frost@globex.com',      '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Emma',    'Frost',      TRUE),
(33, 'oliver.grant',    'oliver.grant@globex.com',    '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Oliver',  'Grant',      TRUE),
(34, 'priya.sharma',    'priya.sharma@globex.com',    '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Priya',   'Sharma',     TRUE),
(35, 'nate.brooks',     'nate.brooks@globex.com',     '$2a$10$ZjukDbYm4QPlvbcFHqpOMeeltGjCBNj7xBDj0n6rMSiCrBpuaFR1W', 'Nate',    'Brooks',     TRUE);

SELECT setval('users_id_seq', 35);



-- ============================================================
-- 2b. COMPANY MEMBERSHIPS
-- Membership id matches user id for seed simplicity.
-- ============================================================
INSERT INTO company_memberships (id, user_id, company_id, role, status, department_id, manager_membership_id) VALUES
(1,  1,  1, 'HR_ADMIN', 'ACTIVE',      2,  NULL),
(2,  2,  1, 'HR_ADMIN', 'ACTIVE',      2,  NULL),
(3,  3,  1, 'MANAGER',  'ACTIVE',      1,  1),
(4,  4,  1, 'MANAGER',  'ACTIVE',      3,  1),
(5,  5,  1, 'MANAGER',  'ACTIVE',      4,  1),
(6,  6,  1, 'MANAGER',  'ACTIVE',      5,  1),
(7,  7,  1, 'MANAGER',  'ACTIVE',      6,  2),
(8,  8,  1, 'MANAGER',  'ACTIVE',      8,  2),
(9,  9,  1, 'EMPLOYEE', 'ACTIVE',      1,  3),
(10, 10, 1, 'EMPLOYEE', 'ACTIVE',      1,  3),
(11, 11, 1, 'EMPLOYEE', 'ACTIVE',      1,  3),
(12, 12, 1, 'EMPLOYEE', 'ACTIVE',      1,  3),
(13, 13, 1, 'EMPLOYEE', 'ACTIVE',      3,  4),
(14, 14, 1, 'EMPLOYEE', 'ACTIVE',      3,  4),
(15, 15, 1, 'EMPLOYEE', 'ACTIVE',      3,  4),
(16, 16, 1, 'EMPLOYEE', 'ACTIVE',      4,  5),
(17, 17, 1, 'EMPLOYEE', 'ACTIVE',      4,  5),
(18, 18, 1, 'EMPLOYEE', 'ACTIVE',      4,  5),
(19, 19, 1, 'EMPLOYEE', 'ACTIVE',      5,  6),
(20, 20, 1, 'EMPLOYEE', 'ACTIVE',      5,  6),
(21, 21, 1, 'EMPLOYEE', 'ACTIVE',      5,  6),
(22, 22, 1, 'EMPLOYEE', 'ACTIVE',      5,  6),
(23, 23, 1, 'EMPLOYEE', 'ACTIVE',      6,  7),
(24, 24, 1, 'EMPLOYEE', 'ACTIVE',      6,  7),
(25, 25, 1, 'EMPLOYEE', 'ACTIVE',      6,  7),
(26, 26, 1, 'EMPLOYEE', 'ACTIVE',      8,  8),
(27, 27, 1, 'EMPLOYEE', 'ACTIVE',      8,  8),
(28, 28, 1, 'EMPLOYEE', 'ACTIVE',      8,  8),
(29, 29, 1, 'EMPLOYEE', 'ACTIVE',      2,  1),
(30, 30, 1, 'EMPLOYEE', 'ACTIVE',      2,  1),
(31, 31, 1, 'EMPLOYEE', 'DEACTIVATED', 1,  3),
(32, 32, 2, 'HR_ADMIN', 'ACTIVE',      10, NULL),
(33, 33, 2, 'MANAGER',  'ACTIVE',      9,  32),
(34, 34, 2, 'EMPLOYEE', 'ACTIVE',      9,  33),
(35, 35, 2, 'EMPLOYEE', 'ACTIVE',      9,  33);

SELECT setval('company_memberships_id_seq', 35);


-- ============================================================
-- 3. PROJECTS
-- ============================================================
INSERT INTO projects (id, company_id, project_name, project_code, description, is_active) VALUES
                                                                                  (1,  1, 'Platform Rebuild',        'PLAT-2026',  'Full rewrite of the core platform',                TRUE),
                                                                                  (2,  1, 'Mobile App v2',           'MOB-2026',   'Second generation mobile application',             TRUE),
                                                                                  (3,  1, 'Data Pipeline',           'DATA-2026',  'ETL pipeline for analytics warehouse',             TRUE),
                                                                                  (4,  1, 'Marketing Automation',    'MKTA-2026',  'Automated campaign management system',             TRUE),
                                                                                  (5,  1, 'Finance Reporting Suite', 'FINR-2026',  'Real-time financial dashboards',                   TRUE),
                                                                                  (6,  1, 'Sales CRM Integration',   'CRM-2026',   'Integration with Salesforce CRM',                  TRUE),
                                                                                  (7,  1, 'Internal Tooling',        'INTL-2026',  'Developer productivity tools',                     TRUE),
                                                                                  (8,  1, 'Customer Portal',         'CPRT-2026',  'Self-service portal for enterprise customers',     TRUE),
                                                                                  (9,  1, 'Legacy Migration',        'LEGM-2025',  'Migrate legacy systems to cloud infrastructure',   FALSE),
                                                                                  (10, 1, 'Security Hardening',      'SEC-2026',   'Pen-testing and remediation project',              TRUE),
                                                                                  (11, 2, 'Globex Core Platform',    'GLOB-2026',  'Globex internal platform',                        TRUE),
                                                                                  (12, 2, 'Globex Sales CRM',        'GCRM-2026',  'Globex CRM rollout',                              TRUE);

SELECT setval('projects_id_seq', 12);


-- ============================================================
-- 4. LEAVE TYPES
-- ============================================================
INSERT INTO leave_types (id, company_id, type_name, description, is_active) VALUES
                                                                    (1,  1, 'Annual Leave',       'Paid yearly vacation days',                        TRUE),
                                                                    (2,  1, 'Sick Leave',         'Paid leave for illness or medical appointments',   TRUE),
                                                                    (3,  1, 'Unpaid Leave',       'Leave without pay, subject to manager approval',   TRUE),
                                                                    (4,  1, 'Maternity Leave',    'Paid leave for birth/adoption of a child',         TRUE),
                                                                    (5,  1, 'Paternity Leave',    'Paid leave for new fathers',                       TRUE),
                                                                    (6,  1, 'Bereavement Leave',  'Leave for the death of a close family member',     TRUE),
                                                                    (7,  1, 'Study Leave',        'Leave for exams or certified study programs',      TRUE),
                                                                    (8,  2, 'Annual Leave',       'Paid yearly vacation days',                        TRUE),
                                                                    (9,  2, 'Sick Leave',         'Paid leave for illness or medical appointments',   TRUE),
                                                                    (10, 2, 'Unpaid Leave',       'Leave without pay, subject to manager approval',   TRUE),
                                                                    (11, 2, 'Maternity Leave',    'Paid leave for birth/adoption of a child',         TRUE),
                                                                    (12, 2, 'Paternity Leave',    'Paid leave for new fathers',                       TRUE),
                                                                    (13, 2, 'Bereavement Leave',  'Leave for the death of a close family member',     TRUE),
                                                                    (14, 2, 'Study Leave',        'Leave for exams or certified study programs',      TRUE);

SELECT setval('leave_types_id_seq', 14);


-- ============================================================
-- 5. LEAVE POLICIES
-- ============================================================
INSERT INTO leave_policies (id, company_id, leave_type_id, annual_allocation, accrual_method, allows_negative_balance, max_rollover_days, requires_manager_approval, requires_hr_approval, min_notice_days) VALUES
                                                                                                                                                                                                    (1,  1, 1, 21.0, 'MONTHLY', FALSE, 5.0,  TRUE,  FALSE, 7),
                                                                                                                                                                                                    (2,  1, 2, 10.0, 'ANNUAL',  FALSE, 0.0,  TRUE,  FALSE, 0),
                                                                                                                                                                                                    (3,  1, 3, 30.0, 'ANNUAL',  FALSE, 0.0,  TRUE,  TRUE,  14),
                                                                                                                                                                                                    (4,  1, 4, 90.0, 'ANNUAL',  FALSE, 0.0,  TRUE,  TRUE,  30),
                                                                                                                                                                                                    (5,  1, 5, 10.0, 'ANNUAL',  FALSE, 0.0,  TRUE,  TRUE,  14),
                                                                                                                                                                                                    (6,  1, 6, 5.0,  'ANNUAL',  FALSE, 0.0,  TRUE,  FALSE, 0),
                                                                                                                                                                                                    (7,  1, 7, 5.0,  'ANNUAL',  FALSE, 0.0,  TRUE,  TRUE,  30),
                                                                                                                                                                                                    (8,  2, 8, 21.0, 'MONTHLY', FALSE, 5.0,  TRUE,  FALSE, 7),
                                                                                                                                                                                                    (9,  2, 9, 10.0, 'ANNUAL',  FALSE, 0.0,  TRUE,  FALSE, 0),
                                                                                                                                                                                                    (10, 2, 10, 30.0, 'ANNUAL',  FALSE, 0.0,  TRUE,  TRUE,  14),
                                                                                                                                                                                                    (11, 2, 11, 90.0, 'ANNUAL',  FALSE, 0.0,  TRUE,  TRUE,  30),
                                                                                                                                                                                                    (12, 2, 12, 10.0, 'ANNUAL',  FALSE, 0.0,  TRUE,  TRUE,  14),
                                                                                                                                                                                                    (13, 2, 13, 5.0,  'ANNUAL',  FALSE, 0.0,  TRUE,  FALSE, 0),
                                                                                                                                                                                                    (14, 2, 14, 5.0,  'ANNUAL',  FALSE, 0.0,  TRUE,  TRUE,  30);

SELECT setval('leave_policies_id_seq', 14);


-- ============================================================
-- 6. LEAVE BALANCES  (year = 2026)
-- Each active employee gets balances for Annual + Sick leave.
-- A few employees also have Unpaid / Paternity / Study balances.
-- ============================================================
INSERT INTO leave_balances (company_id, user_id, leave_type_id, year, current_balance, last_accrual_date) VALUES

-- === Annual Leave (type 1) ===
-- Managers & HR Admins
(1, 1,  1, 2026, 14.0, '2026-04-01'),
(1, 2,  1, 2026, 14.0, '2026-04-01'),
(1, 3,  1, 2026, 14.0, '2026-04-01'),
(1, 4,  1, 2026, 14.0, '2026-04-01'),
(1, 5,  1, 2026, 14.0, '2026-04-01'),
(1, 6,  1, 2026, 14.0, '2026-04-01'),
(1, 7,  1, 2026, 14.0, '2026-04-01'),
(1, 8,  1, 2026, 14.0, '2026-04-01'),
-- Engineering
(1, 9,  1, 2026, 14.0, '2026-04-01'),
(1, 10, 1, 2026, 12.5, '2026-04-01'),
(1, 11, 1, 2026, 14.0, '2026-04-01'),
(1, 12, 1, 2026,  9.0, '2026-04-01'),
-- Marketing
(1, 13, 1, 2026, 14.0, '2026-04-01'),
(1, 14, 1, 2026, 11.0, '2026-04-01'),
(1, 15, 1, 2026, 14.0, '2026-04-01'),
-- Finance
(1, 16, 1, 2026, 14.0, '2026-04-01'),
(1, 17, 1, 2026, 13.0, '2026-04-01'),
(1, 18, 1, 2026, 14.0, '2026-04-01'),
-- Sales
(1, 19, 1, 2026, 14.0, '2026-04-01'),
(1, 20, 1, 2026,  7.0, '2026-04-01'),
(1, 21, 1, 2026, 14.0, '2026-04-01'),
(1, 22, 1, 2026, 14.0, '2026-04-01'),
-- Ops
(1, 23, 1, 2026, 14.0, '2026-04-01'),
(1, 24, 1, 2026, 14.0, '2026-04-01'),
(1, 25, 1, 2026, 12.0, '2026-04-01'),
-- Product
(1, 26, 1, 2026, 14.0, '2026-04-01'),
(1, 27, 1, 2026, 14.0, '2026-04-01'),
(1, 28, 1, 2026, 14.0, '2026-04-01'),
-- HR employees
(1, 29, 1, 2026, 14.0, '2026-04-01'),
(1, 30, 1, 2026, 14.0, '2026-04-01'),

-- === Sick Leave (type 2) ===
(1, 1,  2, 2026, 10.0, NULL),
(1, 2,  2, 2026, 10.0, NULL),
(1, 3,  2, 2026, 10.0, NULL),
(1, 4,  2, 2026, 10.0, NULL),
(1, 5,  2, 2026, 10.0, NULL),
(1, 6,  2, 2026, 10.0, NULL),
(1, 7,  2, 2026, 10.0, NULL),
(1, 8,  2, 2026, 10.0, NULL),
(1, 9,  2, 2026,  8.0, NULL),
(1, 10, 2, 2026, 10.0, NULL),
(1, 11, 2, 2026, 10.0, NULL),
(1, 12, 2, 2026,  7.0, NULL),
(1, 13, 2, 2026, 10.0, NULL),
(1, 14, 2, 2026,  9.0, NULL),
(1, 15, 2, 2026, 10.0, NULL),
(1, 16, 2, 2026, 10.0, NULL),
(1, 17, 2, 2026, 10.0, NULL),
(1, 18, 2, 2026, 10.0, NULL),
(1, 19, 2, 2026, 10.0, NULL),
(1, 20, 2, 2026,  5.0, NULL),
(1, 21, 2, 2026, 10.0, NULL),
(1, 22, 2, 2026, 10.0, NULL),
(1, 23, 2, 2026, 10.0, NULL),
(1, 24, 2, 2026, 10.0, NULL),
(1, 25, 2, 2026,  9.0, NULL),
(1, 26, 2, 2026, 10.0, NULL),
(1, 27, 2, 2026, 10.0, NULL),
(1, 28, 2, 2026, 10.0, NULL),
(1, 29, 2, 2026, 10.0, NULL),
(1, 30, 2, 2026, 10.0, NULL),

-- === Unpaid Leave (type 3) ===
(1, 9,  3, 2026, 30.0, NULL),
(1, 13, 3, 2026, 30.0, NULL),
(1, 19, 3, 2026, 30.0, NULL),

-- === Paternity Leave (type 5) — a few male employees ===
(1, 11, 5, 2026, 10.0, NULL),
(1, 22, 5, 2026, 10.0, NULL),
(1, 27, 5, 2026, 10.0, NULL),

-- === Study Leave (type 7) ===
(1, 10, 7, 2026, 5.0, NULL),
(1, 17, 7, 2026, 5.0, NULL),
(1, 26, 7, 2026, 5.0, NULL),

-- Globex balances (leave types 8=annual, 9=sick)
(2, 32, 8, 2026, 14.0, '2026-04-01'),
(2, 33, 8, 2026, 14.0, '2026-04-01'),
(2, 34, 8, 2026, 14.0, '2026-04-01'),
(2, 35, 8, 2026, 14.0, '2026-04-01'),
(2, 32, 9, 2026, 10.0, NULL),
(2, 33, 9, 2026, 10.0, NULL),
(2, 34, 9, 2026, 10.0, NULL),
(2, 35, 9, 2026, 10.0, NULL);


-- ============================================================
-- 7. TIME ENTRIES
-- Mix of APPROVED, PENDING, DENIED, CANCELLED entries
-- Spread across recent weeks in April 2026
-- approved_by references managers or hr_admins
-- ============================================================
INSERT INTO time_entries (id, company_id, user_id, entry_date, clock_in_time, clock_out_time, total_hours, project_id, description, status, approved_by, approved_at) VALUES

-- Ivan Petrov (id=9) — Engineering / Platform Rebuild
(1, 1, 9,  '2026-04-14', '09:00', '17:30', 8.50, 1, 'Backend API development',               'APPROVED', 3, '2026-04-15 10:00:00'),
(2, 1, 9,  '2026-04-15', '09:00', '18:00', 9.00, 1, 'Code review and unit tests',             'APPROVED', 3, '2026-04-16 09:00:00'),
(3, 1, 9,  '2026-04-16', '08:30', '17:00', 8.50, 7, 'Internal tooling - CI pipeline fixes',  'APPROVED', 3, '2026-04-17 08:30:00'),
(4, 1, 9,  '2026-04-17', '09:00', '17:30', 8.50, 1, 'Sprint planning + feature dev',          'PENDING',  NULL, NULL),
(5, 1, 9,  '2026-04-21', '09:00', '17:30', 8.50, 1, 'Database optimisation',                  'PENDING',  NULL, NULL),

-- Julia Ross (id=10) — Engineering / Mobile App
(6, 1, 10, '2026-04-14', '08:00', '16:30', 8.50, 2, 'UI component library setup',             'APPROVED', 3, '2026-04-15 11:00:00'),
(7, 1, 10, '2026-04-15', '08:00', '16:00', 8.00, 2, 'Navigation refactor',                    'APPROVED', 3, '2026-04-16 08:30:00'),
(8, 1, 10, '2026-04-16', '08:00', '17:00', 9.00, 2, 'Push notifications integration',         'PENDING',  NULL, NULL),
(9, 1, 10, '2026-04-17', '09:00', '13:00', 4.00, 7, 'Knowledge transfer session',             'APPROVED', 3, '2026-04-18 09:00:00'),

-- Kevin Chang (id=11) — Engineering / Data Pipeline
(10, 1, 11, '2026-04-14', '09:30', '18:30', 9.00, 3, 'Spark job development',                  'APPROVED', 3, '2026-04-15 09:30:00'),
(11, 1, 11, '2026-04-15', '09:00', '18:00', 9.00, 3, 'Pipeline testing and validation',        'APPROVED', 3, '2026-04-16 10:00:00'),
(12, 1, 11, '2026-04-17', '09:00', '17:30', 8.50, 3, 'Documentation update',                   'PENDING',  NULL, NULL),

-- Laura White (id=12) — Engineering / Security
(13, 1, 12, '2026-04-14', '08:00', '17:00', 9.00, 10,'Vulnerability scan analysis',            'APPROVED', 3, '2026-04-15 10:00:00'),
(14, 1, 12, '2026-04-16', '08:00', '12:00', 4.00, 10,'Firewall rule review',                   'DENIED',   3, '2026-04-17 09:00:00'),
(15, 1, 12, '2026-04-21', '09:00', '17:30', 8.50, 10,'Patch management tasks',                 'PENDING',  NULL, NULL),

-- Mark Jensen (id=13) — Marketing
(16, 1, 13, '2026-04-14', '09:00', '17:00', 8.00, 4, 'Campaign brief writing',                 'APPROVED', 4, '2026-04-15 09:00:00'),
(17, 1, 13, '2026-04-15', '09:00', '17:30', 8.50, 4, 'Email sequence design',                  'APPROVED', 4, '2026-04-16 09:30:00'),
(18, 1, 13, '2026-04-21', '09:00', '17:00', 8.00, 4, 'A/B test setup',                         'PENDING',  NULL, NULL),

-- Nina Brown (id=14) — Marketing
(19, 1, 14, '2026-04-14', '10:00', '18:00', 8.00, 4, 'Social media content calendar',          'APPROVED', 4, '2026-04-15 10:30:00'),
(20, 1, 14, '2026-04-15', '10:00', '18:30', 8.50, 4, 'Influencer outreach emails',             'CANCELLED', NULL, NULL),
(21, 1, 14, '2026-04-16', '09:00', '17:00', 8.00, 4, 'Analytics dashboard review',             'APPROVED', 4, '2026-04-17 10:00:00'),

-- Paula King (id=16) — Finance
(22, 1, 16, '2026-04-14', '08:30', '17:30', 9.00, 5, 'Q1 financial close tasks',               'APPROVED', 5, '2026-04-15 08:30:00'),
(23, 1, 16, '2026-04-15', '08:30', '17:00', 8.50, 5, 'Board report preparation',               'APPROVED', 5, '2026-04-16 08:30:00'),
(24, 1, 16, '2026-04-21', '08:30', '17:00', 8.50, 5, 'Budget variance analysis',               'PENDING',  NULL, NULL),

-- Sam Turner (id=19) — Sales
(25, 1, 19, '2026-04-14', '09:00', '17:30', 8.50, 6, 'CRM data clean-up',                      'APPROVED', 6, '2026-04-15 09:00:00'),
(26, 1, 19, '2026-04-15', '09:00', '18:00', 9.00, 6, 'Client demo preparation',                'APPROVED', 6, '2026-04-16 09:00:00'),
(27, 1, 19, '2026-04-17', '09:00', '17:30', 8.50, 6, 'Pipeline review with manager',           'PENDING',  NULL, NULL),

-- Tina Clark (id=20) — Sales
(28, 1, 20, '2026-04-14', '09:00', '17:00', 8.00, 6, 'Lead qualification calls',               'APPROVED', 6, '2026-04-15 09:30:00'),
(29, 1, 20, '2026-04-16', '09:00', '13:00', 4.00, 6, 'Proposal writing',                       'APPROVED', 6, '2026-04-17 09:00:00'),
(30, 1, 20, '2026-04-21', '09:00', '17:00', 8.00, 6, 'Contract negotiation support',           'PENDING',  NULL, NULL),

-- Wendy Hall (id=23) — Operations
(31, 1, 23, '2026-04-14', '08:00', '16:30', 8.50, 7, 'Process documentation',                  'APPROVED', 7, '2026-04-15 08:00:00'),
(32, 1, 23, '2026-04-15', '08:00', '17:00', 9.00, 7, 'Vendor onboarding calls',                'APPROVED', 7, '2026-04-16 08:30:00'),
(33, 1, 23, '2026-04-17', '08:00', '16:30', 8.50, 7, 'SLA review',                             'PENDING',  NULL, NULL),

-- Zoe Wright (id=26) — Product
(34, 1, 26, '2026-04-14', '09:00', '17:30', 8.50, 8, 'Roadmap prioritisation',                 'APPROVED', 8, '2026-04-15 09:00:00'),
(35, 1, 26, '2026-04-15', '09:00', '18:00', 9.00, 8, 'User story writing',                     'APPROVED', 8, '2026-04-16 09:00:00'),
(36, 1, 26, '2026-04-21', '09:00', '17:30', 8.50, 8, 'Sprint retrospective facilitation',      'PENDING',  NULL, NULL),

-- Carol Knight (id=3, manager) — Engineering
(37, 1, 3,  '2026-04-14', '09:00', '18:00', 9.00, 1, 'Sprint kick-off + 1:1s',                 'APPROVED', 1, '2026-04-15 09:00:00'),
(38, 1, 3,  '2026-04-15', '09:00', '17:30', 8.50, 1, 'Architecture review meeting',            'APPROVED', 1, '2026-04-16 09:00:00'),
(39, 1, 3,  '2026-04-21', '09:00', '18:00', 9.00, 1, 'Cross-team dependency planning',         'PENDING',  NULL, NULL),
(40, 2, 34, '2026-04-14', '09:00', '17:30', 8.50, 11, 'Globex platform work',                    'APPROVED', 33, '2026-04-15 10:00:00'),
(41, 2, 35, '2026-04-15', '09:00', '17:00', 8.00, 12, 'Globex CRM data import',                  'PENDING',  NULL, NULL);

SELECT setval('time_entries_id_seq', 41);


-- ============================================================
-- 8. TIME ENTRY BREAKS
-- Simulating lunch breaks for the time entries above.
-- Compliance rules:
-- 1. Break required if total_hours > 6.0
-- 2. Required break duration: at least 30 minutes
-- ============================================================
INSERT INTO time_entry_breaks (id, time_entry_id, break_start, break_end, is_unpaid) VALUES
-- Ivan Petrov (id=9)
(1,  1,  '12:30', '13:30', TRUE), -- 8.5h
(2,  2,  '13:00', '14:00', TRUE), -- 9.0h
(3,  3,  '12:00', '13:00', TRUE), -- 8.5h
(4,  4,  '12:30', '13:30', TRUE), -- 8.5h
(5,  5,  '12:30', '13:30', TRUE), -- 8.5h

-- Julia Ross (id=10)
(6,  6,  '12:00', '13:00', TRUE), -- 8.5h
(7,  7,  '12:00', '13:00', TRUE), -- 8.0h
(8,  8,  '12:00', '13:00', TRUE), -- 9.0h
-- Entry 9 is only 4.0 hours, no break required.

-- Kevin Chang (id=11)
(9,  10, '13:30', '14:30', TRUE), -- 9.0h
(10, 11, '13:00', '14:00', TRUE), -- 9.0h
(11, 12, '13:00', '14:00', TRUE), -- 8.5h

-- Laura White (id=12)
(12, 13, '12:00', '13:00', TRUE), -- 9.0h
-- Entry 14 is 4.0 hours, no break required.
(13, 15, '12:30', '13:30', TRUE), -- 8.5h

-- Mark Jensen (id=13)
(14, 16, '12:00', '13:00', TRUE), -- 8.0h
(15, 17, '12:30', '13:30', TRUE), -- 8.5h
(16, 18, '12:00', '13:00', TRUE), -- 8.0h

-- Nina Brown (id=14)
(17, 19, '13:00', '14:00', TRUE), -- 8.0h
-- Entry 20 is cancelled.
(18, 21, '13:00', '14:00', TRUE), -- 8.0h

-- Paula King (id=16)
(19, 22, '12:30', '13:30', TRUE), -- 9.0h
(20, 23, '12:30', '13:30', TRUE), -- 8.5h
(21, 24, '12:30', '13:30', TRUE), -- 8.5h

-- Sam Turner (id=19)
(22, 25, '12:30', '13:30', TRUE), -- 8.5h
(23, 26, '13:00', '14:00', TRUE), -- 9.0h
(24, 27, '12:30', '13:30', TRUE), -- 8.5h

-- Tina Clark (id=20)
(25, 28, '12:00', '13:00', TRUE), -- 8.0h
-- Entry 29 is 4.0 hours, no break required.
(26, 30, '12:00', '13:00', TRUE), -- 8.0h

-- Wendy Hall (id=23)
(27, 31, '12:00', '13:00', TRUE), -- 8.5h
(28, 32, '12:00', '13:00', TRUE), -- 9.0h
(29, 33, '12:00', '13:00', TRUE), -- 8.5h

-- Zoe Wright (id=26)
(30, 34, '12:30', '13:30', TRUE), -- 8.5h
(31, 35, '13:00', '14:00', TRUE), -- 9.0h
(32, 36, '12:30', '13:30', TRUE), -- 8.5h

-- Carol Knight (id=3)
(33, 37, '13:00', '14:00', TRUE), -- 9.0h
(34, 38, '12:30', '13:30', TRUE), -- 8.5h
(35, 39, '13:00', '14:00', TRUE), -- 9.0h
(36, 40, '12:30', '13:30', TRUE),
(37, 41, '12:00', '13:00', TRUE);

SELECT setval('time_entry_breaks_id_seq', 37);


-- ============================================================
-- 9. LEAVE REQUESTS
-- Dates: past (approved/denied), current week, future (pending)
-- total_days excludes weekends for realism
-- ============================================================
INSERT INTO leave_requests (
    id, company_id, user_id, leave_type_id, start_date, end_date, total_days, reason, cancellation_reason,
    status,
    manager_approval_status, manager_approved_by, manager_approved_at, manager_notes,
    hr_approval_status, hr_approved_by, hr_approved_at, hr_notes
) VALUES

-- ── APPROVED (past, fully processed) ──────────────────────

-- Ivan Petrov: 3-day annual leave in March (approved by manager only, no HR needed)
(1, 1, 9, 1,
 '2026-03-10', '2026-03-12', 3.0,
 'Personal vacation - family visit', NULL,
 'APPROVED',
 'APPROVED', 3, '2026-03-05 10:00:00', 'Approved. Enjoy!',
 'PENDING',  NULL, NULL, NULL),

-- Julia Ross: 1-day sick leave in March
(2, 1, 10, 2,
 '2026-03-18', '2026-03-18', 1.0,
 'Flu - doctor advised rest', NULL,
 'APPROVED',
 'APPROVED', 3, '2026-03-18 08:30:00', 'Get well soon.',
 'PENDING',  NULL, NULL, NULL),

-- Mark Jensen: 2-day annual leave in March
(3, 1, 13, 1,
 '2026-03-24', '2026-03-25', 2.0,
 'Long weekend trip', NULL,
 'APPROVED',
 'APPROVED', 4, '2026-03-20 11:00:00', NULL,
 'PENDING',  NULL, NULL, NULL),

-- Paula King: 1-day annual leave end of March
(4, 1, 16, 1,
 '2026-03-31', '2026-03-31', 1.0,
 'Bank appointment - could not schedule outside hours', NULL,
 'APPROVED',
 'APPROVED', 5, '2026-03-27 09:00:00', NULL,
 'PENDING',  NULL, NULL, NULL),

-- Tina Clark: 5-day annual leave in early April (just finished)
(5, 1, 20, 1,
 '2026-04-07', '2026-04-11', 5.0,
 'Holiday - booked months in advance', NULL,
 'APPROVED',
 'APPROVED', 6, '2026-03-25 10:00:00', 'Approved. Handover doc required.',
 'PENDING',  NULL, NULL, NULL),

-- Kevin Chang: unpaid leave fully approved (requires HR too)
(6, 1, 11, 3,
 '2026-03-02', '2026-03-06', 5.0,
 'Extended personal matter requiring unpaid leave', NULL,
 'APPROVED',
 'APPROVED', 3, '2026-02-20 09:00:00', 'Discussed and agreed.',
 'APPROVED', 1, '2026-02-21 14:00:00', 'Approved after review of policy compliance.'),

-- ── DENIED ────────────────────────────────────────────────

-- Oscar Diaz: annual leave denied (insufficient notice)
(7, 1, 15, 1,
 '2026-04-22', '2026-04-23', 2.0,
 'Short break', NULL,
 'DENIED',
 'DENIED', 4, '2026-04-18 09:00:00', 'Insufficient notice — minimum 7 days required.',
 'PENDING', NULL, NULL, NULL),

-- Rachel Scott: study leave denied at HR level
(8, 1, 18, 7,
 '2026-05-12', '2026-05-14', 3.0,
 'Professional accounting exam preparation', NULL,
 'DENIED',
 'APPROVED', 5, '2026-04-10 10:00:00', 'Support this — passing to HR.',
 'DENIED',  2, '2026-04-12 11:00:00', 'Study leave quota exhausted for this period.'),

-- ── CANCELLED ─────────────────────────────────────────────

-- Sam Turner: cancelled his own request
(9, 1, 19, 1,
 '2026-04-28', '2026-04-29', 2.0,
 'Personal plans - may change', 'Plans changed',
 'CANCELLED',
 'CANCELLED', NULL, NULL, NULL,
 'PENDING',  NULL, NULL, NULL),

-- ── PENDING — current/upcoming (manager not yet actioned) ─

-- Zoe Wright: annual leave next week
(10, 1, 26, 1,
 '2026-04-28', '2026-05-01', 4.0,
 'Spring holiday with family', NULL,
 'PENDING',
 'PENDING', NULL, NULL, NULL,
 'PENDING',  NULL, NULL, NULL),

-- Adam Lewis: 2-day annual leave in May
(11, 1, 27, 1,
 '2026-05-05', '2026-05-06', 2.0,
 'Personal appointment and rest day', NULL,
 'PENDING',
 'PENDING', NULL, NULL, NULL,
 'PENDING', NULL, NULL, NULL),

-- Wendy Hall: sick leave today
(12, 1, 23, 2,
 '2026-04-21', '2026-04-21', 1.0,
 'Migraine — unable to come in', NULL,
 'PENDING',
 'PENDING', NULL, NULL, NULL,
 'PENDING',  NULL, NULL, NULL),

-- Bella Garcia: annual leave in May (longer)
(13, 1, 28, 1,
 '2026-05-19', '2026-05-23', 5.0,
 'International travel — flights already booked', NULL,
 'PENDING',
 'PENDING', NULL, NULL, NULL,
 'PENDING',  NULL, NULL, NULL),

-- Diana Miller: annual leave in June
(14, 1, 30, 1,
 '2026-06-02', '2026-06-05', 4.0,
 'Family reunion', NULL,
 'PENDING',
 'PENDING', NULL, NULL, NULL,
 'PENDING',  NULL, NULL, NULL),

-- Quinn Lee: study leave pending full approval chain
(15, 1, 17, 7,
 '2026-06-09', '2026-06-11', 3.0,
 'CFA Level 1 exam prep week', NULL,
 'PENDING',
 'PENDING', NULL, NULL, NULL,
 'PENDING',  NULL, NULL, NULL),

-- Victor Nguyen: paternity leave (big notice, future date — requires HR)
(16, 1, 22, 5,
 '2026-07-01', '2026-07-14', 10.0,
 'Baby due end of June — paternity leave', NULL,
 'PENDING',
 'PENDING', NULL, NULL, NULL,
 'PENDING',  NULL, NULL, NULL),

-- ── APPROVED pending HR step (manager approved, HR pending) ─

-- Xander Adams: annual leave — manager approved, waiting HR (requires_hr_approval=false for annual, but let's use unpaid to show 2-step)
(17, 1, 24, 3,
 '2026-05-26', '2026-05-29', 4.0,
 'Extended travel — taking unpaid days', NULL,
 'PENDING',
 'APPROVED', 7, '2026-04-17 09:30:00', 'Approved at manager level, forwarding to HR.',
 'PENDING',  NULL, NULL, NULL),

-- Carl Robinson: maternity/paternity adjacent — study leave, manager approved, HR pending
(18, 1, 29, 7,
 '2026-06-23', '2026-06-25', 3.0,
 'SHRM certification exam', NULL,
 'PENDING',
 'APPROVED', 1, '2026-04-19 10:00:00', 'Strongly encouraged — good for the team.',
 'PENDING',  NULL, NULL, NULL),

-- ── CANCELLATION PENDING ──────────────────────────────────

-- Grace Allen: approved leave with pending cancellation request
(19, 1, 14, 1,
 '2026-05-15', '2026-05-18', 2.0,
 'Personal trip', 'Trip postponed by airline',
 'CANCELLATION_PENDING',
 'APPROVED', 4, '2026-04-10 09:00:00', 'Approved by manager',
 'PENDING',  NULL, NULL, NULL),

(20, 2, 34, 8,
 '2026-05-11', '2026-05-12', 2.0,
 'Globex team offsite travel', NULL,
 'PENDING',
 'PENDING', NULL, NULL, NULL,
 'PENDING',  NULL, NULL, NULL);

SELECT setval('leave_requests_id_seq', 20);


-- ============================================================
-- 10. AUDIT LOGS
-- Sample audit trail covering CREATE / UPDATE / DELETE actions
-- across users, leave_requests, time_entries
-- ============================================================
INSERT INTO audit_logs (company_id, user_id, action_type, table_name, record_id, old_values, new_values, ip_address, timestamp) VALUES

-- New user created
(1, 1,  'CREATE', 'users', 9,
 NULL,
 '{"username":"ivan.petrov","user_role":"EMPLOYEE","department_id":1}',
 '192.168.1.10', '2026-01-15 09:05:00'),

-- Leave request submitted
(1, 9,  'CREATE', 'leave_requests', 1,
 NULL,
 '{"user_id":9,"leave_type_id":1,"start_date":"2026-03-10","total_days":3,"status":"PENDING"}',
 '10.0.0.42', '2026-03-04 11:00:00'),

-- Manager approves leave request
(1, 3,  'UPDATE', 'leave_requests', 1,
 '{"status":"PENDING","manager_approval_status":"PENDING"}',
 '{"status":"APPROVED","manager_approval_status":"APPROVED","manager_approved_by":3}',
 '10.0.0.11', '2026-03-05 10:00:00'),

-- Time entry approved
(1, 3,  'UPDATE', 'time_entries', 1,
 '{"status":"PENDING"}',
 '{"status":"APPROVED","approved_by":3}',
 '10.0.0.11', '2026-04-15 10:00:00'),

-- Leave request denied (insufficient notice)
(1, 4,  'UPDATE', 'leave_requests', 7,
 '{"status":"PENDING","manager_approval_status":"PENDING"}',
 '{"status":"DENIED","manager_approval_status":"DENIED","manager_notes":"Insufficient notice."}',
 '10.0.0.22', '2026-04-18 09:00:00'),

-- User deactivated
(1, 1,  'UPDATE', 'users', 31,
 '{"is_active":true}',
 '{"is_active":false}',
 '192.168.1.10', '2026-02-01 14:00:00'),

-- HR denies study leave
(1, 2,  'UPDATE', 'leave_requests', 8,
 '{"hr_approval_status":"PENDING"}',
 '{"hr_approval_status":"DENIED","hr_approved_by":2,"hr_notes":"Study leave quota exhausted."}',
 '192.168.1.11', '2026-04-12 11:00:00'),

-- Project deactivated
(1, 1,  'UPDATE', 'projects', 9,
 '{"is_active":true}',
 '{"is_active":false}',
 '192.168.1.10', '2026-01-20 10:00:00'),

-- Leave request cancelled by employee
(1, 19, 'UPDATE', 'leave_requests', 9,
 '{"status":"PENDING"}',
 '{"status":"CANCELLED","manager_approval_status":"CANCELLED"}',
 '10.0.0.55', '2026-04-20 08:30:00'),

-- Time entry denied
(1, 3,  'UPDATE', 'time_entries', 14,
 '{"status":"PENDING"}',
 '{"status":"DENIED","approved_by":3}',
 '10.0.0.11', '2026-04-17 09:00:00'),

-- HR approves unpaid leave
(1, 1,  'UPDATE', 'leave_requests', 6,
 '{"hr_approval_status":"PENDING"}',
 '{"hr_approval_status":"APPROVED","hr_approved_by":1}',
 '192.168.1.10', '2026-02-21 14:00:00'),

(2, 32, 'CREATE', 'users', 34,
 NULL,
 '{"username":"priya.sharma","user_role":"EMPLOYEE"}',
 '10.1.0.10', '2026-01-20 09:00:00');