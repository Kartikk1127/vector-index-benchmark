package core;

import java.util.Arrays;

public class Vector {
    String id;
    float[] data;

    public Vector(String id, float[] val) {
        this.id = id;
        this.data = val;
    }

    public String getId() {
        return id;
    }

    public float[] getData() {
        return data;
    }

    public long getDimensions() {
        return data.length;
    }

    @Override
    public String toString() {
        return "Vector{" +
                "id='" + id + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
