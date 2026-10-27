"""Run the local Java executor in a separate KVM guest inside Ubuntu WSL."""
import argparse
import fcntl
import hashlib
import json
import os
from pathlib import Path
import pwd
import secrets
import shutil
import subprocess
import time
import urllib.request


ROOT = Path(__file__).resolve().parents[2]
STATE = Path('/var/lib/codeduel-executor')
SERVICE = 'codeduel-executor.service'
ACCOUNT = 'codeduel-vm'
IMAGE_URL = 'https://cloud-images.ubuntu.com/minimal/releases/noble/release-20261017/ubuntu-24.04-minimal-cloudimg-amd64.img'
IMAGE_SHA256 = 'b3296484e362ba2e713489a5de6225c719d9500962bde25d15f2f21e512c7658'
MAVEN_IMAGE = 'maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320'


def run(*args, capture=False, **kwargs):
    result = subprocess.run(args, check=True, text=True, capture_output=capture, **kwargs)
    return result.stdout.strip() if capture else result


def settings():
    values = {}
    for name in ('.env', '.env.judge.local'):
        path = ROOT / name
        if path.exists():
            for line in path.read_text(encoding='utf-8-sig').splitlines():
                if line and not line.startswith('#') and '=' in line:
                    key, value = line.split('=', 1)
                    values[key.strip()] = value.strip().strip('\"\'')
    return values


def save_settings():
    values = settings()
    token = values.get('JUDGE_AUTH_TOKEN') or secrets.token_urlsafe(48)
    if len(token) < 32 or not all(c.isalnum() or c in '_-' for c in token):
        raise RuntimeError('Use a strong URL-safe executor token in .env.judge.local.')
    updates = dict(SPRING_PROFILES_ACTIVE='', JUDGE_MODE='GO_JUDGE',
                   JUDGE_URL='http://127.0.0.1:5058', JUDGE_AUTH_TOKEN=token,
                   DATABASE_PASSWORD=values.get('DATABASE_PASSWORD') or 'codeduel_local',
                   COOKIE_SECURE='false', COMPOSE_PROFILES='worker',
                   COMPOSE_PATH_SEPARATOR=';',
                   COMPOSE_FILE='docker-compose.yml;worker/sandbox/compose.windows.yml')
    for name in ('.env.judge.local', '.env'):
        path = ROOT / name
        lines = path.read_text(encoding='utf-8-sig').splitlines() if path.exists() else []
        remaining = dict(updates)
        for index, line in enumerate(lines):
            key = line.split('=', 1)[0].strip()
            if key in remaining:
                lines[index] = key + '=' + remaining.pop(key)
        lines.extend(key + '=' + value for key, value in remaining.items())
        path.write_text('\n'.join(lines) + '\n', encoding='utf-8')
        path.chmod(0o600)
    return token


def ssh(command, *, input=None, capture=False, check=True, timeout=300):
    return subprocess.run([
        'ssh', '-F', '/dev/null', '-i', str(STATE / 'id_ed25519'), '-p', '2227',
        '-o', 'IdentitiesOnly=yes', '-o', 'ForwardAgent=no', '-o', 'BatchMode=yes',
        '-o', 'ConnectTimeout=3', '-o', 'StrictHostKeyChecking=accept-new',
        '-o', 'UserKnownHostsFile=' + str(STATE / 'known_hosts'),
        'ubuntu@127.0.0.1', command,
    ], input=input, text=True, capture_output=capture, check=check, timeout=timeout)


def wait_for_ssh():
    print('Waiting for the executor VM to boot...', flush=True)
    deadline = time.monotonic() + 240
    while time.monotonic() < deadline:
        result = ssh('true', capture=True, check=False, timeout=8)
        if result.returncode == 0:
            return
        if run('systemctl', 'is-active', SERVICE, capture=True) != 'active':
            raise RuntimeError('The VM stopped. Check journalctl -u ' + SERVICE)
        time.sleep(2)
    raise RuntimeError('The VM did not finish booting. Check ' + str(STATE / 'serial.log'))


def wait_for_judge(token):
    deadline = time.monotonic() + 90
    while time.monotonic() < deadline:
        try:
            request = urllib.request.Request('http://127.0.0.1:5058/version',
                                             headers={'Authorization': 'Bearer ' + token})
            with urllib.request.urlopen(request, timeout=3) as response:
                version = json.load(response)
            if version.get('buildVersion', '').strip() == 'v1.13.3' and version.get('os') == 'linux':
                print('The isolated Java executor is ready.', flush=True)
                return
        except (OSError, ValueError):
            pass
        time.sleep(2)
    raise RuntimeError('The judge did not become ready. Check the guest codeduel-judge service.')


