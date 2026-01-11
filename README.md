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