# Vector Index Benchmark Report

## Features Measured

- Build time
- Memory usage
- Latency percentiles (P50 / P95 / P99)
- Throughput (QPS)
- Average distance calculations
- Recall (ground-truth validated)

---

## Test Environment

- **Machine**: MacBook Pro
- **Memory**: 24 GB RAM
- **Storage**: 512 GB SSD

---

## Flat Index Performance

| Dataset | Index Size | Dim | Queries |  k | Build (ms) | Memory (MB) | P50 (μs) | P95 (μs) | P99 (μs) |   QPS | Avg Dist Calcs | Recall |
|---------|-----------:|----:|--------:|---:|-----------:|------------:|---------:|---------:|---------:|------:|---------------:|:------:|
| Random  |      1,000 | 128 |     100 | 10 |        214 |           4 |    131.5 |    144.1 |    128.1 | 7,692 |          1,000 |  100%  |
| Random  |     10,000 | 128 |   1,000 | 10 |        215 |          10 |  1,008.6 |  1,021.3 |    991.2 |   951 |         10,000 |  100%  |
| Random  |    100,000 | 128 |  10,000 | 10 |        220 |          69 | 13,087.5 | 13,217.4 | 14,154.7 |    74 |        100,000 |  100%  |
| SIFT    |     10,000 | 128 |     100 | 10 |        213 |          10 |  1,026.6 |  1,047.9 |  1,100.5 |   952 |         10,000 |  100%  |

---

## HNSW Performance (Jelmark)

| Dataset |  M | efC | efS | Build (ms) | Mem (MB) | P50 (μs) | P95 (μs) | P99 (μs) |    QPS | Avg Dist Calcs | Recall@10 |
|---------|---:|----:|----:|-----------:|---------:|---------:|---------:|---------:|-------:|---------------:|----------:|
| 10K     | 16 | 200 | 200 |      2,071 |       17 |    197.5 |    249.3 |    393.2 |  5,556 |        1,374.6 |      100% |
| 10K     | 16 | 100 | 200 |      1,283 |       17 |    183.2 |    223.9 |    241.5 |  6,250 |        1,279.3 |     99.9% |
| 10K     | 16 | 100 | 100 |      1,323 |       17 |    115.8 |    155.4 |    183.2 | 10,000 |          830.6 |     99.8% |
| 10K     |  8 | 100 | 100 |      1,211 |       16 |    103.8 |    130.5 |    146.3 | 10,000 |          639.2 |     99.8% |
| 10K     |  8 | 200 | 200 |      1,845 |       16 |    174.2 |    213.7 |    348.3 |  6,667 |        1,055.6 |      100% |

---

## JVector-HNSW Benchmark (10K)

|  M | efC | efS | Build (ms) | Mem (MB) | P50 (μs) | P95 (μs) | P99 (μs) |    QPS | Recall@10 |
|---:|----:|----:|-----------:|---------:|---------:|---------:|---------:|-------:|----------:|
| 16 | 200 | 200 |        657 |       20 |    183.1 |    263.9 |    322.0 |  5,882 |     1.000 |
| 16 | 100 | 100 |        631 |       20 |    115.6 |    142.4 |    165.8 | 10,000 |     1.000 |
|  8 | 100 | 100 |        689 |       19 |     84.6 |    106.8 |    134.0 | 12,500 |     0.994 |

---

## IVF Performance (10K)

| nList | nProbe | Build (ms) | Mem (MB) | P50 (μs) | P95 (μs) | P99 (μs) |   QPS | Avg Dist Calcs | Recall@10 |
|------:|-------:|-----------:|---------:|---------:|---------:|---------:|------:|---------------:|----------:|
|   100 |     10 |      1,034 |       11 |    185.5 |    268.9 |    314.0 | 5,000 |          1,209 |     0.981 |
|   100 |     20 |      1,034 |       11 |    328.0 |    473.1 |    642.2 | 2,857 |          2,276 |     0.998 |
|    50 |      5 |        651 |       11 |    206.0 |    300.9 |    317.3 | 4,762 |          1,237 |     0.962 |

---

## Unified Comparison (10K, 128-D)

| Index | Build (ms) | P50 (μs) | QPS | Recall |
|------|-----------:|---------:|----:|-------:|
| Flat | 213 | 1,026 | 952 | 1.000 |
| IVF (best) | 651 | 206 | 4,762 | 0.962 |
| HNSW (Custom) | 1,211 | 103 | 10,000 | 0.998 |
| HNSW (JVector) | 689 | 85 | 12,500 | 0.994 |

---

## Empirical Conclusions

- Flat confirms correctness and O(n) scaling
- HNSW provides the **best latency–recall trade-off**
- IVF balances simplicity and performance
- JVector-HNSW offers fastest queries at slightly higher memory cost