def install_tools():
    if os.uname().machine != 'x86_64' or not Path('/dev/kvm').exists():
        raise RuntimeError('This setup needs x86-64 Ubuntu WSL with nested KVM virtualization.')
    with open('/dev/kvm', 'rb') as device:
        if fcntl.ioctl(device, 0xAE00) != 12:
            raise RuntimeError('KVM is unavailable.')
    if not all(shutil.which(name) for name in ('qemu-system-x86_64', 'qemu-img', 'cloud-localds', 'ssh')):
        run('apt-get', 'update')
        run('apt-get', 'install', '-y', '--no-install-recommends', 'qemu-system-x86',
            'qemu-utils', 'cloud-image-utils', 'openssh-client')
    run('systemctl', 'start', 'docker.service')
    try:
        account = pwd.getpwnam(ACCOUNT)
    except KeyError:
        run('useradd', '--system', '--user-group', '--no-create-home', '--home-dir', str(STATE),
            '--shell', '/usr/sbin/nologin', '--groups', 'kvm', ACCOUNT)
        account = pwd.getpwnam(ACCOUNT)
    STATE.mkdir(mode=0o750, exist_ok=True)
    STATE.chmod(0o750)
    os.chown(STATE, 0, account.pw_gid)
    return account


def prepare_disk(account):
    image = STATE / 'ubuntu.img'
    if not image.exists():
        print('Downloading the pinned Ubuntu executor image...', flush=True)
        temporary = STATE / 'ubuntu.img.download'
        with urllib.request.urlopen(IMAGE_URL, timeout=60) as response, temporary.open('wb') as output:
            shutil.copyfileobj(response, output)
        with temporary.open('rb') as stream:
            if hashlib.file_digest(stream, 'sha256').hexdigest() != IMAGE_SHA256:
                raise RuntimeError('Ubuntu image checksum verification failed.')
        temporary.replace(image)
    with image.open('rb') as stream:
        if hashlib.file_digest(stream, 'sha256').hexdigest() != IMAGE_SHA256:
            raise RuntimeError('The cached Ubuntu image has changed.')
    image.chmod(0o640)
    os.chown(image, 0, account.pw_gid)
    key = STATE / 'id_ed25519'
    if not key.exists():
        run('ssh-keygen', '-q', '-t', 'ed25519', '-N', '', '-f', str(key))
    disk = STATE / 'guest.qcow2'
    if not disk.exists():
        run('qemu-img', 'create', '-f', 'qcow2', '-F', 'qcow2', '-b', str(image), str(disk))
        run('qemu-img', 'resize', str(disk), '12G')
        cloud = {
            'hostname': 'codeduel-executor', 'manage_etc_hosts': True,
            'ssh_pwauth': False, 'disable_root': True,
            'users': [{'name': 'ubuntu', 'lock_passwd': True, 'shell': '/bin/bash',
                       'sudo': ['ALL=(ALL) NOPASSWD:ALL'],
                       'ssh_authorized_keys': [(STATE / 'id_ed25519.pub').read_text().strip()]}],
        }
        (STATE / 'user-data').write_text('#cloud-config\n' + json.dumps(cloud) + '\n')
        (STATE / 'meta-data').write_text('instance-id: codeduel-executor-v1\nlocal-hostname: codeduel-executor\n')
        run('cloud-localds', str(STATE / 'seed.img'), str(STATE / 'user-data'), str(STATE / 'meta-data'))
    for path in (disk, STATE / 'seed.img', STATE / 'serial.log'):
        path.touch(exist_ok=True)
        path.chmod(0o600)
        os.chown(path, account.pw_uid, account.pw_gid)


def configure_service(restricted):
    network = ('user,id=net0,ipv6=off,restrict=' + ('on' if restricted else 'off')
               + ',hostfwd=tcp:127.0.0.1:2227-:22,hostfwd=tcp:127.0.0.1:5058-:5050')
    command = ['/usr/bin/qemu-system-x86_64', '-name', 'codeduel-executor',
               '-machine', 'q35,accel=kvm', '-cpu', 'host', '-smp', '2', '-m', '3072',
               '-nodefaults', '-no-user-config', '-display', 'none', '-monitor', 'none',
               '-serial', 'file:' + str(STATE / 'serial.log'),
               '-drive', 'file=' + str(STATE / 'guest.qcow2') + ',format=qcow2,if=virtio',
               '-drive', 'file=' + str(STATE / 'seed.img') + ',format=raw,if=virtio,readonly=on',
               '-device', 'virtio-net-pci,netdev=net0', '-netdev', network]
    unit = f'''[Unit]
Description=CodeDuel executor VM
After=network.target docker.service
Requires=docker.service
[Service]
User={ACCOUNT}
Group={ACCOUNT}
SupplementaryGroups=kvm
WorkingDirectory={STATE}
ExecStart={' '.join(command)}
Restart=on-failure
RestartSec=3
TimeoutStopSec=30
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true
ProtectKernelTunables=true
ProtectControlGroups=true
InaccessiblePaths={'/mnt ' if restricted else ''}/home /root /var/run/docker.sock /etc/docker
ReadWritePaths={STATE}
MemoryMax=4G
TasksMax=128
UMask=0077
[Install]
WantedBy=multi-user.target
'''
    path = Path('/etc/systemd/system') / SERVICE
    path.write_text(unit)
    run('systemctl', 'daemon-reload')
    run('systemctl', 'enable', SERVICE)
    run('systemctl', 'start', SERVICE)


