#!/bin/sh
set -eu
[ "$(id -u)" = 0 ] || { echo 'Run as root inside the execution VM.' >&2; exit 1; }
[ -f /tmp/codeduel-mount.yaml ] || { echo 'Copy mount.yaml to /tmp/codeduel-mount.yaml first.' >&2; exit 1; }
if findmnt -rn -t virtiofs,9p,fuse.sshfs | read -r _; then
  echo 'Refusing an execution VM with host filesystem mounts.' >&2
  exit 1
fi
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y --no-install-recommends openjdk-21-jdk-headless ca-certificates curl
case "$(uname -m)" in
  aarch64) artifact=linux_arm64; digest=dbc21dfdb9e72a6584b697ea1bb2f29e47349aa3ad8d1d75e70911539b0e03d7 ;;
  x86_64) artifact=linux_amd64; digest=509782f8959987256d99ac6dc8062ec593d776f291401c8e0c28bdc48dcd1880 ;;
  *) echo 'Unsupported execution architecture.' >&2; exit 1 ;;
esac
install -d -m 0755 /opt/codeduel-judge
curl --fail --location --proto '=https' --tlsv1.2 "https://github.com/criyle/go-judge/releases/download/v1.13.3/go-judge_1.13.3_${artifact}" -o /opt/codeduel-judge/go-judge.download
printf '%s  %s\n' "$digest" /opt/codeduel-judge/go-judge.download | sha256sum -c -
install -m 0755 /opt/codeduel-judge/go-judge.download /opt/codeduel-judge/go-judge
rm /opt/codeduel-judge/go-judge.download
install -m 0644 /tmp/codeduel-mount.yaml /opt/codeduel-judge/mount.yaml
cat > /etc/systemd/system/codeduel-judge.service <<'SERVICE'
[Unit]
Description=CodeDuel isolated Java executor
After=network.target
[Service]
Type=simple
WorkingDirectory=/opt/codeduel-judge
EnvironmentFile=/etc/codeduel-judge.env
ExecStart=/opt/codeduel-judge/go-judge -http-addr=0.0.0.0:5050 -mount-conf=/opt/codeduel-judge/mount.yaml -parallelism=2 -pre-fork=2 -container-cred-start=1536 -no-fallback -output-limit=4m -copy-out-limit=4m -open-file-limit=64 -file-timeout=5m -src-prefix=/nonexistent-codeduel-source -silent
Restart=on-failure
RestartSec=2
MemoryMax=2G
TasksMax=256
LimitNOFILE=4096
UMask=0077
[Install]
WantedBy=multi-user.target
SERVICE
java -version 2> /opt/codeduel-judge/java-version.txt
dpkg-query -W openjdk-21-jdk-headless > /opt/codeduel-judge/package-version.txt
systemctl daemon-reload
systemctl enable codeduel-judge
printf 'Installed pinned go-judge v1.13.3 and recorded the Java 21 package version.\n'
