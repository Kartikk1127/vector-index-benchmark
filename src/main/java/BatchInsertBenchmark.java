import benchmark.BenchmarkExecutors;
import benchmark.BenchmarkRunner;
import benchmark.Metrics;
import core.QueryResult;
import core.Vector;
import core.VectorIndex;
import dataset.DatasetLoader;
import index.hnsw.JVectorHNSWIndex;
import index.hnsw.JelmarkHNSWIndex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class BatchInsertBenchmark {

    // =======================
    // Configuration
    // =======================
    private static final String BASE_PATH =
            "/Users/kartikeysrivastava/Desktop/projects/dataset/custom-100k/";

    private static final String BASE_VECTORS = BASE_PATH + "base.fvecs";
    private static final String QUERY_VECTORS = BASE_PATH + "query.fvecs";
    private static final String GROUND_TRUTH = BASE_PATH + "groundtruth.ivecs";

    private static final int K = 10;
    private static final int VECTORS_TO_REINSERT = 10000;

    // =======================
    // Execution Control
    // =======================
    enum ExecutionMode {
        SEQUENTIAL,
        BATCH
    }

    private static final ExecutionMode[] EXECUTION_ORDER = {
            ExecutionMode.BATCH,
            ExecutionMode.SEQUENTIAL
    };

    public static void main(String[] args) throws IOException {

        System.out.println("=== Batch Insert Benchmark: HNSW ===");

        // =======================
        // Load Dataset
        // =======================
        System.out.println("Loading SIFT dataset...");
        List<Vector> indexVectors = DatasetLoader.loadFVectors(BASE_VECTORS);
        List<Vector> queryVectors = DatasetLoader.loadFVectors(QUERY_VECTORS);
        List<int[]> groundTruth = DatasetLoader.loadIVecs(GROUND_TRUTH);

        System.out.println("Dataset: " + indexVectors.size() + " vectors, "
                + queryVectors.size() + " queries");

        // Single warmup with sequential constructor
        System.out.println("=== Warmup Phase ===");
        VectorIndex warmupIndex = new JVectorHNSWIndex(16, 200, 200);
        warmupIndex.build(indexVectors);

        double recall = calculateAverageRecall(warmupIndex, queryVectors, groundTruth);
        System.out.println("Recall@10 on fresh index: " + recall);

        // Delete 1K vectors
        for (int i = 0; i < 1000; i++) {
            warmupIndex.delete(indexVectors.get(i).id());
        }

        // Sequential re-insert
        for (int i = 0; i < 1000; i++) {
            warmupIndex.insert(indexVectors.get(i));
        }

        System.out.println("Warmup complete\n");
        // =======================
        // Prepare delete/reinsert sets
        // =======================
        List<String> idsToDelete = new ArrayList<>();
        List<Vector> vectorsToReinsert = new ArrayList<>();

        for (int i = 0; i < VECTORS_TO_REINSERT; i++) {
            idsToDelete.add(indexVectors.get(i).id());
            vectorsToReinsert.add(indexVectors.get(i));
        }

        ExecutorService executor = BenchmarkExecutors.getBatchInsertExecutor();

        BenchmarkResult seqResult = null;
        BenchmarkResult batchResult = null;

        try {
            for (ExecutionMode mode : EXECUTION_ORDER) {
                switch (mode) {
                    case SEQUENTIAL -> seqResult = runBenchmark(
                            "Sequential inserts (Baseline)",
                            indexVectors,
                            queryVectors,
                            groundTruth,
                            idsToDelete,
                            vectorsToReinsert,
                            false,
                            null
                    );

                    case BATCH -> batchResult = runBenchmark(
                            "Parallel batch inserts",
                            indexVectors,
                            queryVectors,
                            groundTruth,
                            idsToDelete,
                            vectorsToReinsert,
                            true,
                            executor
                    );
                }
            }

            // =======================
            // Comparison
            // =======================
            if (seqResult != null && batchResult != null) {
                printComparison(seqResult, batchResult);
            }

        } finally {
            BenchmarkExecutors.shutdown();
            System.out.println("\nExecutor shutdown complete");
        }
    }

    // =========================================================
    // Benchmark Runner
    // =========================================================

    private static BenchmarkResult runBenchmark(
            String title,
            List<Vector> indexVectors,
            List<Vector> queryVectors,
            List<int[]> groundTruth,
            List<String> idsToDelete,
            List<Vector> vectorsToReinsert,
            boolean parallel,
            ExecutorService executor
    ) {

        System.out.println("=== Test : " + title + " ===");

        VectorIndex index = parallel
                ? new JVectorHNSWIndex(16, 200, 200, executor)
                : new JVectorHNSWIndex(16, 200, 200);

        index.build(indexVectors);
        System.out.println("Initial size: " + index.size() + " vectors");

        // delete
        System.out.println("Deleting " + VECTORS_TO_REINSERT + " vectors");
        for (String id : idsToDelete) {
            index.delete(id);
        }
        System.out.println("After delete: " + index.size() + " vectors");

        // reinsert
        System.out.println("Re-inserting " + VECTORS_TO_REINSERT +
                (parallel ? " vectors in parallel" : " vectors sequentially"));

        long start = System.currentTimeMillis();
        for (Vector v : vectorsToReinsert) {
            index.insert(v);
        }
        long timeMs = System.currentTimeMillis() - start;

        double throughput = 1000.0 * VECTORS_TO_REINSERT / timeMs;

        System.out.println("Insert time: " + timeMs + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " inserts/sec");
        System.out.println("After re-insert: " + index.size() + " vectors");

        Metrics searchMetrics =
                BenchmarkRunner.measureSearchOnly(index, queryVectors, K, "sift");

        double recall =
                calculateAverageRecall(index, queryVectors, groundTruth);

        System.out.println("Search p50: " +
                String.format("%.2f", searchMetrics.getQueryLatencyP50Micros()));
        System.out.println("Recall@10: " +
                String.format("%.4f", recall) + "\n");

        return new BenchmarkResult(
                title,
                timeMs,
                throughput,
                searchMetrics.getQueryLatencyP50Micros(),
                recall
        );
    }

    // =========================================================
    // Comparison
    // =========================================================

    private static void printComparison(
            BenchmarkResult seq,
            BenchmarkResult batch
    ) {
        System.out.println("=== Performance Comparison ===");

        printResult(seq);
        printResult(batch);

        double speedup = (double) seq.timeMs / batch.timeMs;
        double recallDiff = Math.abs(seq.recall - batch.recall);

        System.out.printf("%nSpeedup: %.2fx faster%n", speedup);
        System.out.printf("Recall difference: %.4f (%.2f%%)%n",
                recallDiff, recallDiff / seq.recall * 100);

        if (recallDiff < 0.001) {
            System.out.println("Both methods produce equivalent search quality");
        }
    }

    private static void printResult(BenchmarkResult r) {
        System.out.println("\n" + r.name + ":");
        System.out.println("  Time:        " + r.timeMs + " ms");
        System.out.println("  Throughput:  " +
                String.format("%.2f", r.throughput) + " inserts/sec");
        System.out.println("  Search P50:  " +
                String.format("%.2f", r.searchP50Micros) + " μs");
        System.out.println("  Recall@10:   " +
                String.format("%.4f", r.recall));
    }

    // =========================================================
    // Utilities
    // =========================================================

    private static double calculateAverageRecall(
            VectorIndex index,
            List<Vector> queryVectors,
            List<int[]> groundTruth
    ) {
        List<Double> recalls = new ArrayList<>();

        for (int i = 0; i < queryVectors.size(); i++) {
            List<QueryResult> results =
                    index.search(queryVectors.get(i).vector(), BatchInsertBenchmark.K, "sift");

            recalls.add(
                    BenchmarkRunner.calculateRecall(results, groundTruth.get(i), BatchInsertBenchmark.K)
            );
        }

        return recalls.stream().mapToDouble(d -> d).average().orElse(0.0);
    }

    // =========================================================
    // Result DTO
    // =========================================================

    private record BenchmarkResult(String name, long timeMs, double throughput, double searchP50Micros, double recall) {
    }
}
