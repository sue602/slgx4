package com.slgx4.aoi.algorithm;

public record AoiObjectSnapshot(
        long id,
        boolean watcher,
        boolean marker,
        boolean moving,
        boolean dropped,
        int version,
        AoiPosition lastKeyPosition,
        AoiPosition position) {
}
