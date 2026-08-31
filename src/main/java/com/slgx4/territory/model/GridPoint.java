package com.slgx4.territory.model;

public record GridPoint(int x, int y) implements Comparable<GridPoint> {
    @Override
    public int compareTo(GridPoint other) {
        int byX = Integer.compare(x, other.x);
        return byX != 0 ? byX : Integer.compare(y, other.y);
    }
}

