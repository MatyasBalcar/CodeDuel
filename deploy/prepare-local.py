#!/usr/bin/env python3
"""Create local rehearsal credentials. Existing credentials are never replaced."""
import os
from pathlib import Path
import secrets

root = Path(__file__).resolve().parents[1]
folder = root / '.secrets/deploy'
folder.mkdir(mode=0o700, parents=True, exist_ok=True)
os.chmod(root / '.secrets', 0o700)
os.chmod(folder, 0o700)
for name in ('postgres_password', 'migrator_password', 'api_password', 'worker_password', 'metrics_token', 'bootstrap_password'):
    path = folder / name
    if not path.exists():
        with os.fdopen(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as stream:
            stream.write(secrets.token_urlsafe(48))
judge = root / '.env.judge.local'
if not judge.exists():
    raise SystemExit('Run worker/sandbox/setup-local.sh first to provision real Java execution.')
values = dict(line.split('=', 1) for line in judge.read_text().splitlines() if line)
path = folder / 'judge_token'
if not path.exists():
    with os.fdopen(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as stream:
        stream.write(values['JUDGE_AUTH_TOKEN'])
env = root / '.env.deploy.local'
if not env.exists():
    with os.fdopen(os.open(env, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as stream:
        stream.write('APP_DOMAIN=localhost\nJUDGE_MODE=GO_JUDGE\nJUDGE_URL=http://host.lima.internal:5058\n')
smtp = folder / 'smtp_password'
if not smtp.exists():
    with os.fdopen(os.open(smtp, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as stream:
        stream.write('')
defaults = {'PUBLIC_URL': 'https://localhost:8443', 'BOOTSTRAP_USERNAME': 'operator',
            'BOOTSTRAP_EMAIL': 'operator@example.invalid', 'SMTP_HOST': 'mailpit', 'SMTP_PORT': '1025',
            'SMTP_AUTH': 'false', 'SMTP_TLS': 'false', 'LOCAL_MAIL': 'true', 'MAIL_FROM': 'noreply@codeduel.local'}
existing = dict(line.split('=', 1) for line in env.read_text().splitlines() if line and not line.startswith('#'))
with env.open('a') as stream:
    for key, value in defaults.items():
        if key not in existing:
            stream.write(f'{key}={value}\n')
print('Local rehearsal credentials prepared; no secret values printed.')

for path in folder.iterdir():
    if path.is_file():
        os.chmod(path, 0o444)
