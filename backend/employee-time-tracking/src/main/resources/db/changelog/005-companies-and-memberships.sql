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

CREATE TABLE company_memberships (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL REFERENCES users(id),
    company_id              BIGINT NOT NULL REFERENCES companies(id),
    role                    TEXT NOT NULL CHECK (role IN ('EMPLOYEE', 'MANAGER', 'HR_ADMIN')),
    status                  TEXT NOT NULL CHECK (status IN ('INVITED', 'ACTIVE', 'SUSPENDED', 'DEACTIVATED')),
    department_id           BIGINT REFERENCES departments(id),
    manager_membership_id   BIGINT REFERENCES company_memberships(id),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT company_memberships_user_company_uq UNIQUE (user_id, company_id)
);

CREATE INDEX company_memberships_company_role_idx ON company_memberships (company_id, role);
CREATE INDEX company_memberships_company_status_idx ON company_memberships (company_id, status);
CREATE INDEX company_memberships_manager_membership_idx ON company_memberships (manager_membership_id);

CREATE TABLE invitations (
    id                          BIGSERIAL PRIMARY KEY,
    company_id                  BIGINT NOT NULL REFERENCES companies(id),
    email                       VARCHAR(255) NOT NULL,
    role                        TEXT NOT NULL CHECK (role IN ('EMPLOYEE', 'MANAGER', 'HR_ADMIN')),
    department_id               BIGINT REFERENCES departments(id),
    manager_membership_id       BIGINT REFERENCES company_memberships(id),
    token_hash                  TEXT NOT NULL,
    status                      TEXT NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED')),
    invited_by_membership_id    BIGINT NOT NULL REFERENCES company_memberships(id),
    expires_at                  TIMESTAMP NOT NULL,
    accepted_at                 TIMESTAMP,
    accepted_user_id            BIGINT REFERENCES users(id),
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX invitations_pending_token_hash_uq ON invitations (token_hash) WHERE status = 'PENDING';
CREATE INDEX invitations_company_email_status_idx ON invitations (company_id, email, status);
