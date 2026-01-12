package core;

import java.util.List;

public interface VectorIndex {
    void build(List<Vector> vectors);
    int size();
    List<SearchResult> search(float[] query, int k, String dataset);
    long getDistanceCalculations();
    void resetDistanceCalculations();
}
