package core;

import java.util.List;

public interface VectorIndex {
    void build(List<Vector> vectors);
    int size();
    List<SearchResult> search(float[] query, int k);
    long getDistanceCalculations();
    void resetDistanceCalculations();
}
