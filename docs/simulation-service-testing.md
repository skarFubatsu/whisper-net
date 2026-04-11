# Simulation Service Test Commands

This document defines how to run tests for the simulation engine module.

## Module Path

- `src/simulation-service`

## Standard Test Run

Runs all regular tests and skips performance guardrail tests.

```powershell
mvn -f src/simulation-service/pom.xml test
```

## Performance Guardrail Run

Runs all tests, including performance guardrail tests, via Maven profile `perf`.

```powershell
mvn -f src/simulation-service/pom.xml test -Pperf
```

## Alternative Performance Toggle

You can also enable performance tests with the system property directly.

```powershell
mvn -f src/simulation-service/pom.xml test -DrunPerformanceTests=true
```

## Expected Behavior

- Standard run: performance guardrail tests are skipped.
- `perf` profile run: performance guardrail tests are executed.
