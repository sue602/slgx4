package com.slgx4.territory.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GridMapTest {
    @Test
    void returnsAllOwnedPointsIncludingDisconnectedTerritory() {
        GridMap map = territoryWithDisconnectedIsland();

        List<GridPoint> owned = map.ownedPoints(Faction.BLUE);

        assertEquals(4, owned.size());
        assertTrue(owned.contains(new GridPoint(4, 1)));
        assertTrue(owned.contains(new GridPoint(5, 1)));
    }

    @Test
    void returnsOnlyTerritoryConnectedToCore() {
        GridMap map = territoryWithDisconnectedIsland();

        List<GridPoint> connected = map.connectedOwnedPoints(Faction.BLUE);

        assertEquals(List.of(new GridPoint(0, 1), new GridPoint(1, 1)), connected);
    }

    @Test
    void returnedListsAreReadOnlySnapshots() {
        GridMap map = territoryWithDisconnectedIsland();
        List<GridPoint> owned = map.ownedPoints(Faction.BLUE);

        map.setOwner(new GridPoint(2, 1), Faction.BLUE);

        assertEquals(4, owned.size());
        assertThrows(UnsupportedOperationException.class,
                () -> owned.add(new GridPoint(3, 1)));
    }

    @Test
    void separatesOwnedTerritoryIntoFourConnectedRegions() {
        GridMap map = territoryWithDisconnectedIsland();

        List<Set<GridPoint>> regions = map.ownedRegions(Faction.BLUE);

        assertEquals(2, regions.size());
        assertEquals(Set.of(new GridPoint(0, 1), new GridPoint(1, 1)), regions.get(0));
        assertEquals(Set.of(new GridPoint(4, 1), new GridPoint(5, 1)), regions.get(1));
        assertThrows(UnsupportedOperationException.class,
                () -> regions.get(0).add(new GridPoint(2, 1)));
        assertThrows(UnsupportedOperationException.class,
                () -> regions.add(Set.of(new GridPoint(3, 1))));
    }

    @Test
    void findsFactionAndCompleteRegionAtPoint() {
        GridMap map = territoryWithDisconnectedIsland();

        TerritoryRegion hit = map.ownedRegionAt(new GridPoint(4, 1)).orElseThrow();

        assertEquals(Faction.BLUE, hit.faction());
        assertEquals(Set.of(new GridPoint(4, 1), new GridPoint(5, 1)), hit.cells());
        assertTrue(hit.contains(new GridPoint(5, 1)));
        assertThrows(UnsupportedOperationException.class,
                () -> hit.cells().add(new GridPoint(3, 1)));
    }

    @Test
    void returnsEmptyForNeutralBlockedAndOutOfBoundsPoints() {
        GridMap map = territoryWithDisconnectedIsland();
        map.setBlocked(new GridPoint(2, 0), true);

        assertTrue(map.ownedRegionAt(new GridPoint(2, 1)).isEmpty());
        assertTrue(map.ownedRegionAt(new GridPoint(2, 0)).isEmpty());
        assertTrue(map.ownedRegionAt(new GridPoint(-1, 0)).isEmpty());
        assertTrue(map.ownedRegionAt(null).isEmpty());
    }

    @Test
    void connectsCandidateToAnyFactionRegionIncludingDisconnectedIsland() {
        GridMap map = territoryWithDisconnectedIsland();

        assertTrue(map.canConnectToFaction(new GridPoint(2, 1), Faction.BLUE));
        assertTrue(map.canConnectToFaction(new GridPoint(3, 1), Faction.BLUE));
        assertTrue(map.canConnectToFaction(new GridPoint(4, 1), Faction.BLUE));
        assertFalse(map.canConnectToFaction(new GridPoint(2, 0), Faction.BLUE));
    }

    @Test
    void connectsCandidateOnlyToCoreEffectiveTerritory() {
        GridMap map = territoryWithDisconnectedIsland();

        assertTrue(map.canConnectToCoreTerritory(new GridPoint(2, 1), Faction.BLUE));
        assertFalse(map.canConnectToCoreTerritory(new GridPoint(3, 1), Faction.BLUE));
        assertFalse(map.canConnectToCoreTerritory(new GridPoint(4, 1), Faction.BLUE));
        assertTrue(map.canConnectToCoreTerritory(new GridPoint(1, 1), Faction.BLUE));
    }

    @Test
    void rejectsInvalidConnectionCandidates() {
        GridMap map = territoryWithDisconnectedIsland();
        map.setBlocked(new GridPoint(2, 1), true);
        map.setOwner(new GridPoint(3, 1), Faction.RED);

        assertFalse(map.canConnectToFaction(new GridPoint(2, 1), Faction.BLUE));
        assertFalse(map.canConnectToCoreTerritory(new GridPoint(2, 1), Faction.BLUE));
        assertFalse(map.canConnectToFaction(new GridPoint(3, 1), Faction.BLUE));
        assertFalse(map.canConnectToCoreTerritory(new GridPoint(3, 1), Faction.BLUE));
        assertFalse(map.canConnectToFaction(new GridPoint(-1, 0), Faction.BLUE));
        assertFalse(map.canConnectToFaction(null, Faction.BLUE));
        assertFalse(map.canConnectToFaction(new GridPoint(0, 1), Faction.NONE));
        assertFalse(map.canConnectToFaction(new GridPoint(0, 1), null));
    }

    private static GridMap territoryWithDisconnectedIsland() {
        GridMap map = new GridMap(6, 3);
        map.setCore(Faction.BLUE, new GridPoint(0, 1));
        map.setOwner(new GridPoint(1, 1), Faction.BLUE);
        map.setOwner(new GridPoint(4, 1), Faction.BLUE);
        map.setOwner(new GridPoint(5, 1), Faction.BLUE);
        return map;
    }
}
