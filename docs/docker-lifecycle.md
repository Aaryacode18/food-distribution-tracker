# Docker Image and Container Lifecycle — Task 11

Evidence that the Food Distribution Tracker runs as a container: image built
from source, started, inspected, stopped, restarted, and removed.

**Machine:** Docker Desktop 4.93 on macOS (arm64), engine 29.8.1
**Image:** `food-distribution-tracker:1.0` (`bd28a52e448b`)
**Container:** `food-tracker`, host port **8082** → container port 8080

Port 8082 is deliberate: 8080 is Jenkins and 8081 is the host Tomcat, so the
container never collides with either.

---

## 1. The Dockerfile

Two stages, because the image must build the WAR itself rather than trusting
whatever is in `target/` on the build machine:

```
Stage 1  maven:3.9-eclipse-temurin-21
         copy pom.xml + src/
         mvn -B clean package          <- compiles AND runs the 7 unit tests
                                          (the default surefire config
                                           excludes the Selenium suite)

Stage 2  tomcat:11-jre21-temurin
         install curl                  <- only so HEALTHCHECK can probe
         copy the WAR from stage 1 into webapps/
         EXPOSE 8080
         HEALTHCHECK curl the app every 10s
```

Why these base images:

| Choice | Reason |
|---|---|
| `tomcat:11-jre21-temurin` | Matches the host Tomcat 11 the app is already deployed to, and JRE 21 matches `maven.compiler.release=21` in `pom.xml`. |
| `maven:3.9-eclipse-temurin-21` | Same JDK as the runtime, so bytecode built in stage 1 is guaranteed to load in stage 2. |
| Named context, not `ROOT` | The container URL keeps the same shape as the host URL, so `-Dapp.url=...` works against either. |

`.dockerignore` keeps `.git`, `target/`, `docs/` and the markdown out of the
build context, so a rebuild does not invalidate the cache when only docs change.

---

## 2. Build

```console
$ docker build -t food-distribution-tracker:1.0 .
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building war: /build/target/food-distribution-tracker.war
[INFO] BUILD SUCCESS
...
#14 naming to docker.io/library/food-distribution-tracker:1.0 done
```

```console
$ docker images food-distribution-tracker
IMAGE                           ID             DISK USAGE   CONTENT SIZE   EXTRA
food-distribution-tracker:1.0   bd28a52e448b        524MB          131MB
```

524 MB on disk, 131 MB of actual content — the difference is shared base-image
layers, which is why a second image from the same base costs almost nothing.

---

## 3. Run

```console
$ docker run -d --name food-tracker -p 8082:8080 food-distribution-tracker:1.0
5b2f65030d7acda4b0af2f1dd07628637380c97b80169d955651458b5fb16797

$ docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'
NAMES          IMAGE                           STATUS                    PORTS
food-tracker   food-distribution-tracker:1.0   Up 12 seconds (healthy)   0.0.0.0:8082->8080/tcp, [::]:8082->8080/tcp
```

`-p 8082:8080` publishes the container's 8080 on the host's 8082.

### Health check from outside

```console
$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8082/food-distribution-tracker/
200

$ curl -s http://localhost:8082/food-distribution-tracker/ | grep -i -m2 "title\|Delivery Summary"
<title>Food Distribution Tracker</title>
<h2>Delivery Summary</h2>
```

The same page the host Tomcat serves, from a container.

```console
$ docker inspect -f '{{.State.Health.Status}}' food-tracker
healthy
```

The Dockerfile's `HEALTHCHECK` marks the container `healthy` once the app
answers; `docker ps` shows `(healthy)` in the status column.

---

## 4. Logs

```console
$ docker logs food-tracker 2>&1 | tail -5
06-Oct-2026 10:14:27.162 INFO [main] ... Starting Servlet engine: [Apache Tomcat/11.0.26]
06-Oct-2026 10:14:27.173 INFO [main] ... Deploying web application archive [/usr/local/tomcat/webapps/food-distribution-tracker.war]
06-Oct-2026 10:14:27.354 INFO [main] ... Deployment ... has finished in [181] ms
06-Oct-2026 10:14:27.358 INFO [main] ... Starting ProtocolHandler ["http-nio-8080"]
06-Oct-2026 10:14:27.375 INFO [main] ... Server startup in [241] milliseconds
```

Tomcat started and deployed the WAR in 181 ms. Logs go to stdout, which is the
container-native place for them — no log files to rotate inside the image.

---

## 5. Inspect

```console
$ docker inspect -f 'Image={{.Image}} RestartPolicy={{.HostConfig.RestartPolicy.Name}} Health={{.State.Health.Status}}' food-tracker
Image=sha256:bd28a52e448bda6a4715b4992f1e419ecc0d230e97135c418c02107cfd65034d RestartPolicy=no Health=healthy
```

`RestartPolicy=no` is the Docker default: the container stops when told to and
does not come back on its own. Task 14's provisioning sets a policy where a
service should survive a restart.

---

## 6. Stop, start, restart

```console
$ docker stop food-tracker
food-tracker

$ docker ps -a --filter name=food-tracker --format 'table {{.Names}}\t{{.Status}}'
NAMES          STATUS
food-tracker   Exited (143) Less than a second ago

$ docker start food-tracker
food-tracker

$ docker ps --filter name=food-tracker --format 'table {{.Names}}\t{{.Status}}'
NAMES          STATUS
food-tracker   Up 8 seconds (healthy)

$ docker restart food-tracker
food-tracker

$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8082/food-distribution-tracker/
200
```

Exit code 143 is `SIGTERM`, which is what `docker stop` sends — a clean
shutdown, not a crash. After a restart the application answers `200` again.

Note what survives and what does not: the container's filesystem changes are
gone on restart only if the container was recreated; data written *inside* the
running container persists across stop/start. The application holds state in
the HTTP session, so any delivery data is lost whenever Tomcat restarts — that
is the application's own limitation (US-10), not Docker's.

---

## 7. Remove

```console
$ docker rm -f food-tracker
food-tracker

$ docker ps -a --format 'table {{.Names}}\t{{.Status}}'
NAMES     STATUS

$ docker images food-distribution-tracker
IMAGE                           ID             DISK USAGE   CONTENT SIZE   EXTRA
food-distribution-tracker:1.0   bd28a52e448b        524MB          131MB
```

The container is gone; the image remains, so restarting a new container from
the same image is instant. The image is only deleted with `docker rmi`, which
was deliberately not run.

---

## 8. Lifecycle summary

```
docker build ──→ image bd28a52e448b (:1.0)
                     │
                 docker run ──→ container food-tracker (healthy, :8082)
                     │              │
                     │          docker logs / docker inspect
                     │              │
                     │          docker stop → Exited (143)
                     │          docker start → Up (healthy)
                     │          docker restart → HTTP 200
                     │              │
                 docker rm -f ──→ container gone, image kept
```

## 9. Reproduce it

```bash
docker build -t food-distribution-tracker:1.0 .
docker run -d --name food-tracker -p 8082:8080 food-distribution-tracker:1.0
open http://localhost:8082/food-distribution-tracker/
docker logs -f food-tracker        # Ctrl-C detaches, does not stop it
docker rm -f food-tracker
```
