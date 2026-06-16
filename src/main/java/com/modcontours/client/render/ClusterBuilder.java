package com.modcontours.client.render;

import com.modcontours.client.model.Cluster;
import com.modcontours.client.model.SlotCell;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public final class ClusterBuilder {
    private ClusterBuilder() {
    }

    public static List<Cluster> build(List<SlotCell> cells) {
        Map<Long, SlotCell> byGrid = new HashMap<>();
        for (SlotCell cell : cells) {
            byGrid.put(key(cell.gridX(), cell.gridY()), cell);
        }

        List<Cluster> clusters = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        for (SlotCell seed : cells) {
            long seedKey = key(seed.gridX(), seed.gridY());
            if (visited.contains(seedKey)) {
                continue;
            }

            Cluster cluster = new Cluster(seed.modId());
            Queue<SlotCell> queue = new ArrayDeque<>();
            queue.add(seed);
            visited.add(seedKey);

            while (!queue.isEmpty()) {
                SlotCell current = queue.remove();
                cluster.cells().add(current);
                addNeighbor(current, 1, 0, byGrid, visited, queue);
                addNeighbor(current, -1, 0, byGrid, visited, queue);
                addNeighbor(current, 0, 1, byGrid, visited, queue);
                addNeighbor(current, 0, -1, byGrid, visited, queue);
            }

            clusters.add(cluster);
        }

        return clusters;
    }

    private static void addNeighbor(
            SlotCell current,
            int dx,
            int dy,
            Map<Long, SlotCell> byGrid,
            Set<Long> visited,
            Queue<SlotCell> queue
    ) {
        SlotCell neighbor = byGrid.get(key(current.gridX() + dx, current.gridY() + dy));
        if (neighbor == null || visited.contains(key(neighbor.gridX(), neighbor.gridY()))) {
            return;
        }
        if (!current.modId().equals(neighbor.modId())) {
            return;
        }

        visited.add(key(neighbor.gridX(), neighbor.gridY()));
        queue.add(neighbor);
    }

    static long key(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }
}
