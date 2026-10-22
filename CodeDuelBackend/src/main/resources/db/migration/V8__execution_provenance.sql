ALTER TABLE judge_jobs ADD COLUMN executor_kind varchar(24) CHECK (executor_kind IN ('UNSPECIFIED','FIXTURE','GO_JUDGE','JUDGE0'));
