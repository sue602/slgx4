package com.slgx4.aoi.algorithm;

/** 当前保留在 R 到 2R 距离带内、等待后续判断的热点对。 */
public record AoiHotPairSnapshot(long watcherId, long markerId) {
}
