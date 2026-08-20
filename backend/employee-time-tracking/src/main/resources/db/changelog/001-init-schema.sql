CREATE TABLE companies (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(63) NOT NULL,
    status      TEXT NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT companies_slug_format_chk
        CHECK (slug ~ '^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$' AND slug = lower(slug)),
    CONSTRAINT companies_slug_reserved_chk
        CHECK (slug NOT IN ('www', 'api', 'app', 'admin', 'mail', 'localhost'))
);

CREATE UNIQUE INDEX companies_slug_uq ON companies (slug);

CREATE TABLE departments (
    id                  BIGSERIAL PRIMARY KEY,
    company_id          BIGINT NOT NULL REFERENCES companies(id),
    department_name     VARCHAR(50) NOT NULL,
    department_code     VARCHAR(50) NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT departments_id_company_uq UNIQUE (id, company_id),
    CONSTRAINT departments_company_code_uq UNIQUE (company_id, department_code),
    CONSTRAINT departments_company_name_uq UNIQUE (company_id, department_name)
);

CREATE INDEX departments_company_id_idx ON departments (company_id);

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    first_name      VARCHAR(50) NOT NULL,
    last_name       VARCHAR(50) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE projects (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT NOT NULL REFERENCES companies(id),
    project_name    VARCHAR(50) NOT NULL,
    project_code    VARCHAR(50) NOT NULL,
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT projects_id_company_uq UNIQUE (id, company_id),
    CONSTRAINT projects_company_code_uq UNIQUE (company_id, project_code)
);

CREATE INDEX projects_company_id_idx ON projects (company_id);

CREATE TABLE time_entries (
    id                  BIGSERIAL PRIMARY KEY,
    company_id          BIGINT NOT NULL REFERENCES companies(id),
    user_id             BIGINT NOT NULL REFERENCES users(id),
    entry_date          DATE NOT NULL,
    clock_in_time       TIME NOT NULL,
    clock_out_time      TIME NOT NULL,
    total_hours         NUMERIC(5,2) NOT NULL,
    project_id          BIGINT NOT NULL,
    description         TEXT,
    rejection_reason    TEXT,
    correction_reason   TEXT,
    status              TEXT NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'DENIED' ,'CANCELLED', 'PENDING_CORRECTION')),
    approved_by         BIGINT REFERENCES users(id),
    approved_at         TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT time_entries_project_company_fk
        FOREIGN KEY (project_id, company_id) REFERENCES projects(id, company_id)
);

CREATE INDEX time_entries_company_id_idx ON time_entries (company_id);

CREATE TABLE time_entry_breaks (
    id            BIGSERIAL PRIMARY KEY,
    time_entry_id BIGINT NOT NULL REFERENCES time_entries(id) ON DELETE CASCADE,
    break_start   TIME NOT NULL,
    break_end     TIME NOT NULL,
    is_unpaid     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leave_types (
    id              BIGSERIAL PRIMARY KEY,
    company_id      BIGINT NOT NULL REFERENCES companies(id),
    type_name       VARCHAR(50) NOT NULL,
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT leave_types_id_company_uq UNIQUE (id, company_id),
    CONSTRAINT leave_types_company_name_uq UNIQUE (company_id, type_name)
);

CREATE INDEX leave_types_company_id_idx ON leave_types (company_id);

CREATE TABLE leave_policies (
    id                          BIGSERIAL PRIMARY KEY,
    company_id                  BIGINT NOT NULL REFERENCES companies(id),
    leave_type_id               BIGINT NOT NULL UNIQUE,
    annual_allocation           NUMERIC(4,1) NOT NULL,
    accrual_method              VARCHAR(20) NOT NULL CHECK (accrual_method IN ('MONTHLY', 'ANNUAL')),
    allows_negative_balance     BOOLEAN NOT NULL DEFAULT FALSE,
    max_rollover_days           NUMERIC(4,1) NOT NULL DEFAULT 0,
    requires_manager_approval   BOOLEAN NOT NULL DEFAULT TRUE,
    requires_hr_approval        BOOLEAN NOT NULL DEFAULT FALSE,
    min_notice_days             INT NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT leave_policies_leave_type_fk FOREIGN KEY (leave_type_id) REFERENCES leave_types(id),
    CONSTRAINT leave_policies_leave_type_company_fk
        FOREIGN KEY (leave_type_id, company_id) REFERENCES leave_types(id, company_id)
);

CREATE INDEX leave_policies_company_id_idx ON leave_policies (company_id);

COMMENT ON CONSTRAINT leave_policies_leave_type_company_fk ON leave_policies IS
    'leave_policies.company_id must equal leave_types.company_id for leave_type_id';

CREATE TABLE leave_balances (
    id                  BIGSERIAL PRIMARY KEY,
    company_id          BIGINT NOT NULL REFERENCES companies(id),
    user_id             BIGINT NOT NULL REFERENCES users(id),
    leave_type_id       BIGINT NOT NULL,
    year                SMALLINT NOT NULL,
    current_balance     NUMERIC(5,1) NOT NULL DEFAULT 0,
    last_accrual_date   DATE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT leave_balances_company_user_type_year_uq UNIQUE (company_id, user_id, leave_type_id, year),
    CONSTRAINT leave_balances_leave_type_company_fk
        FOREIGN KEY (leave_type_id, company_id) REFERENCES leave_types(id, company_id)
);

CREATE INDEX leave_balances_company_id_idx ON leave_balances (company_id);

CREATE TABLE leave_requests (
    id                      BIGSERIAL PRIMARY KEY,
    company_id              BIGINT NOT NULL REFERENCES companies(id),
    user_id                 BIGINT NOT NULL REFERENCES users(id),
    leave_type_id           BIGINT NOT NULL,
    start_date              DATE NOT NULL,
    end_date                DATE NOT NULL,
    total_days              NUMERIC(4,1) NOT NULL,
    reason                  TEXT NOT NULL,
    cancellation_reason     TEXT,
    status                  TEXT NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'DENIED', 'CANCELLED', 'CANCELLATION_PENDING')),
    manager_approval_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (manager_approval_status IN ('PENDING', 'APPROVED', 'DENIED' ,'CANCELLED')),
    manager_approved_by     BIGINT REFERENCES users(id),
    manager_approved_at     TIMESTAMP,
    manager_notes           TEXT,
    hr_approval_status      TEXT NOT NULL DEFAULT 'PENDING' CHECK (hr_approval_status IN ('PENDING', 'APPROVED', 'DENIED')),
    hr_approved_by          BIGINT REFERENCES users(id),
    hr_approved_at          TIMESTAMP,
    hr_notes                TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT leave_requests_leave_type_company_fk
        FOREIGN KEY (leave_type_id, company_id) REFERENCES leave_types(id, company_id)
);

