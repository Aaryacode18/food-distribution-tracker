#!/usr/bin/env bash
#
# Build, deploy, and start the Food Distribution Tracker.
#
#   ./start.sh              build, deploy, start services, open the app
#   ./start.sh --tests      also run the browser suite (needs Chrome)
#   ./start.sh --no-open    do not open a browser
#   ./start.sh --skip-build deploy whatever is already in target/
#
# Safe to re-run. If Tomcat or Jenkins are already up they are left alone.

set -euo pipefail

APP_NAME="food-distribution-tracker"
APP_URL="http://localhost:8081/${APP_NAME}/"
JENKINS_URL="http://localhost:8080/"
JENKINS_JOB_URL="${JENKINS_URL}job/Food-Tracker-Pipeline/"
TOMCAT_WEBAPPS="/opt/homebrew/opt/tomcat/libexec/webapps"
TOMCAT_PORT=8081

RUN_TESTS=0
OPEN_BROWSER=1
SKIP_BUILD=0

for arg in "$@"; do
    case "$arg" in
        --tests)     RUN_TESTS=1 ;;
        --no-open)   OPEN_BROWSER=0 ;;
        --skip-build) SKIP_BUILD=1 ;;
        -h|--help)
            sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *)
            echo "Unknown option: $arg (try --help)" >&2
            exit 2
            ;;
    esac
done

cd "$(dirname "$0")"

if [ -t 1 ]; then
    BOLD=$'\033[1m'; GREEN=$'\033[32m'; RED=$'\033[31m'
    YELLOW=$'\033[33m'; DIM=$'\033[2m'; OFF=$'\033[0m'
else
    BOLD=""; GREEN=""; RED=""; YELLOW=""; DIM=""; OFF=""
fi

step()  { printf '\n%s==> %s%s\n' "$BOLD" "$1" "$OFF"; }
ok()    { printf '%s    ok%s  %s\n' "$GREEN" "$OFF" "$1"; }
warn()  { printf '%s    !!%s  %s\n' "$YELLOW" "$OFF" "$1"; }
die()   { printf '%s    xx%s  %s\n' "$RED" "$OFF" "$1" >&2; exit 1; }
note()  { printf '%s        %s%s\n' "$DIM" "$1" "$OFF"; }

# --- 1. Locate a JDK -------------------------------------------------------

step "Checking the toolchain"

if ! command -v mvn >/dev/null 2>&1; then
    die "Maven not found. Install it with: brew install maven"
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME:-}/bin/java" ]; then
    if JAVA_HOME=$(/usr/libexec/java_home 2>/dev/null); then
        export JAVA_HOME
        note "JAVA_HOME set to ${JAVA_HOME}"
    else
        die "No JDK found. Install one with: brew install openjdk"
    fi
fi
ok "Maven $(mvn -v 2>/dev/null | head -1 | awk '{print $3}') on JDK $(java -version 2>&1 | head -1 | sed 's/.*version "\([0-9]*\).*/\1/')"

# --- 2. Build and test -----------------------------------------------------

if [ "$SKIP_BUILD" -eq 1 ]; then
    step "Skipping build (--skip-build)"
else
    step "Building and running unit tests"
    if mvn -q clean package; then
        ok "7 unit tests passed, WAR built"
    else
        die "Build failed. Fix the tests before deploying."
    fi

    if [ "$RUN_TESTS" -eq 1 ]; then
        step "Running the browser suite (needs Chrome)"
        if mvn -q test -Pselenium; then
            ok "3 browser tests passed"
        else
            die "Browser tests failed. The pipeline would not deploy this build."
        fi
    fi
fi

# --- 3. Deploy -------------------------------------------------------------

step "Deploying to Tomcat"
[ -f "target/${APP_NAME}.war" ] || die "target/${APP_NAME}.war not found. Run without --skip-build."
[ -d "$TOMCAT_WEBAPPS" ] || die "Tomcat webapps directory not found at ${TOMCAT_WEBAPPS}"

cp -f "target/${APP_NAME}.war" "${TOMCAT_WEBAPPS}/${APP_NAME}.war"
ok "WAR copied to ${TOMCAT_WEBAPPS}"

# --- 4. Start services -----------------------------------------------------

step "Starting services"

if brew services list 2>/dev/null | grep -q '^tomcat .*started'; then
    ok "Tomcat already running"
else
    brew services start tomcat >/dev/null 2>&1 || warn "Could not start Tomcat automatically; it may already be running"
    note "started Tomcat"
fi

if brew services list 2>/dev/null | grep -q '^jenkins-lts .*started'; then
    ok "Jenkins already running"
else
    brew services start jenkins-lts >/dev/null 2>&1 || warn "Could not start Jenkins automatically"
    note "started Jenkins"
fi

# --- 5. Wait for health ----------------------------------------------------

step "Waiting for the application"

APP_READY=0
for _ in $(seq 1 40); do
    if curl -fsS -o /dev/null "${APP_URL}" 2>/dev/null; then
        APP_READY=1
        break
    fi
    sleep 1
done

if [ "$APP_READY" -eq 1 ]; then
    ok "Application is responding"
else
    die "Application did not come up at ${APP_URL}. Check: ${TOMCAT_WEBAPPS}/../logs"
fi

JENKINS_STATE="not reachable"
JENKINS_CODE=$(curl -s -o /dev/null -w '%{http_code}' "${JENKINS_URL}" 2>/dev/null || echo 000)
case "$JENKINS_CODE" in
    200|403) JENKINS_STATE="up (HTTP ${JENKINS_CODE})" ;;
    000)     JENKINS_STATE="not responding yet" ;;
    *)       JENKINS_STATE="HTTP ${JENKINS_CODE}" ;;
esac
note "Jenkins ${JENKINS_STATE}"

# --- 6. Summary ------------------------------------------------------------

printf '\n%s%s%s\n' "$BOLD" "────────────────────────────────────────────" "$OFF"
printf '%s  Application   %s%s%s\n' "$BOLD" "$GREEN" "$APP_URL" "$OFF"
printf '%s  Jenkins       %s%s%s\n' "$BOLD" "$GREEN" "$JENKINS_JOB_URL" "$OFF"
printf '%s%s%s\n\n' "$BOLD" "────────────────────────────────────────────" "$OFF"

case "$JENKINS_CODE" in
    200|403) note "Jenkins needs a login. Accounts on this machine: aaryap, aaryapanchal224" ;;
    *)       note "Jenkins may still be starting. First run takes longer." ;;
esac
note "State is per browser session, so open the app in a fresh tab for clean data."
note "Demo walkthrough: DEMO.md"

if [ "$OPEN_BROWSER" -eq 1 ]; then
    open "$APP_URL"
    note "Opened the app in your browser."
fi
