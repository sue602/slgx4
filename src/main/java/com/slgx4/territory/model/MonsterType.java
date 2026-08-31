package com.slgx4.territory.model;

import java.awt.Color;

public enum MonsterType {
    BEAST("野兽", "兽", new Color(245, 158, 11)),
    UNDEAD("亡灵", "魂", new Color(167, 139, 250)),
    DEMON("恶魔", "魔", new Color(244, 63, 94)),
    DRAGON("龙族", "龙", new Color(250, 204, 21)),
    ELEMENTAL("元素", "元", new Color(34, 211, 238));

    private final String displayName;
    private final String symbol;
    private final Color color;

    MonsterType(String displayName, String symbol, Color color) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public String symbol() {
        return symbol;
    }

    public Color color() {
        return color;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

