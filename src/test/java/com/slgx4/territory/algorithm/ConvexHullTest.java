package com.slgx4.territory.algorithm;

import com.slgx4.territory.model.Faction;
import com.slgx4.territory.model.GridMap;
import com.slgx4.territory.model.GridPoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConvexHullTest {
    private static final List<GridPoint> POSTS = List.of(
            new GridPoint(1, 1), new GridPoint(3, 1), new GridPoint(3, 3),
            new GridPoint(1, 3), new GridPoint(2, 2));

    @Test
    void excludesInteriorPointsFromHull() {
        List<GridPoint> hull = ConvexHull.compute(POSTS);

        assertEquals(4, hull.size());
        assertTrue(ConvexHull.contains(hull, 2, 2));
        assertTrue(ConvexHull.contains(hull, 1, 2));
        assertFalse(ConvexHull.contains(hull, 0, 0));
    }

    @Test
    void fillsGridCellsInsideHullWithoutOverwritingEnemy() {
        GridMap map = new GridMap(5, 5);
        map.setOwner(new GridPoint(2, 2), Faction.RED);

        Set<GridPoint> filled = ConvexHull.fillTerritory(map, POSTS, Faction.BLUE);

        assertEquals(8, filled.size());
        assertEquals(Faction.RED, map.ownerAt(new GridPoint(2, 2)));
        assertEquals(Faction.BLUE, map.ownerAt(new GridPoint(1, 1)));
    }
}

