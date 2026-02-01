# Apache BookKeeper

Apache BookKeeper is a scalable, fault-tolerant, and low-latency storage service optimized for real-time workloads.

## ISW2 Testing Project

This repository includes testing work for the ISW2 (Ingegneria del Software 2) course, focusing on:

### Main Components Tested
- **EntryMemTable**: In-memory cache management for ledger entries
- **ExponentialBackoffRetryPolicy**: Retry policy with exponential backoff for ZooKeeper connections
- **SafeRunnable**: Robust wrapper for exception-safe runnables, tested con Failsafe e Mockito
- **Backoff**: Policy di backoff (costante, jitter, esponenziale) con test manuali, LLM e mutation

### Test Suites

- Advanced Testing: Category partition, boundary value analysis, mutation-oriented test design

### Testing Tools & Infrastructure
- **Maven Surefire Plugin**: Automated unit test execution
- **Maven Failsafe Plugin**: Integration test execution
- **Mockito**: Mocking and verification for handler and control flow
- **Custom Mocks**: For complex dependencies and edge cases
- **JaCoCo**: Code coverage analysis
- **PITest**: Mutation testing for robustness
### CI/CD
### Testing Workflow
1. `mvn clean`: Clean previous build artifacts
2. `mvn test`: Run unit tests (Surefire)
3. `mvn verify`: Run integration tests (Failsafe)
4. `mvn jacoco:report`: Generate JaCoCo coverage report
5. `mvn pitest:mutationCoverage`: Run mutation testing (PITest)
6. Manual/automated analysis of coverage and mutation score

The project includes a simplified GitHub Actions workflow (`test-pipeline.yml`) for automated testing and report generation.

## Original Apache BookKeeper Documentation

For complete Apache BookKeeper documentation, visit: https://bookkeeper.apache.org/

## License

Licensed under the Apache License, Version 2.0. See LICENSE and NOTICE files for details.
