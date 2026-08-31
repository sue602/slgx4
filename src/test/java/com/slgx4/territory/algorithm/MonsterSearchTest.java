package com.slgx4.territory.algorithm;

import com.slgx4.territory.model.GridPoint;
import com.slgx4.territory.model.Monster;
import com.slgx4.territory.model.MonsterType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MonsterSearchTest {
    private final List<Monster> monsters = List.of(
            new Monster("M01", "荒原狼", MonsterType.BEAST, 3, new GridPoint(0, 0)),
            new Monster("M02", "岩甲熊", MonsterType.BEAST, 6, new GridPoint(2, 0)),
            new Monster("M03", "游魂", MonsterType.UNDEAD, 5, new GridPoint(1, 1)),
            new Monster("M04", "炎魔", MonsterType.DEMON, 9, new GridPoint(4, 0)));

    @Test
    void filtersByTypeAndSortsNearestFirst() {
        List<MonsterSearchResult> result = MonsterSearch.withinRadius(
                monsters, new GridPoint(0, 0), 3, MonsterType.BEAST);

        assertEquals(List.of("M01", "M02"), result.stream()
                .map(item -> item.monster().id())
                .toList());
        assertEquals(List.of(0, 2), result.stream().map(MonsterSearchResult::distance).toList());
    }

    @Test
    void nullTypeSearchesAllMonsterTypesWithinRadius() {
        List<MonsterSearchResult> result = MonsterSearch.withinRadius(
                monsters, new GridPoint(0, 0), 2, null);

        assertEquals(List.of("M01", "M02", "M03"), result.stream()
                .map(item -> item.monster().id())
                .toList());
    }

    @Test
    void radiusZeroOnlyFindsMonsterAtCenter() {
        List<MonsterSearchResult> result = MonsterSearch.withinRadius(
                monsters, new GridPoint(2, 0), 0, null);

        assertEquals(List.of("M02"), result.stream().map(item -> item.monster().id()).toList());
    }

    @Test
    void rejectsNegativeRadius() {
        assertThrows(IllegalArgumentException.class,
                () -> MonsterSearch.withinRadius(monsters, new GridPoint(0, 0), -1, null));
    }
}
