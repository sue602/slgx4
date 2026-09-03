package com.slgx4.aoi.algorithm;

@FunctionalInterface
public interface AoiCallback {
    void onMessage(long watcherId, long markerId);
}
