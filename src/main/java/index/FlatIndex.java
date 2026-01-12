package index;

import core.DistanceMetric;
import core.QueryResult;
import core.Vector;
import core.VectorIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlatIndex implements VectorIndex {

    private List<Vector> vectors;
    long distanceCalculations;

    public FlatIndex() {
        this.vectors = new ArrayList<>();
        this.distanceCalculations = 0;
    }

    @Override
    public void build(List<Vector> vectors) {
        this.vectors = vectors;
    }

    @Override
    public int size() {
        return vectors.size();
    }

    @Override
    public List<QueryResult> search(float[] query, int k, String dataset) {
        List<QueryResult> result = new ArrayList<>();
        for (Vector vector : vectors) {
            float distance = DistanceMetric.calculateDistance(query, vector.vector(), dataset);
            distanceCalculations++;
            QueryResult queryResult = new QueryResult(vector.id(), distance);
            result.add(queryResult);
        }
        Collections.sort(result);
        return result.subList(0, Math.min(k, result.size()));
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
        return "FLAT";
    }
}
