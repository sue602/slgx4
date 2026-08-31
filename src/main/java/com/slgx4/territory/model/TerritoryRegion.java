package com.slgx4.territory.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** 某个阵营拥有的一块四连通区域。 */
public record TerritoryRegion(Faction faction, Set<GridPoint> cells) {
    public TerritoryRegion {
        Objects.requireNonNull(faction, "阵营不能为空");
        Objects.requireNonNull(cells, "区域格子不能为空");
        if (faction == Faction.NONE) {
            throw new IllegalArgumentException("中立格不构成阵营区域块");
        }
        cells = Collections.unmodifiableSet(new LinkedHashSet<>(cells));
    }

    public boolean contains(GridPoint point) {
        return cells.contains(point);
    }
}

