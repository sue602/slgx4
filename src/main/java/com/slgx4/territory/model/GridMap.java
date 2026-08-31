package com.slgx4.territory.model;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public final class GridMap {
    private static final int[][] FOUR_DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final int width;
    private final int height;
    private final Faction[][] owners;
    private final boolean[][] blocked;
    private final Map<Faction, GridPoint> cores = new EnumMap<>(Faction.class);

    public GridMap(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("地图尺寸必须大于 0");
        }
        this.width = width;
        this.height = height;
        this.owners = new Faction[height][width];
        this.blocked = new boolean[height][width];
        clear();
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean contains(GridPoint point) {
        return point != null && point.x() >= 0 && point.x() < width
                && point.y() >= 0 && point.y() < height;
    }

    public Faction ownerAt(GridPoint point) {
        requireInside(point);
        return owners[point.y()][point.x()];
    }

    public void setOwner(GridPoint point, Faction faction) {
        requireInside(point);
        owners[point.y()][point.x()] = Objects.requireNonNull(faction);
    }

    public boolean isBlocked(GridPoint point) {
        requireInside(point);
        return blocked[point.y()][point.x()];
    }

    public void setBlocked(GridPoint point, boolean value) {
        requireInside(point);
        blocked[point.y()][point.x()] = value;
        if (value) {
            owners[point.y()][point.x()] = Faction.NONE;
            cores.entrySet().removeIf(entry -> entry.getValue().equals(point));
        }
    }

    public void setCore(Faction faction, GridPoint point) {
        if (faction == Faction.NONE) {
            throw new IllegalArgumentException("中立方不能拥有主城");
        }
        requireInside(point);
        if (isBlocked(point)) {
            throw new IllegalArgumentException("主城不能放在障碍格上");
        }
        cores.put(faction, point);
        setOwner(point, faction);
    }

    public GridPoint coreOf(Faction faction) {
        return cores.get(faction);
    }

    public List<GridPoint> neighbors4(GridPoint point) {
        List<GridPoint> result = new ArrayList<>(4);
        for (int[] direction : FOUR_DIRECTIONS) {
            GridPoint neighbor = new GridPoint(point.x() + direction[0], point.y() + direction[1]);
            if (contains(neighbor)) {
                result.add(neighbor);
            }
        }
        return result;
    }

    public List<GridPoint> points() {
        List<GridPoint> result = new ArrayList<>(width * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.add(new GridPoint(x, y));
            }
        }
        return result;
    }

    public long countOwned(Faction faction) {
        return ownedPoints(faction).size();
    }

    /**
     * 返回阵营占领的全部格子，包括未与主城连通的失联区域。
     * 返回列表是只读快照，不会随地图后续变化而改变。
     */
    public List<GridPoint> ownedPoints(Faction faction) {
        Objects.requireNonNull(faction, "阵营不能为空");
        return points().stream()
                .filter(point -> ownerAt(point) == faction)
                .toList();
    }

    /**
     * 返回阵营主城通过四方向路径能够到达的有效领土。
     * 阵营没有主城或主城格已不属于该阵营时返回空列表。
     */
    public List<GridPoint> connectedOwnedPoints(Faction faction) {
        Objects.requireNonNull(faction, "阵营不能为空");
        GridPoint core = coreOf(faction);
        if (core == null || isBlocked(core) || ownerAt(core) != faction) {
            return List.of();
        }

        return List.copyOf(collectOwnedRegion(core, faction, new LinkedHashSet<>()));
    }

    /**
     * 按四方向连通性返回阵营拥有的全部区域块，失联领土会成为独立区域块。
     * 外层列表和每个区域集合都是只读快照；区域按首个格子的地图扫描顺序排列。
     */
    public List<Set<GridPoint>> ownedRegions(Faction faction) {
        Objects.requireNonNull(faction, "阵营不能为空");
        List<Set<GridPoint>> regions = new ArrayList<>();
        Set<GridPoint> visited = new LinkedHashSet<>();
        for (GridPoint point : ownedPoints(faction)) {
            if (!visited.contains(point)) {
                Set<GridPoint> region = collectOwnedRegion(point, faction, visited);
                regions.add(Collections.unmodifiableSet(new LinkedHashSet<>(region)));
            }
        }
        return List.copyOf(regions);
    }

    /**
     * 查找格子所在的阵营区域块。命中时返回阵营和完整四连通区域；
     * 中立格、障碍格、null 或越界坐标返回 Optional.empty()。
     */
    public Optional<TerritoryRegion> ownedRegionAt(GridPoint point) {
        if (!contains(point) || isBlocked(point)) {
            return Optional.empty();
        }
        Faction faction = ownerAt(point);
        if (faction == Faction.NONE) {
            return Optional.empty();
        }
        Set<GridPoint> region = collectOwnedRegion(point, faction, new LinkedHashSet<>());
        return Optional.of(new TerritoryRegion(faction, region));
    }

    private Set<GridPoint> collectOwnedRegion(GridPoint start, Faction faction, Set<GridPoint> visited) {
        Set<GridPoint> region = new LinkedHashSet<>();
        Queue<GridPoint> queue = new ArrayDeque<>();
        visited.add(start);
        region.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            GridPoint current = queue.remove();
            for (GridPoint next : neighbors4(current)) {
                if (!visited.contains(next) && !isBlocked(next) && ownerAt(next) == faction) {
                    visited.add(next);
                    region.add(next);
                    queue.add(next);
                }
            }
        }
        return region;
    }

    public void clear() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                owners[y][x] = Faction.NONE;
                blocked[y][x] = false;
            }
        }
        cores.clear();
    }

    private void requireInside(GridPoint point) {
        if (!contains(point)) {
            throw new IndexOutOfBoundsException("格子不在地图内: " + point);
        }
    }
}
