-- Run as the migration owner after every forward migration. No runtime role can change schema.
GRANT SELECT, INSERT, UPDATE ON users, rating_profiles, problems, problem_versions, test_cases TO codeduel_api;
GRANT DELETE ON test_cases TO codeduel_api;
GRANT SELECT, INSERT, UPDATE, DELETE ON active_sessions, matchmaking_entries, request_limits, spring_session, spring_session_attributes TO codeduel_api;
GRANT SELECT, INSERT, UPDATE ON battles, battle_participants, submissions, judge_jobs, submission_case_results TO codeduel_api;
GRANT SELECT, INSERT ON battle_events, rating_events, audit_log TO codeduel_api;
GRANT SELECT ON battles, battle_participants, submissions, judge_jobs, submission_case_results, problem_versions, test_cases, rating_profiles, rating_events, battle_events, active_sessions TO codeduel_worker;
GRANT UPDATE ON judge_jobs, submission_case_results, battles, rating_profiles TO codeduel_worker;
GRANT UPDATE (verdict, judged_at) ON submissions TO codeduel_worker;
GRANT DELETE ON active_sessions TO codeduel_worker;
GRANT INSERT ON battle_events, rating_events TO codeduel_worker;
GRANT USAGE ON SEQUENCE practice_receive_sequence TO codeduel_api;
GRANT SELECT, INSERT, UPDATE ON rematch_rooms TO codeduel_api;
GRANT SELECT, INSERT, UPDATE, DELETE ON auth_tokens, mail_outbox TO codeduel_api;
