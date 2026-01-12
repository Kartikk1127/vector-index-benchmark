package core;

import com.github.jelmerk.hnswlib.core.Item;

import java.util.Arrays;

public class Vector implements Item<String, float[]> {
    String id;
    float[] data;

    public Vector(String id, float[] val) {
        this.id = id;
        this.data = val;
    }
    @Override
    public String toString() {
        return "Vector{" +
                "id='" + id + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public float[] vector() {
        return data;
    }

    @Override
    public int dimensions() {
        return data.length;
    }
}
