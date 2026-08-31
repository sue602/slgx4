package com.slgx4.territory.algorithm;

import com.slgx4.territory.model.Faction;
import com.slgx4.territory.model.GridMap;
import com.slgx4.territory.model.GridPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 对格子中心的二值占领采样执行 Marching Squares。
 * 地图外围补一圈未占领采样点，确保贴近地图边缘的领土也能生成闭合轮廓。
 */
public final class MarchingSquares {
    private MarchingSquares() {
    }

    public static List<LineSegment> extract(GridMap map, Faction faction) {
        List<LineSegment> segments = new ArrayList<>();
        for (int squareY = 0; squareY <= map.height(); squareY++) {
            for (int squareX = 0; squareX <= map.width(); squareX++) {
                int state = 0;
                if (occupied(map, faction, squareX, squareY)) {
                    state |= 1; // 左上
                }
                if (occupied(map, faction, squareX + 1, squareY)) {
                    state |= 2; // 右上
                }
                if (occupied(map, faction, squareX + 1, squareY + 1)) {
                    state |= 4; // 右下
                }
                if (occupied(map, faction, squareX, squareY + 1)) {
                    state |= 8; // 左下
                }
                appendCase(segments, state, squareX, squareY);
            }
        }
        return segments;
    }

    /**
     * 从一个区域块的格子中提取可直接依次连线的闭合轮廓。
     * 每条轮廓的最后一个点与第一个点相同；带孔区域会返回外轮廓和一个或多个内轮廓。
     */
    public static List<List<BoundaryPoint>> extractOrderedContours(Collection<GridPoint> region) {
        Objects.requireNonNull(region, "区域格子不能为空");
        Set<GridPoint> cells = new LinkedHashSet<>();
        for (GridPoint point : region) {
            cells.add(Objects.requireNonNull(point, "区域中不能包含 null 格子"));
        }
        if (cells.isEmpty()) {
            return List.of();
        }

        List<LineSegment> segments = extractSegments(cells);
        return stitchSegments(segments);
    }

    private static List<LineSegment> extractSegments(Set<GridPoint> cells) {
        int minX = cells.stream().mapToInt(GridPoint::x).min().orElseThrow();
        int maxX = cells.stream().mapToInt(GridPoint::x).max().orElseThrow();
        int minY = cells.stream().mapToInt(GridPoint::y).min().orElseThrow();
        int maxY = cells.stream().mapToInt(GridPoint::y).max().orElseThrow();

        List<LineSegment> segments = new ArrayList<>();
        for (int squareY = minY; squareY <= maxY + 1; squareY++) {
            for (int squareX = minX; squareX <= maxX + 1; squareX++) {
                int state = 0;
                if (cells.contains(new GridPoint(squareX - 1, squareY - 1))) {
                    state |= 1;
                }
                if (cells.contains(new GridPoint(squareX, squareY - 1))) {
                    state |= 2;
                }
                if (cells.contains(new GridPoint(squareX, squareY))) {
                    state |= 4;
                }
                if (cells.contains(new GridPoint(squareX - 1, squareY))) {
                    state |= 8;
                }
                appendCase(segments, state, squareX, squareY);
            }
        }
        return segments;
    }

