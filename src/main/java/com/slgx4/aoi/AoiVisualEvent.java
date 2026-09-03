package com.slgx4.aoi;

final class AoiVisualEvent {
    final long watcherId;
    final long markerId;
    int remainingFrames = 16;

    AoiVisualEvent(long watcherId, long markerId) {
        this.watcherId = watcherId;
        this.markerId = markerId;
    }
}
