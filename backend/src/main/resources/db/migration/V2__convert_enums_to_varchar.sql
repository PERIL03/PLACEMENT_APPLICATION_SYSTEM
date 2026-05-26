ALTER TABLE app_user
    ALTER COLUMN role TYPE VARCHAR(50)
    USING role::text;

ALTER TABLE drive_application
    ALTER COLUMN status TYPE VARCHAR(50)
    USING status::text;

ALTER TABLE application_status_history
    ALTER COLUMN old_status TYPE VARCHAR(50)
    USING old_status::text;

ALTER TABLE application_status_history
    ALTER COLUMN new_status TYPE VARCHAR(50)
    USING new_status::text;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN
        DROP TYPE user_role;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'application_status') THEN
        DROP TYPE application_status;
    END IF;
END $$;
