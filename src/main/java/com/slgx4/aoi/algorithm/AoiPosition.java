package com.slgx4.aoi.algorithm;

/** AOI 世界中的三维坐标，对应原实现的 float pos[3]。 */
public record AoiPosition(float x, float y, float z) {
    public float distanceSquared(AoiPosition other) {
        float dx = x - other.x;
        float dy = y - other.y;
        float dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public static AoiPosition of(float x, float y) {
        return new AoiPosition(x, y, 0);
    }
}
