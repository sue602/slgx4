package com.slgx4.territory.model;

import java.awt.Color;

public enum Faction {
    NONE("中立", new Color(92, 102, 116), new Color(52, 58, 68)),
    BLUE("苍穹联盟", new Color(59, 130, 246), new Color(147, 197, 253)),
    RED("赤焰联盟", new Color(239, 68, 68), new Color(252, 165, 165));

    private final String displayName;
    private final Color color;
    private final Color lightColor;

    Faction(String displayName, Color color, Color lightColor) {
        this.displayName = displayName;
        this.color = color;
        this.lightColor = lightColor;
    }

    public String displayName() {
        return displayName;
    }

    public Color color() {
        return color;
    }

    public Color lightColor() {
        return lightColor;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

