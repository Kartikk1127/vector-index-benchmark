package benchmark;

import core.QueryResult;
import core.Vector;
import core.VectorIndex;

import java.util.*;

public class BenchmarkRunner {

    public static Metrics run (
            VectorIndex index,
            List<Vector> indexData,
            List<Vector> queryVectors,
            int k,
            String dataset
    ) throws InterruptedException {

        // time the build
        long buildStart = System.currentTimeMillis();
        System.gc();
        Thread.sleep(100);
        index.build(indexData);
        System.gc();
        Thread.sleep(100);
        long buildEnd = System.currentTimeMillis();
        long buildTimeMs = buildEnd - buildStart;

        // capturing the memory after
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long buildMemoryMB = usedMemory / (1024*1024);

        // some warm up to let jvm optimize the code
        for (int i = 0; i < Math.min(100, queryVectors.size()); i ++) {
            index.search(queryVectors.get(i).vector(), k, dataset);
        }

        // query latency measurement
        List<Long> latencies = new ArrayList<>();
        index.resetDistanceCalculations();

        for (Vector queryVector : queryVectors) {
            long start = System.nanoTime();
            index.search(queryVector.vector(),k, dataset);
            long end = System.nanoTime();

            long latencyNanos = end - start;
            latencies.add(latencyNanos);
        }

        Collections.sort(latencies);

        // convert to microseconds and calculate percentiles
        double p50 = latencies.get(latencies.size()/2)/1000.0;
        double p95 = latencies.get((int)(latencies.size()*0.95))/1000.0;
        double p99 = latencies.get((int)(latencies.size()*0.99))/1000.0;

        double averageDist = index.getDistanceCalculations() / (double) queryVectors.size();

        // throughput measurement
        long throughputStart = System.currentTimeMillis();
        for (Vector queryVector : queryVectors) {
            index.search(queryVector.vector(), k, dataset);
        }
        long throughputEnd = System.currentTimeMillis();
        double totalSeconds = (throughputEnd - throughputStart) / 1000.0;
        double qps = queryVectors.size()/totalSeconds;

        return new Metrics(
                buildTimeMs,buildMemoryMB,p50,p95,p99,qps,averageDist
        );
    }

    public static double calculateRecall(List<QueryResult> results, int[] groundTruth, int k) {
        Set<String> resultIds = new HashSet<>();
        for (int i = 0; i < Math.min(k, results.size()); i ++) {
            resultIds.add(results.get(i).getId());
        }

        // count matches with the ground truth (first k entries)
        int matches = 0;
        for (int i = 0; i < Math.min(k, groundTruth.length); i++) {
            String groundTruthId = "sift_" + groundTruth[i];
            if (resultIds.contains(groundTruthId)) {
                matches++;
            }
        }
        return (double) matches/k;
    }
}
