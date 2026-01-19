import benchmark.*;
import core.QueryResult;
import core.Vector;
import core.VectorIndex;
import dataset.DatasetLoader;
import index.hnsw.JVectorHNSWIndex;
import index.hnsw.JelmarkHNSWIndex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SiftDynamicMain {
    public static void main(String[] args) throws IOException, InterruptedException {

        // Configuration
        int vectorsToDelete = 1000;
        int finalDeletes = 5000;
        int k = 10;

        System.out.println("===Dynamic Benchmark: HNSW Index (SIFT Dataset)===");

        // Load SIFT dataset
        System.out.println("Loading SIFT dataset...");
        List<Vector> indexVectors = DatasetLoader.loadFVectors(
                "/Users/kartikeysrivastava/Desktop/projects/dataset/siftsmall-10k/siftsmall_base.fvecs");
        List<Vector> queryVectors = DatasetLoader.loadFVectors(
                "/Users/kartikeysrivastava/Desktop/projects/dataset/siftsmall-10k/siftsmall_query.fvecs");
        List<int[]> groundTruthBig = DatasetLoader.loadIVecs(
                "/Users/kartikeysrivastava/Desktop/projects/dataset/siftsmall-10k/siftsmall_groundtruth.ivecs");

        System.out.println("Dataset: " + indexVectors.size() + " vectors, " +
                queryVectors.size() + " queries");

        // Create index
        System.out.println("Creating HNSW index...");
        VectorIndex index = new JVectorHNSWIndex(8, 100, 200);

        // Test 1: Initial Build + Query
        System.out.println("\n===Test 1: Initial Build + Query===");
        Metrics initialMetrics = BenchmarkRunner.run(index, indexVectors, queryVectors, k, "sift");
        System.out.println("Build Time: " + initialMetrics.getBuildTimeMs() + " ms");
        System.out.println("Build Memory: " + initialMetrics.getBuildMemoryMB() + " MB");
        System.out.println("Query Latency P50: " + initialMetrics.getQueryLatencyP50Micros() + " μs");
        System.out.println("Throughput: " + initialMetrics.getThroughputQPS() + " QPS");
        System.out.println("Index Size: " + index.size() + " vectors");

        // Calculate initial recall
        double initialRecall = calculateAverageRecall(index, queryVectors, groundTruthBig, k);
        System.out.printf("Recall@%d: %.4f\n", k, initialRecall);

        // Test 2 : Delete 1k vectors (to prepare for re-insertion test)
        System.out.println("===Test 2: Delete performance (1000 vectors)===");
        List<String> idsToDelete = new ArrayList<>();
        List<Vector> vectorsToReinsert = new ArrayList<>();
        for (int i = 0; i < vectorsToDelete; i++) {
            idsToDelete.add(indexVectors.get(i).id());
            vectorsToReinsert.add(indexVectors.get(i));
        }

        DeleteMetrics deleteMetrics1 = BenchmarkRunner.benchmarkDeletes(index, idsToDelete);
        System.out.println("\n" + deleteMetrics1);
        System.out.println("Index size after deletes: " + index.size() + " vectors.");

        // measure search performance after deletion
        Metrics afterDeleteMetrics = BenchmarkRunner.measureSearchOnly(index, queryVectors, k,"sift");
        System.out.println("Query latency p50: " + afterDeleteMetrics.getQueryLatencyP50Micros() + " microSeconds");
        System.out.println("QPS: " + afterDeleteMetrics.getThroughputQPS());

        double afterDeleteRecall = calculateAverageRecall(index, queryVectors, groundTruthBig, k);
        System.out.printf("Recall@%d: %.4ff\n", k, afterDeleteRecall);
        double recallDrop = ((afterDeleteRecall-initialRecall)/initialRecall) * 100;
        System.out.printf("Recall change from deletion: %.2f%%\n", recallDrop);

        // Test 3: re-insert those 1k vectors
        System.out.println("===Test 3: Insert Performance (re-inserting 1000 vectors)===");
        InsertMetrics insertMetrics = BenchmarkRunner.benchmarkInserts(index, vectorsToReinsert);
        System.out.println("\n" + insertMetrics);
        System.out.println("Index size after inserts: " + index.size() + " vectors");

        // Test 4: Search performance after re-insertion
        System.out.println("===Test 4: Search performance after re-insertion===");
        Metrics afterInsertMetrics = BenchmarkRunner.measureSearchOnly(index,queryVectors,k,"sift");
        System.out.println("Query latency p50: " + afterInsertMetrics.getQueryLatencyP50Micros());
        System.out.println("QPS: " + afterDeleteMetrics.getThroughputQPS());

        double afterInsertRecall = calculateAverageRecall(index,queryVectors,groundTruthBig,k);
        System.out.printf("Recall@%d: %.4f\n", k, afterInsertRecall);
        double recallRecovery = ((afterInsertRecall-afterDeleteRecall)/afterDeleteRecall)*100;
        System.out.printf("Recall recovery from re-insertion: %.2f%%\n", recallRecovery);

        // calculate degradation from re-insertion
        double insertDegradation = ((afterInsertMetrics.getQueryLatencyP50Micros() - initialMetrics.getQueryLatencyP50Micros())/initialMetrics.getQueryLatencyP50Micros()) * 100;
        System.out.printf("Latency change from baseline: %.1f%% %s\n", Math.abs(insertDegradation), insertDegradation > 0 ? "slower" : "faster");

        // Test 5 : Large Deletion (5000 vectors)
        System.out.println("===Test 5: Large deletion performance and search degradation===");
        List<String> moreIdsToDelete = new ArrayList<>();
        for (int i = vectorsToDelete; i < vectorsToDelete + finalDeletes; i++) {
            moreIdsToDelete.add(indexVectors.get(i).id());
        }

        SearchDegradationMetrics degradationMetrics = BenchmarkRunner.benchmarkSearchDegradation(index,queryVectors,k,"sift",moreIdsToDelete);
        System.out.println("\n"+degradationMetrics);
        System.out.println("Final index size: " + index.size() + " vectors");

        double finalRecall = calculateAverageRecall(index,queryVectors,groundTruthBig,k);
        System.out.printf("Recall@%d: %.4f\n", k, finalRecall);

        // Summary
        System.out.println("\n=== Summary ===");
        System.out.println("Started with: " + indexVectors.size() + " vectors");
        System.out.println("Deleted: " + vectorsToDelete + " vectors (then re-inserted)");
        System.out.println("Finally deleted: " + finalDeletes + " more vectors");
        System.out.println("Final size: " + index.size() + " vectors");

        System.out.println("\nPerformance Impact:");
        System.out.printf("  Delete latency (1k): P50 = %.2f μs\n", deleteMetrics1.getP50Micros());
        System.out.printf("  Insert latency (1k): P50 = %.2f μs\n", insertMetrics.getP50Micros());
        System.out.printf("  Insert degradation: %.1f%%\n", Math.abs(insertDegradation));
        System.out.printf("  Delete degradation (5k): %.1f%%\n",
                Math.abs(degradationMetrics.getLatencyDegradationPercent()));

        System.out.println("\nRecall Impact:");
        System.out.printf("  Initial recall: %.4f (baseline)\n", initialRecall);
        System.out.printf("  After 1k deletes: %.4f (%.2f%% drop)\n",
                afterDeleteRecall, recallDrop);
        System.out.printf("  After re-insertion: %.4f (%.2f%% recovery)\n",
                afterInsertRecall, recallRecovery);
        System.out.printf("  After 5k deletes: %.4f (%.2f%% from baseline)\n",
                finalRecall,
                ((finalRecall - initialRecall) / initialRecall) * 100);
    }

    private static double calculateAverageRecall(VectorIndex index,
                                                 List<Vector> queryVectors,
                                                 List<int[]> groundTruth,
                                                 int k) {
        List<Double> recalls = new ArrayList<>();
        for (int i = 0; i < queryVectors.size(); i++) {
            List<QueryResult> results = index.search(queryVectors.get(i).vector(), k, "sift");
            double recall = BenchmarkRunner.calculateRecall(results, groundTruth.get(i), k);
            recalls.add(recall);
        }
        return recalls.stream().mapToDouble(d -> d).average().orElse(0.0);
    }
}