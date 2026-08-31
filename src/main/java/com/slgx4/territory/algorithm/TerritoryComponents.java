package com.slgx4.territory.algorithm;

import com.slgx4.territory.model.Faction;
import com.slgx4.territory.model.GridMap;
import com.slgx4.territory.model.GridPoint;

import java.util.HashSet;
import java.util.Set;

public final class TerritoryComponents {
    private TerritoryComponents() {
    }

    public record Summary(int componentCount, int largestComponentSize, int occupiedCellCount) {
    }

    public static Summary analyze(GridMap map, Faction faction) {
        int totalCells = map.width() * map.height();
        UnionFind unionFind = new UnionFind(totalCells);
        int occupied = 0;

        for (GridPoint point : map.points()) {
            if (map.ownerAt(point) != faction) {
                continue;
            }
            occupied++;
            GridPoint right = new GridPoint(point.x() + 1, point.y());
            GridPoint down = new GridPoint(point.x(), point.y() + 1);
            if (map.contains(right) && map.ownerAt(right) == faction) {
                unionFind.union(indexOf(map, point), indexOf(map, right));
            }
            if (map.contains(down) && map.ownerAt(down) == faction) {
                unionFind.union(indexOf(map, point), indexOf(map, down));
            }
        }

        Set<Integer> roots = new HashSet<>();
        int largest = 0;
        for (GridPoint point : map.points()) {
            if (map.ownerAt(point) == faction) {
                int index = indexOf(map, point);
                roots.add(unionFind.find(index));
                largest = Math.max(largest, unionFind.componentSize(index));
            }
        }
        return new Summary(roots.size(), largest, occupied);
    }

    private static int indexOf(GridMap map, GridPoint point) {
        return point.y() * map.width() + point.x();
    }
}
