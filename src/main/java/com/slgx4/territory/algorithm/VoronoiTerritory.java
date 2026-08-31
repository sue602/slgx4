package com.slgx4.territory.algorithm;

import com.slgx4.territory.model.Faction;
import com.slgx4.territory.model.GridMap;
import com.slgx4.territory.model.GridPoint;
import com.slgx4.territory.model.Outpost;

import java.util.Collection;

public final class VoronoiTerritory {
    private VoronoiTerritory() {
    }

    /**
     * 将每个非障碍格分给欧氏距离最近的据点。不同联盟等距时保留为中立，形成争议边界。
     */
    public static void assign(GridMap map, Collection<Outpost> sites) {
        for (GridPoint cell : map.points()) {
            if (map.isBlocked(cell)) {
                continue;
            }
            long bestDistance = Long.MAX_VALUE;
            Faction nearest = Faction.NONE;
            for (Outpost site : sites) {
                long distance = squaredDistance(cell, site.position());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    nearest = site.faction();
                } else if (distance == bestDistance && nearest != site.faction()) {
                    nearest = Faction.NONE;
                }
            }
            map.setOwner(cell, nearest);
        }
        // 核心和据点格必须归原联盟所有。
        for (Outpost site : sites) {
            if (map.contains(site.position()) && !map.isBlocked(site.position())) {
                map.setOwner(site.position(), site.faction());
            }
        }
    }

    private static long squaredDistance(GridPoint first, GridPoint second) {
        long deltaX = first.x() - second.x();
        long deltaY = first.y() - second.y();
        return deltaX * deltaX + deltaY * deltaY;
    }
}
