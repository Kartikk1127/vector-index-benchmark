package index.hnsw;

import core.QueryResult;
import core.Vector;
import core.VectorIndex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.jbellis.jvector.graph.*;
import io.github.jbellis.jvector.graph.similarity.BuildScoreProvider;
import io.github.jbellis.jvector.graph.similarity.DefaultSearchScoreProvider;
import io.github.jbellis.jvector.graph.similarity.SearchScoreProvider;
import io.github.jbellis.jvector.util.Bits;
import io.github.jbellis.jvector.vector.VectorSimilarityFunction;
import io.github.jbellis.jvector.vector.VectorizationProvider;
import io.github.jbellis.jvector.vector.types.VectorFloat;
import io.github.jbellis.jvector.vector.types.VectorTypeSupport;

public class JVectorHNSWIndex implements VectorIndex {
    private static final VectorTypeSupport vts = VectorizationProvider.getInstance().getVectorTypeSupport();
    private final int m;
    private final int efConstruction;
    private int efSearch; // not final since people tend to tune this during runtime

    private ImmutableGraphIndex graph;
    private List<Vector> vectors;
    private RandomAccessVectorValues ravv;
    private long distanceCalculations = 0;

    public JVectorHNSWIndex(int m, int efConstruction, int efSearch) {
        this.m = m;
        this.efConstruction = efConstruction;
        this.efSearch = efSearch;
    }

    @Override
    public void build(List<Vector> vectors) {
        System.out.println("Creating JVector HNSW index with M=" + m + ", efConstruction=" + efConstruction + ", efSearch=" + efSearch);
        System.out.println("Dataset size: " + vectors.size() + " vectors");

        this.vectors = vectors;
        long startTime = System.currentTimeMillis();

        // convert to vector float list
        ArrayList<VectorFloat<?>> jvectorVectors = new ArrayList<>();
        for (Vector v : vectors) {
            VectorFloat<?> vf = vts.createFloatVector(v.dimensions());
            for (int i = 0; i < v.dimensions(); i++) {
                vf.set(i, v.vector()[i]);
            }
            jvectorVectors.add(vf);
        }

        // create ravv
        int dimension = vectors.get(0).dimensions();
        this.ravv = new ListRandomAccessVectorValues(jvectorVectors,dimension);

        // build score provider
        BuildScoreProvider bsp = BuildScoreProvider.randomAccessScoreProvider(ravv, VectorSimilarityFunction.EUCLIDEAN);

        System.out.println("Index structure created, now adding vectors...");

        //build the graph
        try (GraphIndexBuilder builder = new GraphIndexBuilder(
                bsp,
                dimension,
                m,
                efConstruction,
                1.2f,
                1.2f,
                true,
                false
        )) {
            this.graph = builder.build(ravv);
        } catch (IOException e) {
            throw new RuntimeException("Failed to build JVector index", e);
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("Build completed in %.2fs\n", totalTime / 1000.0);
    }

    @Override
    public int size() {
        return vectors.size();
    }

    @Override
    public List<QueryResult> search(float[] query, int k, String dataset) {
        // convert query to vector float
        VectorFloat<?> queryVector = vts.createFloatVector(query.length);
        for (int i = 0; i < query.length; i++) {
            queryVector.set(i, query[i]);
        }

        try (GraphSearcher searcher = new GraphSearcher(graph)) {
            SearchScoreProvider ssp = DefaultSearchScoreProvider.exact(queryVector, VectorSimilarityFunction.EUCLIDEAN, ravv);
            SearchResult result = searcher.search(ssp, k, efSearch, 0.0F, 0.0F, Bits.ALL);

            // convert to our format
            List<QueryResult> results = new ArrayList<>();
            for (SearchResult.NodeScore ns : result.getNodes()) {
                String id = vectors.get(ns.node).id();
                results.add(new QueryResult(id, ns.score));
            }
            return results;
        } catch (IOException e) {
            throw new RuntimeException("Search failed", e);
        }
    }

    @Override
    public long getDistanceCalculations() {
        return 0;
    }

    @Override
    public void resetDistanceCalculations() {
        this.distanceCalculations = 0;
    }

    @Override
    public String getName() {
        return "JVector-HNSW";
    }

    @Override
    public void insert(Vector vector) {

    }

    @Override
    public void delete(String vectorId) {

    }
}
