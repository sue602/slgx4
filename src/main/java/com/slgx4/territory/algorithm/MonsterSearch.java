package com.slgx4.territory.algorithm;

import com.slgx4.territory.model.GridPoint;
import com.slgx4.territory.model.Monster;
import com.slgx4.territory.model.MonsterType;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class MonsterSearch {
    private MonsterSearch() {
    }

    /**
     * 搜索中心点曼哈顿距离 R 内的怪物，并按距离、怪物编号升序返回。
     * typeFilter 为 null 时搜索全部怪物类型。
     */
    public static List<MonsterSearchResult> withinRadius(Collection<Monster> monsters,
                                                          GridPoint center,
                                                          int radius,
                                                          MonsterType typeFilter) {
        Objects.requireNonNull(monsters, "怪物集合不能为空");
        Objects.requireNonNull(center, "搜索中心不能为空");
        if (radius < 0) {
            throw new IllegalArgumentException("搜索半径不能小于 0");
        }

        return monsters.stream()
                .map(monster -> Objects.requireNonNull(monster, "怪物集合中不能包含 null"))
                .filter(monster -> typeFilter == null || monster.type() == typeFilter)
                .map(monster -> new MonsterSearchResult(monster, manhattanDistance(center, monster.position())))
                .filter(result -> result.distance() <= radius)
                .sorted(Comparator.comparingInt(MonsterSearchResult::distance)
                        .thenComparing(result -> result.monster().id()))
                .toList();
    }

    public static int manhattanDistance(GridPoint first, GridPoint second) {
        Objects.requireNonNull(first, "第一个坐标不能为空");
        Objects.requireNonNull(second, "第二个坐标不能为空");
        return Math.abs(first.x() - second.x()) + Math.abs(first.y() - second.y());
    }
}

