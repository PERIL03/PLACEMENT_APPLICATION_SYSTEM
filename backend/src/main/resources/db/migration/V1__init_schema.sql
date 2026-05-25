DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        CREATE TYPE user_role AS ENUM ('STUDENT','PLACEMENT_OFFICER','RECRUITER');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'application_status') THEN
        CREATE TYPE application_status AS ENUM ('APPLIED','SHORTLISTED','INTERVIEW_SCHEDULED','OFFERED','REJECTED');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role user_role NOT NULL
);

CREATE TABLE IF NOT EXISTS student (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    branch VARCHAR(255),
    graduation_year INT,
    cgpa DOUBLE PRECISION,
    user_id BIGINT UNIQUE,
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE TABLE IF NOT EXISTS company (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    website VARCHAR(255),
    location VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS drive (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    role_title VARCHAR(255),
    eligible_branch VARCHAR(255),
    min_cgpa DOUBLE PRECISION,
    package_lpa DOUBLE PRECISION,
    application_deadline DATE,
    CONSTRAINT fk_drive_company FOREIGN KEY (company_id) REFERENCES company (id)
);

CREATE TABLE IF NOT EXISTS drive_application (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    drive_id BIGINT NOT NULL,
    status application_status NOT NULL,
    applied_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_drive_application_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_drive_application_drive FOREIGN KEY (drive_id) REFERENCES drive (id)
);

CREATE TABLE IF NOT EXISTS interview_slot (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL UNIQUE,
    scheduled_at TIMESTAMP NULL,
    mode VARCHAR(100),
    meeting_link VARCHAR(512),
    created_by VARCHAR(255),
    created_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_interview_application FOREIGN KEY (application_id) REFERENCES drive_application (id)
);

CREATE TABLE IF NOT EXISTS application_status_history (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    old_status application_status,
    new_status application_status NOT NULL,
    changed_by VARCHAR(255),
    remarks VARCHAR(512),
    changed_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_status_history_application FOREIGN KEY (application_id) REFERENCES drive_application (id)
);
