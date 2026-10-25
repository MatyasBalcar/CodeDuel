#!/bin/sh
set -eu
cd "$(dirname "$0")/../.."
command -v limactl >/dev/null || { echo 'Install Lima first: brew install lima' >&2; exit 1; }
if [ -f "${LIMA_HOME:-$HOME/.lima}/codeduel-executor/lima.yaml" ]; then
  limactl start --tty=false codeduel-executor
else
  limactl start --tty=false --name=codeduel-executor --mount-none --containerd=none worker/sandbox/lima.yaml
fi
limactl cp worker/sandbox/mount.yaml codeduel-executor:/tmp/codeduel-mount.yaml
limactl cp worker/sandbox/install-linux.sh codeduel-executor:/tmp/codeduel-install.sh
limactl shell --workdir=/tmp codeduel-executor sudo sh /tmp/codeduel-install.sh
python3 - <<'PY'
import os, pathlib, secrets, subprocess
path = pathlib.Path('.env.judge.local')
if not path.exists():
    token = secrets.token_urlsafe(48)
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, 'w') as stream:
        stream.write('SPRING_PROFILES_ACTIVE=\nJUDGE_MODE=GO_JUDGE\nCOOKIE_SECURE=false\n'
                     'DATABASE_PASSWORD=codeduel_local\nJUDGE_URL=http://host.lima.internal:5058\n'
                     f'JUDGE_AUTH_TOKEN={token}\n')
values = dict(line.split('=', 1) for line in path.read_text().splitlines() if line and not line.startswith('#'))
token = values['JUDGE_AUTH_TOKEN']
assert len(token) >= 32 and token.isascii() and '\n' not in token
subprocess.run(['limactl', 'shell', '--workdir=/tmp', 'codeduel-executor', 'sudo',
                'install', '-m', '0600', '/dev/stdin', '/etc/codeduel-judge.env'],
               input=f'ES_AUTH_TOKEN={token}\nES_SILENT=true\n'.encode(), check=True)
PY
limactl shell --workdir=/tmp codeduel-executor sudo systemctl restart codeduel-judge
printf '\nExecutor installed. Private worker settings: .env.judge.local (do not commit).\n'
printf 'Start the application: docker compose --env-file .env.judge.local --profile worker up -d --build\n'
