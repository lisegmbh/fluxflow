# Jenkins test runtime

`Jenkinsfile` loads `build/agent.yml` and inherits the Jenkins Kubernetes pod template `plain`.
The local template adds a Docker-in-Docker sidecar alongside the existing Gradle container.
The generated pod YAML is printed in the Jenkins log so inherited settings can be checked.

## Connection and lifecycle

- Only the Docker sidecar requests `privileged: true` and runs as root. The Kubernetes cluster
  must permit this container; a pod admission rejection cannot be fixed by skipping tests.
- Docker listens only on `unix:///var/run/fluxflow-docker/docker.sock`. Starting the official
  entrypoint with an explicit `dockerd` argument avoids its automatic TCP listener configuration.
- A pod-local `emptyDir` shares the socket. Socket group 1000 matches the Gradle container's
  effective group; its user ID remains inherited. Docker state uses separate ephemeral volumes.
  No host Docker socket or host filesystem is mounted.
- `TESTCONTAINERS_HOST_OVERRIDE=localhost` addresses published Mongo/Ryuk ports in the shared
  pod network. `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE` gives Ryuk the daemon-side socket path.
- The sidecar readiness probe uses `docker info`. Before Gradle starts, `wait-for-docker.sh`
  also checks the API using the Gradle user's actual Unix-socket permissions. It accepts only
  an HTTP-success response containing `OK` from `/_ping`.
- The default preflight makes at most 30 requests, each limited to two seconds, with two seconds
  between attempts. `DOCKER_WAIT_ATTEMPTS` and `DOCKER_WAIT_INTERVAL_SECONDS` control this bound.
- Testcontainers owns the test Mongo containers and Ryuk performs cleanup. Deleting the agent
  pod also removes its daemon and ephemeral data volumes.

The Docker sidecar has a 500m CPU / 1Gi memory request because its daemon hosts the test database
processes. These requests are scheduling reservations, not hard limits. Actual cluster limits and
the inherited pod configuration must be checked on the Jenkins run.

## Tests and failure evidence

Run the readiness command's behavioral tests from a POSIX shell:

```sh
sh build/tests/wait-for-docker-test.sh
```

They replace only the HTTP client at the process boundary and cover readiness, delayed startup,
persistent socket failure, wrong response, HTTP errors, wrong transport, invalid retry settings
and a missing HTTP client. Jenkins runs these tests before the real API preflight.

The authoritative integration check remains the full library build, including both mandatory
Mongo `securityTest` tasks and `buildSrc:test`. Readiness alone does not prove image pulls, Mongo
mapped-port access, replica-set behavior, or Ryuk cleanup. Security-test failures print full
exception causes and stack traces.

The build stage publishes JUnit results and archives test reports even on failure. Empty report
publication is tolerated only to keep a preflight failure understandable; the Gradle security
gate still fails on absent required tests/reports, failed tests or skipped tests. Its requirements
are unchanged.

For the first Jenkins run, distinguish:

1. **Agent pod rejected:** inspect the provisioning log for the privileged-container policy.
2. **Sidecar not ready:** inspect the `docker` container log for daemon, storage or inherited
   security-context errors.
3. **Gradle preflight fails:** inspect its last HTTP/socket error and the effective group/mounts.
4. **Tests fail after readiness:** inspect the archived XML and full Testcontainers exception;
   check registry/image pulls, API compatibility and mapped-port access.

Do not disable the security gate or Ryuk to obtain a successful build. The separate scheduling
timing-test issue and pinned mandatory Mongo-fixture follow-up are not changed by this runtime PR.

## Reference

- [Official Docker entrypoint](https://github.com/docker-library/docker/blob/master/29/dind/dockerd-entrypoint.sh)
- [Testcontainers Docker configuration](https://java.testcontainers.org/features/configuration/)
- [Jenkins Kubernetes pod templates](https://plugins.jenkins.io/kubernetes/)
