package com.slgx4.territory.algorithm;

import com.slgx4.territory.model.Faction;
import com.slgx4.territory.model.GridMap;
import com.slgx4.territory.model.GridPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerritoryComponentsTest {
    @Test
    void reportsComponentsUsingUnionFind() {
        GridMap map = new GridMap(6, 3);
        map.setOwner(new GridPoint(0, 0), Faction.BLUE);
        map.setOwner(new GridPoint(1, 0), Faction.BLUE);
        map.setOwner(new GridPoint(1, 1), Faction.BLUE);
        map.setOwner(new GridPoint(4, 2), Faction.BLUE);
        map.setOwner(new GridPoint(5, 2), Faction.BLUE);

        TerritoryComponents.Summary summary = TerritoryComponents.analyze(map, Faction.BLUE);

        assertEquals(2, summary.componentCount());
        assertEquals(3, summary.largestComponentSize());
        assertEquals(5, summary.occupiedCellCount());
    }
}

