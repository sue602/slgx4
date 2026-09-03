package com.slgx4.aoi.algorithm;

/** Marker 进入 Watcher AOI 范围时产生的单向消息。 */
public record AoiEvent(long watcherId, long markerId) {
}
