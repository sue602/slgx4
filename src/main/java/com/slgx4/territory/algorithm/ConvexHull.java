package com.slgx4.territory.algorithm;

import com.slgx4.territory.model.Faction;
import com.slgx4.territory.model.GridMap;
import com.slgx4.territory.model.GridPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ConvexHull {
    private ConvexHull() {
    }

    /** 使用 Andrew 单调链算法，时间复杂度 O(n log n)。 */
    public static List<GridPoint> compute(Collection<GridPoint> points) {
        List<GridPoint> sorted = points.stream().distinct().sorted().toList();
        if (sorted.size() <= 2) {
            return new ArrayList<>(sorted);
        }

        List<GridPoint> lower = new ArrayList<>();
        for (GridPoint point : sorted) {
            while (lower.size() >= 2
                    && cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), point) <= 0) {
                lower.remove(lower.size() - 1);
            }
            lower.add(point);
        }

        List<GridPoint> upper = new ArrayList<>();
        for (int i = sorted.size() - 1; i >= 0; i--) {
            GridPoint point = sorted.get(i);
            while (upper.size() >= 2
                    && cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), point) <= 0) {
                upper.remove(upper.size() - 1);
            }
            upper.add(point);
        }

        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);
        lower.addAll(upper);
        return lower;
    }

    /** 射线法填充凸包内部格子；敌方和障碍格不会被覆盖。 */
    public static Set<GridPoint> fillTerritory(GridMap map, Collection<GridPoint> posts, Faction faction) {
        List<GridPoint> hull = compute(posts);
        Set<GridPoint> filled = new LinkedHashSet<>();
        if (hull.size() < 3 || faction == Faction.NONE) {
            return filled;
        }
        for (GridPoint point : map.points()) {
            if (!map.isBlocked(point)
                    && (map.ownerAt(point) == Faction.NONE || map.ownerAt(point) == faction)
                    && contains(hull, point.x(), point.y())) {
                map.setOwner(point, faction);
                filled.add(point);
            }
        }
        return filled;
    }

    public static boolean contains(List<GridPoint> polygon, double x, double y) {
        if (polygon.size() < 3) {
            return false;
        }
        boolean inside = false;
        for (int i = 0, previous = polygon.size() - 1; i < polygon.size(); previous = i++) {
            GridPoint first = polygon.get(previous);
            GridPoint second = polygon.get(i);
            if (onSegment(first, second, x, y)) {
                return true;
            }
            boolean crossesY = (first.y() > y) != (second.y() > y);
            if (crossesY) {
                double intersectionX = (double) (second.x() - first.x()) * (y - first.y())
                        / (second.y() - first.y()) + first.x();
                if (x < intersectionX) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    private static long cross(GridPoint origin, GridPoint first, GridPoint second) {
        return (long) (first.x() - origin.x()) * (second.y() - origin.y())
                - (long) (first.y() - origin.y()) * (second.x() - origin.x());
    }

    private static boolean onSegment(GridPoint first, GridPoint second, double x, double y) {
        double cross = (second.x() - first.x()) * (y - first.y())
                - (second.y() - first.y()) * (x - first.x());
        if (Math.abs(cross) > 1.0e-9) {
            return false;
        }
        return x >= Math.min(first.x(), second.x()) && x <= Math.max(first.x(), second.x())
                && y >= Math.min(first.y(), second.y()) && y <= Math.max(first.y(), second.y());
    }
}

