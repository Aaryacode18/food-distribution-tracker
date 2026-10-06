# Configuration Management — Task 13

What the Food Distribution Tracker needs from a server, expressed as an
**Ansible inventory + playbook**, plus the log of its first execution.

**Tool:** Ansible Core 2.21.4 (`brew install ansible`)
**Files:** [`ansible/inventory.ini`](../ansible/inventory.ini),
[`ansible/site.yml`](../ansible/site.yml),
[`ansible/ansible.cfg`](../ansible/ansible.cfg)
**Target node:** container `fdt-node` (Ubuntu 24.04, systemd as PID 1)
**First execution log:** [`evidence/ansible-first-run.log`](evidence/ansible-first-run.log)

---

## 1. Configuration specification

Every prerequisite the application has, and where each one is satisfied.

| Category | Requirement | Why | Satisfied by |
|---|---|---|---|
| **Package** | `openjdk-21-jre-headless` | The WAR is compiled with `maven.compiler.release=21`; an older JRE refuses to load it. | `apt` task |
| **Package** | `tomcat10` | Serves the JSP over HTTP. Ubuntu 24.04's packaged Tomcat, so it arrives with a unit file and a `tomcat` service account. | `apt` task |
| **Package** | `curl` | Health checks run from inside the node. | `apt` task |
| **User** | `fdt` (system, no login shell, home `/opt/food-distribution-tracker`) | The release history should not belong to root or to the Tomcat user; one account owns the application's files. | `user` task |
| **Folder** | `/opt/food-distribution-tracker/releases` | Every release ever deployed stays on disk, which is what makes rollback possible. | `file` task |
| **File** | `food-distribution-tracker-<version>.war` in the release folder | The artifact being deployed, owned by `fdt`, mode `0644`. | `copy` task |
| **File** | `/var/lib/tomcat10/webapps/food-distribution-tracker.war` | The running webapp. Changing this file is what a deploy *is*. | `copy` task |
| **Port** | `8080` in the node, published as `8083` on the host | 8080 is Jenkins and 8081 is the host Tomcat on this machine, so the node takes 8083. | Tomcat's `server.xml`; `docker run -p 8083:8080` |
| **Service** | `tomcat10` enabled at boot, active now | A configured node must survive a reboot and must be running when the playbook leaves it. | `systemd` task |
| **Network check** | `GET /food-distribution-tracker/` returns `200` | Configuration is only "done" when the application answers. | second play, `uri` task |

Not installed deliberately: no database (state is in the HTTP session, US-10),
no sshd (Ansible reaches the node with `docker exec`), no web server in front
of Tomcat (out of scope for the MVP).

---

## 2. The node itself

The node is a container, so "a clean server" is reproducible in one command.
It contains only what Ansible cannot install later — python3 (Ansible's
modules need it), curl, and systemd (so `tomcat10` is a real service that can
be enabled, started and restarted, instead of a `java` process someone
started by hand):

```dockerfile
# ansible/node-image/Dockerfile
FROM ubuntu:24.04
RUN apt-get update && apt-get install -y --no-install-recommends \
        python3 curl ca-certificates systemd systemd-sysv \
    && rm -rf /var/lib/apt/lists/*
CMD ["/sbin/init"]
```

```bash
docker build -t fdt-node:ubuntu24 ansible/node-image
docker run -d --privileged --name fdt-node -p 8083:8080 fdt-node:ubuntu24
```

`--privileged` is required for systemd; `systemctl is-system-running` returns
`running` inside it.

---

## 3. Inventory

```ini
[fdt_nodes]
fdt-node ansible_connection=docker ansible_python_interpreter=/usr/bin/python3 ansible_become=false
```

| Setting | Why |
|---|---|
| `ansible_connection=docker` | Modules run through `docker exec`. The node has no sshd, and starting one would be extra configuration to demo a demo. |
| `ansible_python_interpreter=/usr/bin/python3` | Set explicitly rather than auto-detected, because the image is minimal. |
| `ansible_become=false` | `docker exec` already runs as root, and the container has no `sudo` for Ansible to become with. |

---

## 4. What the playbook does

`ansible/site.yml` — two plays.

**Play 1 — configure**

```
Refresh the apt cache
Install the packages the application needs      openjdk-21-jre-headless, tomcat10, curl
Create the service account for the application fdt
Create the release directory                   /opt/food-distribution-tracker/releases
Store release 1.4.0 in the release history
Deploy release 1.4.0 as the running webapp     → notifies handler
Enable Tomcat at boot and make sure it is running
        ↓ handlers flush here
RUNNING HANDLER [Restart tomcat]               only if the deploy changed something
```

