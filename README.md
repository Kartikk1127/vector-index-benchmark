# Flat Vector Index — Benchmark Report

## Features

- **Multiple distance metrics**
    - Euclidean (L2)
    - Cosine similarity
- **Comprehensive performance metrics**
    - Build time
    - Memory usage
    - Latency percentiles (P50 / P95 / P99)
    - Throughput (QPS)
    - Average distance calculations
- **Real dataset support**
    - ANN Benchmarks format (`.fvecs` / `.ivecs`)
    - Ground-truth validation for recall measurement
- **Synthetic data generation**
    - Normalized random vectors for controlled experiments
***

## Benchmark Results

### Flat Index Performance
    | Dataset  | Index Size | Dimensions | Query Count |  k | Build Time (ms) | Build Memory (MB) | P50 Latency (μs) | P95 Latency (μs) | P99 Latency (μs) | Throughput (QPS) | Avg Distance Calculations | Recall |
    |----------|-----------:|-----------:|------------:|---:|----------------:|------------------:|-----------------:|-----------------:|-----------------:|-----------------:|--------------------------:|:------:|
    | Random   |      1,000 |        128 |         100 | 10 |             214 |                 4 |            131.5 |          144.125 |          128.083 |         7,692.31 |                   1,000.0 |   —    |
    | Random   |     10,000 |        128 |       1,000 | 10 |             215 |                10 |        1,008.625 |        1,021.333 |          991.167 |           950.57 |                  10,000.0 |   —    |
    | Random   |    100,000 |        128 |      10,000 | 10 |             220 |                69 |         13,087.5 |       13,217.375 |       14,154.667 |            74.14 |                 100,000.0 |   —    |
    | SIFT 10K |     10,000 |        128 |         100 | 10 |             213 |                10 |        1,026.625 |        1,047.875 |        1,100.542 |           952.38 |                  10,000.0 | 100%   |

***

## Test Environment
- **Machine**: MacBook Pro
- **Memory**: 24 GB RAM
- **Storage**: 512 GB SSD
***

## Key Findings
- **Perfect O(n) linear scaling**
    - 10× increase in dataset size results in \~10× increase in query latency
- **Correctness**
    - 100% recall on **SIFT 10K**, validated against ground truth
- **Performance at scale**
    - At **100K vectors**:
        - \~13 ms per query
        - \~74 QPS throughput
- **Brute-force confirmation**
    - Distance calculations equal dataset size, confirming true flat (exhaustive) search behavior