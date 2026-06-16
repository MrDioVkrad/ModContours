package com.modcontours.client.render;

import com.modcontours.client.model.Cluster;
import com.modcontours.client.model.LineSegment;
import com.modcontours.client.model.LineSegment.Side;
import com.modcontours.client.model.SlotCell;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds contour outline segments that sit <b>strictly inside</b> cell
 * boundaries.
 *
 * <h3>Corner strategy</h3>
 * <ul>
 *   <li><b>Horizontal edges</b> (TOP / BOTTOM) have priority and cover
 *       the corner pixel.</li>
 *   <li><b>Vertical edges</b> (LEFT / RIGHT) are shortened by
 *       {@code thickness} at any end where the <em>same cell</em> also
 *       has a horizontal edge (outer corner), so that horizontal and
 *       vertical never overlap.</li>
 *   <li>At <b>inner (concave) corners</b> the horizontal segment is
 *       extended by {@code thickness} into the adjacent cluster-cell so
 *       that the two perpendicular edges meet without a diagonal gap.</li>
 * </ul>
 */
public final class ContourGeometry {
    public static final int SLOT_SIZE = 16;
    public static final int SLOT_STEP = 18;
    private static final int SLOT_PADDING = 1;

    private ContourGeometry() {
    }

    /**
     * Builds outline segments for the given cluster.
     *
     * @param cluster   cluster to outline
     * @param thickness line thickness — used to compute corner
     *                  extensions / shortenings so that segments meet
     *                  perfectly
     */
    public static List<LineSegment> outline(Cluster cluster, int thickness) {
        Set<Long> occupied = new HashSet<>();
        for (SlotCell cell : cluster.cells()) {
            occupied.add(ClusterBuilder.key(cell.gridX(), cell.gridY()));
        }

        List<LineSegment> segments = new ArrayList<>();
        for (SlotCell cell : cluster.cells()) {
            int gx = cell.gridX(), gy = cell.gridY();
            int x = cell.x() - SLOT_PADDING;
            int y = cell.y() - SLOT_PADDING;
            int size = SLOT_SIZE + SLOT_PADDING * 2;

            boolean noAbove = !occupied.contains(ClusterBuilder.key(gx, gy - 1));
            boolean noBelow = !occupied.contains(ClusterBuilder.key(gx, gy + 1));
            boolean noLeft  = !occupied.contains(ClusterBuilder.key(gx - 1, gy));
            boolean noRight = !occupied.contains(ClusterBuilder.key(gx + 1, gy));

            // ---- Horizontal segments (priority — they cover corners) ----

            if (noAbove) {
                int extL = innerCorner(noLeft, occupied, gx - 1, gy - 1) ? thickness : 0;
                int extR = innerCorner(noRight, occupied, gx + 1, gy - 1) ? thickness : 0;
                segments.add(new LineSegment(x - extL, y, x + size + extR, y, Side.TOP));
            }

            if (noBelow) {
                int extL = innerCorner(noLeft, occupied, gx - 1, gy + 1) ? thickness : 0;
                int extR = innerCorner(noRight, occupied, gx + 1, gy + 1) ? thickness : 0;
                segments.add(new LineSegment(x - extL, y + size, x + size + extR, y + size, Side.BOTTOM));
            }

            // ---- Vertical segments (shortened at outer corners) ----

            if (noLeft) {
                int shrinkT = noAbove ? thickness : 0;
                int shrinkB = noBelow ? thickness : 0;
                segments.add(new LineSegment(x, y + shrinkT, x, y + size - shrinkB, Side.LEFT));
            }

            if (noRight) {
                int shrinkT = noAbove ? thickness : 0;
                int shrinkB = noBelow ? thickness : 0;
                segments.add(new LineSegment(x + size, y + shrinkT, x + size, y + size - shrinkB, Side.RIGHT));
            }
        }
        return segments;
    }

    /**
     * Returns {@code true} when an <b>inner (concave) corner</b> exists
     * at the given diagonal position — meaning the horizontal segment
     * should extend by {@code thickness} to meet the perpendicular edge.
     * <p>
     * Condition: the perpendicular neighbour IS in the cluster
     * ({@code perpendicularAbsent == false}), and the diagonal cell IS
     * in the cluster.
     */
    private static boolean innerCorner(boolean perpendicularAbsent,
                                       Set<Long> occupied,
                                       int diagX, int diagY) {
        if (perpendicularAbsent) return false; // outer corner — handled by vertical shortening
        return occupied.contains(ClusterBuilder.key(diagX, diagY));
    }

    /* ------------------------------------------------------------------ */
    /*  Hit-testing                                                       */
    /* ------------------------------------------------------------------ */

    /**
     * Side-aware hit testing: checks if the mouse is within the
     * <b>inward-drawn</b> border area (plus a small tolerance).
     */
    public static boolean containsMouse(List<LineSegment> segments, int mouseX, int mouseY,
                                        int thickness, int tolerance) {
        for (LineSegment seg : segments) {
            if (hitTestInward(seg, mouseX, mouseY, thickness, tolerance)) return true;
        }
        return false;
    }

    private static boolean hitTestInward(LineSegment seg, int mx, int my,
                                         int thickness, int tol) {
        int xMin = Math.min(seg.x1(), seg.x2());
        int xMax = Math.max(seg.x1(), seg.x2());
        int yMin = Math.min(seg.y1(), seg.y2());
        int yMax = Math.max(seg.y1(), seg.y2());

        return switch (seg.side()) {
            case TOP -> // drawn downward: y .. y+thickness
                    mx >= xMin - tol && mx <= xMax + tol
                    && my >= seg.y1() - tol && my <= seg.y1() + thickness + tol;
            case BOTTOM -> // drawn upward: y-thickness .. y
                    mx >= xMin - tol && mx <= xMax + tol
                    && my >= seg.y1() - thickness - tol && my <= seg.y1() + tol;
            case LEFT -> // drawn rightward: x .. x+thickness
                    my >= yMin - tol && my <= yMax + tol
                    && mx >= seg.x1() - tol && mx <= seg.x1() + thickness + tol;
            case RIGHT -> // drawn leftward: x-thickness .. x
                    my >= yMin - tol && my <= yMax + tol
                    && mx >= seg.x1() - thickness - tol && mx <= seg.x1() + tol;
        };
    }
}
