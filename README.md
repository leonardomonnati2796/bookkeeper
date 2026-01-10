# Apache BookKeeper

Apache BookKeeper is a scalable, fault-tolerant, and low-latency storage service optimized for real-time workloads.

## ISW2 Testing Project

This repository includes testing work for the ISW2 (Ingegneria del Software 2) course, focusing on:

- **EntryMemTable**: In-memory cache management for ledger entries
- **ExponentialBackoffRetryPolicy**: Retry policy with exponential backoff for ZooKeeper connections

### Test Suites

- Manual Tests: Basic functionality and happy paths
- LLM-Generated Tests: Comprehensive branch coverage and edge cases
- Coverage Analysis: JaCoCo reports
- Mutation Testing: PITest reports

### CI/CD

The project includes a simplified GitHub Actions workflow (`test-pipeline.yml`) for automated testing and report generation.

## Original Apache BookKeeper Documentation

For complete Apache BookKeeper documentation, visit: https://bookkeeper.apache.org/

## License

Licensed under the Apache License, Version 2.0. See LICENSE and NOTICE files for details.
