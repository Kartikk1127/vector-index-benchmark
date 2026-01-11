package index;

import core.DistanceMetric;
import core.SearchResult;
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
    public List<SearchResult> search(float[] query, int k) {
        List<SearchResult> result = new ArrayList<>();
        for (Vector vector : vectors) {
            float distance = DistanceMetric.cosineDistance(query, vector.getData());
            distanceCalculations++;
            SearchResult searchResult = new SearchResult(vector.getId(), distance);
            result.add(searchResult);
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
}
