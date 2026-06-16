package com.modcontours.client.model;

public record LineSegment(int x1, int y1, int x2, int y2, Side side) {
    public boolean isHorizontal() {
        return y1 == y2;
    }

    public LineSegment offset(int amount) {
        return switch (side) {
            case TOP -> new LineSegment(x1, y1 - amount, x2, y2 - amount, side);
            case BOTTOM -> new LineSegment(x1, y1 + amount, x2, y2 + amount, side);
            case LEFT -> new LineSegment(x1 - amount, y1, x2 - amount, y2, side);
            case RIGHT -> new LineSegment(x1 + amount, y1, x2 + amount, y2, side);
        };
    }

    public enum Side {
        TOP,
        BOTTOM,
        LEFT,
        RIGHT
    }
}
