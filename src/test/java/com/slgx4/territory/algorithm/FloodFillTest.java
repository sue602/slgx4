package com.slgx4.territory.algorithm;

import com.slgx4.territory.model.Faction;
import com.slgx4.territory.model.GridMap;
import com.slgx4.territory.model.GridPoint;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FloodFillTest {
    @Test
    void expandsInManhattanDistanceLayers() {
        GridMap map = new GridMap(7, 7);

        Set<GridPoint> occupied = FloodFill.expand(map, new GridPoint(3, 3), Faction.BLUE, 2);

        assertEquals(13, occupied.size());
        assertEquals(Faction.BLUE, map.ownerAt(new GridPoint(3, 1)));
        assertEquals(Faction.NONE, map.ownerAt(new GridPoint(3, 0)));
    }

    @Test
    void doesNotCrossObstaclesOrEnemyTerritory() {
        GridMap map = new GridMap(5, 1);
        map.setBlocked(new GridPoint(2, 0), true);

        Set<GridPoint> occupied = FloodFill.expand(map, new GridPoint(0, 0), Faction.BLUE, 10);

        assertEquals(Set.of(new GridPoint(0, 0), new GridPoint(1, 0)), occupied);
        assertFalse(occupied.contains(new GridPoint(3, 0)));
    }

    @Test
    void removesTerritoryDisconnectedFromCore() {
        GridMap map = new GridMap(5, 2);
        map.setCore(Faction.BLUE, new GridPoint(0, 0));
        map.setOwner(new GridPoint(1, 0), Faction.BLUE);
        map.setOwner(new GridPoint(3, 0), Faction.BLUE);
        map.setOwner(new GridPoint(4, 0), Faction.BLUE);

        Set<GridPoint> removed = FloodFill.removeDisconnected(map, Faction.BLUE);

        assertEquals(Set.of(new GridPoint(3, 0), new GridPoint(4, 0)), removed);
        assertEquals(Faction.BLUE, map.ownerAt(new GridPoint(1, 0)));
        assertEquals(Faction.NONE, map.ownerAt(new GridPoint(3, 0)));
    }
}

