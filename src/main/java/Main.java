import benchmark.BenchmarkRunner;
import benchmark.Metrics;
import core.Vector;
import core.VectorIndex;
import dataset.RandomVectorGenerator;
import index.FlatIndex;

import java.util.List;

public class Main {
    public static void main(String[] args) {

        // configuration
        int indexSize = 1000;
        int dimensions = 128;
        int queryCount = 100;
        int k = 10;
        long seed = 42;

        System.out.println("Generating Vectors");
        RandomVectorGenerator generator = new RandomVectorGenerator(seed);
        List<Vector> indexVectors = generator.generate(indexSize,dimensions);
        List<Vector> queryVectors = generator.generate(queryCount, dimensions);

        System.out.println("Creating flat index...");
        VectorIndex index = new FlatIndex();

        // run the benchmark
        System.out.println("Running benchmark");
        Metrics metrics = BenchmarkRunner.run(index, indexVectors, queryVectors, k);

        // print results
        System.out.println("\n=== Benchmark Results ===");
        System.out.println("Build Time: " + metrics.getBuildTimeMs() + " ms");
        System.out.println("Build Memory: " + metrics.getBuildMemoryMB() + " MB");
        System.out.println("Query Latency P50: " + metrics.getQueryLatencyP50Micros() + " μs");
        System.out.println("Query Latency P95: " + metrics.getQueryLatencyP95Micros() + " μs");
        System.out.println("Query Latency P99: " + metrics.getQueryLatencyP99Micros() + " μs");
        System.out.println("Throughput: " + metrics.getThroughputQPS() + " QPS");
        System.out.println("Avg Distance Calculations: " + metrics.getAvgDistanceCalculations());

    }
}
