# Vector Index — Benchmark Report

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

### HNSW Performance

| Index Type | Dataset Size |  M | efConstruction | efSearch | Build Time (ms) | Build Memory (MB) | P50 Latency (μs) | P95 Latency (μs) | P99 Latency (μs) | Throughput (QPS) | Avg Distance Calculations | Recall@10 (Avg) |
|------------|-------------:|---:|---------------:|---------:|----------------:|------------------:|-----------------:|-----------------:|-----------------:|-----------------:|--------------------------:|----------------:|
| HNSW       |       10,000 | 16 |            200 |      200 |           2,071 |                17 |            197.5 |          249.334 |          393.209 |            5,556 |                   1,374.6 |          100.0% |
| HNSW       |       10,000 | 16 |            100 |      200 |           1,283 |                17 |            183.2 |          223.875 |          241.458 |            6,250 |                   1,279.3 |           99.9% |
| HNSW       |       10,000 | 16 |            100 |      100 |           1,323 |                17 |            115.8 |          155.416 |          183.208 |           10,000 |                     830.6 |           99.8% |
| HNSW       |       10,000 |  8 |            100 |      100 |           1,211 |                16 |            103.8 |          130.458 |           146.25 |           10,000 |                     639.2 |           99.8% |
| HNSW       |       10,000 |  8 |            200 |      100 |           1,881 |                16 |            107.5 |          139.208 |          150.416 |           11,111 |                     658.0 |           99.8% |
| HNSW       |       10,000 |  8 |            200 |      200 |           1,845 |                16 |            174.2 |          213.667 |          348.292 |            6,667 |                   1,055.6 |          100.0% |

#### What Each Parameter Does

##### 1. M (Graph Connectivity)

**M = 8 vs M = 16**
- **Build time**
  - M = 8 → \~1.2 s
  - M = 16 → \~1.3–2.1 s
- **Query latency**
  - Largely similar across both values
- **Memory usage**
  - M = 8 → \~16 MB
  - M = 16 → \~17 MB
**Conclusion:**\
`M = 8` is sufficient for a 10K-scale dataset.
***

##### 2. efConstruction (Build Quality)
**efConstruction = 100 vs 200**
- **Build time**
  - `100` is \~40% faster (≈1.2 s vs ≈1.8 s)
- **Query performance**
  - Nearly identical
- **Recall**
  - No meaningful difference (99.8%–100%)
**Conclusion:**\
`efConstruction = 100` is enough; higher values waste build time.
***

##### 3. efSearch (Query Accuracy vs Speed) _(Most Important)_
**efSearch = 100 vs 200**
- **Latency**
  - \~104 μs vs \~174–197 μs (**70–90% slower**)
- **Distance calculations**
  - \~639 vs \~1,055–1,375 (**\~65% more work**)
- **Recall**
  - 99.8% vs 100%
**Conclusion:**\
`efSearch = 100` achieves near-perfect recall at almost **2× speed**.
***

#### 🏆 Best Configuration
##### **M = 8, efConstruction = 100, efSearch = 100**
**Why this is optimal:**
- **Fastest queries:** \~103.8 μs\
  _(Flat index ≈ 1,027 μs → \~10× slower)_
- **Fastest build:** \~1.2 s
- **Excellent recall:** 99.8%
- **Fewest distance calculations:** 639\
  _(Flat = 10,000 → \~16× reduction)_
- **Lowest memory usage:** 16 MB
***

#### Production Recommendations
##### For 10K-scale datasets
- **Recommended configuration**
  - `M = 8`
  - `efConstruction = 100`
  - `efSearch = 100`
- **If 100% recall is required**
  - Increase `efSearch` to `200`
  - Accept \~70% slower queries
***

##### For 1M-scale datasets
- Keep the same build parameters
- Increase `efSearch` to **150–300** to compensate for deeper graph traversal


## Recommended Parameters by Scale

| Dataset Size |  M | efConstruction | efSearch | Reasoning                    |
|-------------:|---:|---------------:|---------:|------------------------------|
|          10K |  8 |            100 |      100 | Your optimal configuration ✓ |
|         100K | 12 |            100 |  150–200 | Slightly denser graph        |
|           1M | 16 |            100 |  200–400 | Compensate for scale         |
|          10M | 24 |            200 |     500+ | Heavy compensation needed    |
