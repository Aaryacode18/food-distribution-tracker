# Automated Provisioning and Reliability Validation — Task 14

A clean node is provisioned by Ansible, proven idempotent, health-checked, and
then taken through a real failure and rollback.

**Node:** container `fdt-node` — bare Ubuntu 24.04 + systemd, built from
[`ansible/node-image/Dockerfile`](../ansible/node-image/Dockerfile)
**Playbook:** [`ansible/site.yml`](../ansible/site.yml)
**Reachable at:** <http://localhost:8083/food-distribution-tracker/>

| Evidence file | What it shows |
|---|---|
| [`evidence/ansible-provision-run1.log`](evidence/ansible-provision-run1.log) | First run on a clean node: `changed=7` |
| [`evidence/ansible-second-run.log`](evidence/ansible-second-run.log) | Second run: `changed=0` |
| [`evidence/health-check.txt`](evidence/health-check.txt) | HTTP checks and WAR checksums, before and after rollback |
| [`evidence/ansible-bad-release.log`](evidence/ansible-bad-release.log) | The broken release fails the health gate: exit 2 |
| [`evidence/ansible-rollback.log`](evidence/ansible-rollback.log) | Rollback run: `changed=2`, health gate passes |

---

## 1. Provisioning a clean node

The node starts as nothing — no Java, no Tomcat, no application:

```console
$ docker rm -f fdt-node; docker run -d --privileged --name fdt-node -p 8083:8080 fdt-node:ubuntu24
$ docker exec fdt-node bash -c "which java tomcat10; ls /opt"
                        ← no output: neither is installed
$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8083/food-distribution-tracker/
000                     ← nothing is listening
```

Then one command configures it:

```console
$ ansible-playbook site.yml

TASK [Install the package requirements]              changed: [fdt-node]
TASK [Create the service account for the application] changed: [fdt-node]
TASK [Create the release directory]                  changed: [fdt-node]
TASK [Store release 1.4.0 in the release history]    changed: [fdt-node]
TASK [Deploy release 1.4.0 as the running webapp]    changed: [fdt-node]
TASK [Enable Tomcat at boot and make sure it is running]
                                                    changed: [fdt-node]
RUNNING HANDLER [Restart tomcat]                     changed: [fdt-node]
TASK [Wait until the application answers]            ok: [fdt-node -> localhost]

PLAY RECAP *****************************************************************
fdt-node: ok=11 changed=7 unreachable=0 failed=0 skipped=0 rescued=0 ignored=0
```

**Provisioned node:** `ok=11 changed=7 failed=0` — packages installed, user
`fdt` created, release folder created, WAR deployed, Tomcat enabled and
active, application answering.

---

## 2. Idempotency — running it a second time

```console
$ ansible-playbook site.yml

TASK [Refresh the apt cache]                         ok: [fdt-node]
TASK [Install the package requirements]              ok: [fdt-node]
TASK [Create the service account for the application] ok: [fdt-node]
TASK [Create the release directory]                  ok: [fdt-node]
TASK [Store release 1.4.0 in the release history]    ok: [fdt-node]
TASK [Deploy release 1.4.0 as the running webapp]    ok: [fdt-node]
TASK [Enable Tomcat at boot and make sure it is running]
                                                    ok: [fdt-node]
TASK [Wait until the application answers]            ok: [fdt-node -> localhost]

PLAY RECAP *****************************************************************
fdt-node: ok=10 changed=0 unreachable=0 failed=0 skipped=0 rescued=0 ignored=0
```

**`changed=0`.** Three things worth noticing:

1. Every task is `ok`, none is `changed` — apt did not reinstall, the file
   tasks compared checksums and found the same bytes.
2. There is **no `RUNNING HANDLER`** line. The restart handler only fires when
   a task reports `changed`, so Tomcat was not restarted on the second run.
3. `ok` went from 11 to 10 — that one missing task *is* the handler.

---

## 3. Health check

Recorded in [`evidence/health-check.txt`](evidence/health-check.txt):

```console
$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8083/food-distribution-tracker/
200
$ curl -s http://localhost:8083/food-distribution-tracker/ | grep '<title>'
    <title>Food Distribution Tracker</title>
$ docker exec fdt-node systemctl is-active tomcat10
active
$ docker exec fdt-node sha256sum /var/lib/tomcat10/webapps/food-distribution-tracker.war
75fcff65ac26d8e72cd6be48a0bfe3430f515681baad3b454028dd9fca8abb2d
$ shasum -a 256 ansible/files/releases/food-distribution-tracker-1.4.0.war
75fcff65ac26d8e72cd6be48a0bfe3430f515681baad3b454028dd9fca8abb2d   ← identical
```

The bytes served are the bytes of release 1.4.0, not something left over from
an earlier run.

---

## 4. Rollback and recovery

A rollback that is only run against a system that never broke proves nothing.
So a broken release was produced first.