**Play 2 — verify**

```
Wait until the application answers              GET http://localhost:8083/food-distribution-tracker/ → 200
```

The health check is a separate play on purpose: Ansible flushes handlers at
the end of a play, so the Tomcat restart always happens *before* the check.

### Idempotency, and where it comes from

Each task is written so that a second run finds the system already in the
desired state:

| Task | Why the second run reports `ok` |
|---|---|
| apt | `state: present` — already installed |
| user / directory | already exists with the right owner and mode |
| copy (release, webapp) | compares SHA-256; identical files report `ok`, not `changed` |
| systemd | `state: started` on an active service is `ok` |
| handler | runs **only** when a task reported `changed` — so no restart on an unchanged run |
| uri | a `GET` is never a change |

Second run result: `changed=0`. Evidence in
[`evidence/ansible-second-run.log`](evidence/ansible-second-run.log) (Task 14).

---

## 5. Rollback is just a variable

Both releases sit in `ansible/files/releases/`:

| Release | WAR SHA-256 |
|---|---|
| `1.4.0` (current) | `75fcff65ac26d8e72cd6be48a0bfe3430f515681baad3b454028dd9fca8abb2d` |
| `1.3.0` (previous stable, tag `v1.3.0`) | `13f15d24f3fcfdcd7abc0a7d7ada8b1722a8703583018893be5e360556fbbac3` |

```bash
ansible-playbook site.yml                          # deploy 1.4.0
ansible-playbook site.yml -e release_version=1.3.0  # roll back
```

The `copy` into `webapps/` reports `changed` (different checksum) → the
handler restarts Tomcat → the previous release is live. No separate rollback
script, because a rollback is a deploy with a different argument.

The WARs are build artifacts and are git-ignored. To recreate them:

```bash
cp target/food-distribution-tracker.war ansible/files/releases/food-distribution-tracker-1.4.0.war
git archive v1.3.0 | tar -x -C /tmp/rel130 && (cd /tmp/rel130 && mvn -q clean package -DskipTests)
cp /tmp/rel130/target/food-distribution-tracker.war ansible/files/releases/food-distribution-tracker-1.3.0.war
```

---

## 6. First execution

```console
$ cd ansible && ansible-playbook site.yml

PLAY [Configure the Food Distribution Tracker node] ****************************

TASK [Gathering Facts]                       ok: [fdt-node]
TASK [Refresh the apt cache]                 ok: [fdt-node]
TASK [Install the packages the application needs]
                                             changed: [fdt-node]
TASK [Create the service account for the application]
                                             changed: [fdt-node]
TASK [Create the release directory]          changed: [fdt-node]
TASK [Store release 1.4.0 in the release history]
                                             changed: [fdt-node]
TASK [Deploy release 1.4.0 as the running webapp]
                                             changed: [fdt-node]
TASK [Enable Tomcat at boot and make sure it is running]
                                             changed: [fdt-node]
RUNNING HANDLER [Restart tomcat]             changed: [fdt-node]

PLAY [Verify the application answers] ***********************
TASK [Wait until the application answers]    ok: [fdt-node -> localhost]

PLAY RECAP **********************************************************
fdt-node: ok=11 changed=7 unreachable=0 failed=0 skipped=0 rescued=0 ignored=0
```

Verified on the node after the run:

```console
$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8083/food-distribution-tracker/
200
$ docker exec fdt-node bash -c "systemctl is-enabled tomcat10; systemctl is-active tomcat10"
enabled
active
$ docker exec fdt-node id fdt
uid=996(fdt) gid=996(fdt) groups=996(fdt)
```

---

## 7. Running it

```bash
# once: build and start a clean node
docker build -t fdt-node:ubuntu24 ansible/node-image
docker run -d --privileged --name fdt-node -p 8083:8080 fdt-node:ubuntu24

# every time
cd ansible
ansible-playbook site.yml                       # deploy the current release
ansible-playbook site.yml                       # run again: changed=0
ansible-playbook site.yml -e release_version=1.3.0   # roll back
```

---

## 8. Limitations

| Limitation | Consequence |
|---|---|
| One node, defined as a Docker container | Proves the playbook, not a cloud fleet. The inventory is the only thing that would change for real hosts (plus `ansible_connection=ssh`). |
| Packages come from Ubuntu 24.04's own repos | Pinning exact versions is not done, so a rebuilt node could get a newer Tomcat patch. |
| Rollback restores the WAR but keeps the same session state | Data is in the HTTP session and dies with Tomcat — US-10, unchanged by this work. |
| `--privileged` node container | Needed for systemd; not something you would grant a production host lightly. |
