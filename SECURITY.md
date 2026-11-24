# Security Policy

## Supported Versions

The following table shows the versions of the project that are currently supported
and can expect to receive security updates.

| Version | Supported          |
| ------- | ------------------ |
| 0.X.X   | :white_check_mark: |

## CVE assessments

### CVE-2026-59273: Spring Boot 3 integration line

**Assessment date:** 2026-11-24. **Status:** Not affected in the assessed FluxFlow
Spring Boot 3 integration artifacts (`*-spring3`). The Spring 3 dependency
management remains unchanged.

The rationale for retaining the Spring 3 dependency baseline is recorded in
[ADR-0001](docs/adr/0001-retain-spring3-dependencies-for-cve-2026-59273.md).

[CVE-2026-59273](https://spring.io/security/cve-2026-59273/) concerns Spring
Security's `ELRequestMatcher` in `spring-security-web`. An application can expose
the vulnerability when untrusted input is converted into a matcher expression
and the matcher is subsequently evaluated. Properties declared as the
`RequestMatcher` interface also require attention: Spring's automatically
discovered
[`RequestMatcherEditor`](https://github.com/spring-projects/spring-security/blob/6.5.6/web/src/main/java/org/springframework/security/web/util/matcher/RequestMatcherEditor.java)
constructs an `ELRequestMatcher` from a string.

The assessment is based on the following evidence:

- The Spring 3 modules do not declare Spring Security dependencies. Gradle
  `dependencyInsight` found no Spring Security artifacts in the runtime classpaths
  of `springboot`, `springboot-web`, `springboot-mongo`, `springboot-quartz`,
  `springboot-in-memory-persistence`, `springboot-test-scheduling`, and
  `springboot-testing`. In particular, the affected `spring-security-web`
  component is absent.
- FluxFlow's production sources do not use `ELRequestMatcher` or
  `RequestMatcherEditor` and do not define request-bound matcher properties.
- The Spring Boot 3.5.7 BOM manages Spring Security 6.5.6, which is in the affected
  version range. A managed version is a dependency version default; importing
  the BOM does not add the corresponding library to a runtime classpath.
- FluxFlow's own `SpringSelectorExpressionParser` uses SpEL, but its built-in
  caller obtains expressions from `@ModelListener.selector` annotations in
  application code. This is separate from the affected Spring Security matcher
  and is not evidence of exposure to this CVE.

To repeat the dependency check, run the following from `library/` for each module
listed above (use `gradlew.bat` on Windows):

```sh
./gradlew :springboot:<module>:dependencyInsight --dependency spring-security --configuration runtimeClasspath
```

**Scope and reassessment:** This conclusion covers FluxFlow's own artifacts, not
the complete runtime of an application that embeds FluxFlow. A consuming
application may independently add Spring Security and define its own binding or
matcher configuration. FluxFlow's web parameter resolver also converts request
parameters to application-declared parameter types; consumers must therefore
check those types as well as their controller input models. Applications that
include `spring-security-web` must assess their resolved version and whether
untrusted input can reach a `RequestMatcher` or `ELRequestMatcher` and be
evaluated. Repeat this assessment if FluxFlow adds Spring Security dependencies,
matcher usage, or relevant input-binding paths. This is a component-absence
assessment, not a claim that Spring Security 6.5.6 has been patched.

## Reporting a Vulnerability

We gladly accept and appreciate any responsible disclosure of security vulnerabilities.

**Please do**
- contact us directly via [security@lise.de](mailto:security@lise.de).
- provide us with a detailed description of the vulnerability and the steps to reproduce it.
- state your contact information for further communication.

**Please do not**
- disclose the vulnerability publicly until we have had a chance to address it.
- create a GitHub issue for the vulnerability.
- exploit the vulnerability to gain access to systems or data.
- include any personal data (including payment information) in your report.

**You may**
- encrypt your mail using our public key. You can find it as described in our [security.txt file](https://www.lise.de/.well-known/security.txt) or directly on the [key server](https://keys.openpgp.org/vks/v1/by-fingerprint/C0D329867B246314E8D2A808AC5068AE824497C5).
- provide a minimal proof of concept to demonstrate the vulnerability.

**We will**
- acknowledge your report within 24 hours or on the next working day.
- check and address your report as soon as possible.
- award a voluntary bug bounty depending on the severity.
