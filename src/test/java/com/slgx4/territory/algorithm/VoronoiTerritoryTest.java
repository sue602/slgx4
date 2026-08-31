package com.slgx4.territory.algorithm;

import com.slgx4.territory.model.Faction;
import com.slgx4.territory.model.GridMap;
import com.slgx4.territory.model.GridPoint;
import com.slgx4.territory.model.Outpost;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoronoiTerritoryTest {
    @Test
    void assignsNearestSiteAndLeavesCrossFactionTieNeutral() {
        GridMap map = new GridMap(5, 1);
        List<Outpost> sites = List.of(
                new Outpost(new GridPoint(0, 0), Faction.BLUE),
                new Outpost(new GridPoint(4, 0), Faction.RED));

        VoronoiTerritory.assign(map, sites);

        assertEquals(Faction.BLUE, map.ownerAt(new GridPoint(0, 0)));
        assertEquals(Faction.BLUE, map.ownerAt(new GridPoint(1, 0)));
        assertEquals(Faction.NONE, map.ownerAt(new GridPoint(2, 0)));
        assertEquals(Faction.RED, map.ownerAt(new GridPoint(3, 0)));
        assertEquals(Faction.RED, map.ownerAt(new GridPoint(4, 0)));
    }
}

