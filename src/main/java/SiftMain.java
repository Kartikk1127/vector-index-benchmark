import benchmark.BenchmarkRunner;
import benchmark.Metrics;
import core.QueryResult;
import core.Vector;
import core.VectorIndex;
import dataset.DatasetLoader;
import index.FlatIndex;
import index.HNSWIndex;

import java.io.IOException;
import java.util.List;

public class SiftMain {
    public static void main(String[] args) throws IOException, InterruptedException {

        int k = 10;
//        List<Vector> indexVectors = DatasetLoader.loadFVectors("/Users/kartikeysrivastava/Desktop/projects/dataset/siftsmall-10k/siftsmall_base.fvecs");
//        List<Vector> queryVectors = DatasetLoader.loadFVectors("/Users/kartikeysrivastava/Desktop/projects/dataset/siftsmall-10k/siftsmall_query.fvecs");
        List<Vector> indexVectors = DatasetLoader.loadFVectors("/Users/kartikeysrivastava/Desktop/projects/dataset/sift-1M/sift_base.fvecs");
        List<Vector> queryVectors = DatasetLoader.loadFVectors("/Users/kartikeysrivastava/Desktop/projects/dataset/sift-1M/sift_query.fvecs");


//        VectorIndex index = new FlatIndex();
        VectorIndex index = new HNSWIndex(16, 100, 100);
        System.out.println("Creating" + index.getName() +  " index...");

        // run the benchmark
        System.out.println("Running benchmark using : " + index.getName() + " index" );
        Metrics metrics = BenchmarkRunner.run(index, indexVectors, queryVectors, k, "sift");

        // print results
        System.out.println("\n=== Benchmark Results ===");
        System.out.println("Build Time: " + metrics.getBuildTimeMs() + " ms");
        System.out.println("Build Memory: " + metrics.getBuildMemoryMB() + " MB");
        System.out.println("Query Latency P50: " + metrics.getQueryLatencyP50Micros() + " μs");
        System.out.println("Query Latency P95: " + metrics.getQueryLatencyP95Micros() + " μs");
        System.out.println("Query Latency P99: " + metrics.getQueryLatencyP99Micros() + " μs");
        System.out.println("Throughput: " + metrics.getThroughputQPS() + " QPS");
        System.out.println("Avg Distance Calculations: " + metrics.getAvgDistanceCalculations());

        // calculate recall
//        List<int []> groundTruthSmall = DatasetLoader.loadIVecs("/Users/kartikeysrivastava/Desktop/projects/dataset/siftsmall-10k/siftsmall_groundtruth.ivecs");
        List<int []> groundTruthBig = DatasetLoader.loadIVecs("/Users/kartikeysrivastava/Desktop/projects/dataset/sift-1M/sift_groundtruth.ivecs");

        for (int i = 0; i < queryVectors.size(); i++) {
            List<QueryResult> results = index.search(queryVectors.get(i).vector(), 10, "sift");
            double recall = BenchmarkRunner.calculateRecall(results, groundTruthBig.get(i), 10);
            System.out.println("Query " + i + " Recall@10: " + recall);
        }

    }
}
