CREATE TABLE departments (
                             id                  SERIAL PRIMARY KEY,
                             department_name     VARCHAR(50) NOT NULL,
                             department_code     VARCHAR(50) NOT NULL UNIQUE,
                             is_active           BOOLEAN NOT NULL DEFAULT TRUE,
                             created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
                       id              SERIAL PRIMARY KEY,
                       username        VARCHAR(50) NOT NULL UNIQUE,
                       email           VARCHAR(255) NOT NULL UNIQUE,
                       password_hash   TEXT NOT NULL,
                       first_name      VARCHAR(50) NOT NULL,
                       last_name       VARCHAR(50) NOT NULL,
                       user_role       TEXT NOT NULL CHECK (user_role IN ('employee', 'manager', 'hr_admin')),
                       department_id   INT NOT NULL REFERENCES departments(id),
                       manager_id      INT REFERENCES users(id),
                       is_active       BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE projects (
                          id              SERIAL PRIMARY KEY,
                          project_name    VARCHAR(50) NOT NULL,
                          project_code    VARCHAR(50) NOT NULL UNIQUE,
                          description     TEXT,
                          is_active       BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE time_entries (
                              id              SERIAL PRIMARY KEY,
                              user_id         INT NOT NULL REFERENCES users(id),
                              entry_date      DATE NOT NULL,
                              clock_in_time   TIME NOT NULL,
                              clock_out_time  TIME NOT NULL,
                              total_hours     NUMERIC(5,2) NOT NULL,
                              project_id      INT NOT NULL REFERENCES projects(id),
                              description     TEXT,
                              status          TEXT NOT NULL CHECK (status IN ('pending', 'approved', 'denied')),
                              approved_by     INT REFERENCES users(id),
                              approved_at     TIMESTAMP,
                              created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leave_types (
                             id              SERIAL PRIMARY KEY,
                             type_name       VARCHAR(50) NOT NULL UNIQUE,
                             description     TEXT,
                             is_active       BOOLEAN NOT NULL DEFAULT TRUE,
                             created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leave_policies (
                                id                          SERIAL PRIMARY KEY,
                                leave_type_id               INT NOT NULL UNIQUE REFERENCES leave_types(id),
                                annual_allocation           NUMERIC(4,1) NOT NULL,
                                accrual_method              VARCHAR(20) NOT NULL CHECK (accrual_method IN ('monthly', 'annual')),
                                allows_negative_balance     BOOLEAN NOT NULL DEFAULT FALSE,
                                max_rollover_days           NUMERIC(4,1) NOT NULL DEFAULT 0,
                                requires_manager_approval   BOOLEAN NOT NULL DEFAULT TRUE,
                                requires_hr_approval        BOOLEAN NOT NULL DEFAULT FALSE,
                                min_notice_days             INT NOT NULL DEFAULT 0,
                                created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leave_balances (
                                id                  SERIAL PRIMARY KEY,
                                user_id             INT NOT NULL REFERENCES users(id),
                                leave_type_id       INT NOT NULL REFERENCES leave_types(id),
                                year                SMALLINT NOT NULL,
                                current_balance     NUMERIC(5,1) NOT NULL DEFAULT 0,
                                last_accrual_date   DATE,
                                created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                UNIQUE (user_id, leave_type_id, year)
);

CREATE TABLE leave_requests (
                                id                      SERIAL PRIMARY KEY,
                                user_id                 INT NOT NULL REFERENCES users(id),
                                leave_type_id           INT NOT NULL REFERENCES leave_types(id),
                                start_date              DATE NOT NULL,
                                end_date                DATE NOT NULL,
                                total_days              NUMERIC(4,1) NOT NULL,
                                reason                  TEXT NOT NULL,
                                status                  TEXT NOT NULL CHECK (status IN ('pending', 'approved', 'denied', 'cancelled')),
                                manager_approval_status TEXT NOT NULL DEFAULT 'pending' CHECK (manager_approval_status IN ('pending', 'approved', 'denied')),
                                manager_approved_by     INT REFERENCES users(id),
                                manager_approved_at     TIMESTAMP,
                                manager_notes           TEXT,
                                hr_approval_status      TEXT NOT NULL DEFAULT 'pending' CHECK (hr_approval_status IN ('pending', 'approved', 'denied')),
                                hr_approved_by          INT REFERENCES users(id),
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

CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER set_updated_at
    BEFORE UPDATE ON departments
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER set_updated_at
    BEFORE UPDATE ON time_entries
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER set_updated_at
    BEFORE UPDATE ON leave_requests
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER set_updated_at
    BEFORE UPDATE ON leave_policies
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER set_updated_at
    BEFORE UPDATE ON leave_balances
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();



DROP TABLE audit_logs CASCADE;
DROP TABLE leave_requests CASCADE;
DROP TABLE leave_balances CASCADE;
DROP TABLE time_entries CASCADE;
DROP TABLE leave_policies CASCADE;
DROP TABLE leave_types CASCADE;
DROP TABLE projects CASCADE;
DROP TABLE users CASCADE;
DROP TABLE departments CASCADE;

DROP FUNCTION IF EXISTS update_timestamp() CASCADE;