### 4.1 The bad release

`1.5.0` is release 1.4.0 with one line added to `index.jsp`:

```jsp
<% int broken = ; %>
```

That is a realistic defect: it compiles nowhere, so the JSP cannot be
compiled and the request ends in a 500. It is built with:

```bash
cd /tmp/badrel && jar xf .../food-distribution-tracker-1.4.0.war
printf '\n<%% int broken = ; %%>\n' >> index.jsp
jar cf .../food-distribution-tracker-1.5.0.war .
```

### 4.2 Deploying it fails the gate

```console
$ ansible-playbook site.yml -e release_version=1.5.0

TASK [Deploy release 1.5.0 as the running webapp]   changed: [fdt-node]
RUNNING HANDLER [Restart tomcat]                    changed: [fdt-node]
TASK [Wait until the application answers]
FAILED - RETRYING: Wait until the application answers (14 retries left).
... 15 attempts ...
[ERROR]: Status code was 500 and not [200]: HTTP Error 500:
fatal: [fdt-node -> localhost]: FAILED!

PLAY RECAP *****************************************************************
fdt-node: ok=10 changed=3 unreachable=0 failed=1 skipped=0 rescued=0 ignored=0
PLAYBOOK_EXIT=2
```

Manual confirmation of the broken state:

```console
$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8083/food-distribution-tracker/
500
$ docker exec fdt-node sha256sum /var/lib/tomcat10/webapps/food-distribution-tracker.war
2474413430e4070f867f576d5b1a032d2d54a4a0f82a48b2cd1fdd083bf1792a   ← the broken WAR
```

**The playbook exits non-zero when the node it produced is not healthy.**
That is the property that makes the rollback below worth having.

### 4.3 Rolling back to the previous stable release

Rollback is the same playbook with a different argument:

```console
$ ansible-playbook site.yml -e release_version=1.4.0

TASK [Deploy release 1.4.0 as the running webapp]   changed: [fdt-node]
RUNNING HANDLER [Restart tomcat]                    changed: [fdt-node]
TASK [Wait until the application answers]           ok: [fdt-node -> localhost]

PLAY RECAP *****************************************************************
fdt-node: ok=11 changed=2 unreachable=0 failed=0 skipped=0 rescued=0 ignored=0
PLAYBOOK_EXIT=0
```

`changed=2`: the WAR file was replaced (checksum differed from 1.5.0) and the
handler restarted Tomcat. Nothing else was touched — packages, user and
folders were already correct, even after the failed run.

### 4.4 Recovery confirmed

```console
$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8083/food-distribution-tracker/
200
$ docker exec fdt-node sha256sum /var/lib/tomcat10/webapps/food-distribution-tracker.war
75fcff65ac26d8e72cd6be48a0bfe3430f515681baad3b454028dd9fca8abb2d   ← 1.4.0 again
$ docker exec fdt-node systemctl is-active tomcat10
active
$ docker exec fdt-node systemctl is-enabled tomcat10
enabled
$ docker exec fdt-node ls /opt/food-distribution-tracker/releases/
food-distribution-tracker-1.4.0.war
food-distribution-tracker-1.5.0.war     ← the bad release, kept for forensics
```

The service came back by itself (the handler restarted it), the bytes are the
stable release's again, and both releases remain on disk — the broken one is
kept rather than deleted, because that is what you want when you are asking
*what went wrong*.

---

## 5. The whole sequence

```
clean node ──ansible-playbook──→ provisioned (changed=7) ──→ HTTP 200
                                     │
                 ansible-playbook ───┘──→ idempotent (changed=0), no restart
                                     │
      deploy 1.5.0 (broken JSP) ─────┘──→ HTTP 500, playbook exits 2
                                     │
      -e release_version=1.4.0 ──────┘──→ HTTP 200, checksum matches 1.4.0
```

## 6. Reproduce it

```bash
docker rm -f fdt-node
docker run -d --privileged --name fdt-node -p 8083:8080 fdt-node:ubuntu24

cd ansible
ansible-playbook site.yml                          # provision: changed=7
ansible-playbook site.yml                          # idempotency: changed=0
ansible-playbook site.yml -e release_version=1.5.0 # bad release: fails, exit 2
ansible-playbook site.yml -e release_version=1.4.0 # rollback: changed=2, exit 0
open http://localhost:8083/food-distribution-tracker/
```

## 7. Limitations

| Limitation | Consequence |
|---|---|
| The node is a local container, not a cloud VM | Proves the playbook and the workflow, not a provider-specific launch. |
| The bad release was hand-made | The *mechanism* (health gate → non-zero exit → rollback) is real; the defect was chosen to be unambiguous. |
| Rollback does not restore data | State lives in the HTTP session (US-10), so a Tomcat restart loses deliveries regardless of which WAR is deployed. |
| One node | Nothing demonstrates rolling a fleet or a blue/green cutover. |
