package index.hnsw;

import core.QueryResult;
import core.Vector;
import core.VectorIndex;

import java.io.IOException;
import java.util.*;

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

    private GraphIndexBuilder builder;
    private List<Vector> vectors;
    private List<VectorFloat<?>> jvectorVectors;
    private RandomAccessVectorValues ravv;
    private BuildScoreProvider bsp;
    private int dimension;
    private long distanceCalculations = 0;
    private Map<String, Integer> idToNodeMap;
    private int softDeleteCount;
    private int liveNodeCount;

    public JVectorHNSWIndex(int m, int efConstruction, int efSearch) {
        this.m = m;
        this.efConstruction = efConstruction;
        this.efSearch = efSearch;
        this.liveNodeCount = 0;
    }

    @Override
    public void build(List<Vector> vectors) {
        System.out.println("Creating JVector HNSW index with M=" + m + ", efConstruction=" + efConstruction + ", efSearch=" + efSearch);
        System.out.println("Dataset size: " + vectors.size() + " vectors");

        this.vectors = vectors;
        this.dimension = vectors.get(0).dimensions();
        long startTime = System.currentTimeMillis();

        // convert to vector float list
        this.jvectorVectors = new ArrayList<>();
        this.idToNodeMap = new HashMap<>();
        for (int i = 0; i < vectors.size(); i++) {
            Vector v = vectors.get(i);
            VectorFloat<?> vf = vts.createFloatVector(v.dimensions());
            for (int j = 0; j < v.dimensions(); j++) {
                vf.set(j, v.vector()[j]);
            }
            jvectorVectors.add(vf);
            idToNodeMap.put(v.id(), i);
        }

        // create ravv
        int dimension = vectors.get(0).dimensions();
        this.ravv = new ListRandomAccessVectorValues(jvectorVectors,dimension);

        // build score provider
        this.bsp = BuildScoreProvider.randomAccessScoreProvider(ravv, VectorSimilarityFunction.EUCLIDEAN);

        System.out.println("Index structure created, now adding vectors...");

        //build the graph
        this.builder = new GraphIndexBuilder(
                bsp,
                dimension,
                m,
                efConstruction,
                1.2f,
                1.2f,
                true,
                false
        );
        builder.build(ravv);
        this.liveNodeCount = builder.getGraph().size(0);

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("Build completed in %.2fs\n", totalTime / 1000.0);
    }

    @Override
    public int size() {
        return liveNodeCount;
    }

    @Override
    public List<QueryResult> search(float[] query, int k, String dataset) {
        // convert query to vector float
        VectorFloat<?> queryVector = vts.createFloatVector(query.length);
        for (int i = 0; i < query.length; i++) {
            queryVector.set(i, query[i]);
        }

        try (GraphSearcher searcher = new GraphSearcher(builder.getGraph())) {
            SearchScoreProvider ssp = bsp.searchProviderFor(queryVector);
            SearchResult result = searcher.search(ssp, k, efSearch, 0.0F, 0.0F, builder.getGraph().getView().liveNodes());

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
        VectorFloat<?> vf = vts.createFloatVector(vector.dimensions());
        for (int i = 0; i < vector.dimensions(); i++) {
            vf.set(i, vector.vector()[i]);
        }
        // add to lists
        int nodeId = vectors.size();
        vectors.add(vector);
        jvectorVectors.add(vf);
        idToNodeMap.put(vector.id(), nodeId);

        // use add graph node to insert in graph
        builder.addGraphNode(nodeId,vf);
        liveNodeCount++;
    }

    @Override
    public void delete(String vectorId) {
        Integer nodeId = idToNodeMap.get(vectorId);
        if (nodeId == null) return;
        builder.markNodeDeleted(nodeId);
        softDeleteCount++;
        liveNodeCount--;
        if (softDeleteCount>5000) {
//            System.out.println("Soft delete threshold reached (" + softDeleteCount + " deleted nodes). Triggering cleanup...");
            cleanup();
        }
    }

    // cleanup deleted nodes - this is the blocking compaction operation
    // call this periodically when delete percentage gets too high
    public long cleanup() {
        if (softDeleteCount==0) {
            System.out.println("No deleted nodes to cleanup");
            return 0;
        }
//        System.out.println("Starting JVector cleanup (compaction)...");
        long startTime = System.currentTimeMillis();
        long freedMemory = builder.removeDeletedNodes();

        softDeleteCount = 0;
        this.liveNodeCount = builder.getGraph().size(0);

        // J-vector's built-in cleanup repairs the graph and removes deleted nodes
        return freedMemory;
    }
}
