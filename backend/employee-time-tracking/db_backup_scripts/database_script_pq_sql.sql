CREATE TABLE departments (
                             id                  BIGSERIAL PRIMARY KEY,
                             department_name     VARCHAR(50) NOT NULL UNIQUE,
                             department_code     VARCHAR(50) NOT NULL UNIQUE,
                             is_active           BOOLEAN NOT NULL DEFAULT TRUE,
                             created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
                       id              BIGSERIAL PRIMARY KEY,
                       username        VARCHAR(50) NOT NULL UNIQUE,
                       email           VARCHAR(255) NOT NULL UNIQUE,
                       password_hash   TEXT NOT NULL,
                       first_name      VARCHAR(50) NOT NULL,
                       last_name       VARCHAR(50) NOT NULL,
                       user_role       TEXT NOT NULL CHECK (user_role IN ('EMPLOYEE', 'MANAGER', 'HR_ADMIN')),
                       department_id   BIGINT NOT NULL REFERENCES departments(id),
                       manager_id      BIGINT REFERENCES users(id),
                       is_active       BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE projects (
                          id              BIGSERIAL PRIMARY KEY,
                          project_name    VARCHAR(50) NOT NULL,
                          project_code    VARCHAR(50) NOT NULL UNIQUE,
                          description     TEXT,
                          is_active       BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE time_entries (
                              id              BIGSERIAL PRIMARY KEY,
                              user_id         BIGINT NOT NULL REFERENCES users(id),
                              entry_date      DATE NOT NULL,
                              clock_in_time   TIME NOT NULL,
                              clock_out_time  TIME NOT NULL,
                              total_hours     NUMERIC(5,2) NOT NULL,
                              project_id      BIGINT NOT NULL REFERENCES projects(id),
                              description     TEXT,
                              status          TEXT NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'DENIED' ,'CANCELLED')),
                              approved_by     BIGINT REFERENCES users(id),
                              approved_at     TIMESTAMP,
                              created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leave_types (
                             id              BIGSERIAL PRIMARY KEY,
                             type_name       VARCHAR(50) NOT NULL UNIQUE,
                             description     TEXT,
                             is_active       BOOLEAN NOT NULL DEFAULT TRUE,
                             created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leave_policies (
                                id                          BIGSERIAL PRIMARY KEY,
                                leave_type_id               BIGINT NOT NULL UNIQUE REFERENCES leave_types(id),
                                annual_allocation           NUMERIC(4,1) NOT NULL,
                                accrual_method              VARCHAR(20) NOT NULL CHECK (accrual_method IN ('MONTHLY', 'ANNUAL')),
                                allows_negative_balance     BOOLEAN NOT NULL DEFAULT FALSE,
                                max_rollover_days           NUMERIC(4,1) NOT NULL DEFAULT 0,
                                requires_manager_approval   BOOLEAN NOT NULL DEFAULT TRUE,
                                requires_hr_approval        BOOLEAN NOT NULL DEFAULT FALSE,
                                min_notice_days             INT NOT NULL DEFAULT 0,
                                created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leave_balances (
                                id                  BIGSERIAL PRIMARY KEY,
                                user_id             BIGINT NOT NULL REFERENCES users(id),
                                leave_type_id       BIGINT NOT NULL REFERENCES leave_types(id),
                                year                SMALLINT NOT NULL,
                                current_balance     NUMERIC(5,1) NOT NULL DEFAULT 0,
                                last_accrual_date   DATE,
                                created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                UNIQUE (user_id, leave_type_id, year)
);

CREATE TABLE leave_requests (
                                id                      BIGSERIAL PRIMARY KEY,
                                user_id                 BIGINT NOT NULL REFERENCES users(id),
                                leave_type_id           BIGINT NOT NULL REFERENCES leave_types(id),
                                start_date              DATE NOT NULL,
                                end_date                DATE NOT NULL,
                                total_days              NUMERIC(4,1) NOT NULL,
                                reason                  TEXT NOT NULL,
                                status                  TEXT NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'DENIED', 'CANCELLED')),
                                manager_approval_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (manager_approval_status IN ('PENDING', 'APPROVED', 'DENIED' ,'CANCELLED')),
                                manager_approved_by     BIGINT REFERENCES users(id),
                                manager_approved_at     TIMESTAMP,
                                manager_notes           TEXT,
                                hr_approval_status      TEXT NOT NULL DEFAULT 'PENDING' CHECK (hr_approval_status IN ('PENDING', 'APPROVED', 'DENIED')),
                                hr_approved_by          BIGINT REFERENCES users(id),
                                hr_approved_at          TIMESTAMP,
                                hr_notes                TEXT,
                                created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
                            id          BIGSERIAL PRIMARY KEY,
                            user_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
                            action_type VARCHAR(10) NOT NULL CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE')),
                            table_name  VARCHAR(50) NOT NULL,
                            record_id   BIGINT NOT NULL,
                            old_values  JSONB,
                            new_values  JSONB,
                            ip_address  VARCHAR(45),
                            timestamp   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);