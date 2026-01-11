package core;

public class DistanceMetric {
    public static float cosineDistance(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vectors must have the same length.");
        }
        float product = 0.0f;
        for (int i = 0; i < vector1.length; i ++) {
            product += vector1[i] * vector2[i];
        }
        return 1.0f - product;
    }
}