def stop_vm():
    if subprocess.run(['systemctl', 'is-active', '--quiet', SERVICE]).returncode != 0:
        return
    ssh('sudo poweroff', capture=True, check=False, timeout=10)
    for _ in range(30):
        if subprocess.run(['systemctl', 'is-active', '--quiet', SERVICE]).returncode != 0:
            break
        time.sleep(1)
    run('systemctl', 'stop', SERVICE)


def setup_vm(update=False):
    account = install_tools()
    token = save_settings()
    prepare_disk(account)
    installed = STATE / 'installed'
    if not installed.exists() or update:
        stop_vm()
        configure_service(False)
        wait_for_ssh()
        ssh('sudo cloud-init status --wait', timeout=300)
        for source, destination in [('mount.yaml', '/tmp/codeduel-mount.yaml'),
                                    ('install-linux.sh', '/tmp/codeduel-install.sh')]:
            ssh('sudo install -m 0644 /dev/stdin ' + destination,
                input=(ROOT / 'worker/sandbox' / source).read_text(encoding='utf-8'))
        print('Installing Java 21 and the existing go-judge service inside the VM...', flush=True)
        ssh('sudo sh /tmp/codeduel-install.sh', timeout=600)
        installed.write_text('Ubuntu 24.04; go-judge v1.13.3\n')
    else:
        configure_service(True)
        wait_for_ssh()
    ssh('sudo install -m 0600 /dev/stdin /etc/codeduel-judge.env',
        input='ES_AUTH_TOKEN=' + token + '\nES_SILENT=true\n')
    ssh('sudo systemctl restart codeduel-judge')
    stop_vm()
    configure_service(True)
    wait_for_ssh()
    wait_for_judge(token)


def compose(*args):
    run('docker', 'compose', '--env-file', str(ROOT / '.env.judge.local'),
        '--profile', 'worker', *args, cwd=ROOT)


def start_app():
    run('systemctl', 'start', SERVICE)
    wait_for_judge(settings()['JUDGE_AUTH_TOKEN'])
    compose('up', '-d', 'db', 'mailpit', 'api', 'frontend', 'worker')
    print('Real Java mode is configured. Open http://localhost:5173.', flush=True)


def verify():
    values = settings()
    env = os.environ.copy()
    env['CODEDUEL_EXECUTOR_URL'] = 'http://127.0.0.1:5058'
    env['CODEDUEL_EXECUTOR_TOKEN'] = values['JUDGE_AUTH_TOKEN']
    run('docker', 'run', '--rm', '--network', 'host',
        '--mount', 'type=bind,source=' + str(ROOT) + ',target=/build',
        '--mount', 'type=volume,source=codeduel_maven_cache,target=/root/.m2',
        '--mount', 'type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock',
        '--env', 'CODEDUEL_EXECUTOR_URL', '--env', 'CODEDUEL_EXECUTOR_TOKEN', '-w', '/build',
        MAVEN_IMAGE, 'sh', './mvnw', '-B', '-ntp',
        '-Dit.test=SandboxIT,PlatformIT#realJavaWorkerChecksHiddenCasesAndFinalizesEloExactlyOnce',
        'verify', env=env)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['setup', 'start', 'stop', 'status', 'test', 'update'])
    action = parser.parse_args().action
    if os.geteuid() != 0:
        raise SystemExit('Run this through setup-windows.ps1 in Ubuntu WSL.')
    if action in ('setup', 'update'):
        if action == 'update' and (ROOT / '.env.judge.local').exists():
            compose('stop', 'api', 'worker')
        setup_vm(update=action == 'update')
        start_app()
    elif action == 'start':
        save_settings()
        start_app()
    elif action == 'stop':
        compose('stop', 'api', 'worker')
        stop_vm()
    elif action == 'status':
        run('systemctl', 'status', '--no-pager', SERVICE)
        wait_for_judge(settings()['JUDGE_AUTH_TOKEN'])
        compose('ps')
    elif action == 'test':
        verify()


if __name__ == '__main__':
    main()
