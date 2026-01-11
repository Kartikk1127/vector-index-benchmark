package benchmark;

import core.Vector;
import core.VectorIndex;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkRunner {

    public static Metrics run (
            VectorIndex index,
            List<Vector> indexData,
            List<Vector> queryVectors,
            int k
    ) {
        // capturing the memory before
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // time the build
        long buildStart = System.currentTimeMillis();
        index.build(indexData);
        long buildEnd = System.currentTimeMillis();
        long buildTimeMs = buildEnd - buildStart;

        // capturing the memory after
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long buildMemoryMB = (memoryAfter - memoryBefore) / (1024*1024);

        // some warm up to let jvm optimize the code
        for (int i = 0; i < Math.min(100, queryVectors.size()); i ++) {
            index.search(queryVectors.get(i).getData(), k);
        }

        // query latency measurement
        List<Long> latencies = new ArrayList<>();
        index.resetDistanceCalculations();

        for (Vector queryVector : queryVectors) {
            long start = System.nanoTime();
            index.search(queryVector.getData(),k);
            long end = System.nanoTime();

            long latencyNanos = end - start;
            latencies.add(latencyNanos);
        }

        // convert to microseconds and calculate percentiles
        double p50 = latencies.get(latencies.size()/2)/1000.0;
        double p95 = latencies.get((int)(latencies.size()*0.95))/1000.0;
        double p99 = latencies.get((int)(latencies.size()*0.99))/1000.0;

        double averageDist = index.getDistanceCalculations() / (double) queryVectors.size();

        // throughput measurement
        long throughputStart = System.currentTimeMillis();
        for (Vector queryVector : queryVectors) {
            index.search(queryVector.getData(), k);
        }
        long throughputEnd = System.currentTimeMillis();
        double totalSeconds = (throughputEnd - throughputStart) / 1000.0;
        double qps = queryVectors.size()/totalSeconds;

        return new Metrics(
                buildTimeMs,buildMemoryMB,p50,p95,p99,qps,averageDist
        );
    }
}
