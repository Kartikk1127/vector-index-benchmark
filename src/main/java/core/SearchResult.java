package core;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SearchResult implements Comparable<SearchResult> {
    String id;
    float distance;

    public SearchResult(String id, float distance) {
        this.id = id;
        this.distance = distance;
    }

    public String getId() {
        return id;
    }

    public float getDistance() {
        return distance;
    }

    @Override
    public int compareTo(SearchResult otherDistance) {
        return Float.compare(this.distance, otherDistance.distance);
    }
}
