ALTER TABLE problem_versions ADD COLUMN function_contract jsonb
    CHECK (function_contract IS NULL OR jsonb_typeof(function_contract) = 'object');

ALTER TABLE test_cases ADD COLUMN arguments jsonb;
ALTER TABLE test_cases ADD COLUMN expected_value jsonb;
ALTER TABLE test_cases ADD CONSTRAINT function_case_values CHECK (
    (arguments IS NULL AND expected_value IS NULL)
    OR (arguments IS NOT NULL AND jsonb_typeof(arguments) = 'object' AND expected_value IS NOT NULL)
);
