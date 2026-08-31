package com.slgx4.territory.algorithm;

import com.slgx4.territory.model.Faction;
import com.slgx4.territory.model.GridMap;
import com.slgx4.territory.model.GridPoint;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class FloodFill {
    private FloodFill() {
    }

    /**
     * 计算从起点出发、四方向移动、受半径和障碍限制的 BFS 距离。
     * 敌方格子视为不可穿越，LinkedHashMap 保留 BFS 动画所需的层序。
     */
    public static Map<GridPoint, Integer> reachable(GridMap map, GridPoint start,
                                                     Faction faction, int maxDistance) {
        Map<GridPoint, Integer> distance = new LinkedHashMap<>();
        if (!map.contains(start) || map.isBlocked(start) || faction == Faction.NONE || maxDistance < 0
                || (map.ownerAt(start) != Faction.NONE && map.ownerAt(start) != faction)) {
            return distance;
        }

        Queue<GridPoint> queue = new ArrayDeque<>();
        queue.add(start);
        distance.put(start, 0);

        while (!queue.isEmpty()) {
            GridPoint current = queue.remove();
            int currentDistance = distance.get(current);
            if (currentDistance == maxDistance) {
                continue;
            }
            for (GridPoint next : map.neighbors4(current)) {
                Faction owner = map.ownerAt(next);
                boolean passable = !map.isBlocked(next) && (owner == Faction.NONE || owner == faction);
                if (passable && !distance.containsKey(next)) {
                    distance.put(next, currentDistance + 1);
                    queue.add(next);
                }
            }
        }
        return distance;
    }

    public static Set<GridPoint> expand(GridMap map, GridPoint start, Faction faction, int maxDistance) {
        Set<GridPoint> occupied = new LinkedHashSet<>();
        for (GridPoint point : reachable(map, start, faction, maxDistance).keySet()) {
            map.setOwner(point, faction);
            occupied.add(point);
        }
        return occupied;
    }

    public static Set<GridPoint> connectedTerritory(GridMap map, GridPoint core, Faction faction) {
        Set<GridPoint> connected = new LinkedHashSet<>();
        if (!map.contains(core) || map.isBlocked(core) || map.ownerAt(core) != faction) {
            return connected;
        }

        Queue<GridPoint> queue = new ArrayDeque<>();
        queue.add(core);
        connected.add(core);
        while (!queue.isEmpty()) {
            GridPoint current = queue.remove();
            for (GridPoint next : map.neighbors4(current)) {
                if (!connected.contains(next) && !map.isBlocked(next) && map.ownerAt(next) == faction) {
                    connected.add(next);
                    queue.add(next);
                }
            }
        }
        return connected;
    }

    /** 移除所有无法通过四连通路径回到联盟主城的领土。 */
    public static Set<GridPoint> removeDisconnected(GridMap map, Faction faction) {
        GridPoint core = map.coreOf(faction);
        Set<GridPoint> connected = connectedTerritory(map, core, faction);
        Set<GridPoint> removed = new LinkedHashSet<>();
        for (GridPoint point : map.points()) {
            if (map.ownerAt(point) == faction && !connected.contains(point)) {
                map.setOwner(point, Faction.NONE);
                removed.add(point);
            }
        }
        return removed;
    }
}
