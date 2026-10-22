CREATE SEQUENCE practice_receive_sequence;
ALTER TABLE submissions ALTER COLUMN battle_id DROP NOT NULL;
ALTER TABLE submissions ADD CONSTRAINT official_requires_battle CHECK (NOT is_official OR battle_id IS NOT NULL);
ALTER TABLE submissions ADD COLUMN purpose varchar(24);
-- Backfill metadata under the migration owner's table lock, then reinstate the stronger guard.
DROP TRIGGER immutable_submission ON submissions;
UPDATE submissions SET purpose = CASE WHEN is_official THEN 'BATTLE' ELSE 'PUBLIC_EXAMPLES' END;
ALTER TABLE submissions ALTER COLUMN purpose SET NOT NULL;
ALTER TABLE submissions ADD CONSTRAINT submission_purpose CHECK (purpose IN ('BATTLE','PUBLIC_EXAMPLES','PRACTICE','VERIFICATION','DIAGNOSTIC'));
CREATE OR REPLACE FUNCTION protect_submission_payload() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.battle_id, NEW.user_id, NEW.problem_version_id, NEW.language, NEW.source_code, NEW.received_at, NEW.receive_sequence, NEW.is_official, NEW.purpose)
       IS DISTINCT FROM
       (OLD.battle_id, OLD.user_id, OLD.problem_version_id, OLD.language, OLD.source_code, OLD.received_at, OLD.receive_sequence, OLD.is_official, OLD.purpose) THEN
        RAISE EXCEPTION 'Submission payload is immutable' USING ERRCODE = '23514';
    END IF;
    IF OLD.verdict <> 'PENDING' AND NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'Terminal verdict is immutable' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER immutable_submission BEFORE UPDATE ON submissions FOR EACH ROW EXECUTE FUNCTION protect_submission_payload();

ALTER TABLE problem_versions ADD COLUMN reference_source text CHECK (octet_length(reference_source) <= 65536);
ALTER TABLE problem_versions DROP CONSTRAINT problem_versions_status_check;
ALTER TABLE problem_versions ADD CONSTRAINT problem_version_status CHECK (status IN ('DRAFT','REVIEW','PUBLISHED'));
CREATE OR REPLACE FUNCTION protect_published_version() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status = 'PUBLISHED' THEN
        RAISE EXCEPTION 'Published problem versions are immutable' USING ERRCODE = '23514';
    END IF;
    IF OLD.status = 'REVIEW' THEN
        IF TG_OP = 'DELETE' OR NEW.status <> 'PUBLISHED' OR
           (to_jsonb(NEW) - 'status' - 'published_at') IS DISTINCT FROM (to_jsonb(OLD) - 'status' - 'published_at') THEN
            RAISE EXCEPTION 'Reviewed problem content is frozen; clone a new draft' USING ERRCODE = '23514';
        END IF;
    END IF;
    IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
    RETURN NEW;
END;
$$;
CREATE OR REPLACE FUNCTION protect_published_test() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE version_status varchar;
BEGIN
    IF TG_OP <> 'INSERT' THEN
        SELECT status INTO version_status FROM problem_versions WHERE id = OLD.problem_version_id FOR UPDATE;
        IF version_status IN ('REVIEW','PUBLISHED') THEN
            RAISE EXCEPTION 'Reviewed/published tests are immutable' USING ERRCODE = '23514';
        END IF;
    END IF;
    IF TG_OP <> 'DELETE' THEN
        SELECT status INTO version_status FROM problem_versions WHERE id = NEW.problem_version_id FOR UPDATE;
        IF version_status IN ('REVIEW','PUBLISHED') THEN
            RAISE EXCEPTION 'Reviewed/published tests are immutable' USING ERRCODE = '23514';
        END IF;
        RETURN NEW;
    END IF;
    RETURN OLD;
END;
$$;
CREATE INDEX submissions_practice_idx ON submissions(user_id, received_at DESC) WHERE battle_id IS NULL;
ALTER TABLE submissions ADD CONSTRAINT submission_problem_version FOREIGN KEY (problem_version_id) REFERENCES problem_versions(id);
ALTER TABLE submissions ADD CONSTRAINT submission_author FOREIGN KEY (user_id) REFERENCES users(id);
-- Keep old application instances compatible during this additive release transition.
CREATE FUNCTION default_submission_purpose() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.purpose IS NULL THEN NEW.purpose := CASE WHEN NEW.is_official THEN 'BATTLE' ELSE 'PUBLIC_EXAMPLES' END; END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER submission_purpose_default BEFORE INSERT ON submissions FOR EACH ROW EXECUTE FUNCTION default_submission_purpose();
