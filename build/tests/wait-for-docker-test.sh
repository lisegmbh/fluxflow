#!/bin/sh
set -eu

build_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
fixture_dir=$(mktemp -d)
trap 'rm -rf "$fixture_dir"' EXIT HUP INT TERM
mkdir "$fixture_dir/bin"

# Exercise the readiness command as a subprocess. Only the Docker HTTP endpoint
# is replaced; exit status, retries and diagnostics are observed by the caller.
cat > "$fixture_dir/bin/curl" <<'CURL'
#!/bin/sh
set -eu
previous=''
socket=''
for argument in "$@"; do
    if [ "$previous" = '--unix-socket' ]; then socket=$argument; fi
    previous=$argument
done
[ "$socket" = '/var/run/fluxflow-docker/docker.sock' ] || exit 91
[ "$previous" = 'http://localhost/_ping' ] || exit 92
calls=0
if [ -f "$DOCKER_TEST_CALLS" ]; then calls=$(cat "$DOCKER_TEST_CALLS"); fi
calls=$((calls + 1))
printf '%s' "$calls" > "$DOCKER_TEST_CALLS"
case "$DOCKER_TEST_MODE" in
    ready) printf 'OK' ;;
    delayed)
        if [ "$calls" -lt 3 ]; then printf 'Connection refused' >&2; exit 7; fi
        printf 'OK'
        ;;
    unavailable) printf 'Permission denied opening socket' >&2; exit 7 ;;
    http-error) printf 'HTTP 503' >&2; exit 22 ;;
    wrong-service) printf 'Not Docker' ;;
    *) exit 93 ;;
esac
CURL
chmod +x "$fixture_dir/bin/curl"

export PATH="$fixture_dir/bin:$PATH"
export DOCKER_HOST='unix:///var/run/fluxflow-docker/docker.sock'
export DOCKER_WAIT_ATTEMPTS=3
export DOCKER_WAIT_INTERVAL_SECONDS=0
export DOCKER_TEST_CALLS="$fixture_dir/calls"

run_case() {
    DOCKER_TEST_MODE=$1
    export DOCKER_TEST_MODE
    rm -f "$DOCKER_TEST_CALLS"
    status=0
    sh "$build_dir/wait-for-docker.sh" > "$fixture_dir/output" 2>&1 || status=$?
}

assert_status() {
    if [ "$status" -ne "$1" ]; then
        cat "$fixture_dir/output" >&2
        printf 'FAIL: %s (exit %s, expected %s)\n' "$DOCKER_TEST_MODE" "$status" "$1" >&2
        exit 1
    fi
}

run_case ready
assert_status 0
[ "$(cat "$DOCKER_TEST_CALLS")" = 1 ]
grep -q 'Docker is ready' "$fixture_dir/output"
printf 'PASS: ready daemon permits the build\n'

run_case delayed
assert_status 0
[ "$(cat "$DOCKER_TEST_CALLS")" = 3 ]
printf 'PASS: delayed daemon is retried\n'

run_case unavailable
assert_status 1
[ "$(cat "$DOCKER_TEST_CALLS")" = 3 ]
grep -q 'Permission denied' "$fixture_dir/output"
grep -q 'Docker did not become ready' "$fixture_dir/output"
printf 'PASS: unavailable daemon fails with the cause after bounded retries\n'

run_case wrong-service
assert_status 1
printf 'PASS: an unrelated HTTP response is not readiness\n'

run_case http-error
assert_status 1
grep -q 'HTTP 503' "$fixture_dir/output"
printf 'PASS: an HTTP error does not permit the build\n'

DOCKER_HOST='tcp://localhost:2375'
run_case ready
assert_status 1
[ ! -f "$DOCKER_TEST_CALLS" ]
printf 'PASS: this sidecar preflight rejects an unexpected transport\n'

DOCKER_HOST='unix:///var/run/fluxflow-docker/docker.sock'
DOCKER_WAIT_ATTEMPTS=0
run_case ready
assert_status 1
[ ! -f "$DOCKER_TEST_CALLS" ]
printf 'PASS: invalid retry configuration fails immediately\n'

rm "$fixture_dir/bin/curl"
DOCKER_WAIT_ATTEMPTS=3
status=0
PATH="$fixture_dir/bin" /bin/sh "$build_dir/wait-for-docker.sh" > "$fixture_dir/output" 2>&1 || status=$?
assert_status 1
grep -q 'needs curl' "$fixture_dir/output"
printf 'PASS: a missing HTTP client has an actionable error\n'
