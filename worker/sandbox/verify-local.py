#!/usr/bin/env python3
"""Run real sandbox tests without putting the executor credential in command-line arguments."""
import os
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parents[2]
settings = root / '.env.judge.local'
if not settings.exists():
    raise SystemExit('Run worker/sandbox/setup-local.sh first.')
values = dict(line.split('=', 1) for line in settings.read_text().splitlines()
              if line and not line.startswith('#'))
env = os.environ.copy()
env['CODEDUEL_EXECUTOR_URL'] = 'http://127.0.0.1:5058'
env['CODEDUEL_EXECUTOR_TOKEN'] = values['JUDGE_AUTH_TOKEN']
# Testcontainers respects the active Docker context only after resolving its endpoint explicitly.
if not env.get('DOCKER_HOST'):
    env['DOCKER_HOST'] = subprocess.check_output(
        ['docker', 'context', 'inspect', '--format', '{{.Endpoints.docker.Host}}'], text=True).strip()
if env['DOCKER_HOST'].startswith('unix:'):
    env.setdefault('TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE', '/var/run/docker.sock')
args = ['./mvnw', '-B', '-ntp', '-Dmaven.repo.local=.cache/maven']
if '--sandbox-only' in sys.argv:
    args.append('-Dit.test=SandboxIT')
args.append('verify')
raise SystemExit(subprocess.call(args, cwd=root, env=env))
