#!/usr/bin/env python3
"""Verify the isolated localhost release rehearsal, including real Java execution.

Creates two synthetic accounts in this rehearsal's database. No production host is accepted.
Credentials, email tokens, source code and execution output are never printed.
"""
import datetime
import http.cookiejar
import json
from pathlib import Path
import re
import ssl
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
COMPOSE = ['docker', 'compose', '--env-file', '.env.deploy.local', '-f', 'deploy/compose.yml', '-f', 'deploy/compose.local.yml']
BASE = 'https://localhost:8443/api/v1'
PASSWORD = 'local-release-verification-2026!'


def compose(*args):
    subprocess.run([*COMPOSE, *args], cwd=ROOT, check=True)


def sql(statement):
    return subprocess.check_output([*COMPOSE, 'exec', '-T', 'db', 'psql', '-U', 'postgres', '-d', 'codeduel', '-Atc', statement], cwd=ROOT, text=True).strip()


def until(action, predicate=lambda value: bool(value), timeout=90):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        try:
            value = action()
            if predicate(value):
                return value
        except (OSError, urllib.error.HTTPError):
            pass
        time.sleep(0.5)
    raise AssertionError('Timed out waiting for the expected rehearsal state')


cert = ROOT / '.cache/deploy/root.crt'
cert.parent.mkdir(parents=True, exist_ok=True)
compose('cp', 'ingress:/data/caddy/pki/authorities/local/root.crt', str(cert))
context = ssl.create_default_context(cafile=str(cert))


class Client:
    def __init__(self):
        self.cookies = http.cookiejar.CookieJar()
        self.http = urllib.request.build_opener(urllib.request.HTTPSHandler(context=context), urllib.request.HTTPCookieProcessor(self.cookies))

    def read(self, path):
        with self.http.open(BASE + path, timeout=10) as result:
            return json.load(result)

    def write(self, path, body=None, method='POST'):
        csrf = self.read('/auth/csrf')
        request = urllib.request.Request(BASE + path, method=method,
            headers={'Content-Type': 'application/json', csrf['headerName']: csrf['token']},
            data=json.dumps(body).encode() if body is not None else b'')
        with self.http.open(request, timeout=20) as result:
            content = result.read()
            return json.loads(content) if content else None

    def expect_error(self, code, action):
        try:
            action()
        except urllib.error.HTTPError as error:
            assert error.code == code, f'Expected HTTP {code}, received {error.code}'
        else:
            raise AssertionError(f'Expected HTTP {code}')


def mail_token(email, subject):
    def search():
        with urllib.request.urlopen('http://127.0.0.1:8027/api/v1/messages', timeout=5) as response:
            messages = json.load(response)['messages']
        for message in messages:
            if message['Subject'] == subject and any(person['Address'] == email for person in message['To']):
                with urllib.request.urlopen('http://127.0.0.1:8027/api/v1/message/' + message['ID'], timeout=5) as response:
                    text = json.load(response)['Text']
                match = re.search(r'#token=([A-Za-z0-9_-]{43})', text)
                if match:
                    return match.group(1)
        return None
    return until(search, timeout=45)


def judged(client, submission):
    return until(lambda: client.read('/submissions/' + submission['submissionId']), lambda detail: detail['verdict'] != 'PENDING', timeout=120)


one, two = Client(), Client()
info = until(lambda: one.read('/system'))
assert info['judgeMode'] == 'GO_JUDGE' and info['language'] == 'JAVA_21'
assert len(one.read('/problems')) >= 15
suffix = str(time.time_ns())[-12:]
for client, name in ((one, 'va' + suffix), (two, 'vb' + suffix)):
    email = name + '@example.invalid'
    client.write('/auth/register', {'username': name, 'email': email, 'password': PASSWORD})
    client.write('/auth/login', {'username': name, 'password': PASSWORD})
    token = mail_token(email, 'Verify your CodeDuel email')
    client.write('/auth/verify-email', {'token': token})
    client.expect_error(400, lambda: client.write('/auth/verify-email', {'token': token}))
assert one.read('/auth/me')['emailVerified']
assert any(cookie.secure and cookie.name == 'CODEDUEL_SESSION' for cookie in one.cookies)
identity = one.read('/auth/me')
compose('restart', 'api')
assert until(lambda: one.read('/auth/me'))['id'] == identity['id']
print('Passed: trusted TLS, secure persistent sessions, SMTP delivery, one-time email verification.', flush=True)

one.write('/matchmaking/queue')
two.write('/matchmaking/queue')
match = until(lambda: one.read('/matchmaking'), lambda state: state['status'] == 'MATCHED')
battle = match['battleId']
one.write(f'/battles/{battle}/ready')
two.write(f'/battles/{battle}/ready')
snapshot = until(lambda: one.read(f'/battles/{battle}'), lambda value: value['problem'] is not None)
other = two.read(f'/battles/{battle}')
assert snapshot['problem']['versionId'] == other['problem']['versionId']
assert snapshot['startsAt'] == other['startsAt'] and snapshot['endsAt'] == other['endsAt']
start = datetime.datetime.fromisoformat(snapshot['startsAt'].replace('Z', '+00:00')).timestamp()
until(lambda: time.time() >= start)
source = next(problem['referenceSource'] for problem in json.loads((ROOT / 'CodeDuelBackend/src/main/resources/problems/catalog.json').read_text()) if problem['slug'] == snapshot['problem']['slug'])
bad = one.write(f'/battles/{battle}/run', {'language': 'JAVA_21', 'sourceCode': 'public class Main { this does not compile }'})
compilation = judged(one, bad)
assert compilation['verdict'] == 'COMPILATION_ERROR' and 'Main.java' in compilation['compilerDiagnostic']
compose('stop', 'worker')
try:
    early = one.write(f'/battles/{battle}/submissions', {'language': 'JAVA_21', 'sourceCode': source})
    later = two.write(f'/battles/{battle}/submissions', {'language': 'JAVA_21', 'sourceCode': source})
    assert early['sequence'] < later['sequence']
    assert one.read('/submissions/' + early['submissionId'])['verdict'] == 'PENDING'
