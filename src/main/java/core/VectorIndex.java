package core;

import java.util.List;

public interface VectorIndex {
    void build(List<Vector> vectors);
    int size();
    List<QueryResult> search(float[] query, int k, String dataset);
    long getDistanceCalculations();
    void resetDistanceCalculations();
    String getName();
    void insert(Vector vector);
    void delete(String vectorId);
}
