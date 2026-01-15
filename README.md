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


##### Recommended Parameters by Scale

| Dataset Size |  M | efConstruction | efSearch | Reasoning                    |
|-------------:|---:|---------------:|---------:|------------------------------|
|          10K |  8 |            100 |      100 | Your optimal configuration ✓ |
|         100K | 12 |            100 |  150–200 | Slightly denser graph        |
|           1M | 16 |            100 |  200–400 | Compensate for scale         |
|          10M | 24 |            200 |     500+ | Heavy compensation needed    |


## JVector-HNSW Benchmark Results (10K Dataset)

| Index Type   | Dataset Size |  M | efConstruction | efSearch | Build Time (ms) | Build Memory (MB) | P50 Latency (μs) | P95 Latency (μs) | P99 Latency (μs) | Throughput (QPS) | Avg Distance Calculations | Recall@10 (Avg) |
|--------------|-------------:|---:|---------------:|---------:|----------------:|------------------:|-----------------:|-----------------:|-----------------:|-----------------:|--------------------------:|----------------:|
| JVector-HNSW |       10,000 | 16 |            200 |      200 |             657 |                20 |          183.083 |          263.917 |          321.958 |         5,882.35 |                         - |           1.000 |
| JVector-HNSW |       10,000 | 16 |            100 |      200 |             666 |                20 |          176.292 |          207.917 |          217.625 |         5,555.56 |                         - |           1.000 |
| JVector-HNSW |       10,000 | 16 |            100 |      100 |             631 |                20 |          115.625 |          142.416 |          165.833 |        10,000.00 |                         - |           1.000 |
| JVector-HNSW |       10,000 |  8 |            200 |      200 |             611 |                19 |          130.375 |          151.875 |          175.542 |         8,333.33 |                         - |           0.995 |
| JVector-HNSW |       10,000 |  8 |            100 |      200 |             683 |                19 |          135.500 |          169.250 |          183.833 |         7,692.31 |                         - |           0.995 |
| JVector-HNSW |       10,000 |  8 |            100 |      100 |             689 |                19 |           84.625 |          106.792 |          134.042 |        12,500.00 |                         - |           0.994 |


### IVF Performance

| Index Type | Dataset Size | nList | nProbe | Build Time (ms) | Build Memory (MB) | P50 Latency (μs) | P95 Latency (μs) | P99 Latency (μs) | Throughput (QPS) | Avg Distance Calculations | Recall@10 (Avg) |
|------------|-------------:|------:|-------:|----------------:|------------------:|-----------------:|-----------------:|-----------------:|-----------------:|--------------------------:|----------------:|
| IVF        |       10,000 |   100 |     10 |           1,034 |                11 |          185.459 |          268.917 |          314.042 |         5,000.00 |                  1,209.07 |           0.981 |
| IVF        |       10,000 |   100 |     20 |           1,034 |                11 |          328.041 |          473.125 |          642.167 |         2,857.14 |                  2,276.38 |           0.998 |
| IVF        |       10,000 |    50 |      5 |             651 |                11 |          206.041 |          300.875 |          317.250 |         4,761.90 |                  1,236.53 |           0.962 |

## Unified Vector Index Comparison (10K Dataset, 128-D)

| Index Type | Library | Configuration          | Build Time (ms) | Build Memory (MB) | P50 Latency (us) | P95 Latency (us) | P99 Latency (us) | Throughput (QPS) | Avg Distance Calcs | Recall@10 | Notes                  |
|------------|---------|------------------------|----------------:|------------------:|-----------------:|-----------------:|-----------------:|-----------------:|-------------------:|----------:|------------------------|
| Flat       | Custom  | Brute-force            |             213 |                10 |           1026.6 |           1047.9 |           1100.5 |              952 |              10000 |     1.000 | Exact search, baseline |
| IVF        | Custom  | nList=100, nProbe=10   |            1034 |                11 |            185.5 |            268.9 |            314.0 |             5000 |               1209 |     0.981 | Fast, lower recall     |
| IVF        | Custom  | nList=100, nProbe=20   |            1034 |                11 |            328.0 |            473.1 |            642.2 |             2857 |               2276 |     0.998 | Higher recall          |
| IVF        | Custom  | nList=50, nProbe=5     |             651 |                11 |            206.0 |            300.9 |            317.3 |             4762 |               1237 |     0.962 | Faster build           |
| HNSW       | Jelmark | M=8, efC=100, efS=100  |            1211 |                16 |            103.8 |            130.5 |            146.3 |            10000 |                639 |     0.998 | Best trade-off         |
| HNSW       | Jelmark | M=8, efC=200, efS=200  |            1845 |                16 |            174.2 |            213.7 |            348.3 |             6667 |               1056 |     1.000 | Max recall             |
| HNSW       | Jelmark | M=16, efC=100, efS=100 |            1323 |                17 |            115.8 |            155.4 |            183.2 |            10000 |                831 |     0.998 | More memory            |
| HNSW       | Jelmark | M=16, efC=200, efS=200 |            2071 |                17 |            197.5 |            249.3 |            393.2 |             5556 |               1375 |     1.000 | Overkill for 10K       |
| HNSW       | JVector | M=8, efC=100, efS=100  |             689 |                19 |             84.6 |            106.8 |            134.0 |            12500 |                  - |     0.994 | Fastest queries        |
| HNSW       | JVector | M=16, efC=100, efS=100 |             631 |                20 |            115.6 |            142.4 |            165.8 |            10000 |                  - |     1.000 | High recall            |
| HNSW       | JVector | M=16, efC=200, efS=200 |             657 |                20 |            183.1 |            263.9 |            322.0 |             5882 |                  - |     1.000 | Slower, exact recall   |