finally:
    compose('up', '-d', '--no-deps', 'worker')
assert judged(one, early)['verdict'] == 'ACCEPTED'
assert judged(two, later)['verdict'] == 'ACCEPTED'
result = until(lambda: one.read(f'/battles/{battle}'), lambda state: state['status'] == 'FINISHED')
assert result['winnerId'] == identity['id']
assert sorted(player['delta'] for player in result['players']) == [-20, 20]
assert sql(f"SELECT count(*) FROM rating_events WHERE battle_id='{battle}'") == '2'
assert any(event['type'] == 'BATTLE_FINISHED' for event in one.read(f'/battles/{battle}/timeline'))
two.expect_error(404, lambda: two.read('/submissions/' + early['submissionId']))
assert len(one.read('/users/' + identity['username'] + '/rating-history')) == 1
print('Passed: ranked Java duel, compiler feedback, durable jobs across worker restart, receipt order, Elo and replay.', flush=True)

practice = one.write('/practice/' + snapshot['problem']['slug'] + '/submissions', {'language': 'JAVA_21', 'sourceCode': source})
assert judged(one, practice)['verdict'] == 'ACCEPTED'
assert one.read('/auth/me')['rankedGames'] == 1
rematch = one.write(f'/battles/{battle}/rematch')
assert len(one.read('/battles/' + rematch['battleId'])['players']) == 1
assert two.read('/matchmaking')['status'] == 'IDLE'
assert two.write(f'/battles/{battle}/rematch')['battleId'] == rematch['battleId']
one.write('/battles/' + rematch['battleId'] + '/forfeit')
print('Passed: standalone practice and a private rematch with explicit consent.', flush=True)

operator = Client()
operator.write('/auth/login', {'username': 'operator', 'password': (ROOT / '.secrets/deploy/bootstrap_password').read_text()})
draft = {'slug': 'release-check-' + suffix, 'title': 'Beacon Doubler', 'difficulty': 'EASY', 'tags': ['Arithmetic'],
    'statement': 'An observatory doubles a beacon reading. Print twice the supplied integer.',
    'constraints': '-1000000 <= reading <= 1000000', 'inputFormat': 'One integer reading.', 'outputFormat': 'Print twice reading.',
    'starterCode': 'public class Main { public static void main(String[] args) {} }',
    'referenceSource': 'public class Main { public static void main(String[] args) { var in = new java.util.Scanner(System.in); System.out.println(2 * in.nextLong()); } }',
    'timeLimitMs': 2000, 'memoryLimitMb': 256, 'durationSeconds': 600,
    'tests': [{'input': '7\n', 'output': '14\n', 'public': True}, {'input': '-999\n', 'output': '-1998\n', 'public': False}]}
version = operator.write('/admin/problems', draft)['versionId']
operator.write('/admin/problems/versions/' + version, draft, 'PUT')
operator.expect_error(409, lambda: operator.write('/admin/problems/versions/' + version + '/publish'))
review = operator.write('/admin/problems/versions/' + version + '/review')
assert judged(operator, review)['verdict'] == 'ACCEPTED'
operator.expect_error(409, lambda: operator.write('/admin/problems/versions/' + version, draft, 'PUT'))
operator.write('/admin/problems/versions/' + version + '/publish')
assert one.read('/problems/' + draft['slug'])['versionId'] == version
problem = operator.read('/admin/problems/versions/' + version)['problemId']
operator.write('/admin/problems/' + problem + '/retire')
one.expect_error(404, lambda: one.read('/problems/' + draft['slug']))
print('Passed: audited administrator draft, actual reference judging, frozen review, publication and retirement.', flush=True)

second_profile = two.read('/auth/me')
email = 'vb' + suffix + '@example.invalid'
two.write('/auth/forgot-password', {'email': email})
reset = mail_token(email, 'Reset your CodeDuel password')
guest = Client()
guest.write('/auth/reset-password', {'token': reset, 'password': 'new-local-verification-password!'})
two.expect_error(401, lambda: two.read('/auth/me'))
guest.expect_error(400, lambda: guest.write('/auth/reset-password', {'token': reset, 'password': PASSWORD}))
guest.write('/auth/login', {'username': second_profile['username'], 'password': 'new-local-verification-password!'})
guest.write('/auth/delete-account', {'password': 'new-local-verification-password!'})
guest.expect_error(401, lambda: guest.read('/auth/me'))
assert sql(f"SELECT count(*) FROM submissions WHERE user_id='{second_profile['id']}' AND source_deleted_at IS NULL") == '0'
assert sql(f"SELECT count(*) FROM rating_events WHERE battle_id='{battle}'") == '2'
permissions = sql("SELECT has_table_privilege('codeduel_worker','users','SELECT'),has_table_privilege('codeduel_worker','spring_session_attributes','SELECT'),has_table_privilege('codeduel_worker','mail_outbox','SELECT'),has_column_privilege('codeduel_worker','submissions','source_code','UPDATE'),has_schema_privilege('codeduel_api','public','CREATE')")
assert permissions == 'f|f|f|f|f', permissions
print('Passed: SMTP password reset, session revocation, source erasure and restricted database roles.', flush=True)
print('Local release verification completed. No hosting resources or public listeners were created.')
