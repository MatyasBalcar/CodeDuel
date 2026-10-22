ALTER TABLE users ADD COLUMN email_verified boolean NOT NULL DEFAULT false;
ALTER TABLE users ADD COLUMN deleted_at timestamptz;
CREATE TABLE auth_tokens (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id),
    purpose varchar(24) NOT NULL CHECK (purpose IN ('VERIFY_EMAIL','RESET_PASSWORD')),
    token_hash char(64) UNIQUE NOT NULL,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX auth_token_user_idx ON auth_tokens(user_id,purpose);
CREATE TABLE mail_outbox (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id),
    recipient varchar(254) NOT NULL,
    subject varchar(120) NOT NULL,
    body text NOT NULL CHECK (length(body)<=8192),
    status varchar(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SENDING','SENT','DEAD')),
    attempts integer NOT NULL DEFAULT 0,
    lease_token uuid,
    lease_until timestamptz,
    next_retry_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CHECK ((status='SENDING') = (lease_token IS NOT NULL AND lease_until IS NOT NULL))
);
CREATE INDEX mail_outbox_due_idx ON mail_outbox(next_retry_at) WHERE status IN ('PENDING','SENDING');
ALTER TABLE submissions ADD COLUMN source_deleted_at timestamptz;
CREATE OR REPLACE FUNCTION protect_submission_payload() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    -- The only payload exception is a one-way redaction of terminal source to this fixed tombstone.
    IF OLD.source_deleted_at IS NULL AND NEW.source_deleted_at IS NOT NULL AND OLD.verdict <> 'PENDING'
       AND NEW.source_code = '// Source removed by retention policy.'
       AND (to_jsonb(NEW)-'source_code'-'source_deleted_at') = (to_jsonb(OLD)-'source_code'-'source_deleted_at') THEN
        RETURN NEW;
    END IF;
    IF (NEW.battle_id,NEW.user_id,NEW.problem_version_id,NEW.language,NEW.source_code,NEW.received_at,NEW.receive_sequence,NEW.is_official,NEW.purpose,NEW.source_deleted_at)
       IS DISTINCT FROM
       (OLD.battle_id,OLD.user_id,OLD.problem_version_id,OLD.language,OLD.source_code,OLD.received_at,OLD.receive_sequence,OLD.is_official,OLD.purpose,OLD.source_deleted_at) THEN
        RAISE EXCEPTION 'Submission payload is immutable' USING ERRCODE = '23514';
    END IF;
    IF OLD.verdict <> 'PENDING' AND NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'Terminal verdict is immutable' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

ALTER TABLE judge_jobs DROP CONSTRAINT judge_jobs_status_check;
ALTER TABLE judge_jobs ADD CONSTRAINT judge_job_status CHECK (status IN ('PENDING','RUNNING','COMPLETED','DEAD','CANCELLED'));
