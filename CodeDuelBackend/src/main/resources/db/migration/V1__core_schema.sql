CREATE TABLE users (
    id uuid PRIMARY KEY,
    username varchar(24) NOT NULL,
    email varchar(254) NOT NULL,
    password_hash varchar(100) NOT NULL,
    role varchar(16) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE UNIQUE INDEX users_username_key ON users (lower(username));
CREATE UNIQUE INDEX users_email_key ON users (lower(email));

CREATE TABLE rating_profiles (
    user_id uuid PRIMARY KEY REFERENCES users(id),
    rating numeric(14,6) NOT NULL DEFAULT 1200,
    ranked_games integer NOT NULL DEFAULT 0 CHECK (ranked_games >= 0),
    wins integer NOT NULL DEFAULT 0 CHECK (wins >= 0),
    losses integer NOT NULL DEFAULT 0 CHECK (losses >= 0),
    draws integer NOT NULL DEFAULT 0 CHECK (draws >= 0),
    updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CHECK (ranked_games = wins + losses + draws)
);
CREATE INDEX rating_leaderboard_idx ON rating_profiles (rating DESC, user_id);

CREATE TABLE problems (
    id uuid PRIMARY KEY,
    slug varchar(80) UNIQUE NOT NULL,
    title varchar(120) NOT NULL,
    difficulty varchar(16) NOT NULL CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    tags text[] NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE problem_versions (
    id uuid PRIMARY KEY,
    problem_id uuid NOT NULL REFERENCES problems(id),
    version integer NOT NULL CHECK (version > 0),
    status varchar(16) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED')),
    statement text NOT NULL,
    constraints_text text NOT NULL,
    input_format text NOT NULL,
    output_format text NOT NULL,
    starter_code text NOT NULL,
    content_sha256 varchar(64) NOT NULL,
    time_limit_ms integer NOT NULL CHECK (time_limit_ms BETWEEN 100 AND 10000),
    memory_limit_mb integer NOT NULL CHECK (memory_limit_mb BETWEEN 64 AND 512),
    duration_seconds integer NOT NULL CHECK (duration_seconds BETWEEN 60 AND 1800),
    published_at timestamptz,
    UNIQUE (problem_id, version),
    CHECK ((status = 'PUBLISHED') = (published_at IS NOT NULL))
);
CREATE TABLE test_cases (
    id uuid PRIMARY KEY,
    problem_version_id uuid NOT NULL REFERENCES problem_versions(id),
    input_data text NOT NULL,
    expected_output text NOT NULL,
    is_public boolean NOT NULL DEFAULT false,
    ordinal integer NOT NULL CHECK (ordinal >= 0),
    UNIQUE (problem_version_id, ordinal)
);

CREATE FUNCTION protect_published_version() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status = 'PUBLISHED' THEN
        RAISE EXCEPTION 'Published problem versions are immutable' USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER immutable_problem_version BEFORE UPDATE OR DELETE ON problem_versions
FOR EACH ROW EXECUTE FUNCTION protect_published_version();

CREATE FUNCTION protect_published_test() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE version_status text;
BEGIN
    -- Lock the parent to serialize test edits against publication.
    IF TG_OP <> 'INSERT' THEN
        SELECT status INTO version_status FROM problem_versions WHERE id = OLD.problem_version_id FOR UPDATE;
        IF version_status = 'PUBLISHED' THEN
            RAISE EXCEPTION 'Published tests are immutable' USING ERRCODE = '23514';
        END IF;
    END IF;
    IF TG_OP <> 'DELETE' THEN
        SELECT status INTO version_status FROM problem_versions WHERE id = NEW.problem_version_id FOR UPDATE;
        IF version_status = 'PUBLISHED' THEN
            RAISE EXCEPTION 'Published tests are immutable' USING ERRCODE = '23514';
        END IF;
        RETURN NEW;
    END IF;
    RETURN OLD;
END;
$$;
CREATE TRIGGER immutable_test_case BEFORE INSERT OR UPDATE OR DELETE ON test_cases
FOR EACH ROW EXECUTE FUNCTION protect_published_test();

CREATE TABLE battles (
    id uuid PRIMARY KEY,
    problem_version_id uuid NOT NULL REFERENCES problem_versions(id),
    mode varchar(16) NOT NULL CHECK (mode IN ('RANKED', 'PRIVATE')),
    status varchar(20) NOT NULL CHECK (status IN ('WAITING', 'READY_CHECK', 'IN_PROGRESS', 'JUDGING', 'FINISHED', 'CANCELLED')),
    invite_hash varchar(64) UNIQUE,
    ready_deadline timestamptz NOT NULL,
    starts_at timestamptz,
    ends_at timestamptz,
    finished_at timestamptz,
    winner_id uuid REFERENCES users(id),
    finalization_reason varchar(40),
    receive_sequence bigint NOT NULL DEFAULT 0,
    event_sequence bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (id, problem_version_id),
    CHECK ((starts_at IS NULL AND ends_at IS NULL) OR ends_at > starts_at),
    CHECK ((status IN ('FINISHED', 'CANCELLED')) = (finished_at IS NOT NULL)),
    CHECK (status NOT IN ('IN_PROGRESS', 'JUDGING', 'FINISHED') OR starts_at IS NOT NULL)
);
CREATE INDEX battles_due_idx ON battles (status, ends_at, ready_deadline);
CREATE TABLE battle_participants (
    battle_id uuid NOT NULL REFERENCES battles(id),
    user_id uuid NOT NULL REFERENCES users(id),
    slot smallint NOT NULL CHECK (slot IN (1,2)),
    is_ready boolean NOT NULL DEFAULT false,
    forfeited_at timestamptz,
    joined_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    PRIMARY KEY (battle_id, user_id),
    UNIQUE (battle_id, slot)
);
ALTER TABLE battles ADD CONSTRAINT winner_is_participant
    FOREIGN KEY (id, winner_id) REFERENCES battle_participants(battle_id, user_id) DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX participant_history_idx ON battle_participants (user_id, joined_at DESC);

CREATE TABLE active_sessions (
    user_id uuid PRIMARY KEY REFERENCES users(id),
    battle_id uuid REFERENCES battles(id),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE matchmaking_entries (
    user_id uuid PRIMARY KEY REFERENCES active_sessions(user_id),
    rating_snapshot numeric(14,6) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    heartbeat_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX matchmaking_oldest_idx ON matchmaking_entries (created_at);

CREATE TABLE submissions (
    id uuid PRIMARY KEY,
    battle_id uuid NOT NULL,
    user_id uuid NOT NULL,
    problem_version_id uuid NOT NULL,
    language varchar(16) NOT NULL CHECK (language = 'JAVA_21'),
    source_code text NOT NULL CHECK (octet_length(source_code) BETWEEN 1 AND 65536),
    received_at timestamptz NOT NULL,
    receive_sequence bigint NOT NULL,
    verdict varchar(32) NOT NULL DEFAULT 'PENDING' CHECK (verdict IN ('PENDING','ACCEPTED','WRONG_ANSWER','TIME_LIMIT_EXCEEDED','MEMORY_LIMIT_EXCEEDED','RUNTIME_ERROR','COMPILATION_ERROR','JUDGE_ERROR')),
    judged_at timestamptz,
    is_official boolean NOT NULL,
    FOREIGN KEY (battle_id, problem_version_id) REFERENCES battles(id, problem_version_id),
    FOREIGN KEY (battle_id, user_id) REFERENCES battle_participants(battle_id, user_id),
    UNIQUE (battle_id, receive_sequence),
    CHECK ((verdict = 'PENDING') = (judged_at IS NULL))
);
CREATE INDEX submissions_outcome_idx ON submissions (battle_id, receive_sequence) WHERE is_official;
CREATE INDEX submissions_quota_idx ON submissions (user_id, received_at DESC);

CREATE FUNCTION protect_submission_payload() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.battle_id, NEW.user_id, NEW.problem_version_id, NEW.language, NEW.source_code, NEW.received_at, NEW.receive_sequence, NEW.is_official)
       IS DISTINCT FROM
       (OLD.battle_id, OLD.user_id, OLD.problem_version_id, OLD.language, OLD.source_code, OLD.received_at, OLD.receive_sequence, OLD.is_official) THEN
        RAISE EXCEPTION 'Submission payload is immutable' USING ERRCODE = '23514';
    END IF;
    IF OLD.verdict <> 'PENDING' AND NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'Terminal verdict is immutable' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER immutable_submission BEFORE UPDATE ON submissions
FOR EACH ROW EXECUTE FUNCTION protect_submission_payload();

CREATE TABLE judge_jobs (
    id uuid PRIMARY KEY,
    submission_id uuid UNIQUE NOT NULL REFERENCES submissions(id),
    status varchar(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','RUNNING','COMPLETED','DEAD')),
    failures integer NOT NULL DEFAULT 0 CHECK (failures >= 0),
    lease_token uuid,
    lease_until timestamptz,
    next_retry_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    last_error varchar(80),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    CHECK ((status = 'RUNNING') = (lease_token IS NOT NULL AND lease_until IS NOT NULL))
);
CREATE INDEX judge_jobs_claim_idx ON judge_jobs (next_retry_at, created_at) WHERE status IN ('PENDING','RUNNING');
CREATE TABLE submission_case_results (
    submission_id uuid NOT NULL REFERENCES submissions(id),
    test_case_id uuid NOT NULL REFERENCES test_cases(id),
    executor_token varchar(200),
    verdict varchar(32),
    runtime_ms integer CHECK (runtime_ms >= 0),
    memory_kb integer CHECK (memory_kb >= 0),
    PRIMARY KEY (submission_id, test_case_id)
);

CREATE TABLE rating_events (
    id uuid PRIMARY KEY,
    battle_id uuid NOT NULL,
    user_id uuid NOT NULL REFERENCES rating_profiles(user_id),
    old_rating numeric(14,6) NOT NULL,
    delta numeric(14,6) NOT NULL,
    new_rating numeric(14,6) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    FOREIGN KEY (battle_id, user_id) REFERENCES battle_participants(battle_id, user_id),
    UNIQUE (battle_id, user_id),
    CHECK (new_rating = old_rating + delta)
);
CREATE TABLE battle_events (
    battle_id uuid NOT NULL REFERENCES battles(id),
    sequence bigint NOT NULL,
    event_type varchar(40) NOT NULL,
    payload_json jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    PRIMARY KEY (battle_id, sequence)
);
CREATE FUNCTION prevent_history_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'History is append-only' USING ERRCODE = '23514';
END;
$$;
CREATE TRIGGER append_only_ratings BEFORE UPDATE OR DELETE ON rating_events FOR EACH ROW EXECUTE FUNCTION prevent_history_mutation();
CREATE TRIGGER append_only_events BEFORE UPDATE OR DELETE ON battle_events FOR EACH ROW EXECUTE FUNCTION prevent_history_mutation();

CREATE TABLE request_limits (
    bucket_key varchar(200) PRIMARY KEY,
    hits integer NOT NULL CHECK (hits > 0),
    expires_at timestamptz NOT NULL
);