    private static List<List<BoundaryPoint>> stitchSegments(List<LineSegment> segments) {
        List<ContourEdge> edges = new ArrayList<>(segments.size());
        Map<PointKey, List<Integer>> adjacency = new LinkedHashMap<>();
        for (int index = 0; index < segments.size(); index++) {
            LineSegment segment = segments.get(index);
            PointKey first = PointKey.from(segment.x1(), segment.y1());
            PointKey second = PointKey.from(segment.x2(), segment.y2());
            edges.add(new ContourEdge(first, second));
            adjacency.computeIfAbsent(first, ignored -> new ArrayList<>()).add(index);
            adjacency.computeIfAbsent(second, ignored -> new ArrayList<>()).add(index);
        }

        boolean[] used = new boolean[edges.size()];
        List<List<BoundaryPoint>> contours = new ArrayList<>();
        for (int initialEdge = 0; initialEdge < edges.size(); initialEdge++) {
            if (used[initialEdge]) {
                continue;
            }
            ContourEdge firstEdge = edges.get(initialEdge);
            PointKey start = firstEdge.first();
            PointKey current = start;
            int edgeIndex = initialEdge;
            List<BoundaryPoint> contour = new ArrayList<>();
            contour.add(start.toBoundaryPoint());

            while (true) {
                used[edgeIndex] = true;
                PointKey next = edges.get(edgeIndex).other(current);
                contour.add(next.toBoundaryPoint());
                if (next.equals(start)) {
                    break;
                }

                int nextEdge = nextUnusedEdge(adjacency.getOrDefault(next, List.of()), used);
                if (nextEdge < 0) {
                    throw new IllegalStateException("Marching Squares 生成了未闭合的轮廓");
                }
                current = next;
                edgeIndex = nextEdge;
            }
            contours.add(List.copyOf(contour));
        }
        return List.copyOf(contours);
    }

    private static int nextUnusedEdge(List<Integer> candidates, boolean[] used) {
        for (int candidate : candidates) {
            if (!used[candidate]) {
                return candidate;
            }
        }
        return -1;
    }

    private static boolean occupied(GridMap map, Faction faction, int sampleX, int sampleY) {
        int gridX = sampleX - 1;
        int gridY = sampleY - 1;
        GridPoint point = new GridPoint(gridX, gridY);
        return map.contains(point) && map.ownerAt(point) == faction;
    }

    private static void appendCase(List<LineSegment> result, int state, int x, int y) {
        // 采样点坐标分别位于 (index - 0.5)，下列四点为方格四条边的中点。
        double topX = x;
        double topY = y - 0.5;
        double rightX = x + 0.5;
        double rightY = y;
        double bottomX = x;
        double bottomY = y + 0.5;
        double leftX = x - 0.5;
        double leftY = y;

        switch (state) {
            case 1, 14 -> add(result, topX, topY, leftX, leftY);
            case 2, 13 -> add(result, topX, topY, rightX, rightY);
            case 3, 12 -> add(result, leftX, leftY, rightX, rightY);
            case 4, 11 -> add(result, rightX, rightY, bottomX, bottomY);
            case 5 -> {
                // 对角占领按四连通处理为两个独立轮廓。
                add(result, topX, topY, leftX, leftY);
                add(result, rightX, rightY, bottomX, bottomY);
            }
            case 6, 9 -> add(result, topX, topY, bottomX, bottomY);
            case 7, 8 -> add(result, leftX, leftY, bottomX, bottomY);
            case 10 -> {
                add(result, topX, topY, rightX, rightY);
                add(result, bottomX, bottomY, leftX, leftY);
            }
            default -> {
                // 0 和 15 均无边界。
            }
        }
    }

    private static void add(List<LineSegment> result, double x1, double y1, double x2, double y2) {
        result.add(new LineSegment(x1, y1, x2, y2));
    }

    private record ContourEdge(PointKey first, PointKey second) {
        PointKey other(PointKey point) {
            if (first.equals(point)) {
                return second;
            }
            if (second.equals(point)) {
                return first;
            }
            throw new IllegalArgumentException("点不在线段上: " + point);
        }
    }

    /** 使用二倍整数坐标作为连接键，避免浮点数比较误差。 */
    private record PointKey(int doubledX, int doubledY) {
        static PointKey from(double x, double y) {
            return new PointKey((int) Math.round(x * 2), (int) Math.round(y * 2));
        }

        BoundaryPoint toBoundaryPoint() {
            return new BoundaryPoint(doubledX / 2.0, doubledY / 2.0);
        }
    }
}
