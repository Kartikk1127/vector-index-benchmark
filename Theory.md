# **Vector Index: Theory & Design**

## Overview

This document explains the **concepts, algorithms, trade-offs, and decision logic** behind vector indexing methods used in this project. It is intended to help engineers understand _why_ and _when_ to use each index type.

***


## Supported Index Types

### 1. Flat (Brute-force) Index

- Computes distance against **every vector**

- Guarantees **exact results (100% recall)**

- Time complexity: **O(n)** per query

**When it makes sense**

- Very small datasets (<1K vectors)

- Very low query volume

- Validation or correctness testing

***


### 2. HNSW (Hierarchical Navigable Small World)

- Graph-based Approximate Nearest Neighbor (ANN)

- Multi-layer proximity graph

- Logarithmic-like query behavior in practice


#### Core Parameters

##### **M — Graph Connectivity**

- Controls number of neighbors per node

- Higher `M` → better recall, more memory, slower build


##### **efConstruction — Build Quality**

- Controls graph construction accuracy

- Higher values → longer build, marginal recall improvement


##### **efSearch — Query Accuracy vs Speed**

- Controls how much of the graph is explored at query time

- **Most important parameter**

***


### 3. IVF (Inverted File Index)

- K-means clustering of vectors

- Queries search only selected clusters

- Simpler mental model than HNSW

**Key Parameters**

- `nList`: number of clusters

- `nProbe`: number of clusters searched per query

***


## Distance Metrics

- **Euclidean (L2)**

- **Cosine similarity**

***


## Recall vs Latency Trade-off

| Index | Recall  | Latency   | Predictability |
| ----- | ------- | --------- | -------------- |
| Flat  | 100%    | Slow      | Deterministic  |
| HNSW  | 99–100% | Very Fast | Tunable        |
| IVF   | 96–99%  | Fast      | Coarse         |

***


## When to Use What?

### ❓ When is FLAT better?

FLAT is faster when:

    (total_queries × query_time) < index_build_time

Typical cases:

- Validation workloads

- Very infrequent searches

- Small datasets

***


### ❓ When does HNSW win?

- High query volume

- Real-time latency requirements

- Search, recommendation, RAG systems

HNSW amortizes build cost quickly and delivers **sub-millisecond queries**.

## Recommended Parameters by Scale

| Dataset Size | M  | efConstruction | efSearch |
|--------------|----|----------------|----------|
| 10K          | 8  | 100            | 100      |
| 100K         | 12 | 100            | 150–200  |
| 1M           | 16 | 100            | 200–400  |
| 10M          | 24 | 200            | 500+     |

***


### ❓ When does IVF make sense?

- Batch processing

- GPU acceleration

- Offline analytics

- Teams preferring algorithmic simplicity

IVF is easier to debug and parallelize, especially on GPUs.

***