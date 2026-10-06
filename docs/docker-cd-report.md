# Jenkins → Docker Continuous Deployment — Task 12

Evidence for the commit-to-container path: a push to `main` produces a
**versioned image**, publishes it to a **registry**, and starts a **fresh
container** — all only after the Selenium gate has passed.

**Build that proves it:** Jenkins build **#14**, commit `b852834`
**Registry:** local `registry:2` container on `localhost:5001`
**Result:** `Finished: SUCCESS`

---

## 1. The extended pipeline

```
Checkout → Compile → Unit Test → Package → Deploy to Staging
        → Browser Test → Deploy → Verify            ← Task 8/10, unchanged
        → Docker Build → Docker Publish
        → Docker Deploy → Verify Container           ← added by this task
```

The four new stages are appended **after** `Verify`, so the ordering
guarantee is preserved:

```
7 unit + 5 browser tests must be green
              │
        all pass ────── fail
            │               │
    build versioned image   nothing is published,
    push to registry        nothing is deployed
    replace container
    poll it until it answers
```

A container can never be published from a build the tests rejected, because
the stages that publish sit downstream of the gate.

### New build parameters

| Parameter | Default | Purpose |
|---|---|---|
| `RUN_DOCKER` | `true` | Turn the whole container path off if there is no Docker daemon |
| `DOCKER_REGISTRY` | `localhost:5001` | Where the versioned image is pushed |
| `DOCKER_IMAGE` | `food-distribution-tracker` | Image name (also the app context path inside the container) |
| `CONTAINER_NAME` | `food-tracker-ci` | Container replaced by each build |
| `CONTAINER_PORT` | `8082` | Host port — 8080 is Jenkins, 8081 is host Tomcat |

---

## 2. End-to-end run — build #14

Triggered by pushing `b852834` (the Jenkinsfile change). No button pressed;
the job polls `main` every two minutes.

### Docker Build — versioned tag

```
Building versioned image localhost:5001/food-distribution-tracker:build-14
+ docker build -t localhost:5001/food-distribution-tracker:build-14 \
               -t localhost:5001/food-distribution-tracker:latest .
...
[INFO] BUILD SUCCESS
```

Two tags on one image: the immutable `build-14` and the moving `latest`.

### Docker Publish — pushed to the registry

```
+ docker push localhost:5001/food-distribution-tracker:build-14
The push refers to repository [localhost:5001/food-distribution-tracker]
...
build-14: digest: sha256:... size: 856
+ docker push localhost:5001/food-distribution-tracker:latest
```

Registry contents after the push:

```console
$ curl -s http://localhost:5001/v2/_catalog
{"repositories":["food-distribution-tracker"]}

$ curl -s http://localhost:5001/v2/food-distribution-tracker/tags/list
{"name":"food-distribution-tracker","tags":["latest","build-test","build-14"]}
```

`build-test` is the throwaway tag from the manual dry-run before this was
wired into the pipeline — proving the registry flow worked before the
Jenkinsfile was changed to depend on it.

### Docker Deploy — a fresh container

```
Replacing food-tracker-ci with build 14 on port 8082
+ docker rm -f food-tracker-ci 2>/dev/null || true
+ docker run -d --name food-tracker-ci -p 8082:8080 --restart=always \
      localhost:5001/food-distribution-tracker:build-14
```

The previous container is removed and replaced — not restarted in place — so
each deploy starts from the image as it exists in the registry.

### Verify Container — it actually answers

```
Checking the container at http://localhost:8082/food-distribution-tracker/
+ curl --fail --silent http://localhost:8082/food-distribution-tracker/
Container answered after 2 attempt(s)
food-tracker-ci Up 3 seconds (health: starting) 0.0.0.0:8082->8080/tcp
+ exit 0
Finished: SUCCESS
```

Independent check after the build:

```console
$ docker ps --filter name=food-tracker-ci
NAMES             IMAGE                                               STATUS                    PORTS
food-tracker-ci   localhost:5001/food-distribution-tracker:build-14   Up 17 seconds (healthy)   0.0.0.0:8082->8080/tcp

$ curl -s -o /dev/null -w '%{http_code}' http://localhost:8082/food-distribution-tracker/
200
```

---

## 3. What is proven, point by point

| Requirement | Evidence |
|---|---|
| Versioned Docker image | `:build-14` built by the pipeline from the commit under test |
| Published to a registry | `localhost:5001` catalog and tag list, shown above |
| Deployed automatically after successful tests | Stage order: gate → build → push → deploy; run only after 7 + 5 tests passed |
| Fresh container per deploy | `docker rm -f` then `docker run` in the Deploy stage |
| Commit-to-container | Push `b852834` → poll (≤2 min) → build #14 → container serving on `:8082` |

---

## 4. Limitations

| Limitation | Consequence |
|---|---|
| Registry is local (`registry:2` on this machine) | No credentials, no image survives removing the registry container. Swapping in Docker Hub is a one-parameter change (`DOCKER_REGISTRY`). |
| The host Tomcat deploy still happens too | By design: the browser gate needs an app on 8081 to test against. The container is an additional target, not a replacement. |
| One container name, one port | Two overlapping builds would race to replace `food-tracker-ci`. Acceptable at this concurrency, same as the staging context. |
| `latest` is a moving tag | Always re-deploy `build-N` for anything you might need to identify later. |

## 5. Reproduce it

```bash
# once
docker run -d --name local-registry -p 5001:5000 --restart=always registry:2

# every push to main
#   Jenkins: Checkout → ... → Verify → Docker Build → Docker Publish
#                → Docker Deploy → Verify Container
open http://localhost:8082/food-distribution-tracker/
```
