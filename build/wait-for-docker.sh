#!/bin/sh
set -eu

# Run from the Gradle container, so this also checks the build user's socket access.
case "${DOCKER_HOST:-}" in
    unix:///*) socket=${DOCKER_HOST#unix://} ;;
    *) printf 'Expected DOCKER_HOST to name the shared Unix socket.\n' >&2; exit 1 ;;
esac

attempts=${DOCKER_WAIT_ATTEMPTS:-30}
interval=${DOCKER_WAIT_INTERVAL_SECONDS:-2}
case "$attempts" in
    ''|*[!0-9]*|0) printf 'DOCKER_WAIT_ATTEMPTS must be positive.\n' >&2; exit 1 ;;
esac
case "$interval" in
    ''|*[!0-9]*) printf 'DOCKER_WAIT_INTERVAL_SECONDS must be nonnegative.\n' >&2; exit 1 ;;
esac
if ! command -v curl >/dev/null 2>&1; then
    printf 'The Gradle image needs curl for the Docker readiness check.\n' >&2
    exit 1
fi

attempt=1
while [ "$attempt" -le "$attempts" ]; do
    response=''
    if response=$(curl --noproxy '*' --fail --silent --show-error --max-time 2 \
        --unix-socket "$socket" http://localhost/_ping 2>&1) && [ "$response" = 'OK' ]; then
        printf 'Docker is ready through %s.\n' "$DOCKER_HOST"
        exit 0
    fi
    if [ "$attempt" -lt "$attempts" ]; then sleep "$interval"; fi
    attempt=$((attempt + 1))
done

printf 'Docker did not become ready after %s attempts: %s\n' "$attempts" "$response" >&2
printf 'Check the docker sidecar log, privileged-container policy and socket permissions.\n' >&2
exit 1
