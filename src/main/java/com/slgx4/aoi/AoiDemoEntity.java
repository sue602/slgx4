package com.slgx4.aoi;

import com.slgx4.aoi.algorithm.AoiPosition;

final class AoiDemoEntity {
    final long id;
    String mode;
    AoiPosition position;
    final float velocityX;
    final float velocityY;

    AoiDemoEntity(long id, String mode, AoiPosition position, float velocityX, float velocityY) {
        this.id = id;
        this.mode = mode;
        this.position = position;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }

    void advance() {
        float x = wrap(position.x() + velocityX);
        float y = wrap(position.y() + velocityY);
        float z = wrap(position.z());
        position = new AoiPosition(x, y, z);
    }

    boolean watcher() {
        return mode.indexOf('w') >= 0;
    }

    boolean marker() {
        return mode.indexOf('m') >= 0;
    }

    private static float wrap(float value) {
        if (value < 0) {
            return value + 100.0f;
        }
        if (value > 100.0f) {
            return value - 100.0f;
        }
        return value;
    }
}
