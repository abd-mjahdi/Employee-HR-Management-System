INSERT INTO departments (department_name, department_code) VALUES
                                                               ('Engineering', 'ENG'),
                                                               ('Sales', 'SALES'),
                                                               ('Marketing', 'MKT'),
                                                               ('Human Resources', 'HR'),
                                                               ('Finance', 'FIN');

INSERT INTO users (username, email, password_hash, first_name, last_name, user_role, department_id, manager_id) VALUES
                                                                                                                    ('admin', 'admin@company.com', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'Admin', 'User', 'HR_ADMIN', 4, NULL),
                                                                                                                    ('john.manager', 'john@company.com', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'John', 'Smith', 'MANAGER', 1, NULL),
                                                                                                                    ('sarah.manager', 'sarah@company.com', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'Sarah', 'Johnson', 'MANAGER', 2, NULL),
                                                                                                                    ('alice.dev', 'alice@company.com', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'Alice', 'Brown', 'EMPLOYEE', 1, 2),
                                                                                                                    ('bob.dev', 'bob@company.com', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'Bob', 'Wilson', 'EMPLOYEE', 1, 2),
                                                                                                                    ('charlie.sales', 'charlie@company.com', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'Charlie', 'Davis', 'EMPLOYEE', 2, 3),
                                                                                                                    ('diana.sales', 'diana@company.com', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'Diana', 'Martinez', 'EMPLOYEE', 2, 3),
                                                                                                                    ('eve.marketing', 'eve@company.com', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'Eve', 'Garcia', 'EMPLOYEE', 3, NULL);

INSERT INTO projects (project_name, project_code, description) VALUES
                                                                   ('Website Redesign', 'WEB-001', 'Company website redesign project'),
                                                                   ('Mobile App Development', 'APP-001', 'iOS and Android app development'),
                                                                   ('Marketing Campaign Q1', 'MKT-001', 'Q1 marketing campaign'),
                                                                   ('Sales Platform Upgrade', 'SALES-001', 'CRM system upgrade'),
                                                                   ('Internal Tools', 'INT-001', 'Internal productivity tools');

INSERT INTO leave_types (type_name, description) VALUES
                                                     ('Vacation', 'Annual vacation leave'),
                                                     ('Sick Leave', 'Medical and health-related leave'),
                                                     ('Personal Day', 'Personal time off'),
                                                     ('Parental Leave', 'Maternity and paternity leave'),
                                                     ('Bereavement Leave', 'Leave for family bereavement');

INSERT INTO leave_policies (leave_type_id, annual_allocation, accrual_method, allows_negative_balance, max_rollover_days, requires_manager_approval, requires_hr_approval, min_notice_days) VALUES
                                                                                                                                                                                                (1, 15.0, 'MONTHLY', FALSE, 5.0, TRUE, FALSE, 7),
                                                                                                                                                                                                (2, 10.0, 'ANNUAL', FALSE, 0.0, TRUE, FALSE, 0),
                                                                                                                                                                                                (3, 3.0, 'ANNUAL', FALSE, 0.0, TRUE, FALSE, 2),
                                                                                                                                                                                                (4, 60.0, 'ANNUAL', FALSE, 0.0, TRUE, TRUE, 30),
                                                                                                                                                                                                (5, 5.0, 'ANNUAL', FALSE, 0.0, TRUE, FALSE, 0);

INSERT INTO leave_balances (user_id, leave_type_id, year, current_balance, last_accrual_date) VALUES
                                                                                                  (4, 1, 2025, 12.5, '2025-02-01'),
                                                                                                  (4, 2, 2025, 8.0, '2025-01-01'),
                                                                                                  (4, 3, 2025, 3.0, '2025-01-01'),
                                                                                                  (5, 1, 2025, 15.0, '2025-02-01'),
                                                                                                  (5, 2, 2025, 10.0, '2025-01-01'),
                                                                                                  (5, 3, 2025, 2.0, '2025-01-01'),
                                                                                                  (6, 1, 2025, 10.0, '2025-02-01'),
                                                                                                  (6, 2, 2025, 6.0, '2025-01-01'),
                                                                                                  (7, 1, 2025, 14.0, '2025-02-01'),
                                                                                                  (7, 2, 2025, 9.0, '2025-01-01');

INSERT INTO time_entries (user_id, entry_date, clock_in_time, clock_out_time, total_hours, project_id, description, status, approved_by, approved_at) VALUES
                                                                                                                                                          (4, '2025-02-10', '09:00:00', '17:30:00', 8.5, 1, 'Worked on homepage redesign', 'APPROVED', 2, '2025-02-11 10:00:00'),
                                                                                                                                                          (4, '2025-02-11', '09:15:00', '17:00:00', 7.75, 1, 'Fixed responsive layout issues', 'APPROVED', 2, '2025-02-12 09:30:00'),
                                                                                                                                                          (4, '2025-02-12', '09:00:00', '18:00:00', 9.0, 2, 'Started mobile app authentication module', 'PENDING', NULL, NULL),
                                                                                                                                                          (5, '2025-02-10', '08:45:00', '17:00:00', 8.25, 2, 'Database schema design', 'APPROVED', 2, '2025-02-11 10:00:00'),
                                                                                                                                                          (5, '2025-02-11', '09:00:00', '17:30:00', 8.5, 2, 'API endpoint development', 'PENDING', NULL, NULL),
                                                                                                                                                          (6, '2025-02-10', '09:30:00', '18:00:00', 8.5, 3, 'Client meetings and proposals', 'APPROVED', 3, '2025-02-11 11:00:00'),
                                                                                                                                                          (7, '2025-02-10', '09:00:00', '17:00:00', 8.0, 3, 'Sales pitch preparation', 'APPROVED', 3, '2025-02-11 11:00:00');

INSERT INTO leave_requests (user_id, leave_type_id, start_date, end_date, total_days, reason, status, manager_approval_status, manager_approved_by, manager_approved_at, manager_notes) VALUES
                                                                                                                                                                                            (4, 1, '2025-03-10', '2025-03-14', 5.0, 'Family vacation', 'APPROVED', 'APPROVED', 2, '2025-02-15 14:00:00', 'Approved, have a good trip'),
                                                                                                                                                                                            (5, 2, '2025-02-18', '2025-02-18', 1.0, 'Medical appointment', 'PENDING', 'PENDING', NULL, NULL, NULL),
                                                                                                                                                                                            (6, 1, '2025-04-01', '2025-04-05', 5.0, 'Spring break trip', 'DENIED', 'DENIED', 3, '2025-02-16 10:00:00', 'Two other team members already on leave this week'),
                                                                                                                                                                                            (7, 3, '2025-02-20', '2025-02-20', 1.0, 'Personal matters', 'APPROVED', 'APPROVED', 3, '2025-02-17 09:00:00', 'Approved');

INSERT INTO audit_logs (user_id, action_type, table_name, record_id, old_values, new_values, ip_address) VALUES
                                                                                                             (2, 'UPDATE', 'time_entries', 1, '{"status": "PENDING"}', '{"status": "APPROVED", "approved_by": 2, "approved_at": "2025-02-11 10:00:00"}', '192.168.1.101'),
                                                                                                             (3, 'UPDATE', 'leave_requests', 3, '{"status": "PENDING", "manager_approval_status": "PENDING"}', '{"status": "DENIED", "manager_approval_status": "DENIED", "manager_approved_by": 3}', '192.168.1.102'),
                                                                                                             (1, 'CREATE', 'users', 8, NULL, '{"username": "eve.marketing", "email": "eve@company.com", "user_role": "EMPLOYEE"}', '192.168.1.100');