CREATE INDEX leave_requests_company_id_idx ON leave_requests (company_id);

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    company_id  BIGINT NOT NULL REFERENCES companies(id),
    user_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action_type VARCHAR(10) NOT NULL CHECK (action_type IN ('CREATE', 'UPDATE', 'DELETE')),
    table_name  VARCHAR(50) NOT NULL,
    record_id   BIGINT NOT NULL,
    old_values  JSONB,
    new_values  JSONB,
    ip_address  VARCHAR(45),
    timestamp   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX audit_logs_company_id_idx ON audit_logs (company_id);

CREATE TABLE company_memberships (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL REFERENCES users(id),
    company_id              BIGINT NOT NULL REFERENCES companies(id),
    role                    TEXT NOT NULL CHECK (role IN ('EMPLOYEE', 'MANAGER', 'HR_ADMIN')),
    status                  TEXT NOT NULL CHECK (status IN ('INVITED', 'ACTIVE', 'SUSPENDED', 'DEACTIVATED')),
    department_id           BIGINT,
    manager_membership_id   BIGINT REFERENCES company_memberships(id),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT company_memberships_user_company_uq UNIQUE (user_id, company_id),
    CONSTRAINT company_memberships_department_company_fk
        FOREIGN KEY (department_id, company_id) REFERENCES departments(id, company_id)
);

CREATE INDEX company_memberships_company_role_idx ON company_memberships (company_id, role);
CREATE INDEX company_memberships_company_status_idx ON company_memberships (company_id, status);
CREATE INDEX company_memberships_manager_membership_idx ON company_memberships (manager_membership_id);
CREATE INDEX company_memberships_company_id_idx ON company_memberships (company_id);

CREATE TABLE invitations (
    id                          BIGSERIAL PRIMARY KEY,
    company_id                  BIGINT NOT NULL REFERENCES companies(id),
    email                       VARCHAR(255) NOT NULL,
    role                        TEXT NOT NULL CHECK (role IN ('EMPLOYEE', 'MANAGER', 'HR_ADMIN')),
    department_id               BIGINT,
    manager_membership_id       BIGINT REFERENCES company_memberships(id),
    token_hash                  TEXT NOT NULL,
    status                      TEXT NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED')),
    invited_by_membership_id    BIGINT NOT NULL REFERENCES company_memberships(id),
    expires_at                  TIMESTAMP NOT NULL,
    accepted_at                 TIMESTAMP,
    accepted_user_id            BIGINT REFERENCES users(id),
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT invitations_department_company_fk
        FOREIGN KEY (department_id, company_id) REFERENCES departments(id, company_id)
);

CREATE UNIQUE INDEX invitations_pending_token_hash_uq ON invitations (token_hash) WHERE status = 'PENDING';
CREATE INDEX invitations_company_email_status_idx ON invitations (company_id, email, status);
CREATE INDEX invitations_company_id_idx ON invitations (company_id);
