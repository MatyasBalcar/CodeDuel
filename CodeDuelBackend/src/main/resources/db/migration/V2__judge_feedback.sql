ALTER TABLE submission_case_results
  ADD COLUMN compiler_diagnostic text NOT NULL DEFAULT '' CHECK (length(compiler_diagnostic) <= 8192),
  ADD COLUMN public_stdout text NOT NULL DEFAULT '' CHECK (length(public_stdout) <= 8192);
