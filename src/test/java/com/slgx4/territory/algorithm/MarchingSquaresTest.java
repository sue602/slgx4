package com.slgx4.territory.algorithm;

import com.slgx4.territory.model.Faction;
import com.slgx4.territory.model.GridMap;
import com.slgx4.territory.model.GridPoint;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarchingSquaresTest {
    @Test
    void createsAClosedFourSegmentContourAroundOneCell() {
        GridMap map = new GridMap(3, 3);
        map.setOwner(new GridPoint(1, 1), Faction.BLUE);

        List<LineSegment> segments = MarchingSquares.extract(map, Faction.BLUE);

        assertEquals(4, segments.size());
        assertFalse(segments.stream().anyMatch(segment -> segment.x1() == segment.x2()
                && segment.y1() == segment.y2()));
    }

    @Test
    void emptyTerritoryHasNoContour() {
        GridMap map = new GridMap(4, 4);

        assertEquals(List.of(), MarchingSquares.extract(map, Faction.RED));
    }

    @Test
    void returnsClosedPointsInDirectDrawingOrder() {
        List<List<BoundaryPoint>> contours = MarchingSquares.extractOrderedContours(
                Set.of(new GridPoint(1, 1)));

        assertEquals(1, contours.size());
        List<BoundaryPoint> contour = contours.get(0);
        assertEquals(5, contour.size());
        assertEquals(contour.get(0), contour.get(contour.size() - 1));
        assertEquals(4, contour.subList(0, contour.size() - 1).stream().distinct().count());
        assertConsecutivePointsAreConnected(contour);
    }

    @Test
    void returnsOuterAndInnerContoursForRegionWithHole() {
        Set<GridPoint> ring = Set.of(
                new GridPoint(0, 0), new GridPoint(1, 0), new GridPoint(2, 0),
                new GridPoint(0, 1),                      new GridPoint(2, 1),
                new GridPoint(0, 2), new GridPoint(1, 2), new GridPoint(2, 2));

        List<List<BoundaryPoint>> contours = MarchingSquares.extractOrderedContours(ring);

        assertEquals(2, contours.size());
        assertTrue(contours.stream().allMatch(contour -> contour.get(0)
                .equals(contour.get(contour.size() - 1))));
        contours.forEach(MarchingSquaresTest::assertConsecutivePointsAreConnected);
        assertThrows(UnsupportedOperationException.class,
                () -> contours.get(0).add(new BoundaryPoint(9, 9)));
    }

    private static void assertConsecutivePointsAreConnected(List<BoundaryPoint> contour) {
        for (int index = 1; index < contour.size(); index++) {
            BoundaryPoint previous = contour.get(index - 1);
            BoundaryPoint current = contour.get(index);
            double deltaX = Math.abs(current.x() - previous.x());
            double deltaY = Math.abs(current.y() - previous.y());
            assertEquals(1.0, deltaX + deltaY, 1.0e-9);
        }
    }
}
