package index;

import com.github.jelmerk.hnswlib.core.DistanceFunction;
import com.github.jelmerk.hnswlib.core.SearchResult;
import com.github.jelmerk.hnswlib.core.hnsw.HnswIndex;
import core.DistanceMetric;
import core.QueryResult;
import core.Vector;
import core.VectorIndex;

import java.util.ArrayList;
import java.util.List;

public class JelmarkHNSWIndex implements VectorIndex {
    private final int m;
    private final int efConstruction;
    private final int efSearch;
    private HnswIndex<String, float[], Vector, Float> index;
    private long distanceCalculations = 0;
    private final DistanceFunction<float[], Float> distanceFunction;

    public JelmarkHNSWIndex(int m, int efConstruction, int efSearch) {
        this.m = m;
        this.efConstruction = efConstruction;
        this.efSearch = efSearch;

        this.distanceFunction = (vector1, vector2) -> {
            distanceCalculations++;
            return new DistanceMetric().euclideanDistance(vector1, vector2);
        };
    }
    @Override
    public void build(List<Vector> vectors) {
        System.out.println("Creating HNSW index with M=" + m + ", efConstruction=" + efConstruction + ", efSearch=" + efSearch);
        System.out.println("Dataset size: " + vectors.size() + " vectors");

        long startTime = System.currentTimeMillis();

        // Create HNSW index
        this.index = HnswIndex
                .newBuilder(vectors.get(0).dimensions(), distanceFunction, vectors.size())
                .withM(m)
                .withEfConstruction(efConstruction)
                .withEf(efSearch)
                .build();

        System.out.println("Index structure created, now adding vectors...");

        // Add all vectors to index with progress tracking
        int progressInterval = vectors.size() / 10;  // Log every 10%
        for (int i = 0; i < vectors.size(); i++) {
            index.add(vectors.get(i));

            if (i > 0 && i % progressInterval == 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                double percentComplete = (i * 100.0) / vectors.size();
                System.out.printf("Progress: %.1f%% (%d/%d vectors) - Elapsed: %.2fs\n",
                        percentComplete, i, vectors.size(), elapsed / 1000.0);
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("Build completed in %.2fs\n", totalTime / 1000.0);
    }

    @Override
    public int size() {
        return index.size();
    }

    @Override
    public List<QueryResult> search(float[] query, int k, String dataset) {

        Vector queryVector = new Vector("query", query);
        List<SearchResult<Vector, Float>> results =
                index.findNearest(queryVector.vector(), k);

        List<QueryResult> searchResults = new ArrayList<>();
        for (SearchResult<Vector, Float> result : results) {
            searchResults.add(new QueryResult(result.item().id(), result.distance()));
        }

        return searchResults;
    }

    @Override
    public long getDistanceCalculations() {
        return distanceCalculations;
    }

    @Override
    public void resetDistanceCalculations() {
        this.distanceCalculations = 0;
    }

    @Override
    public String getName() {
        return "JelMark-HNSW";
    }
}
