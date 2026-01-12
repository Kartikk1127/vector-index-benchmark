## This is what i have to capture
1. Build time (in milliseconds)
2. Build memory (MB before/after)
3. Query latency p50/p95/p99 (microseconds) - run 1000 queries
4. Throughput (queries per second)
5. Distance calculations per query (algorithm insight)

## Dataset
1. 1000 vectors, 128 dimensions
2. Random normalized vectors (seed = 42)
3. 100 separate query vectors (not in index)

## Warm up
1. Run 100 queries before measurement to let JIT compile


## Benchmarks

### Flat Index

| Dataset  | Index Size | Dimensions | Query Count |  k | Seed | Build Time (ms) | Build Memory (MB) | P50 Latency (μs) | P95 Latency (μs) | P99 Latency (μs) | Throughput (QPS) | Avg Distance Calculations |
|----------|-----------:|-----------:|------------:|---:|-----:|----------------:|------------------:|-----------------:|-----------------:|-----------------:|-----------------:|--------------------------:|
| Random   |      1,000 |        128 |         100 | 10 |   42 |             214 |                 4 |            131.5 |          144.125 |          128.083 |         7,692.31 |                   1,000.0 |
| Random   |     10,000 |        128 |       1,000 | 10 |   42 |             215 |                10 |        1,008.625 |        1,021.333 |          991.167 |           950.57 |                  10,000.0 |
| Random   |    100,000 |        128 |      10,000 | 10 |   42 |             220 |                69 |         13,087.5 |       13,217.375 |       14,154.667 |            74.14 |                 100,000.0 |
| SIFT 10K |     10,000 |        128 |         100 | 10 |    — |             213 |                10 |        1,026.625 |        1,047.875 |        1,100.542 |           952.38 |                  10,000.0 |
